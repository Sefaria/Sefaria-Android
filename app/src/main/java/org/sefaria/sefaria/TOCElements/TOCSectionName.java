package org.sefaria.sefaria.TOCElements;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.TOCActivity;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.Objects;

/**
 * Created by LenJ on 12/29/2015.
 */
public class TOCSectionName extends LinearLayout implements TOCElement {

    private SefariaTextView sectionroot;
    private Context context;
    private Node node;
    private boolean displayLevel;
    private Util.Lang lang;
    private boolean displayingChildren = true;

    public TOCSectionName(Context context, Node node, Util.Lang lang, boolean displayLevel){
        super(context);
        inflate(context, R.layout.toc_sectionname, this);
        this.setOrientation(VERTICAL);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        this.lang = lang;
        this.node = node;
        this.context = context;
        this.displayLevel = displayLevel;
        init(lang);

    }

    private void init(Util.Lang lang){
        sectionroot = (SefariaTextView) findViewById(R.id.toc_sectionroot);
        sectionroot.setFont(lang,true);

        setLang(lang);
        this.setOnClickListener(clickListener);
        this.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void setLang(Util.Lang lang) {
        if (!displayLevel) {
            sectionroot.setVisibility(View.GONE);
            this.setPadding(0, 0, 0, 0);
            return;
        }
        int padding = (int) MyApp.convertDpToPixel(4.5f);
        final int sidePadding = (int) MyApp.convertDpToPixel(13f);
        if(node.isTextSection()) {
            //padding = 21;
            //sidePadding += 4;
        }
        if(lang == Util.Lang.EN) {
            this.setPadding(sidePadding, padding, 0, padding);
            this.setGravity(Gravity.LEFT);
        }else {
            this.setPadding(0, padding, sidePadding, padding);
            this.setGravity(Gravity.RIGHT);
        }
        String text = node.getTitle(lang);

        //http://stackoverflow.com/questions/2701192/character-for-up-down-triangle-arrow-to-display-in-html
        if(node.isTextSection()) {
            text = text + " >";//" \u25b8";// "   " + text
        }else if(node.getChildren().size() == 0){//it has no children but it's not a section, so it should be greyed out
            sectionroot.setTextColor(getResources().getColor(R.color.toc_greyed_out_section_name));
        }else if(text.length() >0){
            if(!displayingChildren){
                //text += " \u2304";////25BC //25BD //25BE //25BF
                if(lang == Util.Lang.EN)
                    text = "\u25b8  " + text;
                else //lang == HE
                    text = "\u25c2  " + text;
            }else {// if(displayingChildren)
                text = "\u25be  " + text;
            }
        }



        //else    ;//text += " " + "\u2228";
        sectionroot.setText(text);
        /*
        This is for a case of a node that holds just a grid and is a sibling of a sectionName which istextSection()
        Look at Ohr Hashem for an example.
         */
        if(text.length() == 0 && !node.isTextSection())
            sectionroot.setVisibility(View.GONE);

    }

    private void setDisplayingChildren(boolean displayingChildren,boolean forceChildren) {
        if (sectionroot.getVisibility() != VISIBLE)
            return;
        this.displayingChildren = displayingChildren;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (!(child instanceof SefariaTextView)) {//don't hid the text of the sectionName itself
                if (displayingChildren) {
                    child.setVisibility(View.VISIBLE);
                } else {
                    child.setVisibility(View.GONE);
                }
                if (forceChildren && child instanceof TOCSectionName) {
                    ((TOCSectionName) child).setDisplayingChildren(displayingChildren,forceChildren);
                }
            }
            setLang(lang);
        }
    }

    public void setDisplayingChildren(boolean displayingChildren){
        setDisplayingChildren(displayingChildren,false);
    }

    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!node.isTextSection()){
                setDisplayingChildren(!displayingChildren);
                return;
            }

            TOCActivity.gotoTextActivity(context, node, lang);
        }
    };

   OnLongClickListener onLongClickListener = new OnLongClickListener() {
       @Override
       public boolean onLongClick(View v) {
           if(!node.isTextSection()) {
               setDisplayingChildren(!displayingChildren,true);
               return true;
           }
           return false;
       }
   };
}
