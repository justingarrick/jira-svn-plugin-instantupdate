package com.atlassian.jira.plugin.ext.soap;

import java.io.IOException;

import org.apache.log4j.Logger;

import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.plugin.ext.subversion.MultipleSubversionRepositoryManager;
import com.atlassian.jira.plugin.ext.subversion.revisions.RevisionIndexer;

/**
 * An implementation of ISubversionSoapService.
 * 
 * @author Justin Garrick
 */
public class SubversionSoapServiceImpl {
	
	/** The logging "aspect". */
	private static Logger log = Logger.getLogger(SubversionSoapServiceImpl.class);

	/** The repository manager. */
	private final MultipleSubversionRepositoryManager manager;

	/** The revision indexer, derived from the manager. */
	private final RevisionIndexer indexer;

	/**
	 * Constructor.
	 * @param manager an instance of a multiple repo manager
	 */
	public SubversionSoapServiceImpl(final MultipleSubversionRepositoryManager manager) {
		this.manager = manager;
		this.indexer = this.manager.getRevisionIndexer();
	}

	/**
	 * {@inheritDoc}
	 */
	public void reindexRepository(final String repositoryUrl) {
		try {
			indexer.updateIndexFor(repositoryUrl);
		} catch (IndexException e) {
			log.warn("Justin - error updating repository " + repositoryUrl + ": " + e.getMessage());
		} catch (IOException e) {
			log.warn("Justin - error updating repository " + repositoryUrl + ": " + e.getMessage());
		}
	}
}
