package org.sefaria.sefaria.TOCElements;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.TextActivity;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.menu.MenuNode;
import org.sefaria.sefaria.menu.MenuState;

/**
 *
 */
public class TOCNumBox extends LinearLayout implements TOCElement {

    private int number;
    private TextView box;
    private Context context;
    private Node node;

    public  TOCNumBox(Context context){
        super(context);
        this.context = context;
        this.setVisibility(View.INVISIBLE);
    }

    public TOCNumBox(Context context, int number, Node node, Util.Lang lang){
        super(context);
        inflate(context, R.layout.toc_chapnumbox, this);

        this.number = number;
        this.node = node;
        this.context = context;
        init(lang);

    }

    private void init(Util.Lang lang){
        box = (TextView) findViewById(R.id.toc_boxitem);
        setLang(lang);

        this.setOnClickListener(clickListener);
    }

    @Override
    public void setLang(Util.Lang lang) {
        if(Util.Lang.HE == lang)
            box.setText(Util.int2heb(number));
        else
            box.setText(""+ number);
    }



    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //TODO go to intent of text page
            Log.d("toc", "go to:" +node +  number);

            Intent intent = new Intent(context, TextActivity.class);
            //intent.putExtra("menuState", newMenuState);
            //context.startActivity(intent);


        }
    };
}
