package org.sefaria.sefaria.TextElements;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.menu.MenuNode;

/**
 * Created by nss on 11/4/15.
 */
public class TextChapterHeader extends LinearLayout {

    private Util.Lang lang;
    private TextView tv;
    public String enText;
    public String heText;

    public TextChapterHeader(Context context, Text segment, Util.Lang lang, float textSize) {
        super(context);
        enText = segment.enText;
        heText = segment.heText;
        init(context,lang,textSize);
    }

    public TextChapterHeader(Context context, AttributeSet attrSet) {
        super(context, attrSet);
        enText = "Section";
        heText = "Section";
        init(context,Util.Lang.HE,0f);
    }

    public TextChapterHeader(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        enText = "Section";
        heText = "Section";
        init(context,Util.Lang.HE,0f);
    }

    private void init(Context context,Util.Lang lang, float textSize) {
        inflate(context, R.layout.text_chapter_header, this);
        tv = (TextView) findViewById(R.id.tv);
        this.lang = lang;

        setLang(lang);
        setTextSize(textSize);
    }

    public void setLang(Util.Lang lang) {

        if (lang == Util.Lang.HE) {
            tv.setText(heText);
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //tv.setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
        }
        else {
            tv.setText(enText);
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            //tv.setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
    }

    public void setSectionTitle(Text segment) {
        enText = segment.enText;
        heText = segment.heText;
        setLang(lang);
    }

    public void setTextSize(float textSize) {
        tv.setTextSize(textSize);
    }
}
