package org.sefaria.sefaria;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

class VerseSpannable extends ClickableSpan{// extend ClickableSpan

    private String clicked;
    private int color;
    private int background;
    public VerseSpannable(String string,int color, int background) {
        super();
        clicked = string;
        this.color = color;
        this.background = background;
    }

    public void onClick(View tv) {
        Log.d("text", "CLICK " + clicked) ;
    }

    public void updateDrawState(TextPaint ds) {// override updateDrawState
        ds.setColor(color);
        ds.bgColor = background;
        ds.setUnderlineText(false); // set to false to remove underline
    }
}
