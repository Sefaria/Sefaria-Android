package org.sefaria.sefaria.menu;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;

/**
 * Created by nss on 9/24/15.
 */
public class MenuSubtitle extends MenuElement {

    private TextView tv;
    private MenuNode node;

    public MenuSubtitle(Context context, MenuNode node, int lang) {
        super(context);
        inflate(context, R.layout.subtitle_menu, this);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f));
        this.node = node;
        this.tv = (TextView) findViewById(R.id.tv);
        setLang(lang);
    }

    public MenuNode getNode() { return node; }

    public void setLang(int lang) {
        tv.setText(node.getPrettyTitle(lang));
        if (lang == Util.HE) {
            tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            tv.setTextSize((getResources().getDimension(R.dimen.subtitle_menu_font_size) * Util.EN_HE_RATIO));
        }
        else if (lang == Util.EN) {
            tv.setTypeface(MyApp.getFont(MyApp.MONTSERRAT_FONT));
            tv.setTextSize(getResources().getDimension(R.dimen.subtitle_menu_font_size));
        }
    }
}
