package org.sefaria.sefaria.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.sefaria.sefaria.Dialog.DialogManager2;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TextElements.SepTextAdapter;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.TextElements.TextListView;

import java.util.ArrayList;
import java.util.List;

public class SepTextActivity extends SuperTextActivity implements AbsListView.OnScrollListener, LinkFragment.OnLinkFragInteractionListener {

    private TextListView listView;
    private SepTextAdapter sepTextAdapter;

    private int preLast;
    private Segment problemLoadedSegment;

    private int scrolledDownTimes = 0;
    //segment formatting props
    //private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once


    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        if(!goodOnCreate){
            finish();
            return;
        }
        setContentView(R.layout.activity_sep_text);

        init();
    }


    protected void init() {
        super.init();
        listView = (TextListView) findViewById(R.id.listview);
        sepTextAdapter = new SepTextAdapter(this,R.layout.adapter_text_mono,new ArrayList<Segment>());


        listView.setAdapter(sepTextAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        listView.setOnItemClickListener(onItemClickListener);
        //listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnScrollStoppedListener(new TextListView.OnScrollStoppedListener() {

            public void onScrollStopped() {
                updateFocusedSegment();
            }
        });


        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION,null);
        als.preExecute();



        registerForContextMenu(listView);
        initTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!veryFirstTime) {
            menuLang = Settings.getMenuLang();
            sepTextAdapter.notifyDataSetChanged();
        }
    }


    /*CONTEXT MENU */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e("SepTextActivity", "bad menuInfo", e);
            return;
        }

        Segment segment = sepTextAdapter.getItem(info.position);
        //it sets the title of the menu to loc string
        String header;
        if(segment.isChapter())
            header = segment.getText(getMenuLang());
        else
            header = segment.getLocationString(getMenuLang());
        menu.setHeaderTitle(header);
        //menu.setHeaderIcon(something.getIcon());
        int spot = 0;
        menu.add(0, v.getId(), spot++, CONTEXT_MENU_COPY_TITLE);
        if(!segment.isChapter()) {
            menu.add(0, v.getId(), spot++, CONTEXT_MENU_SEND_CORRECTION);
            menu.add(0, v.getId(), spot++, CONTEXT_MENU_SHARE);
            menu.add(0, v.getId(), spot++, CONTEXT_MENU_VISIT);
            menu.add(0, v.getId(), spot++, CONTEXT_MENU_SHORTCUT);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e("SepTextActivity", "bad menuInfo", e);
            return false;
        }

        Segment segment = sepTextAdapter.getItem(info.position);
        CharSequence title = item.getTitle();

        if (title == CONTEXT_MENU_COPY_TITLE) {
            copyText(segment);
        } else  if (title == CONTEXT_MENU_SEND_CORRECTION){
            sendCorrection(segment);
        } else  if (title == CONTEXT_MENU_SHARE){
            share(segment);
        } else  if (title == CONTEXT_MENU_VISIT){
            visit(segment);
        } else if(title == CONTEXT_MENU_PIN){
            pin(segment);
        }else if(title == CONTEXT_MENU_SHORTCUT){
            createShortcut(segment);
        }

        //stop processing menu event
        return true;
    }



    private void pin(Segment segment){
        Settings.RecentTexts.addBookmark(segment);
    }

    private void visit(Segment segment){
        try{
            String url = segment.getURL(true);
            if(url.length() <1){
                Toast.makeText(SepTextActivity.this,"Unable to go to site",Toast.LENGTH_SHORT).show();
                return;
            }
            MyApp.openURLInBrowser(this, url);
        }catch (Exception e){
            Log.e("SepTextActivity", e.getMessage());
            Toast.makeText(SepTextActivity.this,"Unable to go to site",Toast.LENGTH_SHORT).show();
            GoogleTracker.sendException(e);
        }
    }

    private void share(Segment segment){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String str = null;
        try {
            str = segment.getURL(true) + "\n\n";
        } catch (Book.BookNotFoundException e) {
            e.printStackTrace();
        }
        if(str == null)
            str = "";

        String plain_str = str + Html.fromHtml(segment.getText(textLang));
        String html_str = str + segment.getText(textLang);

        sendIntent.putExtra(Intent.EXTRA_TEXT, plain_str);

        if(MyApp.validSDKVersion(16)) {
            sendIntent.putExtra(Intent.EXTRA_HTML_TEXT, html_str);
        }
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, MyApp.getRString(R.string.send_to)));
    }

    private void sendCorrection(Segment segment){
        DialogManager2.showDialog(this,DialogManager2.DialogPreset.HOW_TO_REPORT_CORRECTIONS, segment);
    }

    private void copyText(Segment segment){
        // Gets a handle to the clipboard service.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        // Creates a new segment clip to put on the clipboard
        String copiedText;
        if(!segment.isChapter()) {
            copiedText = Html.fromHtml(segment.getText(textLang)).toString();
        }else{
            try {
                List<Segment> list = segment.parentNode.getTexts();
                StringBuilder wholeChap = new StringBuilder();
                String url = null;
                for(Segment subSegment :list){
                    if(url == null) {
                        try{
                            url = subSegment.getURL(true);
                        }catch (Exception e){
                            url = "";
                        }
                    }
                    wholeChap.append("(" + subSegment.levels[0] + ") " + Html.fromHtml(subSegment.getText(textLang)) + "\n\n\n");
                }
                copiedText = url + "\n\n\n" + wholeChap.toString();
            } catch (API.APIException e) {
                API.makeAPIErrorToast(SepTextActivity.this);
                copiedText = "";
            }
        }

        ClipData clip = ClipData.newPlainText("Sefaria Segment", copiedText);
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip);
        Toast.makeText(SepTextActivity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
    }

    protected void setTextLang(Util.Lang textLang) {



        this.textLang = textLang;

        if (textLang != Util.Lang.BI && isCts && canBeCts(book)) {
            setIsCts(isCts,true); //force a restart so that your iscts setting is applied
        }

        sepTextAdapter.notifyDataSetChanged();
        linkFragment.notifyDataSetChanged();
    }

    @Override
    protected void setIsSideBySide(boolean isSideBySide) {
        super.setIsSideBySide(isSideBySide);
        sepTextAdapter.notifyDataSetChanged();
    }

    public void setTextSize(float textSize) {
        super.setTextSize(textSize);
        sepTextAdapter.notifyDataSetChanged();
        linkFragment.notifyDataSetChanged();
    }

    protected void updateFocusedSegment() {
        int numChildren = listView.getChildCount();

        for (int i = 0; i < numChildren; i++) {
            View v = listView.getChildAt(i);
            if (v.getTop() <= SEGMENT_SELECTOR_LINE_FROM_TOP && v.getBottom() > SEGMENT_SELECTOR_LINE_FROM_TOP) {
                if (linkFragment.getIsOpen()) {
                    int currInd = i + listView.getFirstVisiblePosition();
                    Segment currSeg = sepTextAdapter.getItem(currInd);
                    if (currSeg.isChapter()) {//TODO maybe make this select the chapter links...but not actually
                        currSeg = sepTextAdapter.getItem(currInd + 1);
                    }

                    if (currSeg.equals(linkFragment.getSegment())) return; //no need to update


                    linkFragment.updateFragment(currSeg);
                    sepTextAdapter.notifyDataSetChanged(); //redraw visible views to make current segment view darker

                }
                break;
            }
        }

    }


    @Override
    public void onScroll(AbsListView lw, final int firstVisibleItem,
                         final int visibleItemCount, final int totalItemCount) {

        switch(lw.getId()) {
            case R.id.listview:
                if (!isLoadingSection && !isLoadingInit) {
                    int lastItem = firstVisibleItem + visibleItemCount;
                    //Extra condition to make sure it doesn't load multiple prev sections in a row
                    //wait 2 secs from init until you load prev
                    //Log.d("SepTextActivity","TIME = " + (System.currentTimeMillis() - initTime));
                    try {
                        if (firstVisibleItem == 0 && listView.getViewByPosition(firstVisibleItem).getTop() >= 0 && System.currentTimeMillis() - initTime > PREV_DELAY_TIME) {
                            //Log.d("SepTextActivity","STARTING PREV");
                            AsyncLoadSection als = new AsyncLoadSection(TextEnums.PREV_SECTION, sepTextAdapter.getItem(0));
                            als.preExecute();
                        }
                        if (lastItem == totalItemCount) {
                            //Log.d("SepTextActivity","STARTING NEXT");
                            preLast = lastItem;
                            AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION, sepTextAdapter.getItem(lastItem - 1));
                            als.preExecute();
                        }

                        Segment topSegment = sepTextAdapter.getItem(firstVisibleItem);
                        setCurrNode(topSegment);
                    } catch (IndexOutOfBoundsException e) {
                        //listview.getViewByPosition might cause an error if you call this at the wrong time
                    }
                } else {
                    //Log.d("SepTextActivity","BLOCKED  ");
                }
        }
    }


    @Override
    protected void jumpToText(Segment segment) {
        final int index = sepTextAdapter.getPosition(segment);
        sepTextAdapter.highlightIncomingText(segment);

        listView.post(new Runnable() {

            @Override
            public void run() {
                listView.setSelection(index);
            }
        });
    }

    /*public void jumpSection(View view) {
        if (view.getId() == R.id.jump_section_down) {
            Log.d("SepTextActivity","DOWN");
        } else if (view.getId() == R.id.jump_section_up) {
            Log.d("SepTextActivity","UP");
        }
    }*/

    //YOU actually need this function implemented because you're implementing AbsListView, but it's stupid...
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //blah...
    }

    ListView.OnItemClickListener onItemClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //close textMenuBar if its open
            if (isTextMenuVisible) {
                toggleTextMenu();
                return;
            }

            if(getFindOnPageIsOpen()){
                findOnPageClose();
            }

            View linkRoot = findViewById(R.id.linkRoot);
            if (linkFragment.getIsOpen()) {

                //linkRoot.setVisibility(View.GONE);
                AnimateLinkFragClose(linkRoot);

            } else {
                sepTextAdapter.highlightIncomingText(null);
                if (view.getTop() > 0) //don't auto-scroll if the segment is super long.
                    listView.smoothScrollToPositionFromTop(position,SuperTextActivity.SEGMENT_SELECTOR_LINE_FROM_TOP,SuperTextActivity.LINK_FRAG_ANIM_TIME);
                linkFragment.setClicked(true);
                linkFragment.updateFragment(sepTextAdapter.getItem(position));
                //linkRoot.setVisibility(View.VISIBLE);
                AnimateLinkFragOpen(linkRoot);
            }

        }
    };


    @Override
    protected void postFindOnPageBackground() {
        sepTextAdapter.notifyDataSetChanged();
    }



    private class AsyncLoadSection extends AsyncTask<Void,Void,List<Segment>> {

        private TextEnums dir;
        private Segment loaderSegment;
        private Segment catalystSegment;
        private LoadSectionResult loadSectionResult;

        /**
         *
         * @param dir - direction in which you want to load a section (either prev or next)
         * @param catalystSegment - the Segment which caused this loading to happen. Important, in case this segment has already failed to generate any new content, meaning that it's either the beginning or end of a book
         */
        public AsyncLoadSection (TextEnums dir, Segment catalystSegment) {
            this.dir = dir;
            this.catalystSegment = catalystSegment;
        }

        public void preExecute() {
            if (catalystSegment == null || !catalystSegment.equals(problemLoadedSegment)) {
                this.execute();
            } else {
                //Log.d("SepTextActivity","Problem segment not loaded");
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isLoadingSection = true;
            loaderSegment = new Segment(true);

            if (this.dir == TextEnums.NEXT_SECTION) {
                sepTextAdapter.add(loaderSegment);
            } else /*if (this.dir == TextEnums.PREV_SECTION)*/ {
                //sepTextAdapter.add(0,loaderSegment);
            }
        }


        protected List<Segment> doInBackground(Void... params) {
            List<Segment> segmentList = null;
            try {
                segmentList = loadSection(dir);
            } catch (API.APIException e) {
                loadSectionResult = LoadSectionResult.API_EXCEPTION;
                segmentList = new ArrayList<>();
            } catch (Node.LastNodeException e) {
                loadSectionResult = LoadSectionResult.LAST_NODE;
                segmentList = new ArrayList<>();
            }
            return segmentList;
        }


        @Override
        protected void onPostExecute(final List<Segment> textsList) {


            if (loadSectionResult == LoadSectionResult.LAST_NODE) {
                problemLoadedSegment = catalystSegment;
                isLoadingSection = false;
                isLoadingInit = false;
                sepTextAdapter.remove(loaderSegment);
                sepTextAdapter.setLoadedLastText();

                return;
            }
            //if (textsList.size() == 0) return;//removed this line so that when it doesn't find segment it continues to look for the next item for segment

            Segment sectionHeader = getSectionHeaderText(dir);
            if(loadSectionResult == LoadSectionResult.API_EXCEPTION) {
                sectionHeader.setChapterHasTexts(false);
            }
            if (dir == TextEnums.NEXT_SECTION) {
                //Log.d("SepTextActivity","ENDING NEXT");
                sepTextAdapter.remove(loaderSegment);
                if(sectionHeader.getText(Util.Lang.EN).length() > 0 || sectionHeader.getText(Util.Lang.HE).length() > 0)
                    sepTextAdapter.add(sectionHeader);
                sepTextAdapter.addAll(textsList);

                if (openToSegment != null) {
                    jumpToText(openToSegment);
                    openToSegment = null;
                }



                scrolledDownTimes++;
                if(openedNewBookTime >0 && !reportedNewBookScroll && scrolledDownTimes==2 && (System.currentTimeMillis() - openedNewBookTime < 10000)){
                    reportedNewBookScroll = true;
                    String category;
                    if(reportedNewBookTOC || reportedNewBookBack)
                        category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                    else
                        category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                    GoogleTracker.sendEvent(category,"Scrolled down",scrolledDownTimes/openedNewBookTime);
                }
            } else /*if (dir == TextEnums.PREV_SECTION)*/ {
                //Log.d("SepTextActivity","ENDING PREV");
                sepTextAdapter.addAll(0, textsList);
                sepTextAdapter.add(0, sectionHeader);
                listView.setSelection(textsList.size()+1);

            }

            isLoadingSection = false;
            isLoadingInit = false;
            isLoadingInit = false;

        }





    }

    /**
     * LINK FRAGMENT
     */

    @Override
    protected void onFinishLinkFragOpen() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW, R.id.actionbarRoot);
        lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

        listView.setLayoutParams(lp);


    }

    @Override
    protected void onStartLinkFragClose() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(RelativeLayout.BELOW,R.id.actionbarRoot);
        //lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

        listView.setLayoutParams(lp);
    }
}
