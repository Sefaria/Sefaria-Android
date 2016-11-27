package org.sefaria.sefaria.TextElements;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import org.sefaria.sefaria.database.Segment;

public class SegmentSpannable extends ClickableSpan {// extend ClickableSpan

    private CharSequence segmentStr;
    private Segment segment;
    private OnSegmentSpanClickListener onSegmentSpanClickListener;
    private boolean isActive;

    public SegmentSpannable(CharSequence segmentStr, Segment segment, OnSegmentSpanClickListener onSegmentSpanClickListener) {
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

    public Segment getSegment() {
        return segment;
    }

}


