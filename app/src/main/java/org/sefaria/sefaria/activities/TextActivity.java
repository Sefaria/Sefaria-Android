package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.layouts.PerekTextView;
import org.sefaria.sefaria.layouts.ScrollViewExt;
import org.sefaria.sefaria.layouts.ScrollViewListener;
import org.sefaria.sefaria.TextElements.TextChapterHeader;
import org.sefaria.sefaria.TextElements.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.menu.MenuNode;
import org.sefaria.sefaria.menu.MenuState;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

public class TextActivity extends SuperTextActivity {

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            //you're returning to text page b/c chapter was clicked in toc
            if (requestCode == TOC_CHAPTER_CLICKED_CODE) {
                //lang = (Util.Lang) data.getSerializableExtra("lang"); TODO you might need to set lang here if user can change lang in TOC
                int nodeHash = data.getIntExtra("nodeHash", -1);
                firstLoadedNode = Node.getSavedNode(nodeHash);
                lastLoadedNode = null;
                init();
            }
        }
    }

    protected void setLang(Util.Lang lang) {
        this.lang = lang;
        for (TextChapterHeader tch : textChapterHeaders) {
            tch.setLang(lang);
        }

        for (PerekTextView ptv : perekTextViews) {
            ptv.setLang(lang);
            ptv.update();
        }
    }

    protected void setIsCts(boolean isCts) {
        this.isCts = isCts;
        for (PerekTextView ptv : perekTextViews) {
            ptv.setIsCts(isCts);
            ptv.update();
        }
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
                    //Log.d("text","NEXT");
                    AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
                    als.execute();
                }
                if (topDiff >= -LOAD_PIXEL_THRESHOLD && !justLoadedPrevChap) {
                    //Log.d("text","PREV");
                    AsyncLoadSection als = new AsyncLoadSection(TextEnums.PREV_SECTION);
                    als.execute();

                    //if you add prev, you need to update all tops of all chaps
                    for (PerekTextView ptv : perekTextViews) {
                        ptv.setRelativeTop(Util.getRelativeTop(ptv));
                    }
                }
            }

            boolean inVisiblePTVRange = false;
            for (PerekTextView ptv : perekTextViews) {
                if (scrollY > ptv.getTop()-3*scrollH && scrollY < ptv.getBottom()+3*scrollH) {
                    inVisiblePTVRange = true;
                    try {
                        int newFirst = ptv.getNewFirstDrawnLine(scrollY)-PerekTextView.EXTRA_LOAD_LINES;
                        int newLast = ptv.getNewLastDrawnLine(scrollY)+PerekTextView.EXTRA_LOAD_LINES;
                        int currFirst = ptv.getFirstDrawnLine();
                        int currLast = ptv.getLastDrawnLine();
                        //Log.d("text","First " + newFirst + " < " + currFirst + " LAST " + newLast + " > " + currLast);
                        if (newFirst < currFirst || newLast > currLast) {
                            ptv.updateScroll(scrollY,scrollH);
                        }
                    } catch (NullPointerException e) {
                        //in case layout is null, just continue. it'll work out
                        continue;
                    }

                } else {
                    //once you leave the visible ptv range, you're done
                    if (inVisiblePTVRange) break;
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


            //TODO levels is gonna be replaced I believe, so do whatever you want here
            levels = new int[book.textDepth];
            for (int i = 0; i < levels.length; i++) {
                levels[i] = 0;
            }
            if (levels.length > WHERE_PAGE-1) {
                if (levels.length > WHERE_PAGE)
                    levels[WHERE_PAGE] = 1; //TODO make this dynamic!
                levels[WHERE_PAGE - 1] = 0;
            }

            TextChapterHeader tch;
            if (levels.length > WHERE_PAGE-1) {
                //MenuNode tempNode = new MenuNode(book.sectionNamesL2B[wherePage-1] + " " + currLoadedChapter,
                //        book.heSectionNamesL2B[wherePage-1] + " " + Util.int2heb(currLoadedChapter),null,null);

                Text segment = new Text(true,""+ levels[WHERE_PAGE-1], Util.int2heb(levels[WHERE_PAGE-1]));
                tch = new TextChapterHeader(TextActivity.this,segment,lang,textSize);
                textChapterHeaders.add(tch);

            } else tch = null;

            PerekTextView content;

            if (dir == null || dir == TextEnums.NEXT_SECTION) {
                content = new PerekTextView(TextActivity.this,textsList,isCts,lang,textSize,textScrollView.getScrollY(),false);
                perekTextViews.add(content);
                //YES, order that you add these two views matters (note difference in PREV_SECTION)
                if (tch != null)
                    textRoot.addView(tch);
                textRoot.addView(content); //add to end by default
            } else if (dir == TextEnums.PREV_SECTION) {
                content = new PerekTextView(TextActivity.this,textsList,isCts,lang,textSize,textScrollView.getScrollY(),true);
                perekTextViews.add(content);
                oldScroll = textScrollView.getScrollY();
                addH = 0;
                textRoot.addView(content, 0); //add to before
                justLoadedPrevChap = true;
                //make sure to keep equivalent scroll position
                /*textScrollView.post(new Runnable() {
                    public void run() {
                        textScrollView.scrollTo(0,oldScroll + content.getHeight() + tch.getHeight());
                    }
                });*/
            }
        }
    }

    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case PREV_CHAP_DRAWN:

                    PerekTextView.PrevMessage prevMessage = (PerekTextView.PrevMessage) msg.obj;
                    Log.d("text","DRAWN " + prevMessage.height);
                    break;
            }
        }
    };

}
