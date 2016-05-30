package org.sefaria.sefaria.TextElements;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.sefaria.sefaria.database.Text;

public class SegmentSpannable extends ClickableSpan {// extend ClickableSpan

    private CharSequence segmentStr;
    private Text segment;
    private OnSegmentSpanClickListener onSegmentSpanClickListener;
    private boolean isActive;

    public SegmentSpannable(CharSequence segmentStr, Text segment, OnSegmentSpanClickListener onSegmentSpanClickListener) {
        super();
        this.segmentStr = segmentStr;
        this.segment = segment;
        this.onSegmentSpanClickListener = onSegmentSpanClickListener;
        this.isActive = false;
    }

    public void onClick(View tv) {
        //setActive(!isActive);
        //onSegmentSpanClickListener.onSegmentClick((TextView) tv, this);
    }

    private void setActive(boolean isActive) {
        this.isActive = isActive;
        TextPaint tp = new TextPaint();
        if (isActive) {
            tp.bgColor = Color.parseColor("#999999");
        } else {
            tp.bgColor = Color.parseColor("#FFFFFF");
        }
        updateDrawState(tp);
    }

    public void updateDrawState(TextPaint ds) {// override updateDrawState
        //ds.setColor(color);
        //ds.bgColor = background;
        //ds.setUnderlineText(false); // set to false to remove underline
    }

    public Text getSegment() {
        return segment;
    }

}


