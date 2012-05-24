package com.atlassian.jira.plugin.ext.subversion.action;

import com.atlassian.jira.plugin.ext.subversion.MultipleSubversionRepositoryManager;
import com.atlassian.jira.plugin.ext.subversion.SubversionManager;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Manage 1 or more repositories
 */
public class ViewSubversionRepositoriesAction extends SubversionActionSupport
{

    public ViewSubversionRepositoriesAction(MultipleSubversionRepositoryManager manager)
    {
        super (manager);
    }

    public Collection<SubversionManager> getRepositories()
    {
        List<SubversionManager> subversionManagers = new ArrayList<SubversionManager>(getMultipleRepoManager().getRepositoryList());

        Collections.sort(
                subversionManagers,
                new Comparator<SubversionManager>()
                {
                    public int compare(SubversionManager left, SubversionManager right)
                    {
                        return StringUtils.defaultString(left.getDisplayName()).compareTo(
                                StringUtils.defaultString(right.getDisplayName())
                        );
                    }
                }
        );

        return subversionManagers;
    }
}
