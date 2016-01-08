package org.sefaria.sefaria.TOCElements;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;

public class TOCommentary extends LinearLayout implements TOCElement {

    private TextView sectionroot;
    private Context context;
    private Book commentary;
    private Util.Lang lang;

    public TOCommentary(Context context, Book commentary, Util.Lang lang){
        super(context);
        inflate(context, R.layout.toc_sectionname, this);
        this.setOrientation(VERTICAL);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        this.lang = lang;
        this.commentary = commentary;
        this.context = context;
        init(lang);
    }

    private void init(Util.Lang lang){
        sectionroot = (TextView) findViewById(R.id.toc_sectionroot);
        sectionroot.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));

        setLang(lang);
        this.setOnClickListener(clickListener);
    }

    @Override
    public void setLang(Util.Lang lang) {

        final int padding = 12;
        final int sidePadding = 50;
        if(lang == Util.Lang.EN) {
            this.setPadding(sidePadding, padding, 0, padding);
            this.setGravity(Gravity.LEFT);
        }else {
            this.setPadding(0, padding, sidePadding, padding);
            this.setGravity(Gravity.RIGHT);
        }
        String text = commentary.getTitle(lang);
        sectionroot.setText(text);

    }

    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
        //TODO go to intent of text page

        }
    };
}
