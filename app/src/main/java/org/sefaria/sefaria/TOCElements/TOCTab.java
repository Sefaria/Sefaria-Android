package org.sefaria.sefaria.TOCElements;


import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Node;


/**
 *
 * Controls the tabs seen in TOC page
 */
public class TOCTab extends LinearLayout implements TOCElement {

    private Node node;
    private TextView tv;
    private boolean isActive;
    private Util.Lang lang;

    public TOCTab(Context context, Node node, Util.Lang lang) {
        super(context);
        inflate(context, R.layout.tab_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.lang = lang;
        this.node = node;
        this.tv = (TextView) findViewById(R.id.tv);
        tv.setText(node.getTabName(lang));
        tv.setTextSize(6);//TODO dynamic sizes
        setLang(lang);
        setActive(false);
    }

    /**
     * this constructor is for commentaries only
     * @param context
     * @param lang
     */
    public TOCTab(Context context,  Util.Lang lang) {
        super(context);
        inflate(context, R.layout.tab_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.tv = (TextView) findViewById(R.id.tv);

        tv.setTextSize(6);//TODO dynamic sizes
        setLang(lang);
        setActive(false);
    }



    public Node getNode() { return node; }


    public void setLang(Util.Lang lang) {
        this.lang = lang;

        String newTvText;
        if (lang == Util.Lang.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            newTvText = "מפרשים" ;//Mifarshim //
            //tv.setTextSize(Math.round(getResources().getDimension(R.dimen.tab_menu_font_size) * Util.EN_HE_RATIO));
        } else {
            newTvText = "Commentary";
            tv.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
            //tv.setTextSize(getResources().getDimension(R.dimen.tab_menu_font_size));
        }
        if(node != null)//It's a regular TOC tab
            newTvText = node.getTabName(lang);
        //else it will use one of the "Commentary"s
        tv.setText(newTvText);

    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        if (isActive) {
            tv.setTextColor(getResources().getColor(R.color.tab_active));
        } else {
            tv.setTextColor(getResources().getColor(R.color.tab_inactive));
        }
    }

    public boolean getActive(){
        return isActive;
    }
    public Util.Lang getLang() { return lang; }

}
