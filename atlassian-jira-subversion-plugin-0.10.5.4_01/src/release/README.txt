Atlassian JIRA Subversion Plugin
--------------------------------
Version: ${project.version}

What is it?
-----------
This plugin adds a 'Subversion Commits' tab to JIRA issues, containing
svn commit logs associated with the issue.

For example, if your commit message is: "This fixes JRA-52 and JRA-54" -
the commit would be displayed in a tab when viewing JRA-52 and JRA-54.


Quick Install Instructions
--------------------------
1. Copy into JIRA's WEB-INF/lib (removing any existing older versions):
- lib/${project.artifactId}-${project.version}.jar
- lib/trilead-ssh2-build*.jar
- lib/svnkit-*.jar

2. Edit for your installation:
- subversion-jira-plugin.properties

 NOTE: Multiple Subversion repositories are supported. You can specify a default repository (svn.root = {svn root},
etc...) and additional repositories with: svn.root.1=..., svn.root.2=..., and so on. If you do not specify the username,
password, view url, revision indexing, and revision indexing cache size for you additional repository entries then the
values will be taken from your default repository entry (in this way you do not need to copy username and password over
and over if they are the same).

3. Copy into JIRA's WEB-INF/classes
- subversion-jira-plugin.properties

4. If you are upgrading from an older version of the plugin, please delete the Subversion index directory
($jira's_index_dir}/plugins/atlassian-subversion-revisions/). The plugin will recreate it when its service first runs.

5. Restart JIRA - whew, you're done! :)
If you are using JIRA Standalone you can copy the files under the atlassian-jira sub-directory. That is, the jar files into
atlassian-jira/WEB-INF/lib and the .properties file into atlassian-jira/WEB-INF/classes.

If using the WAR distribution, create the directory edit-webapp/WEB-INF/lib and copy the jar files into it. Then copy the
.properties file into edit-webapp/WEB-INF/classes and rebuild and redeploy the JIRA war file.

(note: the first time the service runs it will take a while to index all of your existing issues - be patient)


Scheduling
----------
You can also optionally configure Subversion service's schedule to check for new commits (defaults to 1 hour)

To do this, in JIRA:
- go to the "Administration" tab
- click on the "System" menu item
- click on "Services"
- edit the Subversion Indexing Service
- update the period (in minutes) to whatever you want.


Restarting
----------
If you want to reindex all of your commits or anything goes astray, simply:
- stop the JIRA server
- delete ${jira's_index_dir}/plugins/atlassian-subversion-revisions
- start JIRA


More
----
Detailed installation and usage instructions?

    http://confluence.atlassian.com/display/JIRAEXT

Suggestions, bug reports or feature requests?

    http://jira.atlassian.com/browse/SVN

Support?

    mailto:jira-support@atlassian.com

Enjoy! :)
