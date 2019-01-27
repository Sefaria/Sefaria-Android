package org.sefaria.sefaria.layouts;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;

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
    private View invisibleBtn;
    private View invisibleBtnLeft;
    private LinearLayout wholeTitleLinearLayout;
    private SefariaTextView titleTV;
    private String heText = null;
    private String enText = null;
    private MenuNode menuNode;
    private boolean forTOC = false;
    private boolean isSerif;

    private static final boolean noBackButton = false;
    public CustomActionbar(Activity activity, MenuNode menuNode, Util.Lang lang, OnClickListener homeClick, OnLongClickListener homeLongClick, OnClickListener closeClick, OnClickListener searchClick, OnClickListener titleClick, OnClickListener menuClick, OnClickListener backClick, OnClickListener menuLangClick, int catColor,boolean isSerif, boolean isMenu) {
        this(activity, menuNode, lang, homeClick, homeLongClick, closeClick, searchClick, titleClick, menuClick, backClick, menuLangClick, catColor, isSerif, isMenu, false);
    }

    public CustomActionbar(Activity activity, MenuNode menuNode, Util.Lang lang, OnClickListener homeClick, OnLongClickListener homeLongClick, OnClickListener closeClick, OnClickListener searchClick, OnClickListener titleClick, OnClickListener menuClick, OnClickListener backClick, OnClickListener menuLangClick, int catColor,boolean isSerif, boolean isMenu, boolean showWillDTalmud) {
        super(activity);
        inflate(activity, R.layout.custom_actionbar, this);

        this.menuNode = menuNode;

        homeBtn = findViewById(R.id.home_btn);
        closeBtn = findViewById(R.id.close_btn);
        searchBtn = findViewById(R.id.search_btn);
        menuBtn = findViewById(R.id.menu_btn);
        menuLangBtn = findViewById(R.id.menu_lang_btn);
        backBtn = findViewById(R.id.back_btn);
        invisibleBtn = findViewById(R.id.invisible_btn);
        invisibleBtnLeft = findViewById(R.id.invisible_btn_left);
        colorBar = findViewById(R.id.color_bar);
        wholeTitleLinearLayout = (LinearLayout) findViewById(R.id.whole_title);
        titleTV = (SefariaTextView) findViewById(R.id.title);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            titleTV.setLetterSpacing(0.1f);
        }


        SefariaTextView menuLangBtnTextView = (SefariaTextView) menuLangBtn.findViewById(R.id.langTV);
        menuLangBtnTextView.setFont(Util.Lang.HE,true);

        setLang(lang);

        if (catColor == -1) colorBar.setVisibility(View.GONE);
        else {
            int color = getResources().getColor(catColor);
            int tintColor = Util.tintColor(color,0.3f);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                activity.getWindow().setStatusBarColor(tintColor);
            }
            colorBar.setBackgroundColor(color);
        }

        if (homeClick != null) homeBtn.setOnClickListener(homeClick);
        else{
            homeBtn.setVisibility(View.GONE);
            invisibleBtn.setVisibility(View.GONE);
            invisibleBtnLeft.setVisibility(View.GONE);
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
            wholeTitleLinearLayout.setOnClickListener(titleClick);
            //tocBtn.setOnClickListener(titleClick);
        }
        //else{ tocBtn.setVisibility(View.GONE);


        //TODO - make this look normal centered
        //tocBtn.setVisibility(View.GONE);

        if(showWillDTalmud){
            SefariaTextView williamDTalmud = (SefariaTextView) findViewById(R.id.william_d_talmud);
            williamDTalmud.setVisibility(View.VISIBLE);
        }


        this.isSerif = isSerif;
        titleTV.setAllCaps(isMenu || !isSerif);
        //DEAL WITH DIALOGSNACKBAR
        DialogNoahSnackbar.checkCurrentDialog(activity,(ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));
    }

    private void setTOCbtn(Util.Lang lang, boolean forTOC){
        this.forTOC = forTOC;
        if(forTOC) { //Add down arrow
            if(lang == Util.Lang.HE) {
                findViewById(R.id.toc_btn_left).setVisibility(VISIBLE);
                findViewById(R.id.toc_btn_right).setVisibility(INVISIBLE);
            }else{
                findViewById(R.id.toc_btn_left).setVisibility(INVISIBLE);
                findViewById(R.id.toc_btn_right).setVisibility(VISIBLE);
            }
        }else{
            findViewById(R.id.toc_btn_left).setVisibility(GONE);
            findViewById(R.id.toc_btn_right).setVisibility(GONE);
        }
    }

    public void setTitleText(String title, Util.Lang lang, boolean forceRefresh,boolean forTOC){
        setTOCbtn(lang,forTOC);
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
            tv.setFont(Util.Lang.EN, true,getResources().getDimension(R.dimen.custom_action_bar_lang_font_size), TypedValue.COMPLEX_UNIT_PX);//using pixels b/c when using getDimensions it actually converts to pixels
        }
        else /* if (lang == Util.Lang.EN) */ {
            tv.setText("א");
            tv.setFont(Util.Lang.HE, true, getResources().getDimension(R.dimen.custom_action_bar_lang_font_size), TypedValue.COMPLEX_UNIT_PX);
        }
    }

    public void setLang(Util.Lang lang) {
        /**
         * This is a hack so that the resizing textbox thinks that the segment is bigger so it's more likely to fit everything in.
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

        int textSize = 20;
        if(title.length() > 20) {
            textSize = 10;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                titleTV.setLetterSpacing(0f);
            }
        }
        titleTV.setText(title);

        setTOCbtn(lang,forTOC);
        titleTV.setFont(lang,isSerif,textSize);
    }

    public MenuNode getNode(){
        return menuNode;
    }

}
