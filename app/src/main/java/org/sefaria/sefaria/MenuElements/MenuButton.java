package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.SefariaTextView;

/**
 * Created by nss on 9/10/15.
 */
public class MenuButton extends MenuElement {

    private MenuNode sectionNode;
    private MenuNode menuNode;
    private SefariaTextView entv;
    private SefariaTextView hetv;
    private SefariaTextView tv;
    private View colorBar;

    public MenuButton(Context context) {
        super(context);
        inflate(context, R.layout.menu_button, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
        this.setVisibility(View.INVISIBLE);
    }

    public MenuButton(Context context, MenuNode menuNode, MenuNode sectionNode,Util.Lang lang) {
        super(context);
        //home and menu buttons are slightly different.
        //annoyingly, it's difficult to set margin dynamically, instead I'll just switch views
        if (menuNode.isHomeButton()) {
            inflate(context, R.layout.menu_home_button, this);
            this.entv = (SefariaTextView) this.findViewById(R.id.en_tv);
            this.hetv = (SefariaTextView) this.findViewById(R.id.he_tv);
            this.colorBar = this.findViewById(R.id.color_bar);
            if (android.os.Build.VERSION.SDK_INT >= 14) {
                this.entv.setAllCaps(true);
            }
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                this.entv.setLetterSpacing(0.1f);
                this.hetv.setLetterSpacing(0.1f);
            }
            //Log.d("color", "BTN " + menuNode.getTitle(Util.Lang.EN) + " " + Integer.toHexString(context.getResources().getColor(menuNode.getColor())));
            setColor(menuNode.getColor());
        } else {//menu
            inflate(context, R.layout.menu_button, this);
            this.tv = (SefariaTextView) findViewById(R.id.tv);

        }
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));


        this.menuNode = menuNode;
        this.sectionNode = sectionNode;
        this.setClickable(true);
        setLang(lang);

    }

    private void setColor(int colorInt){
        if (colorInt != -1) {
            this.colorBar.setBackgroundColor(getResources().getColor(colorInt));
            //this.tv.setAllCaps(true); //only when there's color? (ie home page)
        }
    }

    /*public MenuButton(Context context, String segment) {
        super(context);
        inflate(context, R.layout.menu_button, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));

        this.tv = (TextView) this.findViewById(R.id.tv);
        this.colorBar = this.findViewById(R.id.color_bar);
        tv.setText(segment);
        this.setClickable(true);
    }*/

    public MenuNode getSectionNode() { return sectionNode; }

    public MenuNode getNode() { return menuNode; }

    public boolean isBook() { return menuNode.getNumChildren() == 0; }

    public void setLang(Util.Lang lang) {
        SefariaTextView tv;
        if (lang == Util.Lang.EN) {
            try {
                tv = entv;
                findViewById(R.id.en).setVisibility(View.VISIBLE);
                findViewById(R.id.he).setVisibility(View.GONE);
            } catch (NullPointerException e) {
                tv = this.tv;
            }
        } else {
            try {
                tv = hetv;
                findViewById(R.id.en).setVisibility(View.GONE);
                findViewById(R.id.he).setVisibility(View.VISIBLE);
            } catch (NullPointerException e) {
                tv = this.tv;
            }
        }
        tv.setText(menuNode.getPrettyTitle(lang));
        //NOTE: Need to use pixels here b/c I'm using getDimension which already converts
        tv.setFont(lang, true, getResources().getDimension(R.dimen.menu_button_font_size), TypedValue.COMPLEX_UNIT_PX);
    }

    public void setIsMore(boolean isMore) {
        if (isMore) {
            findViewById(R.id.en_moreArrow).setVisibility(View.VISIBLE);
            findViewById(R.id.he_moreArrow).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.en_moreArrow).setVisibility(View.GONE);
            findViewById(R.id.he_moreArrow).setVisibility(View.INVISIBLE);
        }
    }
}
