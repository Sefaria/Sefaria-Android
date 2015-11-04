package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.text.Html;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.text.Bidi;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                if (needScale(line) && i != layout.getLineCount()-1 && true) {
                    drawScaledText(canvas, lineStart, line, width,lang == Util.HE);
                } else {
                    canvas.drawText(line, 0, mLineY, paint);
                }

                mLineY += getLineHeight();
            }
        } else { //seperate
            oldOnDraw(canvas);
        }
    }

    protected void drawScaledText(Canvas canvas, int lineStart, String line, float lineWidth,boolean isHeb) {
        final Pattern r = Pattern.compile(".[\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]*"); //find any char followed by at least 0 nikudot
        String parenString = "([{";
        String flipParenString = ")]}";

        //line = "(4) [3] {%} " + line;
        Matcher m = r.matcher(line);
        float x = isHeb ? mViewWidth : 0;
        TextPaint tempPaint = getPaint();
        float d;
        if (isHeb) {
            for (int i = 0; i < parenString.length(); i++) {
                line = line.replace(parenString.charAt(i),'`')
                        .replace(flipParenString.charAt(i), parenString.charAt(i))
                        .replace('`', flipParenString.charAt(i));
            }

            Matcher tm = r.matcher(line);
            int matchCount = 0;
            while(tm.find())
                matchCount++;


            d = (mViewWidth - lineWidth) / matchCount;
        } else {
            d = (mViewWidth - lineWidth) / line.length() - 1;
        }

        int i = 0;
        float cw;
        String c;
        while (i < line.length() && (!isHeb || m.find())) {
            if (isHeb) {
                c = line.substring(m.start(), m.end());
                cw = StaticLayout.getDesiredWidth(c, tempPaint);
                canvas.drawText(c, x-cw, mLineY, tempPaint);
                x -= (cw + d);
            } else {
                c = String.valueOf(line.charAt(i));
                cw = StaticLayout.getDesiredWidth(c, tempPaint);
                canvas.drawText(c, x, mLineY, tempPaint);
                x += cw + d;
                i++;
            }
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
        if (lang == Util.BI) setIsCts(false);
        update();

    }

    private void update() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        boolean isFirst = true;
        for (Text text : textList) {
            String words;
            if (lang == Util.EN) words = "(" + text.levels[0] + ") " + text.enText;
            else if (lang == Util.HE) words = "(" + Util.int2heb(text.levels[0]) + ") " + text.heText;
            else { //bilingual
                words = "\n\n(" + Util.int2heb(text.levels[0]) + ") " + text.heText
                + "\n\n(" + text.levels[0] + ") " + text.enText;
            }
            words = isCts && !isFirst ? words : "\n" + words;
            SpannableString ss = new SpannableString(Html.fromHtml(words));
            ss.setSpan(new VerseSpannable(words) ,0,ss.length(), 0);
            ssb.append(ss);
            isFirst = false;
        }
        setText(ssb, TextView.BufferType.SPANNABLE);
        setMovementMethod(LinkMovementMethod.getInstance());
        invalidate();
    }
}
