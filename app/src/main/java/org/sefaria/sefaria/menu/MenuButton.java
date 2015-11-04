package org.sefaria.sefaria.menu;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.w3c.dom.Text;

/**
 * Created by nss on 9/10/15.
 */
public class MenuButton extends MenuElement {

    private MenuNode sectionNode;
    private MenuNode node;
    private TextView tv;

    public MenuButton(Context context) {
        super(context);
        inflate(context, R.layout.button_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
        this.setVisibility(View.INVISIBLE);
    }

    public MenuButton(Context context, MenuNode node, MenuNode sectionNode,int lang) {
        super(context);
        inflate(context, R.layout.button_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));

        this.tv = (TextView) this.findViewById(R.id.tv);

        this.node = node;
        this.sectionNode = sectionNode;
        this.setClickable(true);
        setLang(lang);
        if (node.getColor() != -1) {
            this.tv.setBackgroundColor(getResources().getColor(node.getColor()));
            this.tv.setTextColor(Color.parseColor("#FFFFFF"));
            this.tv.setAllCaps(true); //only when there's color? (ie home page)
        }
    }

    public MenuButton(Context context, String text) {
        super(context);
        inflate(context, R.layout.button_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));

        this.tv = (TextView) this.findViewById(R.id.tv);
        tv.setText(text);
        this.setClickable(true);
    }

    public MenuNode getSectionNode() { return sectionNode; }

    public MenuNode getNode() { return node; }

    public boolean isBook() { return node.getNumChildren() == 0; }

    public void setLang(int lang) {
        tv.setText(node.getPrettyTitle(lang));
        if (lang == Util.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            tv.setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
        }
        else {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            tv.setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
    }


}
