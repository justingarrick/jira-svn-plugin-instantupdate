package com.atlassian.jira.plugin.ext.soap;

/**
 * Interface for a simple SOAP service
 * that allows the user to reindex
 * a specific JIRA repository on-demand.
 * 
 * @author Justin Garrick
 */
public interface ISubversionSoapService {
	
	/**
	 * Forces the JIRA SVN Plugin to reindex the
	 * specified repository
	 * 
	 * @param repositoryUrl the url of the repository to reindex
	 */
	void reindexRepository(final String repositoryUrl);
}
