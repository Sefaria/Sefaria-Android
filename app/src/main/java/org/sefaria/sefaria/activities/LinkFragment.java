package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.LinkElements.LinkMainAdapter;
import org.sefaria.sefaria.LinkElements.LinkSelectorBar;
import org.sefaria.sefaria.LinkElements.LinkSelectorBarButton;
import org.sefaria.sefaria.LinkElements.LinkTextAdapter;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

public class LinkFragment extends android.support.v4.app.Fragment {

    public enum State {
        //main shows all linkcounts
        //cat shows all links relevant to one cat - only on one segment
        //book shows all links relevant to one book - on whole section
        MAIN,CAT,BOOK
    }

    public static String ARG_CURR_SECTION = "currSection";

    private OnLinkFragInteractionListener mListener;
    private boolean dontUpdate; //if true, don't update the fragment
    private boolean clicked; //if true, segment has been updated when clicked and you should use that value when updating segment
    private boolean isOpen; //true if fragment is open in activity
    private LinkMainAdapter linkMainAdapter;
    private LinkTextAdapter linkTextAdapter;
    private LinkSelectorBar linkSelectorBar;
    private SefariaTextView linkSelectorBarTitle;
    private RecyclerView linkRecycler;

    private View progressCircle;
    private SuperTextActivity activity;


    private Segment segment;
    private State currState;

    public static LinkFragment newInstance() {
        LinkFragment fragment = new LinkFragment();
        //Bundle args = new Bundle();
        //args.putParcelable(ARG_CURR_SECTION,segment);
        //fragment.setArguments(args);

        return fragment;
    }

    public LinkFragment() {
        currState = State.MAIN;
        isOpen = false;
        clicked = false;
        dontUpdate = false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {

        }

        if (getArguments() != null) {
            //mParam1 = getArguments().getString("param1");
            //mParam2 = getArguments().getString("param2");
            //
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment


        View view = inflater.inflate(R.layout.fragment_link, container, false);
        linkRecycler = (RecyclerView) view.findViewById(R.id.recview);
        progressCircle = view.findViewById(R.id.progressBar);

        LinearLayout linkSelectorBarRoot = (LinearLayout) view.findViewById(R.id.link_selector_bar_root);
        linkSelectorBarTitle = (SefariaTextView) view.findViewById(R.id.link_selector_bar_title);
        linkSelectorBarTitle.setFont(activity.getMenuLang(),false);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            linkSelectorBarTitle.setLetterSpacing(0.1f);
        }

        linkSelectorBar = new LinkSelectorBar(activity,linkSelectorBarButtonClick,linkSelectorBackClick);
        linkSelectorBarRoot.addView(linkSelectorBar);

        //updateFragment((Segment) getArguments().getParcelable(ARG_CURR_SECTION), view);
        gotoState(State.MAIN, view, null);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLinkFragInteractionListener) activity;
            this.activity = (SuperTextActivity) activity;
            updateFragment(segment);
            //mListener.onLinkFragAttached();

        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public State getCurrState() { return currState; }


    //linkCount only necessary when going to CAT or BOOK state
    public void gotoState(State state,View view,LinkFilter linkCount) {
        currState = state;
        View linkBackButton = view.findViewById(R.id.link_back_btn);
        View colorBar = view.findViewById(R.id.main_color_bar);
        View topDivider = view.findViewById(R.id.top_divideer);
        SefariaTextView noLinksTV = (SefariaTextView) view.findViewById(R.id.no_links_tv);
        if (state == State.MAIN) {
            view.setBackgroundColor(Util.getColor(activity, R.attr.link_bg));

            //colorBar.setBackgroundColor(Util.getColor(activity, R.attr.custom_actionbar_border));
            colorBar.setVisibility(View.GONE);
            topDivider.setVisibility(View.VISIBLE);

            linkBackButton.setVisibility(View.INVISIBLE);
            noLinksTV.setVisibility(View.GONE);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(activity,2);
            gridLayoutManager.setSpanSizeLookup(onSpanSizeLookup);

            linkMainAdapter = new LinkMainAdapter(activity,new ArrayList<LinkFilter>(),activity.getBook(),this);

            linkRecycler.setLayoutManager(gridLayoutManager);
            linkRecycler.setAdapter(linkMainAdapter);

            //add margins
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int sideMargin = (int) getContext().getResources().getDimension(R.dimen.main_margin_lr) ;
            lp.setMargins(sideMargin,0,sideMargin,0);
            linkRecycler.setLayoutParams(lp);

            updateFragment(segment);

            //make all buttons gray
            //linkSelectorBar.update(null, activity.getMenuLang());
            linkSelectorBar.setVisibility(View.GONE);
            linkSelectorBarTitle.setVisibility(View.VISIBLE);

        } else { //CAT and BOOK are very similar
            view.setBackgroundColor(Util.getColor(activity,R.attr.text_bg));

            //update linkSelectorQueue
            linkSelectorBarTitle.setVisibility(View.GONE);
            linkSelectorBar.setVisibility(View.VISIBLE);
            linkSelectorBar.add(linkCount, activity.getMenuLang());
            linkBackButton.setVisibility(View.VISIBLE);


            String cat;
            if (linkCount.getDepthType() == LinkFilter.DEPTH_TYPE.BOOK) cat = linkCount.getCategory();
            else cat = linkCount.getRealTitle(Util.Lang.EN); //CAT

            colorBar.setVisibility(View.VISIBLE);
            topDivider.setVisibility(View.GONE);
            int color = MyApp.getCatColor(cat);
            if (color == -1) color = R.color.system;
            colorBar.setBackgroundColor(activity.getResources().getColor(color));
            //linkSelectorBar.setVisibility(View.VISIBLE);
            noLinksTV.setVisibility(View.VISIBLE);
            noLinksTV.setText(Html.fromHtml("<i>" + MyApp.getRString(R.string.no_links_filtered) + "</i>"));
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false);

            List<Segment> linkList = null;

            linkTextAdapter = new LinkTextAdapter(activity, linkList, noLinksTV);
            linkRecycler.setLayoutManager(linearLayoutManager);
            linkRecycler.setAdapter(linkTextAdapter);
            linkTextAdapter.setCurrLinkCount(linkCount,null);

            //remove margins
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int sideMargin = 0;
            lp.setMargins(sideMargin, 0, sideMargin, 0);
            linkRecycler.setLayoutParams(lp);

            AsyncLoadLinks all = new AsyncLoadLinks(linkCount);
            all.execute();
        }
    }

    public interface OnLinkFragInteractionListener {
        // TODO: Update argument type and name
        //public void onLinkFragInteractionListener(Uri uri);
        //public void onLinkFragAttached();
    }



    /**
     *
     * @param segment - used when main list is scrolled and new segment comes into view.
     * @param view - usually from getView() except on first load in which case it's passed in manually
     */
    public void updateFragment(Segment segment, View view) {
        if (view == null) {
            Log.d("frag","VIEW NULL ;(");
            return;
        }
        if (!dontUpdate || clicked) {
            this.segment = segment;
            clicked = false;


            if (currState == State.MAIN) { //load new linkCounts
                AsyncLoadLinkFilter alf = new AsyncLoadLinkFilter();
                alf.execute();
            } else if (currState == State.BOOK || currState == State.CAT) { //change visibility of links
                AsyncLoadLinks all = new AsyncLoadLinks(linkTextAdapter.getCurrLinkCount());
                all.execute();
            } else { //nothing you come here...

            }

        } else {
            //Log.d("frag", "DONT UPDATE");
        }

    }

    public void updateFragment(Segment segment) {
        updateFragment(segment, getView());
    }

    //independent of whether or not you're using linkMainAdapter or linkTextAdapter, notify
    public void notifyDataSetChanged() {
        linkRecycler.getAdapter().notifyDataSetChanged();
        linkSelectorBar.update(activity.getMenuLang());
    }

    public void setDontUpdate(boolean dontUpdate) { this.dontUpdate = dontUpdate; }
    public void setIsOpen(boolean isOpen) {
        if(isOpen)
            GoogleTracker.sendScreen("LinkFragment");
        else
            GoogleTracker.sendScreen("SuperTextActivity");
        this.isOpen = isOpen;
    }
    public boolean getIsOpen() { return isOpen; }
    public void setClicked (boolean clicked) { this.clicked = clicked; }
    public void setSegment(Segment segment) {
        this.segment = segment;
    }

    public Segment getSegment() { return segment; }

    GridLayoutManager.SpanSizeLookup onSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            LinkFilter linkCount = linkMainAdapter.getItem(position);
            if (linkCount.getDepthType() != LinkFilter.DEPTH_TYPE.BOOK) {
                return 2;
            } else {
                return 1;
            }
        }
    };

    View.OnClickListener linkSelectorBarButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LinkSelectorBarButton lsbb = (LinkSelectorBarButton) v;

            //only update if different linkcount selected
            if (lsbb.getLinkCount() != linkTextAdapter.getCurrLinkCount()) {
                gotoState(State.BOOK, getView(), lsbb.getLinkCount());

                linkTextAdapter.setCurrLinkCount(lsbb.getLinkCount(), segment);
                linkSelectorBar.update(lsbb.getLinkCount(), activity.getMenuLang());
            }
        }
    };


    View.OnClickListener linkSelectorBackClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            gotoState(State.MAIN,getView(),null);
        }
    };

    private class AsyncLoadLinkFilter extends AsyncTask<Void, Void, LinkFilter> {

        @Override
        protected void onPreExecute() {
            progressCircle.setVisibility(View.VISIBLE);
        }

        @Override
        protected LinkFilter doInBackground(Void... params) {
            return LinkFilter.getLinkFilters(segment);
        }

        @Override
        protected void onPostExecute(LinkFilter linkFilter) {
            if (!linkSelectorBar.getHasBeenInitialized())
                linkSelectorBar.initialUpdate(linkFilter,activity.getMenuLang());

            linkMainAdapter.setItemList(LinkFilter.getList(linkFilter));

            progressCircle.setVisibility(View.GONE);
        }
    }

    private class AsyncLoadLinks extends AsyncTask<Void, Void, List<Segment>> {

        private LinkFilter linkFilter;

        public AsyncLoadLinks(LinkFilter linkFilter) {
            this.linkFilter = linkFilter;
        }

        @Override
        protected void onPreExecute() {
            progressCircle.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Segment> doInBackground(Void... params) {

            List<Segment> linkList = new ArrayList<>();
            try {
                linkList = Link.getLinkedTexts(segment, this.linkFilter);
            } catch (API.APIException e) {
                API.makeAPIErrorToast(activity);
            }catch (Exception e){
                e.printStackTrace();
            }
            return linkList;
        }

        @Override
        protected void onPostExecute(List<Segment> linkList) {
            linkTextAdapter.setItemList(linkList);
            progressCircle.setVisibility(View.GONE);
            linkRecycler.scrollToPosition(0); //reset scroll to top
        }
    }

}
