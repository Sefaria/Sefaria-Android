package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.widget.LinearLayout;

import org.sefaria.sefaria.Util;

/**
 * Created by nss on 9/26/15.
 * Super class for a few similar MenuElements, like MenuButton, MenuButtonTab and MenuSubtitle
 */
public abstract class MenuElement extends LinearLayout {

    public MenuElement(Context context) {
        super(context);
    }

    /**
     *
     * @param lang
     */
    public abstract void setLang(Util.Lang lang);

    public abstract MenuNode getNode();

}
