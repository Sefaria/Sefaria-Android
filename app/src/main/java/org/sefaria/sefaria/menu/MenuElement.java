package org.sefaria.sefaria.menu;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by nss on 9/26/15.
 */
public abstract class MenuElement extends LinearLayout {


    public MenuElement(Context context) {
        super(context);
    }

    public abstract void setLang(int lang);

    public abstract MenuNode getNode();

}
