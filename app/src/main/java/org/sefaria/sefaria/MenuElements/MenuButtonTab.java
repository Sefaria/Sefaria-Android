package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.SefariaTextView;

/**
 * Created by nss on 9/24/15.
 * Controls the tabs seen in Talmud page
 */
public class MenuButtonTab extends MenuElement {

    private MenuNode node;
    private SefariaTextView tv;

    public MenuButtonTab(Context context, MenuNode node, Util.Lang lang) {
        super(context);
        inflate(context, R.layout.tab_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.node = node;
        this.tv = (SefariaTextView) findViewById(R.id.tv);
        tv.setText(node.getTitle(lang));
        setLang(lang);
        setActive(false);
    }

    public MenuNode getNode() { return node; }

    public void setLang(Util.Lang lang) {
        tv.setText(node.getPrettyTitle(lang));
        tv.setFont(lang,true);
    }

    public void setActive(boolean isActive) {
        if (isActive) {
            tv.setTextColor(getResources().getColor(R.color.tab_active));
        } else {
            tv.setTextColor(getResources().getColor(R.color.tab_inactive));
        }
    }


}
