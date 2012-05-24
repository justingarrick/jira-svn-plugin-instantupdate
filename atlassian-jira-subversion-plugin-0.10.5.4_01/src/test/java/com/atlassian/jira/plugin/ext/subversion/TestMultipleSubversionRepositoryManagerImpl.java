package com.atlassian.jira.plugin.ext.subversion;

import com.atlassian.jira.InfrastructureException;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.util.IndexPathManager;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.plugin.ext.subversion.revisions.RevisionIndexService;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.service.JiraServiceContainer;
import com.atlassian.jira.service.ServiceManager;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.memory.MemoryPropertySet;
import org.apache.commons.lang.BooleanUtils;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TestMultipleSubversionRepositoryManagerImpl extends MockObjectTestCase
{

    private Mock mockApplicationProperties;

    private ApplicationProperties applicationProperties;

    private Mock mockVersionManager;

    private VersionManager versionManager;

    private Mock mockIssueManager;

    private IssueManager issueManager;

    private Mock mockPermissionManager;

    private PermissionManager permissionManager;

    private ChangeHistoryManager changeHistoryManager;

    private Mock mockChangeHistoryManager;

    private Mock mockJiraPropertySetFactory;

    private JiraPropertySetFactory jiraPropertySetFactory;

    private PropertySet jiraPropertySet;

    private Mock mockServiceManager;

    private ServiceManager serviceManager;

    private Mock mockIndexPathManager;

    private IndexPathManager indexPathManager;

    protected void setUp() throws Exception
    {

        mockApplicationProperties = new Mock(ApplicationProperties.class);
        applicationProperties = (ApplicationProperties) mockApplicationProperties.proxy();

        mockVersionManager = new Mock(VersionManager.class);
        versionManager = (VersionManager) mockVersionManager.proxy();

        mockIssueManager = new Mock(IssueManager.class);
        issueManager = (IssueManager) mockIssueManager.proxy();

        mockPermissionManager = new Mock(PermissionManager.class);
        permissionManager = (PermissionManager) mockPermissionManager.proxy();

        mockChangeHistoryManager = new Mock(ChangeHistoryManager.class);
        changeHistoryManager = (ChangeHistoryManager) mockChangeHistoryManager.proxy();

        mockJiraPropertySetFactory = new Mock(JiraPropertySetFactory.class);
        jiraPropertySetFactory = (JiraPropertySetFactory) mockJiraPropertySetFactory.proxy();

        jiraPropertySet = new MemoryPropertySet();
        jiraPropertySet.init(new HashMap(), new HashMap());

        jiraPropertySet.setLong(MultipleSubversionRepositoryManagerImpl.LAST_REPO_ID, 2);
        jiraPropertySet.setString(MultipleSubversionRepositoryManager.SVN_ROOT_KEY, System.getProperty("svn.root"));

        mockServiceManager = new Mock(ServiceManager.class);
        serviceManager = (ServiceManager) mockServiceManager.proxy();

        mockIndexPathManager = new Mock(IndexPathManager.class);
        indexPathManager = (IndexPathManager) mockIndexPathManager.proxy();
    }

    private MultipleSubversionRepositoryManager getMultipleSubversionRepositoryManager()
    {
        return new MultipleSubversionRepositoryManagerImpl(
                versionManager, issueManager, permissionManager, changeHistoryManager, jiraPropertySetFactory, serviceManager, indexPathManager
        );
    }

    public void testInstantiateMultipleSubversionRepositoryManagerWithNoIndexingRepositoryFromJiraProperties()
    {
        /* Called by MultipleSubversionRepositoryManagerImpl#loadManagersFromJiraProperties */
        mockJiraPropertySetFactory
                .expects(once())
                .method("buildCachingDefaultPropertySet")
                .with(eq(MultipleSubversionRepositoryManagerImpl.APP_PROPERTY_PREFIX), eq(true))
                .will(returnValue(jiraPropertySet));
        /* To return a set of properties to be used in the creation of a SubversionManager. We will be creating two repos */
        mockJiraPropertySetFactory
                .expects(exactly(2))
                .method("buildCachingPropertySet")
                .with(eq(MultipleSubversionRepositoryManagerImpl.REPO_PROPERTY), isA(Long.class), eq(true))
                .will(returnValue(jiraPropertySet));

        /* These methods will be called when there are no indexing repositories */
        mockServiceManager
                .expects(once())
                .method("getServiceWithName")
                .with(eq(RevisionIndexService.REVISION_INDEX_SERVICE_NAME))
                .will(returnValue(new Mock(JiraServiceContainer.class).proxy()));
        mockServiceManager
                .expects(once())
                .method("removeServiceByName")
                .with(eq(RevisionIndexService.REVISION_INDEX_SERVICE_NAME));

        /* No indexing repo */
        jiraPropertySet.setBoolean(MultipleSubversionRepositoryManager.SVN_REVISION_INDEXING_KEY, false);

        MultipleSubversionRepositoryManager multipleSubversionRepositoryManager = getMultipleSubversionRepositoryManager();
        Collection subversionManagers = multipleSubversionRepositoryManager.getRepositoryList();
        SubversionManager svnMgr;
        Iterator svnMgrIter;

        assertEquals(2, subversionManagers.size());

        svnMgrIter = subversionManagers.iterator();
        for (int i = 1; svnMgrIter.hasNext(); ++i)
        {
            svnMgr = (SubversionManager) svnMgrIter.next();
            assertEquals(i, svnMgr.getId());
        }

        assertNull(multipleSubversionRepositoryManager.getRevisionIndexer());
        mockServiceManager.verify();
    }

    public void testInstantiateMultipleSubversionRepositoryManagerWithIndexingRepositoriesFromJiraProperties()
    {
        /* Called by MultipleSubversionRepositoryManagerImpl#loadManagersFromJiraProperties */
        mockJiraPropertySetFactory
                .expects(once())
                .method("buildCachingDefaultPropertySet")
                .with(eq(MultipleSubversionRepositoryManagerImpl.APP_PROPERTY_PREFIX), eq(true))
                .will(returnValue(jiraPropertySet));
        /* To return a set of properties to be used in the creation of a SubversionManager. We will be creating two repos */
        mockJiraPropertySetFactory
                .expects(exactly(2))
                .method("buildCachingPropertySet")
                .with(eq(MultipleSubversionRepositoryManagerImpl.REPO_PROPERTY), isA(Long.class), eq(true))
                .will(returnValue(jiraPropertySet));

        /* An indexing repo */
        jiraPropertySet.setBoolean(MultipleSubversionRepositoryManager.SVN_REVISION_INDEXING_KEY, true);

        MultipleSubversionRepositoryManager multipleSubversionRepositoryManager = getMultipleSubversionRepositoryManager();
        Collection subversionManagers = multipleSubversionRepositoryManager.getRepositoryList();
        SubversionManager svnMgr;
        Iterator svnMgrIter;

        assertEquals(2, subversionManagers.size());

        svnMgrIter = subversionManagers.iterator();
        for (int i = 1; svnMgrIter.hasNext(); ++i)
        {
            svnMgr = (SubversionManager) svnMgrIter.next();
            assertEquals(i, svnMgr.getId());
        }

        assertNotNull(multipleSubversionRepositoryManager.getRevisionIndexer());
    }

    public void testInstantiateMultipleSubversionRepositoryManagerWithIndexingRepositoriesFromProperties()
    {
        /* For multiple repositories from properties file */
        System.setProperty(MultipleSubversionRepositoryManager.SVN_ROOT_KEY + ".1", "file://foo/bar");
        System.setProperty(MultipleSubversionRepositoryManager.SVN_REPOSITORY_NAME + ".1", "Foobar");

        /* Called by MultipleSubversionRepositoryManagerImpl#loadManagersFromJiraProperties */
        mockJiraPropertySetFactory
                .expects(once())
                .method("buildCachingDefaultPropertySet")
                .with(eq(MultipleSubversionRepositoryManagerImpl.APP_PROPERTY_PREFIX), eq(true))
                .will(returnValue(jiraPropertySet));
        /*
         * This will get called in MultipleSubversionRepositoryManagerImpl#createRepository. We will still return jiraPropertySet because
         * svnProperties is basically the same as jiraPropertySet after it is filtered by the jiraPropertySetFactory.
         */
        mockJiraPropertySetFactory
                .expects(exactly(2))
                .method("buildCachingPropertySet")
                .with(eq(MultipleSubversionRepositoryManagerImpl.REPO_PROPERTY), isA(Long.class), eq(true))
                .will(returnValue(jiraPropertySet));

        /* Nothing repo defined in application properties */
        final long initialLastRepoId = 0;

        jiraPropertySet.setLong(MultipleSubversionRepositoryManagerImpl.LAST_REPO_ID, initialLastRepoId);
        /* SVN properties */

        MultipleSubversionRepositoryManager multipleSubversionRepositoryManager = getMultipleSubversionRepositoryManager();
        Collection<SubversionManager> subversionManagers = multipleSubversionRepositoryManager.getRepositoryList();

        assertEquals(2, subversionManagers.size());
        assertNotNull(multipleSubversionRepositoryManager.getRevisionIndexer());
    }

    /**
     * <a href="http://jira.atlassian.com/browse/SVN-234">SVN-234</a>.
     */
    public void testRevisionIndexerBlowUpOnJiraStartupDoesNotMakeJiraInaccesible()
    {
        try
        {
            final StringBuffer revisionIndexerStarted = new StringBuffer();
            mockServiceManager.expects(once()).method("getServiceWithName").with(eq("Subversion Revision Indexing Service")).will(returnValue(null));

            MultipleSubversionRepositoryManagerImpl multipleSubversionRepositoryManager = new MultipleSubversionRepositoryManagerImpl(
                    versionManager, issueManager, permissionManager, changeHistoryManager, jiraPropertySetFactory, serviceManager, indexPathManager)
            {
                @Override
                Map<Long, SubversionManager> loadSvnManagers()
                {
                    return Collections.EMPTY_MAP;
                }

                @Override
                public boolean isIndexingRevisions()
                {
                    return true;
                }

                @Override
                void startRevisionIndexer()
                {
                    revisionIndexerStarted.append(Boolean.TRUE);
                    throw new InfrastructureException("Fake InfrastructureException");
                }
            };

            multipleSubversionRepositoryManager.start();
            assertTrue(BooleanUtils.toBoolean(revisionIndexerStarted.toString()));
        }
        catch (InfrastructureException ie)
        {
            fail("InfrastructureException should be handled, not thrown.");
        }
    }
}
