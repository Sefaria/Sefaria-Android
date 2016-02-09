package org.sefaria.sefaria.LinkElements;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkCount;
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 1/17/16.
 */

public class LinkTextAdapter extends RecyclerView.Adapter<LinkTextAdapter.LinkTextHolder> {

    private List<Text> itemList;
    private SuperTextActivity activity;
    private LinkCount currLinkCount;
    private TextView noLinksTV;

    public class LinkTextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView tv;
        public TextView enVerseNum;
        public TextView heVerseNum;
        public TextView title;

        public LinkTextHolder(View v) {
            super(v);
            v.setOnClickListener(this);
            tv = (TextView) v.findViewById(R.id.tv);
            enVerseNum = (TextView) v.findViewById(R.id.enVerseNum);
            heVerseNum = (TextView) v.findViewById(R.id.heVerseNum);
            title = (TextView) v.findViewById(R.id.title);
        }

        @Override
        public void onClick(View v) {
            Text link = itemList.get(getAdapterPosition());
            SuperTextActivity.startNewTextActivityIntent(activity,link);
        }
    }


    public LinkTextAdapter(SuperTextActivity context, List<Text> itemList, TextView noLinksTV) {
        this.itemList = itemList;
        this.activity = context;
        this.noLinksTV = noLinksTV;

        if (itemList.size() == 0) {
            noLinksTV.setVisibility(View.VISIBLE);
        } else {
            noLinksTV.setVisibility(View.GONE);
        }
    }

    @Override
    public LinkTextHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.link_text_mono, null);
        LinkTextHolder linkHolder = new LinkTextHolder(layoutView);
        return linkHolder;
    }

    @Override
    public void onBindViewHolder(LinkTextHolder holder, int position) {
        Text link = itemList.get(position);
        if (currLinkCount.getCategory().equals("Commentary") && currLinkCount.getDepthType() == LinkCount.DEPTH_TYPE.BOOK) {
            holder.title.setVisibility(View.GONE);
            holder.enVerseNum.setVisibility(View.VISIBLE);
            holder.enVerseNum.setText("" + link.levels[1]);
            holder.enVerseNum.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        } else {
            holder.enVerseNum.setVisibility(View.GONE);
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(Html.fromHtml("<i>" + link.getLocationString(Settings.getMenuLang()) + "</i>"));
            holder.title.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            holder.title.setTextSize(20);
        }
        holder.tv.setText(Html.fromHtml(link.heText + "<br>" + link.enText));
        holder.tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
        holder.tv.setTextSize(20);

    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public void setItemList(List<Text> items) {
        itemList = items;
        if (itemList.size() == 0) {
            noLinksTV.setVisibility(View.VISIBLE);
        } else {
            noLinksTV.setVisibility(View.GONE);
        }
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
