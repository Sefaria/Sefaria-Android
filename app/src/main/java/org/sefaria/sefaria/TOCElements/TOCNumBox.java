package org.sefaria.sefaria.TOCElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

import android.support.v7.widget.GridLayout;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SectionActivity;
import org.sefaria.sefaria.database.Node;

/**
 *
 */
public class TOCNumBox extends TextView implements TOCElement {

    private Context context;
    private Node node;
    private Util.Lang lang;

    public  TOCNumBox(Context context){
        super(context);
        this.context = context;
        this.setVisibility(View.INVISIBLE);
    }

    public TOCNumBox(Context context, Node node, Util.Lang lang){
        super(context);

        //FORMATTING
        Resources resources = getResources();
        setBackgroundDrawable(resources.getDrawable(R.drawable.button_ripple_rect_white));
        setTextColor(resources.getColor(R.color.toc_num_box_font));

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(new ViewGroup.MarginLayoutParams(
                (int) resources.getDimension(R.dimen.toc_numbox),(int) resources.getDimension(R.dimen.toc_numbox)));
        int margin = 3;
        lp.setMargins(margin,margin,margin,margin);
        setLayoutParams(lp);

        //setWidth((int) r.getDimension(R.dimen.toc_numbox));
        //setHeight((int) r.getDimension(R.dimen.toc_numbox));

        setPadding(1, 1, 1, 1);
        setTextSize(10);
        setGravity(Gravity.CENTER);

        this.node = node;
        this.context = context;
        this.lang = lang;
        init(lang);

    }

    private void init(Util.Lang lang){
        setLang(lang);

        this.setOnClickListener(clickListener);
    }

    @Override
    public void setLang(Util.Lang lang) {
        setText(node.getNiceGridNum(lang));
    }



    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if(!node.isTextSection()) {
                return;
            }
            gotoTextActivity(context,node,lang);
        }
    };

    /**
     * go from TOC to textActivity
     *
     * @param context
     * @param node
     * @param lang
     */
    public static void gotoTextActivity(Context context,Node node,Util.Lang lang){
        Node.saveNode(node);
        Intent intent = new Intent(context, SectionActivity.class);
        intent.putExtra("nodeHash", node.hashCode());
        intent.putExtra("lang", lang);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //TODO determine if SectionActivity was already open... Make sure to be careful of multi-tab stuff
        //TODO I think it should also actually have the back button work for going to the TOC from textActivity
        context.startActivity(intent);


        //Activity act = (Activity) context; //stupid casting
        //act.setResult(Activity.RESULT_OK,intent);
        //act.finish();//close the TOC
    }
}
