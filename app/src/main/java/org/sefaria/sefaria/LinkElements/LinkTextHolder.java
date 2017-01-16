package org.sefaria.sefaria.LinkElements;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * Created by nss on 2/9/16.
 */
public class LinkTextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


    public SefariaTextView title;

    private List<Segment> itemList;
    private SuperTextActivity activity;
    private int position;


    /*
    *THIS CLASS SHOULD NEVER BE INSTANTIATED. INSTEAD USE ONE OF ITS SUBCLASSES
     */
    public LinkTextHolder(View v, List<Segment> itemList, SuperTextActivity activity) {
        super(v);
        this.itemList = itemList;
        this.activity = activity;

        v.setOnClickListener(this);
        title = (SefariaTextView) v.findViewById(R.id.title);
    }

    @Override
    public void onClick(View v) {
        Segment link = itemList.get(getLayoutPosition());
        Log.d("SepTextActivity","LINK " + link.getLocationString(Util.Lang.EN) + " POS " + position);
        SuperTextActivity.startNewTextActivityIntent(activity, link,false);
    }

    public void setPosition(int position) { this.position = position; }
    public void setItemList(List<Segment> itemList) { this.itemList = itemList;}
    public List<Segment> getItemList() { return itemList; }
}
