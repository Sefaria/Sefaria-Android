/*package org.sefaria.sefaria.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.TextChapterHeader;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.PerekTextView;
import org.sefaria.sefaria.layouts.ScrollViewExt;
import org.sefaria.sefaria.layouts.ScrollViewListener;

import java.util.List;

public class TextActivity1 extends SuperTextActivity implements LinkFragment.OnLinkFragInteractionListener {

    private LinearLayout textRoot;
    //variables to properly handle scrolling on prev loaded chapter
    private boolean justLoadedPrevChap;
    public int oldScroll;
    private int addH;
    private PerekTextView justLoadedPTV;
    private TextChapterHeader justLoadedTCH;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        if(!goodOnCreate){
            //finish();
            return;
        }
        setContentView(R.layout.activity_text);
        justLoadedPrevChap = true;
        init();
    }

    protected void init() {
        super.init();

        textRoot = (LinearLayout) findViewById(R.id.textRoot);
        textRoot.removeAllViews();
        textScrollView.setScrollViewListener(scrollViewListener);
        if (!isLoadingSection) {
            AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
            als.execute();
        }


    }


    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        //out.putParcelable("menuState", menuState);
    }



    protected void setTextLang(Util.Lang textLang) {
        this.textLang = textLang;
        //bilingual texts can't be cts
        if (textLang == Util.Lang.BI) {
            setIsCts(false);
            return; //you're gonna restart anyway and changing lang takes a long time for bilingual
        }

        for (PerekTextView ptv : perekTextViews) {
            ptv.setLang(textLang);
        }


    }

    @Override
    protected void setMenuLang(Util.Lang menuLang) {
        super.setMenuLang(menuLang);
        for (TextChapterHeader tch : textChapterHeaders) {
            tch.setLang(menuLang);
        }
    }

    @Override
    protected void postFindOnPageBackground() {
        //TODO
    }

    protected void setIsSideBySide(boolean isSideBySide) {
        this.isSideBySide = isSideBySide;
        //TODO IMPLEMENT ME!!!
    }

    protected void updateFocusedSegment() {
        //TODO IMPLEMENT ME!!!
    }

    protected void incrementTextSize(boolean isIncrement) {
        float increment = getResources().getDimension(R.dimen.text_font_size_increment);

        for (TextChapterHeader tch : textChapterHeaders) {
            if (isIncrement)
                tch.setTextSize(textSize + increment);
            else
                tch.setTextSize(textSize - increment);
        }

        for (PerekTextView ptv : perekTextViews) {
            if (isIncrement)
                ptv.setTextSize(textSize + increment);
            else
                ptv.setTextSize(textSize - increment);
        }
    }


    ScrollViewListener scrollViewListener = new ScrollViewListener() {
        @Override
        public void onScrollChanged(ScrollViewExt scrollView, int x, int y, int oldx, int oldy) {
            // We take the last son in the scrollview
            View view = (View) scrollView.getChildAt(scrollView.getChildCount() - 1);

            int scrollY = scrollView.getScrollY();
            int scrollH = scrollView.getHeight();
            int topDiff = (view.getTop() - scrollY);
            int bottomDiff = (view.getBottom() - (scrollView.getHeight() + scrollY));

            // if diff is zero, then the bottom has been reached


            if (!isLoadingSection) {
                if (bottomDiff <= LOAD_PIXEL_THRESHOLD) {
                    Log.d("text","NEXT");
                    AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
                    als.execute();
                }
                if (topDiff >= -LOAD_PIXEL_THRESHOLD && !justLoadedPrevChap) {
                    Log.d("text","PREV");
                    AsyncLoadSection als = new AsyncLoadSection(TextEnums.PREV_SECTION);
                    als.execute();

                    //if you add prev, you need to update all tops of all chaps
                    for (PerekTextView ptv : perekTextViews) {
                        //ptv.setRelativeTop(Util.getRelativeTop(ptv));
                    }
                }
            }
        }
    };

    public class AsyncLoadSection extends AsyncTask<Void,Void,List<Text>> {

        private TextEnums dir;
        private int levels[];

        public AsyncLoadSection (TextEnums dir) {
            this.dir = dir;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoadingSection = true;
        }

        @Override
        protected List<Text> doInBackground(Void... params) {
            return loadSection(dir);
        }

        @Override
        protected void onPostExecute(List<Text> textsList) {
            isLoadingSection = false;
            if (textsList.size() == 0) return;


            TextChapterHeader tch;
            Text segment = getSectionHeaderText(dir);
            tch = new TextChapterHeader(TextActivity1.this,segment,textSize);
            textChapterHeaders.add(tch);

            PerekTextView content = new PerekTextView(TextActivity1.this,textsList,textLang,textSize);

            if (dir == null || dir == TextEnums.NEXT_SECTION) {
                //content = new PerekTextView(TextActivity.this,textsList,isCts,textLang,textSize,textScrollView.getScrollY(),false);
                perekTextViews.add(content);
                //YES, order that you add these two views matters (note difference in PREV_SECTION)
                if (tch != null)
                    textRoot.addView(tch);
                textRoot.addView(content); //add to end by default
            } else if (dir == TextEnums.PREV_SECTION) {
                //content = new PerekTextView(TextActivity.this,textsList,isCts,textLang,textSize,textScrollView.getScrollY(),true);
                perekTextViews.add(content);
                oldScroll = textScrollView.getScrollY();
                addH = 0;
                textRoot.addView(content, 0); //add to before
                justLoadedPrevChap = true;
                //make sure to keep equivalent scroll position
                textScrollView.post(new Runnable() {
                    public void run() {
                        textScrollView.scrollTo(0,oldScroll + content.getHeight() + tch.getHeight());
                    }
                });
            }
        }
    }

    //TODO fill in this function
    @Override
    protected void jumpToText(Text incomingLink) {
        //blah. this is actually going to be really annoying to do in cts texts...
    }


    @Override
    protected void onStartLinkFragClose() {

    }

    @Override
    protected void onFinishLinkFragOpen() {

    }
}*/
