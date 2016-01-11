package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.database.Text;

public class LinkFragment extends Fragment {

    private OnLinkFragInteractionListener mListener;

    private String mParam1;
    private String mParam2;

    private Text segment;

    public static LinkFragment newInstance(String param1, String param2) {
        LinkFragment fragment = new LinkFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
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
            mParam1 = getArguments().getString("param1");
            mParam2 = getArguments().getString("param2");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_link, container, false);

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnLinkFragInteractionListener) activity;
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
        public void onLinkFragInteractionListener(Uri uri);
    }

    public void setCurrSegment(Text segment) {
        this.segment = segment;
        TextView tv = (TextView) getView().findViewById(R.id.tv);
        tv.setText("THE CURRENT SEG IS " + segment.levels[0]);
    }

}
