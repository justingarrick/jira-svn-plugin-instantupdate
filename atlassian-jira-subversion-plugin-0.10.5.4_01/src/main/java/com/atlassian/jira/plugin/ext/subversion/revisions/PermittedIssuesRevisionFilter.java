package com.atlassian.jira.plugin.ext.subversion.revisions;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.PermissionManager;
import com.opensymphony.user.User;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;

import java.io.IOException;
import java.util.BitSet;
import java.util.Set;

public class PermittedIssuesRevisionFilter extends AbstractRevisionFilter
{
    private final Set<String> permittedIssueKeys;

    public PermittedIssuesRevisionFilter(IssueManager issueManager, PermissionManager permissionManager, User user, Set<String> permittedIssueKeys)
    {
        super(issueManager, permissionManager, user);
        this.permittedIssueKeys = permittedIssueKeys;
    }

    public BitSet bits(IndexReader indexReader) throws IOException
    {
        BitSet bitSet = new BitSet(indexReader.maxDoc());

        for (String issueKey : permittedIssueKeys)
        {
            TermDocs termDocs = indexReader.termDocs(new Term(RevisionIndexer.FIELD_ISSUEKEY, issueKey));

            while (termDocs.next())
                bitSet.set(termDocs.doc(), true);
        }

        return bitSet;
    }
}
