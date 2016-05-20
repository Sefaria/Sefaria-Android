package org.sefaria.sefaria.LinkElements;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * Created by nss on 2/9/16.
 */
public class LinkTextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {


    public SefariaTextView title;

    private List<Text> itemList;
    private SuperTextActivity activity;


    /*
    *THIS CLASS SHOULD NEVER BE INSTANTIATED. INSTEAD USE ONE OF ITS SUBCLASSES
     */
    public LinkTextHolder(View v,List<Text> itemList, SuperTextActivity activity) {
        super(v);
        this.itemList = itemList;
        this.activity = activity;

        v.setOnClickListener(this);
        title = (SefariaTextView) v.findViewById(R.id.title);
    }

    @Override
    public void onClick(View v) {
        Text link = itemList.get(getAdapterPosition());
        SuperTextActivity.startNewTextActivityIntent(activity, link,false);
    }
}
