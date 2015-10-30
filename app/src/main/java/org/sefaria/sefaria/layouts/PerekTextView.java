package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.text.Bidi;
import java.util.List;

/**
 * Created by nss on 10/28/15.
 */
public class PerekTextView extends JustifyTextView {

    private boolean isCts; //is text continuous or seperated by passuk
    private int lang;
    private int mLineY;
    private int mViewWidth;
    private List<Text> textList;

    public PerekTextView (Context context, List<Text> textList) {
        super(context);
        lang = Util.EN;
        this.textList = textList;
        setIsCts(true);
        setLang(lang);

    }
    public PerekTextView (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TextPaint paint = getPaint();
        paint.setColor(getCurrentTextColor());
        paint.drawableState = getDrawableState();
        mViewWidth = getMeasuredWidth();
        String text = (String) getText();
        mLineY = 0;
        mLineY += getTextSize();
        Layout layout = getLayout();

        if (isCts) {
            for (int i = 0; i < layout.getLineCount(); i++) {
                int lineStart = layout.getLineStart(i);
                int lineEnd = layout.getLineEnd(i);
                String line = text.substring(lineStart, lineEnd);
                float width = StaticLayout.getDesiredWidth(text, lineStart, lineEnd, getPaint());
                if (needScale(line) && i != layout.getLineCount()-1 ) {
                    drawScaledText(canvas, lineStart, line, width);

                } else {
                    canvas.drawText(line, 0, mLineY, paint);
                }

                mLineY += getLineHeight();
            }
        } else { //seperate
            oldOnDraw(canvas);
            //super.onDraw(canvas);
        }
    }

    protected void drawScaledText(Canvas canvas, int lineStart, String line, float lineWidth) {
        float x = 0;

        float d = (mViewWidth - lineWidth) / line.length() - 1;
        for (int i = 0; i < line.length(); i++) {
            String c = String.valueOf(line.charAt(i));
            float cw = StaticLayout.getDesiredWidth(c, getPaint());
            canvas.drawText(c, x, mLineY, getPaint());
            x += cw + d;
        }
    }

    protected boolean needScale(String line) {
        if (line.length() == 0) {
            return false;
        } else {
            return line.charAt(line.length() - 1) != '\n';
        }
    }

    @Override
    public CharSequence getText() {
        return super.getText().toString();
    }
    public void setIsCts(boolean isCts) {
        this.isCts = isCts;
        update();
    }

    public void setLang(int lang) {
        this.lang = lang;
        update();

    }

    private void update() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        boolean isFirst = true;
        for (Text text : textList) {
            String words;
            if (lang == Util.EN) words = "(" + text.levels[0] + ") " + text.enText;
            else words = "(" + Util.int2heb(text.levels[0]) + ") " + text.heText;

            words = isCts && !isFirst ? words : "\n" + words;
            SpannableString ss = new SpannableString(words);
            ss.setSpan(new VerseSpannable(words) ,0,ss.length(), 0);
            ssb.append(ss);
            isFirst = false;
        }
        setText(ssb, TextView.BufferType.SPANNABLE);
        setMovementMethod(LinkMovementMethod.getInstance());
        invalidate();
    }
}
