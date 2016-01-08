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
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;

public class TOCCommentary extends LinearLayout implements TOCElement {

    private TextView content_root;
    private Context context;
    private Book commentary;
    private Book mainBook;
    private Util.Lang lang;

    public TOCCommentary(Context context, Book commentary, Book mainBook, Util.Lang lang){
        super(context);
        Log.d("TOCCommentary", "TOCCommentary starting");
        inflate(context, R.layout.toc_commentary, this);
        this.setOrientation(VERTICAL);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        this.mainBook = mainBook;
        this.lang = lang;
        this.commentary = commentary;
        this.context = context;
        init(lang);
    }

    private void init(Util.Lang lang){
        content_root = (TextView) findViewById(R.id.content_root);
        content_root.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));

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
        String text = commentary.getTitle(lang).replace(" on " + mainBook.title, "").replace(" על " + mainBook.heTitle, "");
        content_root.setText(text);

    }

    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //go to text page
            SuperTextActivity.startNewTextActivityIntent(context,commentary,lang);

        }
    };
}
