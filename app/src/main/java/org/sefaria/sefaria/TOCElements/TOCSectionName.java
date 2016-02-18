package org.sefaria.sefaria.TOCElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SectionActivity;
import org.sefaria.sefaria.activities.TextActivity;
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
    }

    @Override
    public void setLang(Util.Lang lang) {
        if (!displayLevel) {
            sectionroot.setVisibility(View.INVISIBLE);
            this.setPadding(0, 0, 0, 0);
            return;
        }
        final int padding = 12;
        final int sidePadding = 50;
        if(lang == Util.Lang.EN) {
            this.setPadding(sidePadding, padding, 0, padding);
            this.setGravity(Gravity.LEFT);
        }else {
            this.setPadding(0, padding, sidePadding, padding);
            this.setGravity(Gravity.RIGHT);
        }
        String text = node.getTitle(lang);
        if(node.isTextSection()) {
            text += " >";
        }else if(node.getChildren().size() == 0){//it has no children but it's not a section, so it should be greyed out
            sectionroot.setTextColor(getResources().getColor(R.color.toc_greyed_out_section_name));
        }
        //else    ;//text += " " + "\u2228";
        sectionroot.setText(text);
        /*
        This is for a case of a node that holds just a grid and is a sibling of a sectionName which istextSection()
        Look at Ohr Hashem for an example.
         */
        if(text.length() ==0 && !node.isTextSection())
            sectionroot.setVisibility(View.GONE);

    }

    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO go to intent of text page
            Log.d("toc", "sectionanem _ node:" + node);
            if(false && !node.isTextSection()){ //TODO maybe try to make this work at some point .. && get rid of false
                for(int i=0;i<TOCSectionName.this.getChildCount();i++){
                    View child = TOCSectionName.this.getChildAt(i);
                    if(child == sectionroot)
                        continue;
                    if(child.getVisibility() == View.INVISIBLE)
                        setVisibility(View.VISIBLE);
                    else
                        setVisibility(View.INVISIBLE);
                }
            }

            //TODO determine if it's a leaf and if so then display text
            if(!node.isTextSection())
                return;
            TOCNumBox.gotoTextActivity(context,node,lang);
        }
    };
}
