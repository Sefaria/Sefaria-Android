package org.sefaria.sefaria.TOCElements;


import android.content.Context;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.SefariaTextView;


/**
 *
 * Controls the tabs seen in TOC page
 */
public class TOCTab extends LinearLayout implements TOCElement {

    private Context context;
    private Node node;
    private SefariaTextView tv;
    private boolean isActive;
    private Util.Lang lang;


    public TOCTab(Context context, Node node, Util.Lang lang) {
        super(context);
        inflate(context, R.layout.menu_tab, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        this.context = context;
        this.lang = lang;
        this.node = node;
        this.tv = (SefariaTextView) findViewById(R.id.tv);
        tv.setText(node.getTabName(lang));
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
        inflate(context, R.layout.menu_tab, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));

        this.context = context;
        this.tv = (SefariaTextView) findViewById(R.id.tv);

        setLang(lang);
        setActive(false);
    }



    public Node getNode() { return node; }


    public void setLang(Util.Lang lang) {
        this.lang = lang;
        tv.setFont(lang,false,15);
        String newTvText;
        if (lang == Util.Lang.HE) {
            newTvText = "מפרשים" ;//Mifarshim //
        } else {
            newTvText = "Commentary";
        }
        if(node != null)//It's a regular TOC tab
            newTvText = node.getTabName(lang);
        //else it will use one of the "Commentary"s
        tv.setText(newTvText);

    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
        if (isActive) {
            tv.setTextColor(Util.getColor(context,R.attr.text_color_main));
        } else {
            tv.setTextColor(Util.getColor(context,R.attr.text_color_english));
        }
    }

    public boolean getActive(){
        return isActive;
    }
    public Util.Lang getLang() { return lang; }

}
