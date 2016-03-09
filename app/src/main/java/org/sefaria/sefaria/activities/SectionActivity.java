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

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TextElements.SectionAdapter;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.ListViewExt;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends SuperTextActivity implements AbsListView.OnScrollListener, LinkFragment.OnLinkFragInteractionListener {

    private ListViewExt listView;
    private SectionAdapter sectionAdapter;

    private int preLast;

    private int scrolledDownTimes = 0;
    //text formatting props
    //private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once


    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        if(badOnCreate){
            finish();
            return;
        }
        setContentView(R.layout.activity_section);

        init();
    }

    protected void init() {
        super.init();
        listView = (ListViewExt) findViewById(R.id.listview);
        sectionAdapter = new SectionAdapter(this,R.layout.adapter_text_mono,new ArrayList<Text>());


        listView.setAdapter(sectionAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        listView.setOnItemClickListener(onItemClickListener);
        //listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnScrollStoppedListener(new ListViewExt.OnScrollStoppedListener() {

            public void onScrollStopped() {
                updateFocusedSegment();
            }
        });

        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
        als.execute();



        registerForContextMenu(listView);


    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!veryFirstTime) {
            menuLang = Settings.getMenuLang();
            sectionAdapter.notifyDataSetChanged();
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
            Log.e("SectionActivity", "bad menuInfo", e);
            return;
        }

        Text segment = sectionAdapter.getItem(info.position);
        //it sets the title of the menu to loc string
        String header;
        if(segment.isChapter())
            header = segment.getText(getMenuLang());
        else
            header = segment.getLocationString(getMenuLang());
        menu.setHeaderTitle(header);
        //menu.setHeaderIcon(something.getIcon());
        menu.add(0, v.getId(), 0, CONTEXT_MENU_COPY_TITLE);
        if(!segment.isChapter()) {
            menu.add(0, v.getId(), 1, CONTEXT_MENU_SEND_CORRECTION);
            menu.add(0, v.getId(), 2, CONTEXT_MENU_SHARE);
            menu.add(0, v.getId(), 3, CONTEXT_MENU_VISIT);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e("SectionActivity", "bad menuInfo", e);
            return false;
        }

        Text segment = sectionAdapter.getItem(info.position);
        CharSequence title = item.getTitle();

        if (title == CONTEXT_MENU_COPY_TITLE) {
            copyText(segment);
        } else  if (title == CONTEXT_MENU_SEND_CORRECTION){
            sendCorrection(segment);
        } else  if (title == CONTEXT_MENU_SHARE){
            share(segment);
        } else  if (title == CONTEXT_MENU_VISIT){
            visit(segment);
        }

        //stop processing menu event
        return true;
    }

    private void visit(Text text){
        String url = text.getURL(true);
        if(url.length() <1){
            Toast.makeText(SectionActivity.this,"Unable to go to site",Toast.LENGTH_SHORT).show();
            return;
        }
        try{
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }catch (Exception e){
            Log.e("SectionActivity", e.getMessage());
            Toast.makeText(SectionActivity.this,"Unable to go to site",Toast.LENGTH_SHORT).show();
            GoogleTracker.sendException(e,"URL:" + url);
        }
    }

    private void share(Text text){
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        String str = text.getURL() + "\n\n" + Html.fromHtml(text.getText(textLang));

        sendIntent.putExtra(Intent.EXTRA_TEXT,str);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void sendCorrection(Text text){
        String email = "android@sefaria.org";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Sefaria Text Correction");
        emailIntent.putExtra(Intent.EXTRA_TEXT,

                HomeActivity.getEmailHeader()
                + text.getURL() + "\n\n"
                + Html.fromHtml(text.getText(Util.Lang.BI))
                + "\n\nDescribe the error: \n\n"
                );
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    private void copyText(Text text){
        // Gets a handle to the clipboard service.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        // Creates a new text clip to put on the clipboard
        String copiedText;
        if(!text.isChapter()) {
            copiedText = Html.fromHtml(text.getText(textLang)).toString();
        }else{
            try {
                List<Text> list = text.parentNode.getTexts();
                StringBuilder wholeChap = new StringBuilder();
                String url = null;
                for(Text subText:list){
                    if(url == null)
                        url =  subText.getURL();
                    wholeChap.append("(" + subText.levels[0] + ") " + Html.fromHtml(subText.getText(textLang)) + "\n\n\n");
                }
                copiedText = url + "\n\n\n" + wholeChap.toString();
            } catch (API.APIException e) {
                API.makeAPIErrorToast(SectionActivity.this);
                copiedText = "";
            }
        }

        ClipData clip = ClipData.newPlainText("Sefaria Text", copiedText);
        // Set the clipboard's primary clip.
        clipboard.setPrimaryClip(clip);
        Toast.makeText(SectionActivity.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();
    }

    protected void setTextLang(Util.Lang textLang) {
        this.textLang = textLang;
        sectionAdapter.notifyDataSetChanged();
        linkFragment.notifyDataSetChanged();
    }



    protected void setIsCts(boolean isCts) {
        this.isCts = isCts;
        sectionAdapter.notifyDataSetChanged();
    }

    @Override
    protected void setIsSideBySide(boolean isSideBySide) {
        super.setIsSideBySide(isSideBySide);
        sectionAdapter.notifyDataSetChanged();
    }

    protected void incrementTextSize(boolean isIncrement) {
        super.incrementTextSize(isIncrement);
        sectionAdapter.notifyDataSetChanged();
    }

    protected void updateFocusedSegment() {
        float mid = ((float)listView.getHeight())/2;
        int numChildren = listView.getChildCount();



        for (int i = 0; i < numChildren; i++) {
            View v = listView.getChildAt(i);
            if (v.getTop() <= SEGMENT_SELECTOR_LINE_FROM_TOP && v.getBottom() > SEGMENT_SELECTOR_LINE_FROM_TOP) {
                if (linkFragment.getIsOpen()) {
                    int currInd = i + listView.getFirstVisiblePosition();
                    Text currSeg = sectionAdapter.getItem(currInd);

                    if (currSeg.isChapter()) {//TODO maybe make this select the chapter links...but not actually
                        currSeg = sectionAdapter.getItem(currInd + 1);
                    }

                    if (currSeg.equals(linkFragment.getSegment())) return; //no need to update


                    linkFragment.updateFragment(currSeg);
                    sectionAdapter.notifyDataSetChanged(); //redraw visible views to make current segment view darker

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
                    if (firstVisibleItem == 0) {
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.PREV_SECTION);
                        als.execute();
                    }
                    if (lastItem == totalItemCount && preLast != lastItem) {
                        preLast = lastItem;
                        AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
                        als.execute();
                    }

                    Text topSegment = sectionAdapter.getItem(firstVisibleItem);
                    setCurrNode(topSegment);
                }
        }
    }


    @Override
    protected void jumpToText(Text text) {
        Log.d("SectionAct", "calling jump to Text");
        int index = sectionAdapter.getPosition(text);
        Log.d("sec","INDEX " + index);
        listView.setSelection(index);
    }

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

            View linkRoot = findViewById(R.id.linkRoot);
            if (linkFragment.getIsOpen()) {

                //linkRoot.setVisibility(View.GONE);
                AnimateLinkFragClose(linkRoot);

            } else {
                if (view.getTop() > 0) //don't auto-scroll if the text is super long.
                    listView.smoothScrollToPositionFromTop(position,SuperTextActivity.SEGMENT_SELECTOR_LINE_FROM_TOP,SuperTextActivity.LINK_FRAG_ANIM_TIME);
                linkFragment.setClicked(true);
                linkFragment.updateFragment(sectionAdapter.getItem(position));
                //linkRoot.setVisibility(View.VISIBLE);
                AnimateLinkFragOpen(linkRoot);




            }

        }
    };




    public class AsyncLoadSection extends AsyncTask<Void,Void,List<Text>> {

        private TextEnums dir;

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
            isLoadingInit = false;
            if (textsList.size() == 0) return;

            Text sectionHeader = getSectionHeaderText(dir);
            if (dir == TextEnums.NEXT_SECTION) {
                if(sectionHeader.getText(Util.Lang.EN).length() > 0 || sectionHeader.getText(Util.Lang.HE).length() > 0)
                    sectionAdapter.add(sectionHeader);
                sectionAdapter.addAll(textsList);

                scrolledDownTimes++;
                if(openedNewBook >0 && !reportedNewBookScroll && scrolledDownTimes==2 && (System.currentTimeMillis() - openedNewBook < 10000)){
                    reportedNewBookScroll = true;
                    String category;
                    if(reportedNewBookTOC || reportedNewBookBack)
                        category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                    else
                        category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                    GoogleTracker.sendEvent(category,"Scrolled down",scrolledDownTimes/openedNewBook);
                }

            } else if (dir == TextEnums.PREV_SECTION) {
                sectionAdapter.addAll(0, textsList);
                sectionAdapter.add(0, sectionHeader);
                listView.setSelection(textsList.size()+1);
            }

            if (openToText != null) {
                jumpToText(openToText);
                openToText = null;
            }

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
