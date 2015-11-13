package org.sefaria.sefaria.menu;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

/**
 * Created by nss on 9/24/15.
 * Controls the tabs seen in Talmud page
 */
public class MenuButtonTab extends MenuElement {

    private MenuNode node;
    private TextView tv;

    public MenuButtonTab(Context context, MenuNode node, Util.Lang lang) {
        super(context);
        inflate(context, R.layout.tab_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.node = node;
        this.tv = (TextView) findViewById(R.id.tv);
        tv.setText(node.getTitle(lang));
        setLang(lang);
        setActive(false);
    }

    public MenuNode getNode() { return node; }

    public void setLang(Util.Lang lang) {
        tv.setText(node.getPrettyTitle(lang));
        if (lang == Util.Lang.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            tv.setTextSize(Math.round(getResources().getDimension(R.dimen.tab_menu_font_size) * Util.EN_HE_RATIO));
        } else {
            tv.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
            tv.setTextSize(getResources().getDimension(R.dimen.tab_menu_font_size));
        }


    }

    public void setActive(boolean isActive) {
        if (isActive) {
            tv.setTextColor(getResources().getColor(R.color.tab_active));
        } else {
            tv.setTextColor(getResources().getColor(R.color.tab_inactive));
        }
    }


}
