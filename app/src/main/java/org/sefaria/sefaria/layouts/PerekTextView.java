package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
import android.view.ViewTreeObserver;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.TextElements.VerseSpannable;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by nss on 10/28/15.
 */
public class PerekTextView extends JustifyTextView {

    public static final int EXTRA_LOAD_LINES = 30;

    private boolean isCts; //is text continuous or seperated by passuk
    private Util.Lang lang;
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

    public PerekTextView (Context context, List<Text> textList,boolean isCts,Util.Lang lang, float textSize, int scrollY) {
        super(context);
        this.textList = textList;
        this.scrollY = scrollY;
        this.inited = false;

        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateLayoutParams(true);
                if (!inited) {

                    updateScroll(0, 1000);
                    inited = true;
                } else {
                    noahDraw();
                }
            }
        });
        setTextColor(Color.parseColor("#000000"));
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
        if (/*drawText != null && drawText != ""*/ true ) {
            //Log.d("text","DRAW");
            for (int i = firstDrawnLine; i < lastDrawnLine; i++) {
                int textStart = layout.getLineStart(i);
                int textEnd = layout.getLineEnd(i);
                String tempLine = text.substring(textStart, textEnd);
                boolean hasHeb = Util.hasHebrew(tempLine);
                //if (hasHeb) Log.d("text","HAS HEBREW");

                float offset = mViewWidth - StaticLayout.getDesiredWidth(tempLine, paint); //TODO less random number
                float startX = hasHeb ? offset : 0f;
                canvas.drawText(tempLine, startX, layout.getLineBottom(i), paint);


            }

            //https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
            /*StaticLayout mTextLayout = new StaticLayout(Html.fromHtml(drawText), paint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            float offset = scrollY % lineHeight;

            canvas.save();
            //offset = currScreenTop % lineHeight
            canvas.translate(0, startY);//-offset);
            mTextLayout.draw(canvas);
            canvas.restore();*/
        }
    }

    private void noahDraw() {
        updateVisibleLines();
        invalidate();
        draw(new Canvas());
        //AsyncDraw asyncDraw = new AsyncDraw(this);
        //asyncDraw.execute();
    }

    @Override
    public CharSequence getText() {
        return super.getText().toString();
    }
    public void setIsCts(boolean isCts) {
        this.isCts = isCts;
    }

    public void setLang(Util.Lang lang) {
        this.lang = lang;
        if (lang == Util.Lang.HE) {
            //setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            //setTextSize((getResources().getDimension(R.dimen.button_menu_font_size) * Util.EN_HE_RATIO));
            //setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        }
        else if (lang == Util.Lang.EN){
            //setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT)); //actually, currently there's no nafka mina
            //setTextSize(getResources().getDimension(R.dimen.button_menu_font_size));
        } else { //bilingual
            //setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            setIsCts(false);
        }

    }

    @Override
    public void setTextSize(float size) {
        Log.d("textSize","START");
        super.setTextSize(size);
        Log.d("textSize","UPDATED TEXT SIZE");
        updateLayoutParams(false);
        Log.d("textSize","UPDATED LAYOUT PARAMS");
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
        AsyncLoadText alt = new AsyncLoadText();
        alt.execute();


    }

    private void updateLayoutParams(boolean updateText) {
        layout = getLayout();
        if (layout == null) return;

        //this is expensive
        if (updateText) {

            relativeTop = Util.getRelativeTop(PerekTextView.this);
            paint = getPaint();
            paint.setColor(getCurrentTextColor());
            paint.drawableState = getDrawableState();
            text = (String) getText();
            lineCount = layout.getLineCount();
        }
        Log.d("updateLayoutParams","START");
        mViewWidth = getMeasuredWidth();
        this.textSize = getTextSize();
        Log.d("updateLayoutParams","GOT TEXT SIZE");
        lineHeight = getLineHeight(); //(int)Math.round(getLineHeight() - getLineHeight()/12);
    }

    public class AsyncLoadText extends AsyncTask<Void, Void, String> {

        private static final int TEXT_INCREMENT = 50;
        private SpannableStringBuilder ssb;
        private int currIndex;
        private boolean isFirst;
        private boolean isFirstUpdate;

        public AsyncLoadText() {
            super();
            this.ssb = new SpannableStringBuilder();
            this.currIndex = 0;
            this.isFirst = true;
            this.isFirstUpdate = true;
            setMovementMethod(LinkMovementMethod.getInstance());
        }

        @Override
        protected void onPreExecute() {
            Log.d("async","STARTED");
        }

        @Override
        protected String doInBackground(Void... params) {
            while (currIndex < textList.size()) {
                for (int i = currIndex; i < currIndex + TEXT_INCREMENT && i < textList.size(); i++) {
                    Text text = textList.get(i);
                    String words;
                    if (lang == Util.Lang.EN) words = "(" + text.levels[0] + ") " + text.enText;
                    else if (lang == Util.Lang.HE)
                        words = "(" + Util.int2heb(text.levels[0]) + ") " + text.heText;
                    else { //bilingual
                        words = "(" + Util.int2heb(text.levels[0]) + ") " + text.heText
                                + "<br><br>(" + text.levels[0] + ") " + text.enText;
                    }
                    if (!isFirst)
                        words = isCts ? " " + words : "<br><br>" + words;


                    SpannableString ss = new SpannableString(Html.fromHtml(words));
                    ss.setSpan(new VerseSpannable(words), 0, ss.length(), 0);
                    ssb.append(ss);
                    isFirst = false;
                }

                currIndex += TEXT_INCREMENT;
                publishProgress();
            }
            return "";
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            if (isFirstUpdate) {
                Log.d("async","FIRST");
                setText(ssb, TextView.BufferType.SPANNABLE);
                isFirstUpdate = false;
            }


        }

        @Override
        protected void onPostExecute(String s) {
            Log.d("aysnc","DONE");
            setText(ssb, TextView.BufferType.SPANNABLE);
            updateLayoutParams(true);
            noahDraw();
            Log.d("async","REALLY DONE");

        }

    }

    /*
    //this class draws justified text (kinda) and draws only the text on the screen (kinda)
    public class AsyncDraw extends AsyncTask<Void, Void, String> {

        //public String drawText;
        public int startY;
        private final WeakReference<PerekTextView> ptvRef;

        public AsyncDraw(PerekTextView ptv) {
            super();
            this.ptvRef = new WeakReference<>(ptv);
            //Log.d("text","NOAH DRAW");
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
                    if (isCts && false) {//justified  //TODO make this work
                        int lineStart = layout.getLineStart(i);
                        int lineEnd = layout.getLineEnd(i);
                        String line = text.substring(lineStart, lineEnd);
                        float width = StaticLayout.getDesiredWidth(line, paint);
                        if (needScale(line) && i != lineCount-1) {

                            //drawScaledText(canvas, line, width, lang == Util.HE);
                        } else {
                            float startX = lang == Util.Lang.HE ? mViewWidth-width : 0;
                            //canvas.drawText(line,o,mLineY,paint);
                        }
                    } else { //not justified
                        int textStart = layout.getLineStart(i);
                        int textEnd = layout.getLineEnd(lastDrawnLine);
                        drawText = text.substring(textStart, textEnd);
                        startY = layout.getLineTop(i);


                        break; //you've gotten the text. get out
                    }
                } else {
                    //canvas.drawText("EMPTY", 0, mLineY, paint);
                }

                //TODO figure out less random number
                //mLineY += (int)Math.round(lineHeight - lineHeight/13);
                mLineY += lineHeight;
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

    }*/
}
