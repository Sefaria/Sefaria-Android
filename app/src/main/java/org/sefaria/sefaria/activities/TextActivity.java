package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.layouts.JustifyTextView;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.layouts.PerekTextView;
import org.sefaria.sefaria.layouts.ScrollViewExt;
import org.sefaria.sefaria.layouts.ScrollViewListener;
import org.sefaria.sefaria.layouts.TextChapterHeader;
import org.sefaria.sefaria.layouts.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.layouts.VerseSpannable;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.menu.MenuNode;
import org.sefaria.sefaria.menu.MenuState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextActivity extends Activity {

    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }
    private static final int WHERE_PAGE = 2;

    private MenuState menuState;
    private boolean isTextMenuVisible;
    private LinearLayout textMenuRoot;
    private LinearLayout textRoot;
    private ScrollViewExt textScrollView;
    private Book book;

    private int firstLoadedChap;
    private int lastLoadedChapter;
    private List<PerekTextView> perekTextViews;
    private List<TextChapterHeader> textChapterHeaders;

    //text formatting props
    private Util.Lang lang;
    private boolean isCts;
    private float textSize;
    private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setContentView(R.layout.activity_text);

        Intent intent = getIntent();
        menuState = intent.getParcelableExtra("menuState");
        if (in != null) {
            menuState = in.getParcelable("menuState");
        }
        init();
    }

    private void init() {
        //defaults
        isCts = false;
        lang = Util.Lang.BI;
        textSize = getResources().getDimension(R.dimen.default_text_font_size);
        //end defaults

        perekTextViews = new ArrayList<>();
        textChapterHeaders = new ArrayList<>();
        firstLoadedChap = 0;
        lastLoadedChapter = 0;
        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);
        textRoot = (LinearLayout) findViewById(R.id.textRoot);
        textScrollView = (ScrollViewExt) findViewById(R.id.textScrollView);
        textScrollView.setScrollViewListener(new ScrollViewListener() {
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
                     if (bottomDiff <= 0) {
                         //Log.d("text","NEXT");
                         AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
                         als.execute();
                     }
                     if (topDiff >= 0) {
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
         });

                setTitle(menuState.getCurrNode().getTitle(menuState.getLang()));

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        CustomActionbar cab = new CustomActionbar(this, menuState.getCurrNode(), Util.Lang.EN,searchClick,null,null,menuClick);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);



        String title = menuState.getCurrNode().getTitle(Util.Lang.EN);
        book = new Book(title);

        if (!isLoadingSection) {
            AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
            als.execute();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putParcelable("menuState", menuState);
    }

    private void toggleTextMenu() {
        if (isTextMenuVisible) {
            textMenuRoot.removeAllViews();
        } else {
            TextMenuBar tmb = new TextMenuBar(TextActivity.this,textMenuBtnClick);
            textMenuRoot.addView(tmb);
        }
        isTextMenuVisible = !isTextMenuVisible;
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(TextActivity.this, HomeActivity.class);
            intent.putExtra("searchClicked",true);
            intent.putExtra("isPopup",true);
            startActivity(intent);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleTextMenu();
        }
    };

    View.OnClickListener textMenuBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            boolean updatedTextSize = false;
            switch (v.getId()) {
                case R.id.en_btn:
                    lang = Util.Lang.EN;
                    break;
                case R.id.he_btn:
                    lang = Util.Lang.HE;
                    break;
                case R.id.bi_btn:
                    lang = Util.Lang.BI;
                    break;
                case R.id.cts_btn:
                    isCts = true;
                    break;
                case R.id.sep_btn:
                    isCts = false;
                    break;
                case R.id.white_btn:
                    Log.d("text","WHITE");
                    break;
                case R.id.grey_btn:
                    Log.d("text","GREY");
                    break;
                case R.id.black_btn:
                    Log.d("text","BLACK");
                    break;
                case R.id.small_btn:
                    Log.d("text","SMALL");
                    if (textSize >= getResources().getDimension(R.dimen.min_text_font_size)-getResources().getDimension(R.dimen.text_font_size_increment)) {
                        textSize -= getResources().getDimension(R.dimen.text_font_size_increment);
                        updatedTextSize = true;
                    }
                    break;
                case R.id.big_btn:
                    Log.d("text","BIG");
                    if (textSize <= getResources().getDimension(R.dimen.max_text_font_size)+getResources().getDimension(R.dimen.text_font_size_increment)) {
                        textSize += getResources().getDimension(R.dimen.text_font_size_increment);
                        updatedTextSize = true;
                    }
                    break;
            }
            for (TextChapterHeader tch: textChapterHeaders) {
                tch.setLang(lang);
                tch.setTextSize(textSize);
            }
            for (PerekTextView ptv: perekTextViews) {
                ptv.setLang(lang);
                ptv.setIsCts(isCts);
                if (updatedTextSize)
                    ptv.setTextSize(textSize);
                else
                    ptv.update();
            }

            if (!updatedTextSize) toggleTextMenu();
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
            return loadSection();
        }

        @Override
        protected void onPostExecute(List<Text> textsList) {
            if (textsList.size() == 0) return;

            if (levels.length > WHERE_PAGE-1) {
                //MenuNode tempNode = new MenuNode(book.sectionNamesL2B[wherePage-1] + " " + currLoadedChapter,
                //        book.heSectionNamesL2B[wherePage-1] + " " + Util.int2heb(currLoadedChapter),null,null);
                MenuNode tempNode = new MenuNode(""+levels[WHERE_PAGE-1],""+Util.int2heb(levels[WHERE_PAGE-1]),null);

                TextChapterHeader tch = new TextChapterHeader(TextActivity.this,tempNode,lang,textSize);
                textChapterHeaders.add(tch);
                textRoot.addView(tch);
            }

            PerekTextView content = new PerekTextView(TextActivity.this,textsList,isCts,lang,textSize,textScrollView.getScrollY());

            perekTextViews.add(content);

            isLoadingSection = false;

            if (dir == null || dir == TextEnums.NEXT_SECTION)
                textRoot.addView(content); //add to end by default
            else if (dir == TextEnums.PREV_SECTION)
                textRoot.addView(content,0); //add to before
        }

        private List<Text> loadSection() {
            int tempChap = 0;
            if (dir == TextEnums.NEXT_SECTION) {
                lastLoadedChapter++;
                tempChap = lastLoadedChapter;
                if (firstLoadedChap == 0) firstLoadedChap = 1; //initialize if first load TODO make this dynamic depending on where they first go
            }

            else if (dir == TextEnums.PREV_SECTION) {
                firstLoadedChap--;
                tempChap = firstLoadedChap;
            }

            //TODO need to add end and start detection. Special case is 1-level, b/c you load it all at once so you're done right then
            if (firstLoadedChap < 1) { //you went too far, reset and return
                firstLoadedChap = 1;
                return new ArrayList<>();
            }

            levels= new int[book.textDepth];
            for (int i = 0; i < levels.length; i++) {
                levels[i] = 0;
            }
            if (levels.length > WHERE_PAGE-1) {
                if (levels.length > WHERE_PAGE)
                    levels[WHERE_PAGE] = 1; //TODO make this dynamic!
                levels[WHERE_PAGE - 1] = tempChap;
            }


            //int[] levels = {0,currLoadedChapter};
            //Text.getNextChap(book,levels,next);
            List<Text> textsList;
            try {
                textsList = Text.get(book, levels);
                return textsList;
            } catch (API.APIException e) {
                Toast.makeText(TextActivity.this,"API Exception!!!",Toast.LENGTH_SHORT).show();
                return new ArrayList<>();
            }


        }

    }

}
