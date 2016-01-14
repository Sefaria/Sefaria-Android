package org.sefaria.sefaria.activities;

import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextElements.SectionAdapter;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.ListViewExt;

import java.util.ArrayList;
import java.util.List;

public class SectionActivity extends SuperTextActivity implements AbsListView.OnScrollListener, LinkFragment.OnLinkFragInteractionListener {

    private static int LINK_FRAG_ANIM_TIME = 300; //ms

    private ListViewExt listView;
    private SectionAdapter sectionAdapter;

    private int preLast;
    //text formatting props
    private boolean isLoadingSection; //to make sure multiple sections don't get loaded at once

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
        listView.setOnScrollStoppedListener(new ListViewExt.OnScrollStoppedListener() {

            public void onScrollStopped() {
                updateFocusedSegment();
            }
        });
        if (!isLoadingSection) {
            AsyncLoadSection als = new AsyncLoadSection(TextEnums.NEXT_SECTION);
            als.execute();
        }


    }



    protected void setTextLang(Util.Lang textLang) {
        this.textLang = textLang;
        sectionAdapter.notifyDataSetChanged();
    }

    protected void setMenuLang(Util.Lang menuLang){
        this.menuLang = menuLang;
        //TODO change the menuItems' lang
    }

    public boolean getIsCts(){ return isCts;}

    protected void setIsCts(boolean isCts) {
        Log.d("sectionActi", "isCts called");
        this.isCts = isCts;
        sectionAdapter.notifyDataSetChanged();
    }

    protected void incrementTextSize(boolean isIncrement) {
        float increment = getResources().getDimension(R.dimen.text_font_size_increment);
        if (isIncrement) textSize  += increment;
        else textSize -= increment;

        sectionAdapter.notifyDataSetChanged();
    }

    private void updateFocusedSegment() {
        float mid = ((float)listView.getHeight())/2;
        int numChildren = listView.getChildCount();

        for (int i = 0; i < numChildren; i++) {
            View v = listView.getChildAt(i);
            if (v.getTop() < mid && v.getBottom() > mid) {
                if (linkFragment != null) {
                    int currInd = i + listView.getFirstVisiblePosition();
                    Text currSeg = sectionAdapter.getItem(currInd);
                    if (currSeg.isChapter()) //TODO maybe make this select the chapter links...but not actually
                        currSeg = sectionAdapter.getItem(currInd + 1);

                    linkFragment.updateFragment(currSeg);
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

                // Make your calculation stuff here. You have all your
                // needed info from the parameters of this function.

                // Sample calculation to determine if the last
                // item is fully visible.
                final int lastItem = firstVisibleItem + visibleItemCount;
                if(lastItem == totalItemCount) {
                    if(preLast!=lastItem){ //to avoid multiple calls for last item
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

    public void onLinkFragAttached() {
        SlideToAbove(findViewById(R.id.linkRoot));
        Log.d("link","ATTACHED");
    }

    ListView.OnItemClickListener onItemClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            View linkRoot = findViewById(R.id.linkRoot);

            if (isLinkOpen) {
                isLinkOpen = false;
                //linkRoot.setVisibility(View.GONE);
                SlideToDown(linkRoot);

            } else {
                isLinkOpen = true;
                if (linkFragment == null) {
                    linkFragment = new LinkFragment();
                    Bundle args = new Bundle();
                    args.putParcelable(LinkFragment.ARG_CURR_SECTION, sectionAdapter.getItem(position));
                    //args.putString("param1", "HIII");
                    //args.putString("param2", "YOOOO");
                    linkFragment.setArguments(args);
                    FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.add(R.id.linkRoot,linkFragment);
                    fragmentTransaction.commit();

                    //SlideToAbove(linkRoot); //TODO make animation work when first clicked. problem is it's starting before linkRoot is filled
                } else {

                    //linkRoot.setVisibility(View.VISIBLE);
                    SlideToAbove(linkRoot);
                }




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
            if (textsList.size() == 0) return;

            Text sectionHeader = getSectionHeaderText();
            sectionAdapter.add(sectionHeader);
            sectionAdapter.addAll(textsList);
            isLoadingSection = false;
        }





    }

    //-----
    //LINK FRAGMENT
    //-----


    //Thank you Farhan Shah! https://stackoverflow.com/questions/20323628/android-layout-animations-from-bottom-to-top-and-top-to-bottom-on-imageview-clic

    public void SlideToAbove(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                v.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                v.clearAnimation();
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                lp.addRule(RelativeLayout.ALIGN_TOP,R.id.useless);
                lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

                listView.setLayoutParams(lp);

                linkFragment.setDontUpdate(false);
            }

        });

    }

    public void SlideToDown(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 1.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                linkFragment.setDontUpdate(true);

                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                lp.addRule(RelativeLayout.ALIGN_TOP,R.id.useless);
                //lp.addRule(RelativeLayout.ABOVE,R.id.linkRoot);

                listView.setLayoutParams(lp);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                v.clearAnimation();

                v.setVisibility(View.GONE);

            }

        });

    }



}
