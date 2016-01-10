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

    private View homeBtn;
    private View closeBtn;
    private View searchBtn;
    private View tocBtn;
    private View colorBar;
    private View menuBtn;
    private View backBtn;
    private View invisableBtn;
    private TextView titleTV;
    private MenuNode node;


    public CustomActionbar(Context context, MenuNode node, Util.Lang lang, OnClickListener homeClick, OnClickListener closeClick, OnClickListener searchClick, OnClickListener titleClick, OnClickListener menuClick, OnClickListener backClick) {
        super(context);
        inflate(context, R.layout.custom_actionbar, this);

        this.node = node;

        homeBtn = findViewById(R.id.home_btn);
        closeBtn = findViewById(R.id.close_btn);
        searchBtn = findViewById(R.id.search_btn);
        tocBtn = findViewById(R.id.toc_btn);
        menuBtn = findViewById(R.id.menu_btn);
        backBtn = findViewById(R.id.back_btn);
        invisableBtn = findViewById(R.id.invisable_btn);
        colorBar = findViewById(R.id.color_bar);
        titleTV = (TextView) findViewById(R.id.title);


        setLang(lang);
        int topColor = node.getTopLevelColor();
        if (topColor == -1) colorBar.setVisibility(View.GONE);
        else colorBar.setBackgroundColor(getResources().getColor(topColor));

        if (homeClick != null) homeBtn.setOnClickListener(homeClick);
        else{
            homeBtn.setVisibility(View.GONE);
            invisableBtn.setVisibility(View.GONE);
        }

        if (closeClick != null)  closeBtn.setOnClickListener(closeClick);
        else closeBtn.setVisibility(View.GONE);

        if (searchClick != null)  searchBtn.setOnClickListener(searchClick);
        else searchBtn.setVisibility(View.GONE);

        if (menuClick != null) menuBtn.setOnClickListener(menuClick);
        else menuBtn.setVisibility(View.INVISIBLE);

        if (backClick != null) backBtn.setOnClickListener(backClick);
        else backBtn.setVisibility(View.INVISIBLE);

        if (titleClick != null ) { titleTV.setOnClickListener(titleClick); tocBtn.setOnClickListener(titleClick); }
        else tocBtn.setVisibility(View.GONE);


        //TODO - make this look normal centered
        tocBtn.setVisibility(View.GONE);
    }

    private void setTitle(String title) {
        titleTV.setText(title);
    }

    public void setLang(Util.Lang lang) {
        /**
         * This is a hack so that the resizing textbox thinks that the text is bigger so it's more likely to fit everything in.
         * \u3000 is a type of space char
         */
        String title = node.getTitle(lang);
        if(title.length() > 18)
            titleTV.setTextSize(9);
        if(title.length()< 19)
            title = "\u3000\u3000" + title + "\u3000\u3000";

        setTitle(title);
        if (lang == Util.Lang.HE) {
            titleTV.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //titleTV.setTextSize((getResources().getDimension(R.dimen.custom_actionbar_font_size) * Util.EN_HE_RATIO));
        }
        else if (lang == Util.Lang.EN) {
            titleTV.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
            //titleTV.setTextSize(getResources().getDimension(R.dimen.custom_actionbar_font_size));
        }
    }

    public MenuNode getNode(){
        return node;
    }

}
