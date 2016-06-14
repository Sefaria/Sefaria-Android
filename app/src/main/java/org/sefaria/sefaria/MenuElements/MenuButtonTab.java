package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.util.TypedValue;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.SefariaTextView;

/**
 * Created by nss on 9/24/15.
 * Controls the tabs seen in Talmud page
 */
public class MenuButtonTab extends MenuElement {

    private Context context;
    private MenuNode node;
    private SefariaTextView tv;

    public MenuButtonTab(Context context, MenuNode node, Util.Lang lang) {
        super(context);
        inflate(context, R.layout.menu_tab, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.context = context;
        this.node = node;
        this.tv = (SefariaTextView) findViewById(R.id.tv);
        tv.setText(node.getTitle(lang));
        setLang(lang);
        setActive(false);
    }

    public MenuNode getNode() { return node; }

    public void setLang(Util.Lang lang) {
        tv.setText(node.getPrettyTitle(lang));
        tv.setFont(lang,true,getResources().getDimension(R.dimen.tab_menu_font_size), TypedValue.COMPLEX_UNIT_PX);
    }

    public void setActive(boolean isActive) {
        if (isActive) {
            tv.setTextColor(Util.getColor(context,R.attr.text_color_main));
        } else {
            tv.setTextColor(Util.getColor(context,R.attr.text_color_faded));
        }
    }


}
