package org.sefaria.sefaria.TOCElements;

import android.util.Log;

/**
 * Created by nss on 9/12/16.
 */
public class TOCVersionsAdapterItem {

    private String versionTitle;
    private String versionLang;

    /**
     *
     * @param dbString - string of the form (lang)/(versionTitle)
     */
    public TOCVersionsAdapterItem(String dbString) {
        if (dbString != null) {
            this.versionTitle = dbString.split("/")[1];
            this.versionLang = dbString.split("/")[0];
        } else {
            this.versionTitle = null;
            this.versionLang = null;
        }
    }

    public TOCVersionsAdapterItem(String versionTitle,String versionLang) {
        this.versionTitle = versionTitle;
        this.versionLang = versionLang;
    }

    public String getVersionTitle() {return versionTitle;}
    public String getVersionLang() {return versionLang;}
    public String getPrettyString() {
        if (getVersionLang() != null)
            return getVersionTitle() + " (" + getVersionLang() + ")";
        else
            return getVersionTitle();
    }
    public String getDBString() {
        if (getVersionLang() != null)
            return getVersionLang() + "/" + getVersionTitle();
        else
            return getVersionTitle();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TOCVersionsAdapterItem))
            return false;

        TOCVersionsAdapterItem tvai = (TOCVersionsAdapterItem) o;
        if (this.getDBString() == null || tvai.getDBString() == null)
            return this.getDBString() == null && tvai.getDBString() == null;
        return this.getDBString().equals(tvai.getDBString());
    }
}
