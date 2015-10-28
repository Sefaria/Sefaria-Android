package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.menu.MenuElement;
import org.sefaria.sefaria.menu.MenuNode;

/**
 * Created by nss on 10/4/15.
 */
public class CustomActionbar extends MenuElement {

    private View searchBtn;
    private View closeBtn;
    private View tocBtn;
    private View colorBar;
    private View menuBtn;
    private TextView titleTV;
    private MenuNode node;


    public CustomActionbar(Context context, MenuNode node, int lang, OnClickListener searchClick, OnClickListener closeClick, OnClickListener titleClick, OnClickListener menuClick) {
        super(context);
        inflate(context, R.layout.custom_actionbar, this);

        this.node = node;

        searchBtn = findViewById(R.id.search_btn);
        closeBtn = findViewById(R.id.close_btn);
        tocBtn = findViewById(R.id.toc_btn);
        menuBtn = findViewById(R.id.menu_btn);
        colorBar = findViewById(R.id.color_bar);
        titleTV = (TextView) findViewById(R.id.title);


        setLang(lang);
        int topColor = node.getTopLevelColor();
        if (topColor == -1) colorBar.setVisibility(View.GONE);
        else colorBar.setBackgroundColor(getResources().getColor(topColor));

        if (searchClick != null) searchBtn.setOnClickListener(searchClick);
        else searchBtn.setVisibility(View.GONE);
        if (closeClick != null)  closeBtn.setOnClickListener(closeClick);
        else closeBtn.setVisibility(View.GONE);
        if (menuClick != null) menuBtn.setOnClickListener(menuClick);
        else menuBtn.setVisibility(View.GONE);
        if (titleClick != null) { titleTV.setOnClickListener(titleClick); tocBtn.setOnClickListener(titleClick); }
        else tocBtn.setVisibility(View.GONE);
    }

    private void setTitle(String title) {
        titleTV.setText(title);
    }

    public void setLang(int lang) {
        setTitle(node.getTitle(lang));
        if (lang == Util.HE) {
            titleTV.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            titleTV.setTextSize((getResources().getDimension(R.dimen.custom_actionbar_font_size) * Util.EN_HE_RATIO));
        }
        else if (lang == Util.EN) {
            titleTV.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
            titleTV.setTextSize(getResources().getDimension(R.dimen.custom_actionbar_font_size));
        }
    }

    public MenuNode getNode(){
        return node;
    }
}
