package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.Util;

/**
 * Created by nss on 2/17/16.
 */
public class SefariaTextView extends TextView {

    public SefariaTextView(Context context) {
        super(context);
    }

    public SefariaTextView(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
    }

    public void setFont(Util.Lang lang, boolean isSerif) {
        MyApp.Font font;
        if (lang == Util.Lang.HE) {
            if (isSerif) font = MyApp.Font.TAAMEY_FRANK;
            else font = MyApp.Font.OPEN_SANS_HE;
        } else {
            if (isSerif) font = MyApp.Font.GARAMOND;
            else font = MyApp.Font.OPEN_SANS_EN;
        }
        setTypeface(MyApp.getFont(font));
    }


}
