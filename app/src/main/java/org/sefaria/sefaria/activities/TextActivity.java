package org.sefaria.sefaria.activities;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.text.SpannableString;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TextElements.OnSegmentSpanClickListener;
import org.sefaria.sefaria.TextElements.TextAdapter;
import org.sefaria.sefaria.TextElements.SegmentSpannable;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Section;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.ListViewExt;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

public class TextActivity extends SuperTextActivity implements AbsListView.OnScrollListener, LinkFragment.OnLinkFragInteractionListener {

    private ListViewExt listView;
    private TextAdapter textAdapter;

    private int preLast;
    private Section problemLoadedSection;

    private int scrolledDownTimes = 0;

    private int clickX;
    private int clickY;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        if(!goodOnCreate){
            finish();
            return;
        }
        //NOTE: using the same layout as SectionActivity
        setContentView(R.layout.activity_text);

        init();
    }

    protected void init() {
        super.init();
        listView = (ListViewExt) findViewById(R.id.listview);
        listView.setSensitivity(10);
        textAdapter = new TextAdapter(this,R.layout.adapter_text_mono,new ArrayList<Section>(),onSegmentSpanClickListener);

        listView.setAdapter(textAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        listView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                listView.setClickPos(new Point((int)event.getX(),(int)event.getY()));
                return false; // not consumed; forward to onClick
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //Log.d("yo"," " + linkFragment.getIsOpen());

                if (!linkFragment.getIsOpen()) {
                    Point clickPos = listView.getClickPos();
                    listView.smoothScrollBy(clickPos.y-SEGMENT_SELECTOR_LINE_FROM_TOP,LINK_FRAG_ANIM_TIME);
                }

                updateFocusedSegment();
                onSegmentClick(linkFragment.getSegment());

                //listView.smoothScrollByOffset(clickPos.y);
                //listView.scrollBy(0,clickPos.y-SEGMENT_SELECTOR_LINE_FROM_TOP);

            }
        });
        //listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnScrollStoppedListener(new ListViewExt.OnScrollStoppedListener() {

            public void onScrollStopped() {
                updateFocusedSegment();
            }
        });

        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION,null);
        als.preExecute();



        registerForContextMenu(listView);
    }

    private class AsyncLoadSection extends AsyncTask<Void,Void,List<Text>> {

        private TextEnums dir;
        private Section loaderSection;
        private Section catalystSection;

        /**
         * @param dir          - direction in which you want to load a section (either prev or next)
         * @param catalystSection - the Text which caused this loading to happen. Important, in case this text has already failed to generate any new content, meaning that it's either the beginning or end of a book
         */
        public AsyncLoadSection(TextEnums dir, Section catalystSection) {
            this.dir = dir;
            this.catalystSection = catalystSection;
        }

        public void preExecute() {
            if (catalystSection == null || !catalystSection.equals(problemLoadedSection)) {
                this.execute();
            } else {
                //Log.d("SectionActivity","Problem text not loaded");
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoadingSection = true;
            loaderSection = new Section(true);

            if (this.dir == TextEnums.NEXT_SECTION) {
                textAdapter.add(loaderSection);
            } else /*if (this.dir == TextEnums.PREV_SECTION)*/ {
                //textAdapter.add(0, loaderSection);
            }
        }

        @Override
        protected List<Text> doInBackground(Void... params) {
            return loadSection(dir);
        }


        @Override
        protected void onPostExecute(List<Text> textsList) {
            isLoadingSection = false;
            isLoadingInit = false;



            if (textsList == null) {
                problemLoadedSection = catalystSection;
                return;
            }
            //if (textsList.size() == 0) return;//removed this line so that when it doesn't find text it continues to look for the next item for text

            Text sectionHeader = getSectionHeaderText(dir);
            Section newSection = new Section(textsList, sectionHeader);
            if (dir == TextEnums.NEXT_SECTION) {

                textAdapter.remove(loaderSection);
                /*if(sectionHeader.getText(Util.Lang.EN).length() > 0 || sectionHeader.getText(Util.Lang.HE).length() > 0)
                    textAdapter.add(sectionHeader);*/
                textAdapter.add(newSection);

                scrolledDownTimes++;
                if (openedNewBookTime > 0 && !reportedNewBookScroll && scrolledDownTimes == 2 && (System.currentTimeMillis() - openedNewBookTime < 10000)) {
                    reportedNewBookScroll = true;
                    String category;
                    if (reportedNewBookTOC || reportedNewBookBack)
                        category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                    else
                        category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                    GoogleTracker.sendEvent(category, "Scrolled down", scrolledDownTimes / openedNewBookTime);
                }

            } else /*if (dir == TextEnums.PREV_SECTION)*/ {
                textAdapter.add(0, newSection);
                listView.setSelection(1);
            }

            if (openToText != null) {
                try {
                    Thread.sleep(50);
                    //this is to help solve the race condition causing it to jump to the wrong place
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                jumpToText(openToText);
                openToText = null;
            }

        }
    }

    protected void setTextLang(Util.Lang textLang) {
        this.textLang = textLang;
        if (textLang == Util.Lang.BI) {
            Settings.setIsCts(false);
            restartActivity();
        }

        textAdapter.notifyDataSetChanged();
        linkFragment.notifyDataSetChanged();
    }

    @Override
    protected void setIsSideBySide(boolean isSideBySide) {
        super.setIsSideBySide(isSideBySide);
        textAdapter.notifyDataSetChanged();
    }

    protected void incrementTextSize(boolean isIncrement) {
        super.incrementTextSize(isIncrement);
        textAdapter.notifyDataSetChanged();
    }

    protected void updateFocusedSegment() {

        int midViewPos = listView.getFirstVisiblePosition();
        View midView = null;
        boolean foundMidView = false;
        while(!foundMidView && midViewPos < textAdapter.getCount()) {
            midView = listView.getViewByPosition(midViewPos);

            foundMidView = midView.getBottom() > SEGMENT_SELECTOR_LINE_FROM_TOP;

            midViewPos++;
        }
        if (midView != null) {
            SefariaTextView sectionTv = (SefariaTextView) midView.findViewById(R.id.sectionTV);
            if (sectionTv != null) {
                int midmid = (int) SEGMENT_SELECTOR_LINE_FROM_TOP - sectionTv.getTop() - midView.getTop();
                /*int lineNum = layout.getLineForVertical((int) mid - sectionTv.getTop() - midView.getTop());
                int lineStart = layout.getLineStart(lineNum);
                int lineEnd = layout.getLineEnd(lineNum);

                String allText =  sectionTv.getText().toString();*/



                SegmentSpannable midVs = getSpanNearY(sectionTv, midmid);

                Text currSeg = midVs.getSegment();

                if (currSeg.equals(linkFragment.getSegment())) return; //no need to update


                linkFragment.updateFragment(currSeg);
                textAdapter.notifyDataSetChanged(); //redraw visible views to make current segment view darker
            }
        }

    }


    private SegmentSpannable getSpanNearY(TextView stv, int y) {
        //pre-initaliaze
        Rect parentTextViewRect = new Rect();
        Layout textViewLayout = stv.getLayout();

        SpannableString ss = (SpannableString) stv.getText();
        SegmentSpannable[] segmentSpannables = ss.getSpans(0, ss.length(), SegmentSpannable.class);

        int lo = 0;
        int hi = segmentSpannables.length-1;

        int[] spanYs = new int[segmentSpannables.length];
        int[] mids = new int[segmentSpannables.length];
        int counter = 0;
        while (lo <= hi) {
            mids[counter] = lo + (hi - lo) / 2;
            spanYs[counter] = getSpanYFast(ss, stv, segmentSpannables[mids[counter]],parentTextViewRect,textViewLayout);

            if (y > spanYs[counter]) {
                lo = mids[counter] + 1;
            } else if (y < spanYs[counter]) {
                hi = mids[counter] - 1;
            } else {
                return segmentSpannables[mids[counter]]; //perfect match
            }
            counter++;
        }

        SegmentSpannable vs = null;
        if (counter > 1) {
            if (Math.abs(y - spanYs[counter-1]) < Math.abs(y - spanYs[counter-2])) {
                vs = segmentSpannables[mids[counter-1]];
            } else {
                vs = segmentSpannables[mids[counter-2]];
            }
        } else if (counter == 1){
            vs = segmentSpannables[0];
        }

        return vs;
    }

    private int getSpanYFast(SpannableString ss, TextView stv, SegmentSpannable vs, Rect parentTextViewRect, Layout textViewLayout) {
        int startOffsetOfClickedText = ss.getSpanStart(vs);
        int currentLineStartOffset = textViewLayout.getLineForOffset(startOffsetOfClickedText);
        textViewLayout.getLineBounds(currentLineStartOffset, parentTextViewRect);

        return (parentTextViewRect.top + parentTextViewRect.bottom)/2;
    }

    private int getSpanY(SpannableString ss, TextView stv, SegmentSpannable vs) {
        // Initialize global value
        Rect parentTextViewRect = new Rect();


        // Initialize values for the computing of clickedText position
        Layout textViewLayout = stv.getLayout();

        int startOffsetOfClickedText = ss.getSpanStart(vs);
        //int endOffsetOfClickedText = ss.getSpanEnd(vs);
        //double startXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(startOffsetOfClickedText);
        //double endXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(endOffsetOfClickedText);


        // Get the rectangle of the clicked text
        int currentLineStartOffset = textViewLayout.getLineForOffset(startOffsetOfClickedText);
        //int currentLineEndOffset = textViewLayout.getLineForOffset(endOffsetOfClickedText);
        //boolean keywordIsInMultiLine = currentLineStartOffset != currentLineEndOffset;
        textViewLayout.getLineBounds(currentLineStartOffset, parentTextViewRect);

        return (parentTextViewRect.top + parentTextViewRect.bottom)/2;
        /*
        // Update the rectangle position to his real position on screen
        int[] parentTextViewLocation = {0,0};
        stv.getLocationOnScreen(parentTextViewLocation);

        double parentTextViewTopAndBottomOffset = (
                parentTextViewLocation[1] -
                        stv.getScrollY() +
                        stv.getCompoundPaddingTop()
        );
        parentTextViewRect.top += parentTextViewTopAndBottomOffset;
        parentTextViewRect.bottom += parentTextViewTopAndBottomOffset;

        // In the case of multi line text, we have to choose what rectangle take
        if (keywordIsInMultiLine){

            int screenHeight = this.mWindowManager.getDefaultDisplay().getHeight();
            int dyTop = this.parentTextViewRect.top;
            int dyBottom = screenHeight - this.parentTextViewRect.bottom;
            boolean onTop = dyTop > dyBottom;

            if (onTop){
                endXCoordinatesOfClickedText = textViewLayout.getLineRight(currentLineStartOffset);
            }
            else{
                this.parentTextViewRect = new Rect();
                textViewLayout.getLineBounds(currentLineEndOffset, this.parentTextViewRect);
                this.parentTextViewRect.top += parentTextViewTopAndBottomOffset;
                this.parentTextViewRect.bottom += parentTextViewTopAndBottomOffset;
                startXCoordinatesOfClickedText = textViewLayout.getLineLeft(currentLineEndOffset);
            }

        }

        parentTextViewRect.left += (
                parentTextViewLocation[0] +
                        startXCoordinatesOfClickedText +
                        stv.getCompoundPaddingLeft() -
                        stv.getScrollX()
        );
        parentTextViewRect.right = (int) (
                parentTextViewRect.left +
                        endXCoordinatesOfClickedText -
                        startXCoordinatesOfClickedText
        );

        return (parentTextViewRect.top + parentTextViewRect.bottom)/2;*/
    }


    @Override
    public void onScroll(AbsListView lw, final int firstVisibleItem,
                         final int visibleItemCount, final int totalItemCount) {

        switch(lw.getId()) {
            case R.id.listview:

                int scrollY = 0;
                if (totalItemCount > 0) {
                    View firstView = listView.getViewByPosition(0);
                    scrollY = firstView.getTop();
                }

                if (!isLoadingSection && !isLoadingInit) {
                    int lastItem = firstVisibleItem + visibleItemCount;
                    if (firstVisibleItem == 0 && scrollY > -3) {
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.PREV_SECTION,textAdapter.getItem(0));
                        als.preExecute();
                    }
                    if (lastItem == totalItemCount ) {
                        preLast = lastItem;
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION,textAdapter.getItem(lastItem - 1));
                        als.preExecute();
                    }

                    Section topSegment = textAdapter.getItem(firstVisibleItem);
                    setCurrNode(topSegment);
                }

        }
    }

    @Override
    protected void jumpToText(Text text) {
        //int index = textAdapter.getPosition(text);
        //textAdapter.highlightIncomingText(text);
        //listView.setSelection(index);
    }

    /*public void jumpSection(View view) {
        if (view.getId() == R.id.jump_section_down) {
            Log.d("SectionActivity","DOWN");
        } else if (view.getId() == R.id.jump_section_up) {
            Log.d("SectionActivity","UP");
        }
    }*/

    //YOU actually need this function implemented because you're implementing AbsListView, but it's stupid...
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //blah...
    }

    @Override
    protected void postFindOnPageBackground() {
        textAdapter.notifyDataSetChanged();
    }

    /**
     * LINK FRAGMENT
     */

    @Override
    protected void onFinishLinkFragOpen() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW, R.id.actionbarRoot);
        lp.addRule(RelativeLayout.ABOVE, R.id.linkRoot);

        listView.setLayoutParams(lp);


    }

    @Override
    protected void onStartLinkFragClose() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW, R.id.actionbarRoot);
        //lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

        listView.setLayoutParams(lp);
    }

    OnSegmentSpanClickListener onSegmentSpanClickListener = new OnSegmentSpanClickListener() {
        @Override
        public void onSegmentClick(TextView tv,SegmentSpannable segmentSpannable) {
            TextActivity.this.onSegmentClick(segmentSpannable.getSegment());
        }
    };

    public void onSegmentClick(Text currSegment) {

        if (isTextMenuVisible) {
            toggleTextMenu();
            return;
        }

        if(getFindOnPageIsOpen()){
            findOnPageClose();
        }

        View linkRoot = findViewById(R.id.linkRoot);
        if (linkFragment.getIsOpen()) { //&& false

            //linkRoot.setVisibility(View.GONE);
            AnimateLinkFragClose(linkRoot);

        } else {
            //sectionAdapter.highlightIncomingText(null);
            //SpannableString ss = (SpannableString) tv.getText();
            //int spanY = getSpanY(ss,tv,segmentSpannable);

            //if (spanY > 0) //don't auto-scroll if the text is super long.
            //    listView.smoothScrollToPositionFromTop(position,SuperTextActivity.SEGMENT_SELECTOR_LINE_FROM_TOP,SuperTextActivity.LINK_FRAG_ANIM_TIME);
            linkFragment.setClicked(true);
            linkFragment.updateFragment(currSegment);
            //linkRoot.setVisibility(View.VISIBLE);
            AnimateLinkFragOpen(linkRoot);
        }
    }
}