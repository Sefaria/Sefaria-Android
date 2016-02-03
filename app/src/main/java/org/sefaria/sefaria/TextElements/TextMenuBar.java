package org.sefaria.sefaria.TextElements;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

/**
 * Created by nss on 10/7/15.
 */
public class TextMenuBar extends LinearLayout {

    private TextView enBtn,heBtn,biBtn;
    private View ctsBtn,sepBtn,whiteBtn,greyBtn,blackBtn,smallBtn,bigBtn;
    private TextView[] langBtns = {enBtn,heBtn,biBtn};
    private View[] lineBtns = {ctsBtn, sepBtn};
    private View[] colorBtns = {whiteBtn,greyBtn,blackBtn};
    private View[] fontBtns = {smallBtn, bigBtn};

    private Context context;

    public TextMenuBar(Context context, OnClickListener btnListener) {
        super(context);
        inflate(context, R.layout.text_menu_bar, this);
        this.context = context;

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

    public void setState(Util.Lang lang, boolean isCts, Util.TextBG textBG) {
        //LANG
        int currLangViewId;
        if (lang == Util.Lang.EN) currLangViewId = R.id.en_btn;
        else if (lang == Util.Lang.BI) currLangViewId = R.id.bi_btn;
        else /*if (lang == Util.Lang.HE)*/ currLangViewId = R.id.he_btn;

        if (currLangViewId == R.id.en_btn)
           enBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_left_clicked));
        else
            enBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_left));
        if (currLangViewId == R.id.bi_btn)
            biBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_center_clicked));
        else
            biBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_center));
        if (currLangViewId == R.id.he_btn)
            heBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_right_clicked));
        else
            heBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_right));


        //LINE
        int currLineViewId;
        if (isCts) currLineViewId = R.id.cts_btn;
        else currLineViewId = R.id.sep_btn;

        if (currLineViewId == R.id.cts_btn)
            ctsBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_left_clicked));
        else
            ctsBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_left));
        if (currLineViewId == R.id.sep_btn)
            sepBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_right_clicked));
        else
            sepBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_right_ripple));



        //COLOR
        int currColorViewId;
        if (textBG == Util.TextBG.WHITE) currColorViewId = R.id.white_btn;
        else if (textBG == Util.TextBG.GREY) currColorViewId = R.id.grey_btn;
        else /*if (textBG == Util.TextBG.BLACK)*/ currColorViewId = R.id.black_btn;

        if (currColorViewId == R.id.white_btn)
            whiteBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_white_clicked));
        else
            whiteBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_white));
        if (currColorViewId == R.id.grey_btn)
            greyBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_grey_clicked));
        else
            greyBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_grey));
        if (currColorViewId == R.id.black_btn)
            blackBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_black_clicked));
        else
            blackBtn.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.text_menu_button_background_black));


    }
}
