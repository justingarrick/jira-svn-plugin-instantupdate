/*
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Sep 16, 2004
 * Time: 1:57:17 PM
 */
package com.atlassian.jira.plugin.ext.subversion.issuetabpanels.changes;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.tabpanels.GenericMessageAction;
import com.atlassian.jira.plugin.ext.subversion.MultipleSubversionRepositoryManager;
import com.atlassian.jira.plugin.issuetabpanel.AbstractIssueTabPanel;
import com.atlassian.jira.plugin.issuetabpanel.IssueTabPanelModuleDescriptor;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.EasyList;
import com.atlassian.jira.web.action.issue.ViewIssue;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.opensymphony.user.User;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNLogEntry;
import webwork.action.Action;
import webwork.action.ActionContext;
import webwork.action.factory.ActionFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SubversionRevisionsTabPanel extends AbstractIssueTabPanel
{
    private static Logger log = Logger.getLogger(SubversionRevisionsTabPanel.class);

    private final MultipleSubversionRepositoryManager multipleSubversionRepositoryManager;

    private final PermissionManager permissionManager;

    private final WebResourceManager webResourceManager;

    /**
     * The number of commits to show in the tab initially. 100 should be good enough for most issues.
     */
    public static final int NUMBER_OF_REVISIONS = 100;

    public SubversionRevisionsTabPanel(MultipleSubversionRepositoryManager multipleSubversionRepositoryManager, PermissionManager permissionManager, WebResourceManager webResourceManager)
    {
        this.multipleSubversionRepositoryManager = multipleSubversionRepositoryManager;
        this.permissionManager = permissionManager;
        this.webResourceManager = webResourceManager;
    }

    public List getActions(Issue issue, User remoteUser)
    {
        webResourceManager.requireResource("com.atlassian.jira.plugin.ext.subversion:subversion-resource-js");

        try
        {
            final boolean sortAscending = isSortingActionsInAscendingOrder();
            int pageSize = getPageSizeRequestParameter();

            Map<Long, List<SVNLogEntry>> logEntries = multipleSubversionRepositoryManager.getRevisionIndexer().getLogEntriesByRepository(
                    issue,
                    getPageRequestParameter() * pageSize,
                    pageSize + 1,
                    sortAscending
            );

            if (logEntries == null)
            {
                GenericMessageAction action = new GenericMessageAction(getText("no.index.error.message"));
                return EasyList.build(action);
            }
            else if (logEntries.isEmpty())
            {
                GenericMessageAction action = new GenericMessageAction(getText("no.log.entries.message"));
                return EasyList.build(action);
            }
            else
            {
                List<SubversionRevisionAction> actions = new ArrayList<SubversionRevisionAction>();

                for (Map.Entry<Long, List<SVNLogEntry>> entry : logEntries.entrySet())
                    for (SVNLogEntry logEntry : entry.getValue())
                        actions.add(createSubversionRevisionAction(entry.getKey(), logEntry));

                if (!sortAscending)
                    Collections.reverse(actions);

                /*
                 * Hack! If we have more than a page of actions, that means we should show the 'More' button.
                 */
                if (!actions.isEmpty() && actions.size() > pageSize)
                {
                    /**
                     * ViewIssue will reverse the list of actions if the action sort order is descending, so we
                     * need to sublist based on the order.
                     */
                    actions = sortAscending ? actions.subList(0, pageSize) : actions.subList(1, actions.size());

                    int lastActionIndex = sortAscending ? actions.size() - 1 : 0;
                    SubversionRevisionAction lastAction = actions.get(lastActionIndex);

                    /**
                     * The last action should have specialized class name so that we can use it to tell us when
                     * to render the more button.
                     */
                    actions.set(
                            lastActionIndex,
                            createLastSubversionRevisionActionInPage(
                                    lastAction.getRepoId(),
                                    lastAction.getRevision()
                            )
                    );
                }

                return actions;
            }
        }
        catch (IndexException ie)
        {
            log.error("There's a problem with the Subversion index.", ie);
        }
        catch (IOException ioe)
        {
            log.error("Unable to read Subversion index.", ioe);
        }

        return Collections.emptyList();
    }

    /**
     * Tells us if the current sort order for issue tab panel actions is ascending.
     *
     * @return Returns <tt>true</tt> if the order is ascending; <tt>false</tt> otherwise.
     */
    boolean isSortingActionsInAscendingOrder()
    {
        try
        {
            Action viewIssueAction = ActionFactory.getActionFactory().getActionImpl(ViewIssue.class.getName());
            return !StringUtils.equalsIgnoreCase(((ViewIssue) viewIssueAction).getActionOrder(), "desc");
        }
        catch (Exception e)
        {
            log.error("Unable to figure out how actions are sorted. I'm going to default to ascending", e);
            return true;
        }
    }

    private int getPageRequestParameter()
    {
        HttpServletRequest req = ActionContext.getRequest();

        if (null != req)
        {
            String pageIndexString = req.getParameter("pageIndex");
            return StringUtils.isBlank(pageIndexString) ? 0 : Integer.parseInt(pageIndexString);
        }

        return 0;
    }

    private int getPageSizeRequestParameter()
    {
        HttpServletRequest req = ActionContext.getRequest();

        if (null != req)
        {
            String pageIndexString = req.getParameter("pageSize");
            return StringUtils.isBlank(pageIndexString) ? NUMBER_OF_REVISIONS : Integer.parseInt(pageIndexString);
        }

        return NUMBER_OF_REVISIONS;
    }

    SubversionRevisionAction createSubversionRevisionAction(long repoId, SVNLogEntry logEntry)
    {
        return new SubversionRevisionAction(logEntry, multipleSubversionRepositoryManager, descriptor, repoId);
    }

    SubversionRevisionAction createLastSubversionRevisionActionInPage(long repoId, SVNLogEntry logEntry)
    {
        return new LastSubversionRevisionActionInPage(logEntry, multipleSubversionRepositoryManager, descriptor, repoId);
    }

    String getText(String key)
    {
        return descriptor.getI18nBean().getText(key);
    }

    public boolean showPanel(Issue issue, User remoteUser)
    {
        return multipleSubversionRepositoryManager.isIndexingRevisions() &&
                permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, remoteUser);
    }

    /**
     * A class specifically created for its unique name so that the action view VMs know that
     * the action it is processing is the last one and render a 'More' button.
     */
    private class LastSubversionRevisionActionInPage extends SubversionRevisionAction
    {
        public LastSubversionRevisionActionInPage(SVNLogEntry logEntry, MultipleSubversionRepositoryManager multipleSubversionRepositoryManager, IssueTabPanelModuleDescriptor descriptor, long repoId)
        {
            super(logEntry, multipleSubversionRepositoryManager, descriptor, repoId);
        }
    }
}