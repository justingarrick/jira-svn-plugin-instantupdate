package com.atlassian.jira.plugin.ext.subversion.revisions;

import junit.framework.TestCase;
import com.atlassian.jira.plugin.ext.subversion.MultipleSubversionRepositoryManager;

public class TestRevisionIndexService extends TestCase
{
    private RevisionIndexService revisionIndexService;

    public void testDoesNotRunIfPluginDisabled()
    {
        revisionIndexService = new RevisionIndexService()
        {
            @Override
            MultipleSubversionRepositoryManager getMultipleSubversionRepositoryManager()
            {
                return null; // To pretend that the plugin is disabled.
            }
        };

        revisionIndexService.run(); // Should exit with no errors.
    }
}
