package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.os.AsyncTask;
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
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.lang.ref.WeakReference;
import java.text.Bidi;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nss on 10/28/15.
 */
public class PerekTextView extends JustifyTextView {

    public static final int EXTRA_LOAD_LINES = 30;

    private boolean isCts; //is text continuous or seperated by passuk
    private int lang;
    private int mLineY;
    private int mViewWidth;
    private List<Text> textList;

    //dynamic drawing! (please work...)
    private int scrollY;
    private int scrollH;
    private int relativeTop;
    private int firstDrawnLine;
    private int lastDrawnLine;
    private int lineHeight;
    private TextPaint paint;
    private String text;
    private float textSize;
    private int lineCount;
    private Layout layout;
    private final Pattern r = Pattern.compile(".[\u0591-\u05BD\u05BF\u05C1\u05C2\u05C4\u05C5\u05C7]*"); //find any char followed by at least 0 nikudot
    private Matcher m;
    private String parenString = "([{)]}";
    private String flipParenString = ")]}([{";
    private String drawText;
    private int startY;

    private boolean inited;

    public PerekTextView (Context context, List<Text> textList,boolean isCts,int lang, float textSize, int scrollY) {
        super(context);
        this.textList = textList;
        this.scrollY = scrollY;
        this.inited = false;

        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateLayoutParams();
                if (!inited) {

                    updateScroll(0, 1000);
                    inited = true;
                } else {
                    noahDraw();
                }
            }
        });
        setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        setIsCts(isCts);
        setLang(lang);
        setTextSize(textSize);
        update();
    }
    public PerekTextView (Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //oldOnDraw(canvas);
        if (drawText != null && drawText != "") {
            //canvas.drawText(drawText, 0, 100, paint);

            //https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
            StaticLayout mTextLayout = new StaticLayout(drawText, paint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            canvas.save();
            canvas.translate(0, startY);
            mTextLayout.draw(canvas);
            canvas.restore();
        }
    }

    private void noahDraw() {
        AsyncDraw asyncDraw = new AsyncDraw(this);
        asyncDraw.execute();
    }

    @Override
    public CharSequence getText() {
        return super.getText().toString();
    }
    public void setIsCts(boolean isCts) {
        this.isCts = isCts;
    }

    public void setLang(int lang) {
        this.lang = lang;
        if (lang == Util.HE) {
            //setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
            //setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
        else if (lang == Util.EN){
            //setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            //setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        } else { //bilingual
            //setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            setIsCts(false);
        }

    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        updateLayoutParams();
    }

    public int getFirstDrawnLine() {
        return firstDrawnLine;
    }

    public int getLastDrawnLine() {
        return lastDrawnLine;
    }

    public int getNewFirstDrawnLine(int newScrollY) {
        int min = newScrollY - relativeTop;
        try {
            return layout.getLineForVertical(min);
        } catch (NullPointerException e) {
            throw e;
        }
    }

    public int getNewLastDrawnLine(int newScrollY) {
        int min = newScrollY - relativeTop;
        try {
            return layout.getLineForVertical(min + scrollH);
        } catch (NullPointerException e) {
            throw e;
        }
    }

    public void setRelativeTop(int newTop) {
        this.relativeTop = newTop;
    }
    public void updateScroll(int scrollY,int scrollH) {
        this.scrollY = scrollY;
        this.scrollH = scrollH;
        updateVisibleLines();
        noahDraw();
    }

    private void updateVisibleLines() {
        try {
            int min = this.scrollY - relativeTop;
            if (min < 0) min = 0;
            else if (min > getHeight() - scrollH) min = getHeight() - scrollH;
            this.firstDrawnLine = layout.getLineForVertical(min) - EXTRA_LOAD_LINES;
            if (firstDrawnLine < 0) firstDrawnLine = 0;
            this.lastDrawnLine = layout.getLineForVertical(min + scrollH) + EXTRA_LOAD_LINES;
            if (lastDrawnLine > lineCount - 1) lastDrawnLine = lineCount - 1;
        } catch (NullPointerException e) {
            return;
        }
    }

    public void update() {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        boolean isFirst = true;
        for (Text text : textList) {
            String words;
            if (lang == Util.EN) words = "(" + text.levels[0] + ") " + text.enText;
            else if (lang == Util.HE) words = "(" + Util.int2heb(text.levels[0]) + ") " + text.heText;
            else { //bilingual
                words = "(" + Util.int2heb(text.levels[0]) + ") " + text.heText
                + "\n\n(" + text.levels[0] + ") " + text.enText;
            }
            if (!isFirst)
                words = isCts ? " " + words : "\n\n" + words;

            SpannableString ss = new SpannableString(words);
            ss.setSpan(new VerseSpannable(words), 0, ss.length(), 0);
            ssb.append(ss);
            isFirst = false;
        }
        setText(ssb, TextView.BufferType.SPANNABLE);
        setMovementMethod(LinkMovementMethod.getInstance());

        //text props
        updateLayoutParams();
        noahDraw();
    }

    private void updateLayoutParams() {
        try {
            layout = getLayout();
            relativeTop = Util.getRelativeTop(PerekTextView.this);
            paint = getPaint();
            paint.setColor(getCurrentTextColor());
            paint.drawableState = getDrawableState();
            text = (String) getText();

            mViewWidth = getMeasuredWidth();
            this.textSize = getTextSize();
            lineCount = layout.getLineCount();
            lineHeight = getLineHeight();
        } catch (NullPointerException e) {
            return; //layout was probably null. :(
        }
    }

    public class AsyncDraw extends AsyncTask<Void, Void, String> {

        //public String drawText;
        public int startY;
        private final WeakReference<PerekTextView> ptvRef;

        public AsyncDraw(PerekTextView ptv) {
            super();
            this.ptvRef = new WeakReference<>(ptv);
        }

        @Override
        protected void onPreExecute() {
            // Runs on UI thread
            //drawText = "";
            //updateLayoutParams();
            updateVisibleLines();
        }

        @Override
        protected String doInBackground(Void... params) {
            mLineY = 0;
            String drawText = "";
            for (int i = 0; i < lineCount; i++) {
                if (i >= firstDrawnLine && i < lastDrawnLine) {
                    if (isCts && false) {  //TODO make this work
                        int lineStart = layout.getLineStart(i);
                        int lineEnd = layout.getLineEnd(i);
                        String line = text.substring(lineStart, lineEnd);
                        float width = StaticLayout.getDesiredWidth(line, paint);
                        if (needScale(line) && i != lineCount-1) {

                            //drawScaledText(canvas, line, width, lang == Util.HE);
                        } else {
                            float startX = lang == Util.HE ? mViewWidth-width : 0;
                            //canvas.drawText(line,startX,mLineY,paint);
                        }
                    } else { //seperated

                        int textStart = layout.getLineStart(i);
                            int textEnd = layout.getLineEnd(lastDrawnLine);
                            drawText = text.substring(textStart, textEnd);
                            startY = mLineY;

                            break; //you've gotten the text. get out
                    }
                } else {
                    //canvas.drawText("EMPTY", 0, mLineY, paint);
                }

                //TODO figure out less random number
                mLineY += (int)Math.round(lineHeight - lineHeight/13);
            }
            if (drawText == null) drawText = "";
            return drawText;
        }

        @Override
        protected void onPostExecute(String drawText) {
            invalidate();
            PerekTextView ptv = ptvRef.get();
            ptv.drawText = drawText;
            ptv.startY = startY;
            ptv.draw(new Canvas());
        }
        protected void drawScaledText(Canvas canvas, String line, float lineWidth,boolean isHeb) {

            //line = "(4) [3] {%} " + line;

            float x = isHeb ? mViewWidth : 0;
            float d;
            if (isHeb) {
                m = r.matcher(line);
                int matchCount = 0;
                while(m.find())
                    matchCount++;

                //TODO this needs to be checked
                m.reset();


                d = (mViewWidth - lineWidth) / matchCount;
            } else {
                d = (mViewWidth - lineWidth) / (line.length() - 1);
            }

            int i = 0;
            float cw;
            String c;
            while (i < line.length() && (!isHeb || m.find())) {
                if (isHeb) {
                    c = line.substring(m.start(), m.end());
                    boolean isparen = false;
                    int count = 0;
                    while (!isparen && count < parenString.length()) {
                        if (c.equals(String.valueOf(parenString.charAt(count)))) {
                            c = String.valueOf(flipParenString.charAt(count));
                            isparen = true;
                        }
                        count++;
                    }
                    cw = StaticLayout.getDesiredWidth(c, paint);
                    //canvas.drawText(c, x-cw, mLineY, paint);
                    x -= (cw + d);
                } else {
                    c = String.valueOf(line.charAt(i));
                    cw = StaticLayout.getDesiredWidth(c, paint);
                    //canvas.drawText(c, x, mLineY, paint);
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

    }
}
