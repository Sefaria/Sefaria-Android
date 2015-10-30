package org.sefaria.sefaria.layouts;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

public class VerseSpannable extends ClickableSpan{// extend ClickableSpan

    private String clicked;
    public VerseSpannable(String string) {
        super();
        clicked = string;
    }

    public void onClick(View tv) {
        Log.d("text", "CLICK " + clicked) ;
    }

    public void updateDrawState(TextPaint ds) {// override updateDrawState
        //ds.setColor(color);
        //ds.bgColor = background;
        ds.setUnderlineText(false); // set to false to remove underline
    }
}
