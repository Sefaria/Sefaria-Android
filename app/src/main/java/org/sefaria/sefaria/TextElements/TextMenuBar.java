package org.sefaria.sefaria.TextElements;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.SefariaTextView;

/**
 * Created by nss on 10/7/15.
 */
public class TextMenuBar extends LinearLayout {

    private SefariaTextView enBtn,heBtn,biBtn;
    private View ctsBtn,sepBtn,sbsBtn,tbBtn,whiteBtn,greyBtn,blackBtn,smallBtn,bigBtn;

    private Context context;

    public TextMenuBar(Context context, OnClickListener btnListener) {
        super(context);
        inflate(context, R.layout.text_menu_bar, this);
        this.context = context;

        enBtn = (SefariaTextView) findViewById(R.id.en_btn);
        heBtn = (SefariaTextView) findViewById(R.id.he_btn);
        biBtn = (SefariaTextView) findViewById(R.id.bi_btn);
        ctsBtn = findViewById(R.id.cts_btn);
        sepBtn = findViewById(R.id.sep_btn);
        sbsBtn = findViewById(R.id.sbs_btn);
        tbBtn = findViewById(R.id.tb_btn);
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
        sbsBtn.setOnClickListener(btnListener);
        tbBtn.setOnClickListener(btnListener);
        whiteBtn.setOnClickListener(btnListener);
        greyBtn.setOnClickListener(btnListener);
        blackBtn.setOnClickListener(btnListener);
        smallBtn.setOnClickListener(btnListener);
        bigBtn.setOnClickListener(btnListener);

        enBtn.setFont(Util.Lang.HE,true);
        biBtn.setFont(Util.Lang.HE, true);
        heBtn.setFont(Util.Lang.HE, true);

    }

    //to make either the mono or bi formatting options visible
    private void setLang(Util.Lang lang) {
        if (lang == Util.Lang.BI) {
            findViewById(R.id.mono_formatting).setVisibility(View.GONE);
            findViewById(R.id.bi_formatting).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.mono_formatting).setVisibility(View.VISIBLE);
            findViewById(R.id.bi_formatting).setVisibility(View.GONE);
        }
    }

    public void setState(Util.Lang lang, boolean isCts, boolean isSideBySide, int colorTheme) {
        //LANG
        int currLangViewId;
        if (lang == Util.Lang.EN) currLangViewId = R.id.en_btn;
        else if (lang == Util.Lang.BI) currLangViewId = R.id.bi_btn;
        else /*if (lang == Util.Lang.HE)*/ currLangViewId = R.id.he_btn;

        if (currLangViewId == R.id.en_btn)
            enBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_left_clicked_drawable));
        else
            enBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_left_ripple_drawable));
        if (currLangViewId == R.id.bi_btn)
            biBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_center_clicked_drawable));
        else
            biBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_center_ripple_drawable));
        if (currLangViewId == R.id.he_btn)
            heBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_right_clicked_drawable));
        else
            heBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_right_ripple_drawable));


        //LINE MONO
        int currLineMonoViewId;
        if (isCts) currLineMonoViewId = R.id.cts_btn;
        else currLineMonoViewId = R.id.sep_btn;

        if (currLineMonoViewId == R.id.cts_btn)
            ctsBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_left_clicked_drawable));
        else
            ctsBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_left_ripple_drawable));
        if (currLineMonoViewId == R.id.sep_btn)
            sepBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_right_clicked_drawable));
        else
            sepBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_right_ripple_drawable));

        //LINE BI
        int currLineBiViewId;
        if (isSideBySide) currLineBiViewId = R.id.sbs_btn;
        else currLineBiViewId = R.id.tb_btn;

        if (currLineBiViewId == R.id.sbs_btn)
            sbsBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_left_clicked_drawable));
        else
            sbsBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_left_ripple_drawable));
        if (currLineBiViewId == R.id.tb_btn)
            tbBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_right_clicked_drawable));
        else
            tbBtn.setBackgroundResource(Util.getDrawable(context,R.attr.text_menu_button_background_right_ripple_drawable));



        //COLOR
        int currColorViewId;
        if (colorTheme == R.style.SefariaTheme_White) currColorViewId = R.id.white_btn;
        else if (colorTheme == R.style.SefariaTheme_Grey) currColorViewId = R.id.grey_btn;
        else /*if (textBG == Util.TextBG.BLACK)*/ currColorViewId = R.id.black_btn;

        if (currColorViewId == R.id.white_btn)
            whiteBtn.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.text_menu_button_background_white_clicked));
        else
            whiteBtn.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.text_menu_button_background_white));
        if (currColorViewId == R.id.grey_btn)
            greyBtn.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.text_menu_button_background_grey_clicked));
        else
            greyBtn.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.text_menu_button_background_grey));
        if (currColorViewId == R.id.black_btn)
            blackBtn.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.text_menu_button_background_black_clicked));
        else
            blackBtn.setBackgroundDrawable(ContextCompat.getDrawable(context,R.drawable.text_menu_button_background_black));


        setLang(lang);
    }
}
