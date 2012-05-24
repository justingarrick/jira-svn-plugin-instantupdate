package com.atlassian.jira.plugin.ext.subversion;

import com.atlassian.jira.InfrastructureException;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.plugin.ext.subversion.revisions.RevisionIndexService;
import com.atlassian.jira.plugin.ext.subversion.revisions.RevisionIndexer;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.service.ServiceManager;
import com.opensymphony.module.propertyset.PropertySet;
import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * This is a wrapper class for many SubversionManagers.
 * Configured via {@link SvnPropertiesLoader#PROPERTIES_FILE_NAME}
 *
 * @author Dylan Etkin
 * @see {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}
 */
public class MultipleSubversionRepositoryManagerImpl implements MultipleSubversionRepositoryManager
{
    private static Logger log = Logger.getLogger(MultipleSubversionRepositoryManagerImpl.class);

    public static final String APP_PROPERTY_PREFIX = "jira.plugins.subversion";

    public static final String REPO_PROPERTY = "jira.plugins.subversion.repo";

    public static final String LAST_REPO_ID = "last.repo.id";

    public static final long FIRST_REPO_ID = 1;

    private PropertySet pluginProperties;

    private RevisionIndexer revisionIndexer;

    private Map<Long, SubversionManager> managerMap = new HashMap<Long, SubversionManager>();

    private final JiraPropertySetFactory jiraPropertySetFactory;

    private long lastRepoId;

    public MultipleSubversionRepositoryManagerImpl(
            VersionManager versionManager,
            IssueManager issueManager,
            PermissionManager permissionManager,
            ChangeHistoryManager changeHistoryManager,
            JiraPropertySetFactory jiraPropertySetFactory,
            ServiceManager serviceManager,
            IndexPathManager indexPathManager)

    {
        this.jiraPropertySetFactory = jiraPropertySetFactory;

        // Initialize the SVN tools
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();

        managerMap = loadSvnManagers();

        boolean revisionIndexing = false;
        for (SubversionManager mgr : managerMap.values())
        {
            if (mgr.isRevisionIndexing())
            {
                revisionIndexing = true;
                break;
            }
        }

        if (!revisionIndexing) // they might have removed the property - let's check there is no service anyway
        {
            try
            {
                RevisionIndexService.remove(serviceManager);
            }
            catch (Exception e)
            {
                throw new InfrastructureException("Failure removing revision indexing service", e);
            }
        }
        else
        {
            // create revision indexer once we know we have succeed initializing our repositories
            revisionIndexer = new RevisionIndexer(this, versionManager, issueManager, permissionManager, changeHistoryManager, serviceManager, indexPathManager);
        }
    }

    /**
     * Loads a {@link java.util.Map} of {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager} IDs to the {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}.
     * The repositories are loaded from persistent storage. If they couldn't be found there, we will try to look for them in the plugin's configuration file.
     *
     * @return A map of {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager} IDs to the {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}.
     * @throws InfrastructureException Thrown if there's a problem loading repositories configuration from the plugin's configuration file.
     */
    Map<Long, SubversionManager> loadSvnManagers()
    {

        Map<Long, SubversionManager> managers = loadManagersFromJiraProperties();

        if (managers.isEmpty())
        {
            log.info("Could not find any subversion repositories configured, trying to load from " + SvnPropertiesLoader.PROPERTIES_FILE_NAME);
            managers = loadFromProperties();
        }

        return managers;
    }

    /**
     * The Subversion configuration properties are stored in the application properties.  It's not the best place to
     * store collections of information, like multiple repositories, but it will work.  Keys for the properties look
     * like:
     * <p/>
     * <tt>jira.plugins.subversion.&lt;repoId&gt;;&lt;property name&gt;</tt>
     * <p/>
     * Using this scheme we can get all the properties and put them into buckets corresponding to the <tt>repoId</tt>.
     * Then when we have all the properties we can go about building the {@link com.atlassian.jira.plugin.ext.subversion.SubversionProperties}
     * objects and creating our {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}s.
     *
     * @return A {@link java.util.Map} of {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager} IDs to the {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}.
     *         loaded from JIRA's application properties.
     */
    private Map<Long, SubversionManager> loadManagersFromJiraProperties()
    {

        pluginProperties = jiraPropertySetFactory.buildCachingDefaultPropertySet(APP_PROPERTY_PREFIX, true);

        lastRepoId = pluginProperties.getLong(LAST_REPO_ID);

        // create the SubversionManagers
        Map<Long, SubversionManager> managers = new LinkedHashMap<Long, SubversionManager>();
        for (long i = FIRST_REPO_ID; i <= lastRepoId; i++)
        {
            SubversionManager mgr = createManagerFromPropertySet(i, jiraPropertySetFactory.buildCachingPropertySet(REPO_PROPERTY, i, true));
            if (mgr != null)
                managers.put(i, mgr);
        }
        return managers;
    }

    SubversionManager createManagerFromPropertySet(long index, PropertySet properties)
    {
        try
        {
            if (properties.getKeys().isEmpty())
                return null;

            return new SubversionManagerImpl(index, properties);
        }
        catch (IllegalArgumentException e)
        {
            log.error("Error creating SubversionManager " + index + ". Probably was missing a required field (e.g., repository name or root). Skipping it.", e);
            return null;
        }
    }


    /**
     * Loads a {@link java.util.Map} of {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager} IDs to the {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}.
     * The configuration is loaded from the plugin's configuration file.
     *
     * @return A {@link java.util.Map} of {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager} IDs to the {@link com.atlassian.jira.plugin.ext.subversion.SubversionManager}.
     * @throws InfrastructureException Thrown if there's a problem loading repositories configuration from the plugin's configuration file.
     */
    Map<Long, SubversionManager> loadFromProperties()
    {
        Map<Long, SubversionManager> managers = new HashMap<Long, SubversionManager>();

        try
        {
            List<SubversionProperties> propertiesSet = SvnPropertiesLoader.getSVNProperties();
            for (SubversionProperties svnProperties : propertiesSet)
            {
                SubversionManager mgr = createRepository(svnProperties);
                managers.put(mgr.getId(), mgr);
            }
        }
        catch (InfrastructureException ie)
        {
            log.warn("There's a problem adding a subversion manager.", ie);
        }

        return managers;
    }

    public SubversionManager createRepository(SvnProperties properties)
    {
        long repoId;
        synchronized (this)
        {
            repoId = ++lastRepoId;
            pluginProperties.setLong(LAST_REPO_ID, lastRepoId);
        }

        PropertySet set = jiraPropertySetFactory.buildCachingPropertySet(REPO_PROPERTY, repoId, true);
        SubversionManager subversionManager = new SubversionManagerImpl(repoId, SvnProperties.Util.fillPropertySet(properties, set));

        managerMap.put(subversionManager.getId(), subversionManager);
        if (isIndexingRevisions())
        {
            revisionIndexer.addRepository(subversionManager);
        }

        return subversionManager;
    }

    public SubversionManager updateRepository(long repoId, SvnProperties properties)
    {
        SubversionManager subversionManager = getRepository(repoId);
        subversionManager.update(properties);
        return subversionManager;
    }

    public void removeRepository(long repoId)
    {
        SubversionManager original = managerMap.get(repoId);
        if (original == null)
        {
            return;
        }
        try
        {
            managerMap.remove(repoId);

            // Would like to just call remove() but this version doesn't appear to have that, remove all of it's properties instead
            for (String key : new ArrayList<String>(original.getProperties().getKeys()))
                original.getProperties().remove(key);

            if (revisionIndexer != null)
                revisionIndexer.removeEntries(original);
        }
        catch (Exception e)
        {
            throw new InfrastructureException("Could not remove repository index", e);
        }
    }

    public boolean isIndexingRevisions()
    {
        return revisionIndexer != null;
    }

    public RevisionIndexer getRevisionIndexer()
    {
        return revisionIndexer;
    }

    public Collection<SubversionManager> getRepositoryList()
    {
        return managerMap.values();
    }

    public SubversionManager getRepository(long id)
    {
        return managerMap.get(id);
    }

    void startRevisionIndexer()
    {
        getRevisionIndexer().start();
    }

    public void start()
    {
        try
        {
            if (isIndexingRevisions())
            {
                startRevisionIndexer();
            }
        }
        catch (InfrastructureException ie)
        {
            /* Log error, don't throw. Otherwise, we get SVN-234 */
            log.error("Error starting " + getClass(), ie);
        }
    }
}
