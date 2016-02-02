package org.sefaria.sefaria.TextElements;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;

/**
 * Created by nss on 10/7/15.
 */
public class TextMenuBar extends LinearLayout {

    private TextView enBtn,heBtn,biBtn;
    private View ctsBtn,sepBtn,whiteBtn,greyBtn,blackBtn,smallBtn,bigBtn;

    public TextMenuBar(Context context, OnClickListener btnListener) {
        super(context);
        inflate(context, R.layout.text_menu_bar, this);

        enBtn = (TextView) findViewById(R.id.en_btn);
        heBtn = (TextView) findViewById(R.id.he_btn);
        biBtn = (TextView) findViewById(R.id.bi_btn);
        ctsBtn = findViewById(R.id.cts_btn);
        sepBtn = findViewById(R.id.sep_btn);
        whiteBtn = findViewById(R.id.white_btn);
        greyBtn = findViewById(R.id.grey_btn);
        blackBtn = findViewById(R.id.black_btn);
        smallBtn = findViewById(R.id.small_btn);
        bigBtn = findViewById(R.id.big_btn);

        enBtn.setOnClickListener(btnListener);
        heBtn.setOnClickListener(btnListener);
        biBtn.setOnClickListener(btnListener);
        ctsBtn.setOnClickListener(btnListener);
        sepBtn.setOnClickListener(btnListener);
        whiteBtn.setOnClickListener(btnListener);
        greyBtn.setOnClickListener(btnListener);
        blackBtn.setOnClickListener(btnListener);
        smallBtn.setOnClickListener(btnListener);
        bigBtn.setOnClickListener(btnListener);

        enBtn.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        biBtn.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        heBtn.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));

    }
}
