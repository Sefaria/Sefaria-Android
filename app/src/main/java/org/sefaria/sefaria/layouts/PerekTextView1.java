/*package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;

import org.sefaria.sefaria.TextElements.SegmentSpannable;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PerekTextView1 extends JustifyTextView {

    public static final int EXTRA_LOAD_LINES = 5;

    private boolean isCts; //is text continuous or separated by passuk
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

    private List<String[]> characters;
    private List<float[]> charWidths;

    private boolean inited;
    private boolean isPrev; //true when chapter tv has been loaded as a previous tv
    private boolean hasBeenDrawn; //goes along with isPrev

    public PerekTextView1(Context context, List<Text> textList, boolean isCts, Util.Lang lang, float textSize, int scrollY, boolean isPrev) {
        super(context);
        this.textList = textList;
        this.scrollY = scrollY;
        this.inited = false;
        this.isPrev = isPrev;
        this.hasBeenDrawn = false;

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
        setFont(lang,true);
        setIsCts(isCts);
        setLang(lang);
        setTextSize(textSize);
        update();
    }
    public PerekTextView1(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //oldOnDraw(canvas);
        //Log.d("text","DRAW");
        if (layout == null) return;
        if (isPrev && !hasBeenDrawn) {
            hasBeenDrawn = true;
            //Log.d("text","FIRST DRAW " + getHeight());
            //Message msg = TextActivity.handler.obtainMessage();
            //msg.what = TextActivity.PREV_CHAP_DRAWN;
            //msg.obj = new PrevMessage(getHeight());
            //TextActivity.handler.sendMessage(msg);
        }

        for (int i = firstDrawnLine; i <= lastDrawnLine; i++) {
            int lineStart = layout.getLineStart(i);
            int lineEnd = layout.getLineEnd(i);
            int startY = layout.getLineBottom(i);
            String line = text.substring(lineStart, lineEnd);

            if (isCts && false) {//justified  //TODO make this work. to use justified, also remove false from updateVisibleLines()

                float width = StaticLayout.getDesiredWidth(line, paint);
                if (needScale(line) && i != lineCount-1) {

                    drawScaledText(canvas, line, i-firstDrawnLine, startY,width, lang == Util.Lang.HE);
                } else {
                    //float startX = lang == Util.Lang.HE ? mViewWidth-width : 0;
                    canvas.drawText(line,0,startY,paint);
                }
            } else {

                boolean hasHeb = Util.hasHebrew(line);
                //if (hasHeb) Log.d("text","HAS HEBREW");

                float offset = mViewWidth - StaticLayout.getDesiredWidth(line, paint); //TODO less random number
                float startX = hasHeb ? offset : 0f;
                canvas.drawText(line, startX, startY, paint);
            }


        }

        //https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
        /*StaticLayout mTextLayout = new StaticLayout(Html.fromHtml(drawText), paint, canvas.getWidth(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        float offset = scrollY % lineHeight;

        canvas.save();
        //offset = currScreenTop % lineHeight
        canvas.translate(0, startY);//-offset);
        mTextLayout.draw(canvas);
        canvas.restore();
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
        noahDraw();
    }

    private void updateVisibleLines() {
        try {
            int oldFirst = firstDrawnLine;
            int oldLast = lastDrawnLine;

            int min = this.scrollY - relativeTop;
            if (min < 0) min = 0;
            else if (min > getHeight() - scrollH) min = getHeight() - scrollH;
            this.firstDrawnLine = layout.getLineForVertical(min) - EXTRA_LOAD_LINES;
            if (firstDrawnLine < 0) firstDrawnLine = 0;
            this.lastDrawnLine = layout.getLineForVertical(min + scrollH) + EXTRA_LOAD_LINES;
            if (lastDrawnLine > lineCount - 1) lastDrawnLine = lineCount - 1;

            //update justified characters - assuming this is always run on update()

            //TODO make this work when switching to and from isCts
            //TODO also, this is slightly too slow. and there's a bug when you scroll up then down
            if (characters == null && isCts && false) {
                characters = new ArrayList<>();
                charWidths = new ArrayList<>();
                for (int i = firstDrawnLine; i <= lastDrawnLine; i++) {
                    int lineStart = layout.getLineStart(i);
                    int lineEnd = layout.getLineEnd(i);
                    String line = text.substring(lineStart, lineEnd);

                    String[] charList = new String[line.length()];
                    float[] cwList = new float[line.length()];

                    for (int j = 0; j < line.length(); j++) {
                        String c = String.valueOf(line.charAt(j));
                        charList[j] = c;
                        cwList[j] = StaticLayout.getDesiredWidth(c, paint);
                    }

                    characters.add(charList);
                    charWidths.add(cwList);
                }
            } else if (isCts) {
                int starti, endi;
                if (firstDrawnLine - oldFirst >= 0) {
                    characters = characters.subList(firstDrawnLine-oldFirst,characters.size());
                    charWidths = charWidths.subList(firstDrawnLine-oldFirst,charWidths.size());
                    starti = oldLast + 1;
                    endi = lastDrawnLine;

                    for (int i = starti; i <= endi; i++) {
                        int lineStart = layout.getLineStart(i);
                        int lineEnd = layout.getLineEnd(i);
                        String line = text.substring(lineStart, lineEnd);

                        String[] charList = new String[line.length()];
                        float[] cwList = new float[line.length()];

                        for (int j = 0; j < line.length(); j++) {
                            String c = String.valueOf(line.charAt(j));
                            charList[j] = c;
                            cwList[j] = StaticLayout.getDesiredWidth(c, paint);
                        }

                        characters.add(charList);
                        charWidths.add(cwList);
                    }
                } else {
                    characters = characters.subList(0, lastDrawnLine - oldFirst + 1);
                    charWidths = charWidths.subList(0, lastDrawnLine - oldFirst + 1);
                    starti = firstDrawnLine;
                    endi = oldFirst - 1;

                    for (int i = endi; i >= starti; i--) {
                        int lineStart = layout.getLineStart(i);
                        int lineEnd = layout.getLineEnd(i);
                        String line = text.substring(lineStart, lineEnd);

                        String[] charList = new String[line.length()];
                        float[] cwList = new float[line.length()];

                        for (int j = 0; j < line.length(); j++) {
                            String c = String.valueOf(line.charAt(j));
                            charList[j] = c;
                            cwList[j] = StaticLayout.getDesiredWidth(c, paint);
                        }

                        characters.add(0, charList);
                        charWidths.add(0, cwList);
                    }
                }
            }




        } catch (NullPointerException e) {
            return;
        }
    }

    public void update() {

        boolean isFirst = true;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        setMovementMethod(LinkMovementMethod.getInstance());

        for (int i = 0; i < textList.size(); i++) {
            Text segment = textList.get(i);
            String words;
            if (lang == Util.Lang.BI) {
                if (segment.displayNum) {
                    words = "(" + Util.int2heb(segment.levels[0]) + ") " + segment.getText(Util.Lang.HE)
                            + "\n\n(" + segment.levels[0] + ") " + segment.getText(Util.Lang.EN);
                } else {
                    words = segment.getText(Util.Lang.HE) + "\n\n" + segment.getText(Util.Lang.EN);
                }

            } else { //mono lingual
                if (segment.displayNum) {
                    words = "(" + Util.int2heb(segment.levels[0]) + ") " + segment.getText(lang);
                } else {
                    words = segment.getText(lang);
                }
            }
            if (!isFirst)
                words = isCts ? " " + words : "\n\n" + words;


            SpannableString ss = new SpannableString(words);
            ss.setSpan(new SegmentSpannable(words), 0, ss.length(), 0);
            ssb.append(ss);
            isFirst = false;
        }

        setText(ssb, BufferType.SPANNABLE);
        updateLayoutParams(true);
        noahDraw();





        //AsyncLoadText alt = new AsyncLoadText();
        //alt.execute();


    }

    private void updateLayoutParams(boolean updateText) {
        layout = getLayout();
        if (layout == null) return;

        //this is expensive
        if (updateText) {

            relativeTop = Util.getRelativeTop(PerekTextView1.this);
            paint = getPaint();
            paint.setColor(getCurrentTextColor());
            paint.drawableState = getDrawableState();
            text = (String) getText();
            lineCount = layout.getLineCount();
        }
        mViewWidth = getMeasuredWidth();
        this.textSize = getTextSize();
        lineHeight = getLineHeight(); //(int)Math.round(getLineHeight() - getLineHeight()/12);
    }


    //lineindex is relative to the characters and charWidth ArrayLists
    protected void drawScaledText(Canvas canvas, String line, int lineindex, float y, float lineWidth,boolean isHeb) {


        float x = isHeb ? mViewWidth : 0;
        float d;
        if (isHeb && false) {
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
            if (isHeb && false) {
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
                cw = 15f; //StaticLayout.getDesiredWidth(c, paint);
                canvas.drawText(c, x-cw, y, paint);
                x -= (cw + d);
            } else {
                c = characters.get(lineindex)[i];//String.valueOf(line.charAt(i));
                cw = charWidths.get(lineindex)[i]; //15f; //StaticLayout.getDesiredWidth(c, paint);
                //Log.d("yo","C " + cw);
                canvas.drawText(c, x, y, paint);
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


    }

    //used to inform TextActivity that the PTV has just been drawn
    public class PrevMessage {

        public int height;

        public PrevMessage(int height) {
            this.height = height;
        }
    }
}*/


