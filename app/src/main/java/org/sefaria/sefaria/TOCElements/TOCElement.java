package org.sefaria.sefaria.TOCElements;

/**
 * Created by LenJ on 12/21/2015.
 */

import android.content.Context;
import android.widget.LinearLayout;

import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.menu.MenuNode;


import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.Util;

/**
 *  * Super class for a few similar
 */
public interface TOCElement {

    /**
     *
     * @param lang
     */
    public abstract void setLang(Util.Lang lang);

    //public abstract MenuNode getNode();

}
