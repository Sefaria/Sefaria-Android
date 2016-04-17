package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.SefariaTextView;

/**
 * Created by nss on 4/11/16.
 */
public class HomeActionbar extends LinearLayout {

    public HomeActionbar(Context context, Util.Lang lang, OnClickListener searchClick, OnClickListener langClick) {
        super(context);
        inflate(context, R.layout.home_actionbar, this);

        SefariaTextView tv = (SefariaTextView) findViewById(R.id.search_tv);
        tv.setFont(Util.Lang.EN, true);
        tv.setOnClickListener(searchClick);
        findViewById(R.id.search_btn).setOnClickListener(searchClick);
        findViewById(R.id.lang_btn).setOnClickListener(langClick);


        setLang(lang);

    }

    public void setLang(Util.Lang lang) {
        SefariaTextView tv = (SefariaTextView) findViewById(R.id.langTV);
        tv.setFont(lang,true);
        if (lang == Util.Lang.HE) tv.setText("A");
        else /* if (lang == Util.Lang.EN) */ tv.setText("א");
    }
}
