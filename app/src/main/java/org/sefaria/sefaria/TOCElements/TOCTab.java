package org.sefaria.sefaria.TOCElements;


import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.menu.MenuElement;
import org.sefaria.sefaria.menu.MenuNode;


/**
 *
 * Controls the tabs seen in TOC page
 */
public class TOCTab extends LinearLayout implements TOCElement {

    private Node node;
    private TextView tv;

    public TOCTab(Context context, Node node, Util.Lang lang) {
        super(context);
        inflate(context, R.layout.tab_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.node = node;
        this.tv = (TextView) findViewById(R.id.tv);
        tv.setText(node.getTitle(lang));
        tv.setTextSize(6);//TODO dynamic sizes
        setLang(lang);
        setActive(false);
    }

    public Node getNode() { return node; }

    public void setLang(Util.Lang lang) {
        tv.setText(node.getTitle(lang));
        if (lang == Util.Lang.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //tv.setTextSize(Math.round(getResources().getDimension(R.dimen.tab_menu_font_size) * Util.EN_HE_RATIO));
        } else {
            tv.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
            //tv.setTextSize(getResources().getDimension(R.dimen.tab_menu_font_size));
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
