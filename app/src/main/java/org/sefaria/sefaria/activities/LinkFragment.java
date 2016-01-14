package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.Text;

public class LinkFragment extends Fragment {

    public static String ARG_CURR_SECTION = "currSection";

    private OnLinkFragInteractionListener mListener;
    private boolean dontUpdate; //if true, don't update the fragment
    private boolean clicked; //if true, segment has been updated when clicked and you should use that value when updating segment


    private Text segment;

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
        // Required empty public constructor
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
            TextView tv = (TextView) view.findViewById(R.id.tv);
            //RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recview);

            Link.LinkCount linkCount = Link.LinkCount.getFromLinks_small(segment);

            if(!segment.isChapter())
                tv.setText("SEGMENT: " + segment.levels[0] + "\n\n" + Link.LinkCount.getStringTree(linkCount,0));
            else{
                tv.setText("chapter heading: " + segment.enText + " " + segment.heText + "\n\n" + Link.LinkCount.getStringTree(linkCount,0));
            }
        }
    }

    public void updateFragment(Text segment) {
        updateFragment(segment, getView());
    }

    public void setDontUpdate(boolean dontUpdate) { this.dontUpdate = dontUpdate; }
    public void setArgCurrSection(boolean clicked) { this.clicked = clicked; }

}
