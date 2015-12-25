package org.sefaria.sefaria.TOCElements;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.menu.MenuNode;

/**
 *
 */
public class TOCNumBox extends LinearLayout implements TOCElement {

    private int number;
    private TextView box;
    private Context context;

    public  TOCNumBox(Context context){
        super(context);
        this.context = context;

        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
        this.setVisibility(View.INVISIBLE);
    }

    public TOCNumBox(Context context, int number, Util.Lang lang){
        super(context);
        inflate(context, R.layout.toc_chapnumbox, this);

        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));

        this.number = number;
        init(lang);

    }

    private void init(Util.Lang lang){
        box = (TextView) findViewById(R.id.toc_boxitem);
        setLang(lang);

        this.setOnClickListener(blah);
    }

    @Override
    public void setLang(Util.Lang lang) {
        box.setText(""+ number);
        ;
    }



    OnClickListener blah = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //todo go to intent of text page
            Log.d("toc", "YOYOYOY" + number);


        }
    };
}
