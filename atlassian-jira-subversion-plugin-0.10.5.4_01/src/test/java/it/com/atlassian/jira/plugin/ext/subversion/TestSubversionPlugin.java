package it.com.atlassian.jira.plugin.ext.subversion;

import com.atlassian.jira.functest.framework.locator.XPathLocator;
import com.atlassian.jira.webtests.JIRAWebTest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: detkin
 * Date: May 8, 2006
 * Time: 4:24:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestSubversionPlugin extends JIRAWebTest
{
    private static final String SUBVERSION_REVISION_INDEXING_SERVICE_NAME = "Subversion Revision Indexing Service";
    
    private String svnRepositoryUrl;

    private SVNRepository repository = null;

    private Properties testSubversionConfiguration;

    private void setupSubversionTestConfiguration() {
        InputStream in = null;

        try {
            testSubversionConfiguration = new Properties();

            in = getClass().getClassLoader().getResourceAsStream("subversion-jira-plugin.properties");
            assertNotNull(in);

            testSubversionConfiguration.load(in);

        } catch (final IOException ioe) {
            fail("Unable to get test SVN URL: " + ioe.getMessage());
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public void setUp()
    {
        super.setUp();
        setupSubversionTestConfiguration();
        
        try
        {
            svnRepositoryUrl = "file://" + testSubversionConfiguration.getProperty("svn.test.root.path");
            assertNotNull(svnRepositoryUrl);

            /* Just in case stuff is left over */
            removeSvnRepository();

            DAVRepositoryFactory.setup();
            SVNRepositoryFactoryImpl.setup();
            FSRepositoryFactory.setup();
            repository = SVNRepositoryFactory.create(SVNRepositoryFactory.createLocalRepository(
                    new File(new URI(svnRepositoryUrl)),
                    true, false));
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager("dchui", "changeit");
            repository.setAuthenticationManager(authManager);


        }
        catch (SVNException e)
        {
            throw new RuntimeException("SVN error", e);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException("Unable to remove SVN repository previously created by integeration tests at: " + svnRepositoryUrl, ioe);
        }
        catch (URISyntaxException use)
        {
            throw new RuntimeException("Property svn.root not setup to point to a local repository for tests.", use);
        }

        restoreData("TestSubversionPlugin.xml");
        activateSubversionRepository();
    }

    private void activateSubversionRepository()
    {
        gotoPage("/secure/ActivateSubversionRepository.jspa?repoId=1");
    }

    protected void removeSvnRepository() throws IOException, URISyntaxException {
        final File unzipDestination = new File(new URI(svnRepositoryUrl));

        if (unzipDestination.exists())
            FileUtils.deleteDirectory(unzipDestination);
    }

    public void tearDown()
    {
        deleteSubversionRepository();

        try
        {
            // https://studio.plugins.atlassian.com/browse/SVN-258
            // Library upgrade broke some test. By removing the SVN Directory would be sufficient for the next test to execute.
            removeSvnRepository();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Generic IO error", e);
        }
        catch (URISyntaxException use)
        {
            throw new RuntimeException("Invalid svn.root specified in jira-subversion-plugin.properties.", use);
        }
        finally
        {
            super.tearDown();
        }
    }

    private void deleteSubversionRepository()
    {
        gotoPage("/secure/DeleteSubversionRepository!default.jspa?repoId=1");
        setWorkingForm("jiraform");
        submit("delete");
    }

    public TestSubversionPlugin(String string)
    {
        super(string);
    }

    /**
     * Anti test for <a href="http://jira.atlassian.com/browse/SVN-93">SVN-93</a>
     *
     * @throws SVNException
     * Thrown if the is a problem performing SVN operations.
     * @throws InterruptedException
     * Thrown if the wait for reindexing throws it.
     */
    public void testCommitWithJiraIssueKeysInLowerCase() throws SVNException, InterruptedException
    {
        addProject("Another Homo", "AHSP", null, "admin", null);

        createIssueStep1("Another Homo", "Bug");
        setFormElement("summary", "A new issue");

        submit("Create"); /* Create another issue which will be mentioned in the commit message */


        createLogEntry("hsp-1, ahsp-1 A commit message with a valid JIRA issue key but in lower case form.");
        waitForReindex();

        /* Check at HSP-1 */
        gotoIssue("HSP-1");
        assertLinkPresentWithText("Subversion Commits");
        clickLinkWithText("Subversion Commits");

        assertTextPresent(
                "<a href=\"" + getEnvironmentData().getContext() + "/browse/HSP-1\" title=\"This is a test bug for the subversion plugin\">HSP-1</a>"
                        + ", <a href=\"" + getEnvironmentData().getContext() + "/browse/AHSP-1\" title=\"A new issue\">AHSP-1</a> "
                        + "A commit message with a valid JIRA issue key but in lower case form.");
        assertLinkPresentWithText("AHSP-1");

        /* Check at AHSP-1, and the subversion issue tab panel should already be selected. */
        gotoIssue("AHSP-1");

        assertTextPresent(
                "<a href=\"" + getEnvironmentData().getContext() + "/browse/HSP-1\" title=\"This is a test bug for the subversion plugin\">HSP-1</a>"
                        + ", <a href=\"" + getEnvironmentData().getContext() + "/browse/AHSP-1\" title=\"A new issue\">AHSP-1</a> "
                        + "A commit message with a valid JIRA issue key but in lower case form.");
        assertLinkPresentWithText("HSP-1");

        /* Check if the issues are rendered properly in the Subversion project tab panel */
        gotoPage("/browse/HSP?selectedTab=com.atlassian.jira.plugin.ext.subversion:subversion-project-tab");
        assertTextPresent(
                "<a href=\"" + getEnvironmentData().getContext() + "/browse/HSP-1\" title=\"This is a test bug for the subversion plugin\">HSP-1</a>"
                        + ", <a href=\"" + getEnvironmentData().getContext() + "/browse/AHSP-1\" title=\"A new issue\">AHSP-1</a> "
                        + "A commit message with a valid JIRA issue key but in lower case form.");

        gotoPage("/browse/AHSP?selectedTab=com.atlassian.jira.plugin.ext.subversion:subversion-project-tab");
        assertTextPresent(
                "<a href=\"" + getEnvironmentData().getContext() + "/browse/HSP-1\" title=\"This is a test bug for the subversion plugin\">HSP-1</a>"
                        + ", <a href=\"" + getEnvironmentData().getContext() + "/browse/AHSP-1\" title=\"A new issue\">AHSP-1</a> "
                        + "A commit message with a valid JIRA issue key but in lower case form.");
    }
    
    public void testIssueTabPanelForCommitMessages() throws SVNException, InterruptedException
    {
        gotoIssue("HSP-1");

        // Browse and make sure nothing exists on the tab

        // Assert the Subversion tab exists
        assertLinkPresentWithText("Subversion Commits");

        clickLinkWithText("Subversion Commits");

        // Assert that no log entries exist
        //assertTextPresent("There are no subversion log entries for this issue yet.");

        // Connect to SVN and create a log entrywait
        createLogEntry("HSP-1 adding a svn log message");

        createModifyLogEntry("HSP-1 a modify svn log message");

        // NOTE: this is trying to create a log entry that will be marked as replace but the api is a bit unclear
        // as to how to do this, it does not work at the moment.
        //createReplaceLogEntry("HSP-1 a modify svn log message");

        createRemoveLogEntry("HSP-1 a remove svn log message");

        // Trigger a re-index in the plugin
        // wait for the system to re-index
        waitForReindex();

        // Borwse and verify the commit log exists
        gotoIssue("HSP-1");

        // Assert the Subversion tab exists
        assertTextPresent("Subversion Commits");

        // Assert that an add log entry exists
        assertTextPresent("adding a svn log message");
        assertTextPresent("ADD");
        // Make sure that the two add viewcvs links are present and in the correct format
        assertTextPresent("http://svn.atlassian.com/fisheye/changelog/public?cs=1"); /* Changeset  */
        assertTextPresent("http://svn.atlassian.com/fisheye/viewrep/public/test/file.txt?r=1"); /* First file */
        assertTextPresent("http://svn.atlassian.com/fisheye/viewrep/public/test?r=1"); /* Second file */

        // Assert that a modify log entry exists
        assertTextPresent("modify svn log message");
        assertTextPresent("MODIFY");
        // Make sure that the modify viewcvs link exists
        assertTextPresent("http://svn.atlassian.com/fisheye/viewrep/public/test/file.txt#2");

        // Assert that a delete log entry exists
        assertTextPresent("remove svn log message");
        assertTextPresent("DEL");
        // Make sure that the del viewcvs link exists
        assertTextPresent("http://svn.atlassian.com/fisheye/viewrep/public/test");
    }

    public void testIssueTabPanelWithSimilarProjectKeysInCommitMessages() throws SVNException, InterruptedException
    {
        createCommitsWithSimilarProjectKeys();

        gotoPage("/browse/HSP-1?page=com.atlassian.jira.plugin.ext.subversion:subversion-commits-tabpanel");

        assertTextPresent("a modify svn log message");
        assertTextNotPresent("AHSP");
        assertTextNotPresent("This is an entry with a URL");

        gotoPage("/browse/AHSP-1?page=com.atlassian.jira.plugin.ext.subversion:subversion-commits-tabpanel");
        assertTextNotPresent("a modify svn log message");
        assertTextNotPresent("This is an entry with a URL");
        assertTextPresent("This is a commit for");

        createRemoveLogEntry("Removed.");
    }

    public void testProjectTabPanelWithSimilarProjectKeysInCommitMessages() throws SVNException, InterruptedException {
        createCommitsWithSimilarProjectKeys();

        gotoPage("/browse/HSP?selectedTab=com.atlassian.jira.plugin.ext.subversion:subversion-project-tab");
        assertTextPresent("a modify svn log message");
        assertTextNotPresent("AHSP");
        assertTextNotPresent("This is an entry with a URL");

        gotoPage("/browse/AHSP?page=com.atlassian.jira.plugin.ext.subversion:subversion-commits-tabpanel");
        assertTextNotPresent("a modify svn log message");
        assertTextNotPresent("This is an entry with a URL");
        assertTextPresent("This is a commit for");

        createRemoveLogEntry("Removed.");
    }

    public void testFilterCommitMessagesInProjectTabPanelByVersion() throws SVNException, InterruptedException {
    	final String versionId2;
    	final String versionId1;

		addProject("Test Project", "TST", null, "admin", null);
        addVersion("Test Project", "1.0", null);
        addVersion("Test Project", "2.0", null);

		addIssue("Test Project", "TST", ISSUE_TYPE_BUG, "Summary Of Bug 1", null, null,
				new String[] { "1.0" }, null, null, null, null, null, null, null);
		addIssue("Test Project", "TST", ISSUE_TYPE_BUG, "Summary Of Bug 2", null, null,
				new String[] { "2.0" }, null, null, null, null, null, null, null);

        createLogEntry("This is a commit for TST-1.");
        createModifyLogEntry("This is a commit for TST-2.");

        waitForReindex();

        gotoPage("/browse/TST?selectedTab=com.atlassian.jira.plugin.ext.subversion:subversion-project-tab");
        versionId1 = new XPathLocator(tester, "//select[@name='selectedVersion']//option[text()='1.0']/@value").getText();
        versionId2 = new XPathLocator(tester, "//select[@name='selectedVersion']//option[text()='2.0']/@value").getText();

        assertTextPresent("This is a commit for <a href=\"" + getEnvironmentData().getContext() + "/browse/TST-1\" title=\"Summary Of Bug 1\">TST-1</a>");
        assertTextPresent("This is a commit for <a href=\"" + getEnvironmentData().getContext() + "/browse/TST-2\" title=\"Summary Of Bug 2\">TST-2</a>");

        gotoPage("/browse/TST?selectedTab=com.atlassian.jira.plugin.ext.subversion:subversion-project-tab&selectedVersion=" + versionId1);
        assertTextPresent("This is a commit for <a href=\"" + getEnvironmentData().getContext() + "/browse/TST-1\" title=\"Summary Of Bug 1\">TST-1</a>");
        assertTextNotPresent("This is a commit for <a href=\"" + getEnvironmentData().getContext() + "/browse/TST-2\" title=\"Summary Of Bug 2\">TST-2</a>");

        gotoPage("/browse/TST?selectedTab=com.atlassian.jira.plugin.ext.subversion:subversion-project-tab&selectedVersion=" + versionId2);
        assertTextNotPresent("This is a commit for <a href=\"" + getEnvironmentData().getContext() + "/browse/TST-1\" title=\"Summary Of Bug 1\">TST-1</a>");
        assertTextPresent("This is a commit for <a href=\"" + getEnvironmentData().getContext() + "/browse/TST-2\" title=\"Summary Of Bug 2\">TST-2</a>");
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/SVN-121">SVN-121</a>
     */
    public void testRestoreDataWithSubversionIndexServiceDoesNotCreateDuplicatesInJira()
    {
        gotoPage("/secure/admin/jira/ViewServices!default.jspa");

        String pageSource = getDialog().getResponseText();
        assertEquals(
                pageSource.indexOf(SUBVERSION_REVISION_INDEXING_SERVICE_NAME),
                pageSource.lastIndexOf(SUBVERSION_REVISION_INDEXING_SERVICE_NAME)
        );
    }

	protected void createCommitsWithSimilarProjectKeys() throws SVNException,
			InterruptedException {
        addProject("Another Homo", "AHSP", null, "admin", null);

        createIssueStep1("Another Homo", "Bug");
        setFormElement("summary", "A new issue");

        submit("Create");


        createLogEntry("This is a commit for AHSP-1.");
        createModifyLogEntry("HSP-1 a modify svn log message");
        createModifyLogEntry("This is an entry with a URL http://jira.atlassian.com/browse/HSP-1");

        waitForReindex();
	}

    private void createRemoveLogEntry(String message) throws SVNException
    {
        ISVNEditor editor = repository.getCommitEditor(message, null);

        deleteDir(editor, "test");
    }

    private void createModifyLogEntry(String message) throws SVNException
    {
        ISVNEditor editor = repository.getCommitEditor(message, null);

        byte[] modifiedContents = "This is the same file but modified a little.".getBytes();

        modifyFile(editor, "test", "test/file.txt", modifiedContents);
    }


    private void waitForReindex() throws InterruptedException
    {
        Thread.sleep(150000);
    }

    private void createLogEntry(String message) throws SVNException
    {

        ISVNEditor editor = repository.getCommitEditor(message, null);

        /*
        * Add a directory and a file within that directory.
        *
        * SVNCommitInfo object contains basic information on the committed revision, i.e.
        * revision number, author name, commit date and commit message.
        */

        /*
        * Sample file contents.
        */
        byte[] contents = "This is a new file".getBytes();


        SVNCommitInfo commitInfo = addDir(editor, "test", "test/file.txt", contents);
    }

    /*
    * This method performs commiting an addition of a  directory  containing  a
    * file.
    */
    private SVNCommitInfo addDir(ISVNEditor editor, String dirPath,
                                 String filePath, byte[] data) throws SVNException
    {
        /*
         * Always called first. Opens the current root directory. It  means  all
         * modifications will be applied to this directory until  a  next  entry
         * (located inside the root) is opened/added.
         *
         * -1 - revision is HEAD, of course (actually, for a comit  editor  this
         * number is irrelevant)
         */
        editor.openRoot(-1);
        /*
         * Adds a new directory to the currently opened directory (in this  case
         * - to the  root  directory  for which the SVNRepository was  created).
         * Since this moment all changes will be applied to this new  directory.
         *
         * dirPath is relative to the root directory.
         *
         * copyFromPath (the 2nd parameter) is set to null and  copyFromRevision
         * (the 3rd) parameter is set to  -1  since  the  directory is not added
         * with history (is not copied, in other words).
         */
        if (dirPath != null)
        {
            editor.addDir(dirPath, null, -1);
        }

        /*
         * Adds a new file to the just added  directory. The  file  path is also
         * defined as relative to the root directory.
         *
         * copyFromPath (the 2nd parameter) is set to null and  copyFromRevision
         * (the 3rd parameter) is set to -1 since  the file is  not  added  with
         * history.
         */
        editor.addFile(filePath, null, -1);
        /*
         * The next steps are directed to applying delta to the  file  (that  is
         * the full contents of the file in this case).
         */
        editor.applyTextDelta(filePath, null);
        /*
         * Use delta generator utility class to generate and send delta
         *
         * Note that you may use only 'target' data to generate delta when there is no
         * access to the 'base' (previous) version of the file. However, using 'base'
         * data will result in smaller network overhead.
         *
         * SVNDeltaGenerator will call editor.textDeltaChunk(...) method for each generated
         * "diff window" and then editor.textDeltaEnd(...) in the end of delta transmission.
         * Number of diff windows depends on the file size.
         *
         */
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
        String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(data), editor, true);

        /*
        * Closes the new added file.
        */
        editor.closeFile(filePath, checksum);
        if (dirPath != null)
        {
            /*
            * Closes the new added directory.
            */
            editor.closeDir();
        }

        /*
        * Closes the root directory.
        */
        editor.closeDir();
        /*
        * This is the final point in all editor handling. Only now all that new
        * information previously described with the editor's methods is sent to
        * the server for committing. As a result the server sends the new
        * commit information.
        */
        return editor.closeEdit();
    }

    /*
    * This method performs committing file modifications.
    */
    private SVNCommitInfo modifyFile(ISVNEditor editor, String dirPath,
                                     String filePath, byte[] newData) throws SVNException
    {
        /*
         * Always called first. Opens the current root directory. It  means  all
         * modifications will be applied to this directory until  a  next  entry
         * (located inside the root) is opened/added.
         *
         * -1 - revision is HEAD
         */
        editor.openRoot(-1);
        /*
         * Opens a next subdirectory (in this example program it's the directory
         * added  in  the  last  commit).  Since this moment all changes will be
         * applied to this directory.
         *
         * dirPath is relative to the root directory.
         * -1 - revision is HEAD
         */
        editor.openDir(dirPath, -1);
        /*
         * Opens the file added in the previous commit.
         *
         * filePath is also defined as a relative path to the root directory.
         */
        editor.openFile(filePath, -1);

        /*
        * The next steps are directed to applying and writing the file delta.
        */
        editor.applyTextDelta(filePath, null);

        /*
        * Use delta generator utility class to generate and send delta
        *
        * Note that you may use only 'target' data to generate delta when there is no
        * access to the 'base' (previous) version of the file. However, using 'base'
        * data will result in smaller network overhead.
        *
        * SVNDeltaGenerator will call editor.textDeltaChunk(...) method for each generated
        * "diff window" and then editor.textDeltaEnd(...) in the end of delta transmission.
        * Number of diff windows depends on the file size.
        *
        */
        SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
        String checksum = deltaGenerator.sendDelta(filePath, new ByteArrayInputStream(newData), editor, true);

        /*
        * Closes the file.
        */
        editor.closeFile(filePath, checksum);

        /*
        * Closes the directory.
        */
        editor.closeDir();

        /*
        * Closes the root directory.
        */
        editor.closeDir();

        /*
        * This is the final point in all editor handling. Only now all that new
        * information previously described with the editor's methods is sent to
        * the server for committing. As a result the server sends the new
        * commit information.
        */
        return editor.closeEdit();
    }

    /*
    * This method performs committing a deletion of a directory.
    */
    private SVNCommitInfo deleteDir(ISVNEditor editor, String dirPath) throws SVNException
    {
        /*
         * Always called first. Opens the current root directory. It  means  all
         * modifications will be applied to this directory until  a  next  entry
         * (located inside the root) is opened/added.
         *
         * -1 - revision is HEAD
         */
        editor.openRoot(-1);
        /*
         * Deletes the subdirectory with all its contents.
         *
         * dirPath is relative to the root directory.
         */
        editor.deleteEntry(dirPath, -1);

        /*
        * Closes the root directory.
        */
        editor.closeDir();
        /*
        * This is the final point in all editor handling. Only now all that new
        * information previously described with the editor's methods is sent to
        * the server for committing. As a result the server sends the new
        * commit information.
        */

        return editor.closeEdit();
    }

//    /*
//    * This method performs committing file modifications.
//    */
//    private SVNCommitInfo replaceFile(ISVNEditor editor, String filePath, byte[] newData) throws SVNException {
//        deleteDir(editor, filePath);
//        addDir(editor, null, filePath, newData, false);
//        editor.closeDir();
//        return editor.closeEdit();
//    }

//    /*
//    * This  method  performs how a directory in the repository can be copied to
//    * branch.
//    */
//    private SVNCommitInfo copyDir(ISVNEditor editor, String srcDirPath,
//                                         String dstDirPath, long revision) throws SVNException {
//        /*
//         * Always called first. Opens the current root directory. It  means  all
//         * modifications will be applied to this directory until  a  next  entry
//         * (located inside the root) is opened/added.
//         *
//         * -1 - revision is HEAD
//         */
//        editor.openRoot(-1);
//
//        /*
//        * Adds a new directory that is a copy of the existing one.
//        *
//        * srcDirPath   -  the  source  directory  path (relative  to  the  root
//        * directory).
//        *
//        * dstDirPath - the destination directory path where the source will be
//        * copied to (relative to the root directory).
//        *
//        * revision    - the number of the source directory revision.
//        */
//        editor.addDir(dstDirPath, srcDirPath, revision);
//        /*
//         * Closes the just added copy of the directory.
//         */
//        editor.closeDir();
//        /*
//         * Closes the root directory.
//         */
//        editor.closeDir();
//        /*
//         * This is the final point in all editor handling. Only now all that new
//         * information previously described with the editor's methods is sent to
//         * the server for committing. As a result the server sends the new
//         * commit information.
//         */
//        return editor.closeEdit();
//    }
//
//
//    private void createReplaceLogEntry(String message) throws SVNException
//    {
//        ISVNEditor editor = repository.getCommitEditor(message, null);
//        byte[] contents = "This is a new file".getBytes();
//        replaceFile(editor, "test/file.txt", contents);
//    }

}
