package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuElement;
import org.sefaria.sefaria.MenuElements.MenuNode;

/**
 * Created by nss on 10/4/15.
 */
public class CustomActionbar extends MenuElement {

    private View homeBtn;
    private View closeBtn;
    private View searchBtn;
    private View colorBar;
    private View menuBtn;
    private View backBtn;
    private View invisableBtn;
    private View invisableBtnLeft;
    private TextView titleTV;
    private String heText = null;
    private String enText = null;
    private MenuNode menuNode;

    private static final boolean noBackButton = true;

    public CustomActionbar(Context context, MenuNode menuNode, Util.Lang lang, OnClickListener homeClick, OnClickListener closeClick, OnClickListener searchClick, OnClickListener titleClick, OnClickListener menuClick, OnClickListener backClick, int catColor) {
        super(context);
        inflate(context, R.layout.custom_actionbar, this);

        this.menuNode = menuNode;

        homeBtn = findViewById(R.id.home_btn);
        closeBtn = findViewById(R.id.close_btn);
        searchBtn = findViewById(R.id.search_btn);
        menuBtn = findViewById(R.id.menu_btn);
        backBtn = findViewById(R.id.back_btn);
        invisableBtn = findViewById(R.id.invisable_btn);
        invisableBtnLeft = findViewById(R.id.invisable_btn_left);
        colorBar = findViewById(R.id.color_bar);
        titleTV = (TextView) findViewById(R.id.title);

        TextView langBtn = (TextView) menuBtn.findViewById(R.id.lang_btn);
        langBtn.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));


        setLang(lang);

        if (catColor == -1) colorBar.setVisibility(View.GONE);
        else colorBar.setBackgroundColor(getResources().getColor(catColor));

        if (homeClick != null) homeBtn.setOnClickListener(homeClick);
        else{
            homeBtn.setVisibility(View.GONE);
            invisableBtn.setVisibility(View.GONE);
            invisableBtnLeft.setVisibility(View.GONE);
        }

        if (closeClick != null)  closeBtn.setOnClickListener(closeClick);
        else closeBtn.setVisibility(View.INVISIBLE);

        if (searchClick != null)  searchBtn.setOnClickListener(searchClick);
        else searchBtn.setVisibility(View.GONE);

        if (menuClick != null) {
            menuBtn.setOnClickListener(menuClick);
            /*
            if(lang == Util.Lang.HE)
                findViewById(R.id.lang_btn_img).setBackgroundDrawable(getResources().getDrawable(R.drawable.he_icon2));
            else if(lang == Util.Lang.EN)
                findViewById(R.id.lang_btn_img).setBackgroundDrawable(getResources().getDrawable(R.drawable.en_icon2));
             */
        }
        else menuBtn.setVisibility(View.INVISIBLE);



        if (backClick != null) backBtn.setOnClickListener(backClick);
        else  backBtn.setVisibility(View.INVISIBLE);
        if(noBackButton) backBtn.setVisibility(View.GONE);



        if (titleClick != null ) {
            titleTV.setOnClickListener(titleClick);
            //tocBtn.setOnClickListener(titleClick);
        }
        //else{ tocBtn.setVisibility(View.GONE);


        //TODO - make this look normal centered
        //tocBtn.setVisibility(View.GONE);
    }

    private void setTitle(String title) {
        if(title.length() > 18)
            titleTV.setTextSize(9);
        if(title.length()< 19)
            title = "\u3000\u3000" + title + "\u3000\u3000";
        titleTV.setText(title);
    }

    public void setTitleText(String title, Util.Lang lang, boolean forceRefresh, boolean forTOC){
        Log.d("cab","title: "+  title);
        if(forTOC) //Add down arrow
            title = title + " \u25be"; // "\u25bc " //"\u25be" 23F7 //http://unicode-search.net/unicode-namesearch.pl?term=triangle
        if(lang == Util.Lang.HE)
            heText = title;
        else// if(lang == Util.Lang.HE || Lang.BI)
            enText = title;
        if(forceRefresh)
            setLang(lang);
    }

    public void setLang(Util.Lang lang) {
        /**
         * This is a hack so that the resizing textbox thinks that the text is bigger so it's more likely to fit everything in.
         * \u3000 is a type of space char
         */
        String title;

        if(lang == Util.Lang.HE && heText != null){
            title = heText;
            Log.d("Cab", "here in hetext" + lang + " " + heText + "--" + enText);
        } else if(lang == Util.Lang.EN && enText != null) {
            title = enText;
            Log.d("Cab", "here in entext" + lang + " " + heText + "--" + enText);
        }else{
            Log.d("Cab", "here in menunode" + lang + " " + heText + "--" + enText);
            title = menuNode.getTitle(lang);
        }


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
        return menuNode;
    }

}
