package com.atlassian.jira.plugin.ext.subversion;

/**
 * Created by IntelliJ IDEA.
 * User: detkin
 * Date: 27/05/2005
 * Time: 13:11:18
 * To change this template use File | Settings | File Templates.
 */
public class SubversionProperties implements SvnProperties {
	private String root;
	private String displayName;
	private String username;
	private String password;
	private Boolean revisionIndexing;
	private Integer revisioningCacheSize;
	private String privateKeyFile;
	private String changeSetFormat;
    private String webLinkType;
    private String viewFormat;
	private String fileAddedFormat;
	private String fileDeletedFormat;
	private String fileModifiedFormat;
	private String fileReplacedFormat;

    public void fillPropertiesFromOther(SubversionProperties other) {
		if (this.getUsername() == null) {
			this.username = other.getUsername();
		}
		if (this.getPassword() == null) {
			this.password = other.getPassword();
		}
		if (this.getPrivateKeyFile() == null) {
			this.privateKeyFile = other.getPrivateKeyFile();
		}
		if (this.getRevisionIndexing() == null) {
			this.revisionIndexing = other.getRevisionIndexing();
		}
		if (this.getRevisionCacheSize() == null) {
			this.revisioningCacheSize = other.getRevisionCacheSize();
		}

        if (webLinkType == null)
            setWebLinkType(other.getWebLinkType());

        if (changeSetFormat == null)
			setChangeSetFormat(other.getChangesetFormat());

		if (viewFormat == null)
			setViewFormat(other.getViewFormat());

		if (fileAddedFormat == null)
			setFileAddedFormat(other.getFileAddedFormat());

		if (fileDeletedFormat == null)
			setFileDeletedFormat(other.getFileDeletedFormat());

		if (fileModifiedFormat == null)
			setFileModifiedFormat(other.getFileModifiedFormat());

		if (fileReplacedFormat == null)
			setFileReplacedFormat(other.getFileReplacedFormat());
	}

    public String getWebLinkType() {
        return webLinkType;
    }

    public void setWebLinkType(String webLinkType) {
        this.webLinkType = webLinkType;
    }

    public String toString() {
		return "username: " + getUsername() + " password: " + getPassword() + " privateKeyFile: " + getPrivateKeyFile() + " revisioningIndex: " + getRevisionIndexing() + " revisioningCacheSize: " + getRevisionCacheSize();
	}

	public String getRoot() {
		return root;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public Boolean getRevisionIndexing() {
		return revisionIndexing;
	}

	public Integer getRevisionCacheSize() {
		return revisioningCacheSize;
	}

	public String getPrivateKeyFile() {
		return privateKeyFile;
	}

	public String getChangesetFormat() {
		return changeSetFormat;
	}

	public String getViewFormat() {
		return viewFormat;
	}

	public String getFileAddedFormat() {
		return fileAddedFormat;
	}

	public String getFileDeletedFormat() {
		return fileDeletedFormat;
	}

	public String getFileModifiedFormat() {
		return fileModifiedFormat;
	}

	public String getFileReplacedFormat() {
		return fileReplacedFormat;
	}

	public SubversionProperties setRoot(String root) {
		this.root = root;
		return this;
	}

	public SubversionProperties setDisplayName(String displayName) {
		this.displayName = displayName;
		return this;
	}

	public SubversionProperties setUsername(String username) {
		this.username = username;
		return this;
	}

	public SubversionProperties setPassword(String password) {
		this.password = password;
		return this;
	}

	public SubversionProperties setRevisionIndexing(Boolean revisionIndexing) {
		this.revisionIndexing = revisionIndexing;
		return this;
	}

	public SubversionProperties setRevisioningCacheSize(Integer revisioningCacheSize) {
		this.revisioningCacheSize = revisioningCacheSize;
		return this;
	}

	public SubversionProperties setPrivateKeyFile(String privateKeyFile) {
		this.privateKeyFile = privateKeyFile;
		return this;
	}

	public SubversionProperties setChangeSetFormat(String changeSetFormat) {
		this.changeSetFormat = changeSetFormat;
		return this;
	}

	public SubversionProperties setViewFormat(String viewFormat) {
		this.viewFormat = viewFormat;
		return this;
	}

	public SubversionProperties setFileAddedFormat(String fileAddedFormat) {
		this.fileAddedFormat = fileAddedFormat;
		return this;
	}

	public SubversionProperties setFileDeletedFormat(String fileDeletedFormat) {
		this.fileDeletedFormat = fileDeletedFormat;
		return this;
	}

	public SubversionProperties setFileModifiedFormat(String fileModifiedFormat) {
		this.fileModifiedFormat = fileModifiedFormat;
		return this;
	}

	public SubversionProperties setFileReplacedFormat(String fileReplacedFormat) {
		this.fileReplacedFormat = fileReplacedFormat;
		return this;
	}

}
