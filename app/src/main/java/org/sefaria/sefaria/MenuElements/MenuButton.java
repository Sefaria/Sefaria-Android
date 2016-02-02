package org.sefaria.sefaria.MenuElements;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

/**
 * Created by nss on 9/10/15.
 */
public class MenuButton extends MenuElement {

    private MenuNode sectionNode;
    private MenuNode menuNode;
    private TextView tv;
    private View colorBar;

    public MenuButton(Context context) {
        super(context);
        inflate(context, R.layout.button_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
        this.setVisibility(View.INVISIBLE);
    }

    public MenuButton(Context context, MenuNode menuNode, MenuNode sectionNode,Util.Lang lang) {
        super(context);
        //home and menu buttons are slightly different.
        //annoyingly, it's difficult to set margin dynamically, instead I'll just switch views
        if (menuNode.isHomeButton()) {
            inflate(context, R.layout.button_home, this);
            this.tv = (TextView) this.findViewById(R.id.tv);
            this.colorBar = this.findViewById(R.id.color_bar);
            setColor(menuNode.getColor());
        } else {//menu
            inflate(context, R.layout.button_menu, this);
            this.tv = (TextView) this.findViewById(R.id.tv);
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
            this.tv.setAllCaps(true); //only when there's color? (ie home page)
        }
    }

    /*public MenuButton(Context context, String text) {
        super(context);
        inflate(context, R.layout.button_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));

        this.tv = (TextView) this.findViewById(R.id.tv);
        this.colorBar = this.findViewById(R.id.color_bar);
        tv.setText(text);
        this.setClickable(true);
    }*/

    public MenuNode getSectionNode() { return sectionNode; }

    public MenuNode getNode() { return menuNode; }

    public boolean isBook() { return menuNode.getNumChildren() == 0; }

    public void setLang(Util.Lang lang) {
        tv.setText(menuNode.getPrettyTitle(lang));
        if (lang == Util.Lang.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //tv.setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
        }
        else {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            //tv.setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
    }
}
