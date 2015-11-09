package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.menu.MenuNode;

/**
 * Created by nss on 11/4/15.
 */
public class TextChapterHeader extends LinearLayout {

    private int lang;
    private TextView tv;
    public MenuNode node;

    public TextChapterHeader(Context context, MenuNode node, int lang, float textSize) {
        super(context);
        inflate(context, R.layout.text_chapter_header, this);
        this.node = node;
        this.lang = lang;
        tv = (TextView) findViewById(R.id.tv);

        setLang(lang);
        setTextSize(textSize);

    }

    public void setLang(int lang) {
        tv.setText(node.getPrettyTitle(lang));
        if (lang == Util.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //tv.setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
        }
        else {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            //tv.setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
    }

    public void setTextSize(float textSize) {
        tv.setTextSize(textSize);
    }
}
