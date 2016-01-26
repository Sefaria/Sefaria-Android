package org.sefaria.sefaria.LinkElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.LinkFragment;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkCount;
import org.sefaria.sefaria.database.Text;

import java.util.List;
import java.util.Set;

/**
 * Created by nss on 1/17/16.
 */

public class LinkTextAdapter extends RecyclerView.Adapter<LinkTextAdapter.LinkTextHolder> {

    private List<Text> itemList;
    private SuperTextActivity context;
    private LinkCount currLinkCount;

    public class LinkTextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView tv;
        public TextView verseNum;

        public LinkTextHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            tv = (TextView) v.findViewById(R.id.tv);
            verseNum = (TextView) v.findViewById(R.id.verseNum);
        }

        @Override
        public void onClick(View v) {
            Text link = itemList.get(getAdapterPosition());
            SuperTextActivity.startNewTextActivityIntent(context,link);
        }
    }


    public LinkTextAdapter(SuperTextActivity context, List<Text> itemList) {
        this.itemList = itemList;
        this.context = context;
    }

    @Override
    public LinkTextHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.link_text, null);
        LinkTextHolder linkHolder = new LinkTextHolder(layoutView);
        return linkHolder;
    }

    @Override
    public void onBindViewHolder(LinkTextHolder holder, int position) {
        Text link = itemList.get(position);
        holder.verseNum.setText("");//+link.levels[0]);
        holder.tv.setText(Html.fromHtml("<i>" + link.getLocationString(Settings.getSavedMenuLang()) + "</i><br>" + link.heText + "<br>" + link.enText));
        holder.tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        holder.tv.setTextSize(20);

    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public void setItemList(List<Text> items) {
        itemList = items;
        notifyDataSetChanged();
    }

    public Text getItem(int position) {
        return itemList.get(position);
    }

    //segment is used to update text list
    public void setCurrLinkCount(LinkCount linkCount, Text segment) {
        //try not to update too often
        if (!linkCount.equals(currLinkCount)) {
            currLinkCount = linkCount;
            if (segment != null) //o/w no need to update itemList. You probably just initialized LinkTextAdapter
                setItemList(Link.getLinkedTexts(segment, currLinkCount));
        }
    }
    public LinkCount getCurrLinkCount() { return currLinkCount; }

}
