package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Context;
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

public class SectionActivity extends SuperTextActivity implements AbsListView.OnScrollListener {
    private ListView listView;
    private SectionAdapter sectionAdapter;

    private int preLast;
    //text formatting props
    private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setContentView(R.layout.activity_section);
        init();
    }

    protected void init() {
        super.init();

        listView = (ListView) findViewById(R.id.listview);
        sectionAdapter = new SectionAdapter(this,R.layout.adapter_text_mono,new ArrayList<Text>());

        listView.setAdapter(sectionAdapter);
        listView.setOnScrollListener(this);
        listView.setDivider(null);

        if (!isLoadingSection) {
            AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
            als.execute();
        }


    }



    protected void setLang(Util.Lang lang) {
        this.lang = lang;
        sectionAdapter.notifyDataSetChanged();
    }

    protected void setIsCts(boolean isCts) {
        //TODO actually, this should never run
    }

    protected void incrementTextSize(boolean isIncrement) {
        float increment = getResources().getDimension(R.dimen.text_font_size_increment);
        if (isIncrement) textSize  += increment;
        else textSize -= increment;

        sectionAdapter.notifyDataSetChanged();
    }


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


    //YOU actually need this function implemented because you're implementing AbsListView, but it's stupid...
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //blah...
    }

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
            if (textsList.size() == 0) return;

            sectionAdapter.add(getSectionHeaderText());
            sectionAdapter.addAll(textsList);
            isLoadingSection = false;

            //SectionView sv = new SectionView(TextActivity.this,textsList,lang,isCts,textSize);

            /*if (dir == null || dir == TextEnums.NEXT_SECTION)
                textRoot.addView(content); //add to end by default
            else if (dir == TextEnums.PREV_SECTION)
                textRoot.addView(content,0); //add to before*/
        }



    }

}
