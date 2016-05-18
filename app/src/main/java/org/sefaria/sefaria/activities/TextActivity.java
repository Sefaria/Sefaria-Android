package org.sefaria.sefaria.activities;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.TextAdapter;
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

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        if(!goodOnCreate){
            finish();
            return;
        }
        //NOTE: using the same layout as SectionActivity
        setContentView(R.layout.activity_section);

        init();
    }

    protected void init() {
        super.init();
        listView = (ListViewExt) findViewById(R.id.listview);
        textAdapter = new TextAdapter(this,R.layout.adapter_text_mono,new ArrayList<Section>());


        listView.setAdapter(textAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        //listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnScrollStoppedListener(new ListViewExt.OnScrollStoppedListener() {

            public void onScrollStopped() {
                //updateFocusedSegment();
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
                textAdapter.add(0, loaderSection);
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


            textAdapter.remove(loaderSection);
            if (textsList == null) {
                problemLoadedSection = catalystSection;
                return;
            }
            //if (textsList.size() == 0) return;//removed this line so that when it doesn't find text it continues to look for the next item for text

            Text sectionHeader = getSectionHeaderText(dir);
            Section newSection = new Section(textsList, sectionHeader);
            if (dir == TextEnums.NEXT_SECTION) {


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
        float mid = ((float)listView.getHeight())/2;

        int midViewPos = listView.getFirstVisiblePosition();
        View midView = null;
        boolean foundMidView = false;
        while(!foundMidView && midViewPos < textAdapter.getCount()) {
            midView = listView.getViewByPosition(midViewPos);

            foundMidView = midView.getBottom() > mid;

            midViewPos++;
        }
        if (midView != null) {
            SefariaTextView sectionTv = (SefariaTextView) midView.findViewById(R.id.sectionTV);
            if (sectionTv != null) {
                Layout layout = sectionTv.getLayout();
                int lineNum = layout.getLineForVertical((int) mid - sectionTv.getTop() - midView.getTop());
                int lineStart = layout.getLineStart(lineNum);
                int lineEnd = layout.getLineEnd(lineNum);

                String allText =  sectionTv.getText().toString();

                String line = allText.substring(lineStart, lineEnd);
                Log.d("TextActivity", line);
            }
        }

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
                    //setCurrNode(topSegment);
                }

                updateFocusedSegment();
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
}