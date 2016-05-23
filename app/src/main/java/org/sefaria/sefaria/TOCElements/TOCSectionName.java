package org.sefaria.sefaria.TOCElements;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

    private SefariaTextView sectionNameTitle;
    private LinearLayout sectionNameGroup;
    private LinearLayout sectionChildren;
    private Context context;
    private Node node;
    private boolean displayLevel;
    private Util.Lang lang;
    private boolean displayingChildren = true;
    ImageView iconLeft;
    ImageView iconRight;

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
        sectionNameTitle = (SefariaTextView) findViewById(R.id.toc_section_name_title);
        sectionNameTitle.setFont(lang,true);

        sectionNameGroup = (LinearLayout) findViewById(R.id.toc_section_name_group);
        sectionChildren = (LinearLayout) findViewById(R.id.toc_section_name_children);

        iconLeft = (ImageView) findViewById(R.id.toc_section_icon_left);
        iconRight = (ImageView) findViewById(R.id.toc_section_icon_right);

        setLang(lang);
        sectionNameTitle.setOnClickListener(clickListener);
        sectionNameTitle.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void setLang(Util.Lang lang) {
        if (!displayLevel) {
            sectionNameGroup.setVisibility(View.GONE);
            this.setPadding(0, 0, 0, 0);
            return;
        }
        int padding = (int) MyApp.convertDpToPixel(4.5f);
        final int sidePadding = (int) MyApp.convertDpToPixel(13f);
        if(lang == Util.Lang.EN) {
            this.setPadding(sidePadding, padding, 0, padding);
            this.setGravity(Gravity.LEFT);
        }else {
            this.setPadding(0, padding, sidePadding, padding);
            this.setGravity(Gravity.RIGHT);
        }
        String text = node.getTitle(lang);
        Log.d("TOCSection",text + " " + node);

        //http://stackoverflow.com/questions/2701192/character-for-up-down-triangle-arrow-to-display-in-html
        if(node.isTextSection()) {
            text = text + " >";
        }else if(node.getChildren().size() == 0){//it has no children but it's not a section, so it should be greyed out
            sectionNameTitle.setTextColor(getResources().getColor(R.color.toc_greyed_out_section_name));
        }else if(text.length() >0){
            ImageView viewableIcon;
            if(lang == Util.Lang.EN){
                iconLeft.setVisibility(VISIBLE);
                iconLeft.setImageResource(R.drawable.right_arrow);
                iconRight.setVisibility(GONE);
                viewableIcon = iconLeft;
            }
            else{//lang == HE
                iconRight.setVisibility(VISIBLE);
                iconRight.setImageResource(R.drawable.left_arrow);
                iconLeft.setVisibility(GONE);
                viewableIcon = iconRight;
            }
            if(displayingChildren){
                viewableIcon.setImageResource(R.drawable.down_arrow);
            }
        }


        sectionNameTitle.setText(text);
        /*
        This is for a case of a node that holds just a grid and is a sibling of a sectionName which istextSection()
        Look at Ohr Hashem for an example.
         */
        if(text.length() == 0 && !node.isTextSection())
            sectionNameGroup.setVisibility(View.GONE);

    }

    @Override
    public void setGravity(int gravity) {
        super.setGravity(gravity);
        sectionNameGroup.setGravity(gravity);
        sectionNameTitle.setGravity(gravity);
        ((LinearLayout) findViewById(R.id.section_name_total_root)).setGravity(gravity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.gravity = gravity;
        this.setLayoutParams(params);
    }

    public LinearLayout getChildrenView(){
        return sectionChildren;
    }

    private void setDisplayingChildren(boolean displayingChildren,boolean forceChildren) {
        if (sectionNameGroup.getVisibility() != VISIBLE)
            return;
        this.displayingChildren = displayingChildren;

        for (int i = 0; i < sectionChildren.getChildCount(); i++) {
            View child = sectionChildren.getChildAt(i);
            if (displayingChildren) {
                child.setVisibility(View.VISIBLE);
            } else {
                child.setVisibility(View.GONE);
            }
            if (forceChildren && child instanceof TOCSectionName) {
                ((TOCSectionName) child).setDisplayingChildren(displayingChildren,forceChildren);
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
