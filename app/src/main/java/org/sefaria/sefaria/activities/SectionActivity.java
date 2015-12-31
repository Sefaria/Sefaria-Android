package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.SectionAdapter;
import org.sefaria.sefaria.TextElements.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.menu.MenuNode;
import org.sefaria.sefaria.menu.MenuState;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends Activity implements AbsListView.OnScrollListener {
    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }
    private static final int WHERE_PAGE = 2;

    private Util.Lang lang;
    private boolean isTextMenuVisible;
    private Book book;
    private int firstLoadedChap = 0;
    private int lastLoadedChapter = 0;
    private LinearLayout textMenuRoot;
    private ListView listView;
    private SectionAdapter sectionAdapter;
    private Node node;

    private int preLast;

    //text formatting props
    private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setContentView(R.layout.activity_section);

        Intent intent = getIntent();
        MenuState menuState;
        menuState = intent.getParcelableExtra("menuState");
        if (in != null) {
            menuState = in.getParcelable("menuState");
        }
        if(menuState != null){//menuState means it came in from the menu
            String title = menuState.getCurrNode().getTitle(Util.Lang.EN);
            book = new Book(title);
            lang = menuState.getLang();
            Node root = book.getTOCroots().get(0);
            node = Node.getFirstDescendant(root);
        }
        else { // no menuState means it came in from TOC
            lang = (Util.Lang) intent.getSerializableExtra("lang");
            Integer nodeHash = intent.getIntExtra("nodeHash", -1);
            firstLoadedChap = intent.getIntExtra("firstLoadedChap",0);
            if(in != null){
                lang = (Util.Lang) in.getSerializable("lang");
                nodeHash = in.getInt("nodeHash",-1);
                firstLoadedChap = in.getInt("firstLoadedChap", 0);
            }
            lastLoadedChapter = firstLoadedChap -1;
            Log.d("Section","firstLoadedChap init:" + firstLoadedChap);
            node = Node.getSavedNode(nodeHash);
            book = new Book(node.getBid());
        }
        init();
    }

    private void init() {
        setTitle(book.getTitle(lang));
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);
        listView = (ListView) findViewById(R.id.listview);

        sectionAdapter = new SectionAdapter(this,R.layout.adapter_text_mono,new ArrayList<Text>(),Util.Lang.HE,20);

        listView.setAdapter(sectionAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        MenuNode menuNode = new MenuNode(book.getTitle(Util.Lang.EN),book.getTitle(Util.Lang.HE),null);
        CustomActionbar cab = new CustomActionbar(this, menuNode, Util.Lang.EN,searchClick,null,titleClick,menuClick);
        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        abRoot.addView(cab);



        if (!isLoadingSection) {
            AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
            als.execute();
        }

    }

    private void toggleTextMenu() {
        if (isTextMenuVisible) {
            textMenuRoot.removeAllViews();
        } else {
            TextMenuBar tmb = new TextMenuBar(SectionActivity.this,textMenuBtnClick);
            textMenuRoot.addView(tmb);
        }
        isTextMenuVisible = !isTextMenuVisible;
    }

    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SectionActivity.this, HomeActivity.class);
            intent.putExtra("searchClicked",true);
            intent.putExtra("isPopup",true);
            startActivity(intent);
        }
    };


    View.OnClickListener titleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(SectionActivity.this, TOCActivity.class);
            intent.putExtra("currBook",book);
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

            boolean shouldClose = true; //should close menu after click
            switch (v.getId()) {
                case R.id.en_btn:
                    sectionAdapter.setLang(Util.Lang.EN);
                    break;
                case R.id.he_btn:
                    sectionAdapter.setLang(Util.Lang.HE);
                    break;
                case R.id.bi_btn:
                    sectionAdapter.setLang(Util.Lang.BI);
                    break;
                case R.id.cts_btn:
                    //isCts = true;
                    break;
                case R.id.sep_btn:
                    //isCts = false;
                    break;
                case R.id.white_btn:
                    Log.d("text", "WHITE");
                    break;
                case R.id.grey_btn:
                    Log.d("text","GREY");
                    break;
                case R.id.black_btn:
                    Log.d("text","BLACK");
                    break;
                case R.id.small_btn:
                    Log.d("text","SMALL");
                    sectionAdapter.incrementTextSize(false);
                    shouldClose = false;
                    break;
                case R.id.big_btn:
                    Log.d("text","BIG");
                    sectionAdapter.incrementTextSize(true);
                    shouldClose = false;
                    break;
            }

            if (shouldClose) toggleTextMenu();

        }
    };

    @Override
    public void onScroll(AbsListView lw, final int firstVisibleItem,
                         final int visibleItemCount, final int totalItemCount) {

        switch(lw.getId()) {
            case R.id.listview:

                // Make your calculation stuff here. You have all your
                // needed info from the parameters of this function.

                // Sample calculation to determine if the last
                // item is fully visible.
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount) {
                    if(preLast!=lastItem){ //to avoid multiple calls for last item
                        Log.d("Last", "Last");
                        preLast = lastItem;
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
                        als.execute();
                    }
                }
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //blah...
    }

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

                //TextChapterHeader tch = new TextChapterHeader(TextActivity.this,tempNode,lang,textSize);
                //textChapterHeaders.add(tch);
                //textRoot.addView(tch);
                sectionAdapter.add(new Text(true,levels[WHERE_PAGE-1]));
            }

            sectionAdapter.addAll(textsList);

            isLoadingSection = false;

            //SectionView sv = new SectionView(TextActivity.this,textsList,lang,isCts,textSize);

            /*if (dir == null || dir == TextEnums.NEXT_SECTION)
                textRoot.addView(content); //add to end by default
            else if (dir == TextEnums.PREV_SECTION)
                textRoot.addView(content,0); //add to before*/
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

            Log.d("Section","firstLoadedChap in loadSection" + firstLoadedChap);
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
                textsList = node.getTexts(tempChap);
                //textsList = Text.get(book, levels); //TODO remove this method as it shuold always be using nodes (so that it can handle complex stuff)
                return textsList;
            } catch (API.APIException e) {
                Toast.makeText(SectionActivity.this, "API Exception!!!", Toast.LENGTH_SHORT).show();
                return new ArrayList<>();
            }


        }

    }

}
