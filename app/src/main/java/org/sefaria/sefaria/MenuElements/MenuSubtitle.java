package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.SefariaTextView;

/**
 * Created by nss on 9/24/15.
 */
public class MenuSubtitle extends MenuElement {

    private SefariaTextView tv;
    private MenuNode node;

    public MenuSubtitle(Context context, MenuNode node, Util.Lang lang, boolean isFirst) {
        super(context);
        inflate(context, R.layout.menu_subtitle, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
        this.node = node;
        this.tv = (SefariaTextView) findViewById(R.id.tv);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tv.setLetterSpacing(0.1f);
        }

        int pad = (int)Util.dpToPixels(25);
        if (isFirst) {
            tv.setPadding(0,0,0,pad);
        } else {
            tv.setPadding(0,pad,0,pad);
        }

        setLang(lang);
    }

    public MenuNode getNode() { return node; }

    public void setLang(Util.Lang lang) {
        tv.setText(node.getPrettyTitle(lang));
        tv.setFont(lang,false,17);
    }
}
