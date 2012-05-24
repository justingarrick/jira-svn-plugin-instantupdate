/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Oct 1, 2004
 * Time: 5:06:44 PM
 */
package com.atlassian.jira.plugin.ext.subversion.revisions;

import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.plugin.ext.subversion.MultipleSubversionRepositoryManager;
import com.atlassian.jira.service.AbstractService;
import com.atlassian.jira.service.ServiceManager;

public class RevisionIndexService extends AbstractService
{
    public static final String REVISION_INDEX_SERVICE_NAME = "Subversion Revision Indexing Service";
    public static final long REVISION_INDEX_SERVICE_DELAY = 60 * 60 * 1000L;

    public void run()
    {
        try
        {
            MultipleSubversionRepositoryManager multipleSubversionRepositoryManager = getMultipleSubversionRepositoryManager();

            if (null == multipleSubversionRepositoryManager)
                return; // Just return --- the plugin is disabled. Don't log anything.

            if (multipleSubversionRepositoryManager.getRevisionIndexer() != null)
            {
                multipleSubversionRepositoryManager.getRevisionIndexer().updateIndex();
            }
            else
            {
                log.warn("Tried to index changes but SubversionManager has no revision indexer?");
            }
        }
        catch (Throwable t)
        {
            log.error("Error indexing changes: " + t, t);
        }
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException
    {
        return getObjectConfiguration("SVNREVISIONSERVICE", "services/plugins/subversion/revisionindexservice.xml", null);
    }

    public static void install(ServiceManager serviceManager) throws Exception
    {
        if (serviceManager.getServiceWithName(REVISION_INDEX_SERVICE_NAME) == null)
        {
            serviceManager.addService(REVISION_INDEX_SERVICE_NAME, RevisionIndexService.class.getName(), REVISION_INDEX_SERVICE_DELAY);
        }
    }

    public static void remove(ServiceManager serviceManager) throws Exception
    {
        if (serviceManager.getServiceWithName(REVISION_INDEX_SERVICE_NAME) != null)
        {
            serviceManager.removeServiceByName(REVISION_INDEX_SERVICE_NAME);
        }
    }

    MultipleSubversionRepositoryManager getMultipleSubversionRepositoryManager()
    {
        return (MultipleSubversionRepositoryManager) ComponentManager.getInstance().getContainer().getComponentInstance(MultipleSubversionRepositoryManager.class);
    }

    public boolean isUnique()
    {
        return true;
    }

    public boolean isInternal()
    {
        return true;
    }

    public String getDescription()
    {
        return "This service indexes Subversion revisions.";
    }
}