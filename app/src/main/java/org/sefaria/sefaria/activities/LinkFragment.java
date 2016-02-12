package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.LinkElements.LinkMainAdapter;
import org.sefaria.sefaria.LinkElements.LinkSelectorBar;
import org.sefaria.sefaria.LinkElements.LinkSelectorBarButton;
import org.sefaria.sefaria.LinkElements.LinkTextAdapter;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkCount;
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

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
    private RecyclerView linkRecycler;

    private SuperTextActivity activity;


    private Text segment;
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


        LinearLayout linkSelectorBarRoot = (LinearLayout) view.findViewById(R.id.link_selector_bar_root);

        linkSelectorBar = new LinkSelectorBar(activity,linkSelectorBarButtonClick,linkSelectorBackClick);
        linkSelectorBarRoot.addView(linkSelectorBar);

        //updateFragment((Text) getArguments().getParcelable(ARG_CURR_SECTION), view);
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

    public void gotoState(State state,View view,LinkCount linkCount) {
        currState = state;
        View colorBar = view.findViewById(R.id.main_color_bar);
        TextView noLinksTV = (TextView) view.findViewById(R.id.no_links_tv);
        if (state == State.MAIN) {
            view.setBackgroundColor(getResources().getColor(R.color.menu_background));

            colorBar.setVisibility(View.GONE);
            linkSelectorBar.setVisibility(View.GONE);
            noLinksTV.setVisibility(View.GONE);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(activity,2);
            gridLayoutManager.setSpanSizeLookup(onSpanSizeLookup);

            linkMainAdapter = new LinkMainAdapter(activity,new ArrayList<LinkCount>(),activity.getBook(),this);

            linkRecycler.setLayoutManager(gridLayoutManager);
            linkRecycler.setAdapter(linkMainAdapter);
            updateFragment(segment);

        } else { //CAT and BOOK are very similar
            view.setBackgroundColor(Color.parseColor("#FFFFFF"));

            //update linkSelectorQueue
            linkSelectorBar.add(linkCount,activity.getMenuLang());



            String cat;
            if (linkCount.getDepthType() == LinkCount.DEPTH_TYPE.BOOK) cat = linkCount.getCategory();
            else cat = linkCount.getRealTitle(Util.Lang.EN); //CAT

            colorBar.setVisibility(View.VISIBLE);
            int color = MyApp.getCatColor(cat);
            colorBar.setBackgroundColor(activity.getResources().getColor(color));
            linkSelectorBar.setVisibility(View.VISIBLE);
            noLinksTV.setVisibility(View.VISIBLE);
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false);

            List<Text> linkList = Link.getLinkedTexts(segment,linkCount);

            linkTextAdapter = new LinkTextAdapter(activity,linkList,noLinksTV);
            linkRecycler.setLayoutManager(linearLayoutManager);
            linkRecycler.setAdapter(linkTextAdapter);
            linkTextAdapter.setCurrLinkCount(linkCount,null);

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
    public void updateFragment(Text segment, View view) {
        if (view == null) {
            Log.d("frag","VIEW NULL ;(");
            return;
        }
        if (!dontUpdate || clicked) {
            this.segment = segment;
            clicked = false;
            if(!segment.isChapter()) Log.d("frag", "UPDATE FRAG TEXT " + segment.levels[0]);

            if (currState == State.MAIN) { //load new linkCounts
                LinkCount linkCount = LinkCount.getFromLinks_small(segment);
                linkMainAdapter.setItemList(LinkCount.getList(linkCount));
            } else if (currState == State.BOOK || currState == State.CAT) { //change visibilty of links
                linkTextAdapter.setItemList(Link.getLinkedTexts(segment,linkTextAdapter.getCurrLinkCount()));
            } else { //CAT load new cat links

            }
            linkRecycler.scrollToPosition(0); //reset scroll to top

        } else {
            Log.d("frag", "DONT UPDATE");
        }

    }

    public void updateFragment(Text segment) {
        updateFragment(segment, getView());
    }

    //independent of whether or not you're using linkMainAdapter or linkTextAdapter, notify
    public void notifyDataSetChanged() {
        linkRecycler.getAdapter().notifyDataSetChanged();
        linkSelectorBar.update(activity.getMenuLang());
    }

    public void setDontUpdate(boolean dontUpdate) { this.dontUpdate = dontUpdate; }
    public void setIsOpen(boolean isOpen) { this.isOpen = isOpen; }
    public boolean getIsOpen() { return isOpen; }
    public void setClicked (boolean clicked) { this.clicked = clicked; }
    public void setSegment(Text segment) {
        this.segment = segment;
    }

    public Text getSegment() { return segment; }

    GridLayoutManager.SpanSizeLookup onSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            LinkCount linkCount = linkMainAdapter.getItem(position);
            if (linkCount.getDepthType() != LinkCount.DEPTH_TYPE.BOOK) {
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
            linkTextAdapter.setCurrLinkCount(lsbb.getLinkCount(), segment);
            linkSelectorBar.update(lsbb.getLinkCount(),activity.getMenuLang());
        }
    };


    View.OnClickListener linkSelectorBackClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            gotoState(State.MAIN,getView(),null);
        }
    };

}
