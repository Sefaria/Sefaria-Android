package org.sefaria.sefaria.layouts;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuElement;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.database.Database;

/**
 * Created by nss on 10/4/15.
 */
public class CustomActionbar extends MenuElement {

    private View homeBtn;
    private View closeBtn;
    private View searchBtn;
    private View colorBar;
    private View menuBtn;
    private View menuLangBtn;
    private View backBtn;
    private View invisableBtn;
    private View invisableBtnLeft;
    private SefariaTextView titleTV;
    private String heText = null;
    private String enText = null;
    private MenuNode menuNode;

    private static final boolean noBackButton = false;

    public CustomActionbar(Activity activity, MenuNode menuNode, Util.Lang lang, OnClickListener homeClick, OnLongClickListener homeLongClick, OnClickListener closeClick, OnClickListener searchClick, OnClickListener titleClick, OnClickListener menuClick, OnClickListener backClick, OnClickListener menuLangClick, int catColor) {
        super(activity);
        inflate(activity, R.layout.custom_actionbar, this);

        this.menuNode = menuNode;

        homeBtn = findViewById(R.id.home_btn);
        closeBtn = findViewById(R.id.close_btn);
        searchBtn = findViewById(R.id.search_btn);
        menuBtn = findViewById(R.id.menu_btn);
        menuLangBtn = findViewById(R.id.menu_lang_btn);
        backBtn = findViewById(R.id.back_btn);
        invisableBtn = findViewById(R.id.invisable_btn);
        invisableBtnLeft = findViewById(R.id.invisable_btn_left);
        colorBar = findViewById(R.id.color_bar);
        titleTV = (SefariaTextView) findViewById(R.id.title);

        SefariaTextView langBtn = (SefariaTextView) menuBtn.findViewById(R.id.lang_btn);
        langBtn.setFont(Util.Lang.HE,true);

        SefariaTextView menuLangBtnTextView = (SefariaTextView) menuLangBtn.findViewById(R.id.langTV);
        menuLangBtnTextView.setFont(Util.Lang.HE,true);

        setLang(lang);

        if (catColor == -1) colorBar.setVisibility(View.GONE);
        else colorBar.setBackgroundColor(getResources().getColor(catColor));

        if (homeClick != null) homeBtn.setOnClickListener(homeClick);
        else{
            homeBtn.setVisibility(View.GONE);
            invisableBtn.setVisibility(View.GONE);
            invisableBtnLeft.setVisibility(View.GONE);
        }

        if(homeLongClick != null)
            homeBtn.setOnLongClickListener(homeLongClick);

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

        if(menuLangClick != null){
            menuLangBtn.setOnClickListener(menuLangClick);
        }else menuLangBtn.setVisibility(GONE);

        if (backClick != null) backBtn.setOnClickListener(backClick);
        else  backBtn.setVisibility(View.GONE);
        if(noBackButton) backBtn.setVisibility(View.GONE);



        if (titleClick != null ) {
            titleTV.setOnClickListener(titleClick);
            //tocBtn.setOnClickListener(titleClick);
        }
        //else{ tocBtn.setVisibility(View.GONE);


        //TODO - make this look normal centered
        //tocBtn.setVisibility(View.GONE);

        //DEAL WITH DIALOGSNACKBAR
        DialogNoahSnackbar.checkCurrentDialog(activity,(ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));
    }


    private void setTitle(String title) {
        if(title.length() > 18)
            titleTV.setTextSize(9);
        if(title.length()< 19)
            title = "\u3000\u3000" + title + "\u3000\u3000";
        titleTV.setText(title);
    }

    public void setTitleText(String title, Util.Lang lang, boolean forceRefresh, boolean forTOC){
        if(forTOC) //Add down arrow
            title = title + " \u25be"; // "\u25bc " //"\u25be" 23F7 //http://unicode-search.net/unicode-namesearch.pl?term=triangle
        if(lang == Util.Lang.HE)
            heText = title;
        else// if(lang == Util.Lang.HE || Lang.BI)
            enText = title;
        if(forceRefresh)
            setLang(lang);
    }

    public void setMenuBtnLang(Util.Lang lang) {
        SefariaTextView tv = (SefariaTextView) findViewById(R.id.langTV);

        if (lang == Util.Lang.HE) {
            tv.setText("A");
            tv.setFont(Util.Lang.EN, true,getResources().getDimension(R.dimen.custom_action_bar_lang_font_size), TypedValue.COMPLEX_UNIT_PX);
        }
        else /* if (lang == Util.Lang.EN) */ {
            tv.setText("א");
            tv.setFont(Util.Lang.HE, true, getResources().getDimension(R.dimen.custom_action_bar_lang_font_size), TypedValue.COMPLEX_UNIT_PX);
        }
    }

    public void setLang(Util.Lang lang) {
        /**
         * This is a hack so that the resizing textbox thinks that the text is bigger so it's more likely to fit everything in.
         * \u3000 is a type of space char
         */
        String title;

        if(lang == Util.Lang.HE && heText != null){
            title = heText;
        } else if(lang == Util.Lang.EN && enText != null) {
            title = enText;
        }else{
            title = menuNode.getTitle(lang);
        }


        setTitle(title);
        titleTV.setFont(lang,false);
    }

    public MenuNode getNode(){
        return menuNode;
    }

}
