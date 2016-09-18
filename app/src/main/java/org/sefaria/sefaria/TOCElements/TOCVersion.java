package org.sefaria.sefaria.TOCElements;

/**
 * Created by nss on 9/12/16.
 */
public class TOCVersion {

    public static final String DEFAULT_TEXT_VERSION = "Default Version";

    private String versionTitle;
    private String versionLang;
    private boolean isDefaultVersion;

    /**
     *
     * @param dbString - string of the form (lang)/(versionTitle)
     */
    public TOCVersion(String dbString) {
        if (dbString != null && !dbString.equals(DEFAULT_TEXT_VERSION)) {
            this.versionTitle = dbString.split("/")[1];
            this.versionLang = dbString.split("/")[0];
            this.isDefaultVersion = false;
        } else {
            this.versionTitle = null;
            this.versionLang = null;
            this.isDefaultVersion = true;
        }
    }

    public TOCVersion() {
        this.versionTitle = null;
        this.versionLang = null;
        this.isDefaultVersion = true;
    }

    public TOCVersion(String versionTitle, String versionLang) {
        if (versionTitle.equals(DEFAULT_TEXT_VERSION)) {
            this.versionLang = null;
            this.versionTitle = null;
            this.isDefaultVersion = true;
        } else {
            this.versionTitle = versionTitle;
            this.versionLang = versionLang;
            this.isDefaultVersion = false;
        }

    }

    public String getVersionTitle() {return versionTitle;}
    public String getVersionLang() {return versionLang;}
    public boolean isDefaultVersion() { return this.isDefaultVersion; }
    public String getPrettyString() {
        if (isDefaultVersion)
            return DEFAULT_TEXT_VERSION;

        if (getVersionLang() != null)
            return getVersionTitle() + " (" + getVersionLang() + ")";
        else
            return getVersionTitle();
    }
    public String getDBString() {
        if (isDefaultVersion)
            return DEFAULT_TEXT_VERSION;

        if (getVersionLang() != null)
            return getVersionLang() + "/" + getVersionTitle();
        else
            return getVersionTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TOCVersion))
            return false;

        TOCVersion tvai = (TOCVersion) o;
        if (this.getDBString() == null || tvai.getDBString() == null)
            return this.getDBString() == null && tvai.getDBString() == null;
        return this.getDBString().equals(tvai.getDBString());
    }
}
