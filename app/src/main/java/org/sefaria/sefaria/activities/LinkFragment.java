package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.sefaria.sefaria.LinkElements.LinkMainAdapter;
import org.sefaria.sefaria.LinkElements.LinkSelectorBarButton;
import org.sefaria.sefaria.LinkElements.LinkTextAdapter;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

public class LinkFragment extends Fragment {

    public enum State {
        //main shows all linkcounts
        //cat shows all links relevant to one cat - only on one segment
        //book shows all links relevant to one book - on whole section
        MAIN,CAT,BOOK
    }
    public static final int MAX_NUM_LINK_SELECTORS = 5;
    public static String ARG_CURR_SECTION = "currSection";

    private OnLinkFragInteractionListener mListener;
    private boolean dontUpdate; //if true, don't update the fragment
    private boolean clicked; //if true, segment has been updated when clicked and you should use that value when updating segment
    private boolean isOpen; //true if fragment is open in activity
    private LinkMainAdapter linkMainAdapter;
    private LinkTextAdapter linkTextAdapter;
    private RecyclerView linkRecycler;
    private LinkedList<Link.LinkCount> linkSelectorQueue; //holds the linkCounts that display the previously selected linkCounts

    private Text segment;
    private State currState;

    public static LinkFragment newInstance(Text segment) {
        LinkFragment fragment = new LinkFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CURR_SECTION,segment);
        //args.putString("param1", param1);
        //args.putString("param2", param2);
        fragment.setArguments(args);

        return fragment;
    }

    public LinkFragment() {
        currState = State.MAIN;
        isOpen = false;
        linkSelectorQueue = new LinkedList<>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString("param1");
            //mParam2 = getArguments().getString("param2");
            //
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment


        View view = inflater.inflate(R.layout.fragment_link, container, false);
        linkRecycler = (RecyclerView) view.findViewById(R.id.recview);
        gotoState(State.MAIN,view,null);
        Log.d("link", "VIEW == NULL " + (view));

        updateFragment((Text) getArguments().getParcelable(ARG_CURR_SECTION), view);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLinkFragInteractionListener) activity;
            mListener.onLinkFragAttached();

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

    public void gotoState(State state,View view,Link.LinkCount linkCount) {
        currState = state;
        SuperTextActivity activity = (SuperTextActivity) getActivity();
        View colorBar = view.findViewById(R.id.main_color_bar);
        View linkSelectionBar = view.findViewById(R.id.link_selection_bar);
        if (state == State.MAIN) {
            colorBar.setVisibility(View.GONE);
            linkSelectionBar.setVisibility(View.GONE);
            GridLayoutManager gridLayoutManager = new GridLayoutManager(activity,2);
            gridLayoutManager.setSpanSizeLookup(onSpanSizeLookup);

            linkMainAdapter = new LinkMainAdapter(activity,new ArrayList<Link.LinkCount>(),activity.getBook(),this);

            linkRecycler.setLayoutManager(gridLayoutManager);
            linkRecycler.setAdapter(linkMainAdapter);
            updateFragment(segment);

        } else { //CAT and BOOK are very similar
            //update linkSelectorQueue
            
            if (linkSelectorQueue.size() >= MAX_NUM_LINK_SELECTORS) linkSelectorQueue.remove();
            linkSelectorQueue.add(linkCount);


            String cat;
            if (linkCount.getDepthType() == Link.LinkCount.DEPTH_TYPE.BOOK) cat = linkCount.getCategory(activity.getBook());
            else cat = linkCount.getRealTitle(Util.Lang.EN); //CAT

            colorBar.setVisibility(View.VISIBLE);
            int color = MyApp.getCatColor(cat);
            colorBar.setBackgroundColor(activity.getResources().getColor(color));
            linkSelectionBar.setVisibility(View.VISIBLE);

            LinearLayout linkSelectionBarList = (LinearLayout) view.findViewById(R.id.link_selection_bar_list);
            linkSelectionBarList.removeAllViews();

            ListIterator<Link.LinkCount> linkIt = linkSelectorQueue.listIterator(linkSelectorQueue.size());
            while(linkIt.hasPrevious()) {
                //add children in reverse order
                LinkSelectorBarButton lssb = new LinkSelectorBarButton(getActivity(),linkIt.previous(),activity.getBook());
                lssb.setOnClickListener(linkSelectorBarButtonClick);
                linkSelectionBarList.addView(lssb);

            }


            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activity,LinearLayoutManager.VERTICAL,false);

            List<Text> linkList = Link.getLinkedTexts(segment,linkCount);

            linkTextAdapter = new LinkTextAdapter(activity,linkList);
            linkRecycler.setLayoutManager(linearLayoutManager);
            linkRecycler.setAdapter(linkTextAdapter);
            linkTextAdapter.setCurrLinkCount(linkCount,null);

        }
    }

    public interface OnLinkFragInteractionListener {
        // TODO: Update argument type and name
        //public void onLinkFragInteractionListener(Uri uri);
        public void onLinkFragAttached();
    }



    /**
     *
     * @param segment - used when main list is scrolled and new segment comes into view. NOT used when segment is clicked
     * @param view - usually from getView() except on first load in which case it's passed in manually
     */
    public void updateFragment(Text segment, View view) {
        if (view == null) return;
        if (!dontUpdate) {
            if (!clicked)
                this.segment = segment;
            else //the value of segment has already been set
                clicked = false;

            if (currState == State.MAIN) { //load new linkCounts
                Link.LinkCount linkCount = Link.LinkCount.getFromLinks_small(segment);
                linkMainAdapter.setItemList(Link.LinkCount.getList(linkCount));
            } else if (currState == State.BOOK) { //change visibilty of links
                linkTextAdapter.setItemList(Link.getLinkedTexts(segment,linkTextAdapter.getCurrLinkCount()));
            } else { //CAT load new cat links

            }
            linkRecycler.scrollToPosition(0); //reset scroll to top
        }
    }

    public void updateFragment(Text segment) {
        updateFragment(segment, getView());
    }

    public void setDontUpdate(boolean dontUpdate) { this.dontUpdate = dontUpdate; }
    public void setIsOpen(boolean isOpen) { this.isOpen = isOpen; }
    public boolean getIsOpen() { return isOpen; }
    public void setArgCurrSection(boolean clicked) { this.clicked = clicked; }
    public Text getSegment() { return segment; }

    GridLayoutManager.SpanSizeLookup onSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            Link.LinkCount linkCount = linkMainAdapter.getItem(position);
            if (linkCount.getDepthType() != Link.LinkCount.DEPTH_TYPE.BOOK) {
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
            linkTextAdapter.setCurrLinkCount(lsbb.getLinkCount(),segment);
        }
    };

}
