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
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 1/17/16.
 */

public class LinkTextAdapter extends RecyclerView.Adapter<LinkTextHolder> {

    private static final int BI_LINK_TEXT_VIEW_TYPE = 1;
    private static final int MONO_LINK_TEXT_VIEW_TYPE = 0;

    private List<Text> itemList;
    private SuperTextActivity activity;
    private LinkFilter currLinkCount;
    private TextView noLinksTV;




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
        View layoutView;
        LinkTextHolder linkHolder;
        if (viewType == MONO_LINK_TEXT_VIEW_TYPE) {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.link_text_mono, null);
            linkHolder = new LinkMonoTextHolder(layoutView,itemList,activity);
        } else {
            layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.link_text_bilingual, null);
            linkHolder = new LinkBiTextHolder(layoutView,itemList,activity);
        }

        return linkHolder;
    }

    @Override
    public int getItemViewType(int position) {
        Util.Lang lang = activity.getTextLang();
        if (lang == Util.Lang.BI) return BI_LINK_TEXT_VIEW_TYPE;
        else return MONO_LINK_TEXT_VIEW_TYPE;
    }

    @Override
    public void onBindViewHolder(LinkTextHolder holder, int position) {

        Util.Lang lang = activity.getTextLang();

        Text link = itemList.get(position);
        boolean showFullTitle = currLinkCount.getCategory().equals("Commentary") && currLinkCount.getDepthType() == LinkFilter.DEPTH_TYPE.BOOK;
        if (showFullTitle) {
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

        if (holder instanceof LinkMonoTextHolder) {
            LinkMonoTextHolder monoHolder = (LinkMonoTextHolder) holder;

            String text;
            if (link.getText(lang).length() == 0)
                text = activity.getResources().getString(R.string.no_text);
            else
                text = link.getText(lang);

            monoHolder.monoTv.setText(Html.fromHtml(text));
            monoHolder.monoTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            monoHolder.monoTv.setTextSize(20);
        } else if (holder instanceof LinkBiTextHolder) {
            LinkBiTextHolder biHolder = (LinkBiTextHolder) holder;

            biHolder.enTv.setVisibility(View.VISIBLE);
            biHolder.heTv.setVisibility(View.VISIBLE);

            String enText = link.getText(Util.Lang.EN);
            String heText = link.getText(Util.Lang.HE);

            if (enText.length() == 0)
                biHolder.enTv.setVisibility(View.GONE);
            else
                biHolder.enTv.setText(Html.fromHtml(enText));
            biHolder.enTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            biHolder.enTv.setTextSize(20);

            if (heText.length() == 0)
                biHolder.heTv.setVisibility(View.GONE);
            else
                biHolder.heTv.setText(Html.fromHtml(heText));
            biHolder.heTv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));
            biHolder.heTv.setTextSize(20);
        }

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
    public void setCurrLinkCount(LinkFilter linkCount, Text segment) {
        //try not to update too often
        if (!linkCount.equals(currLinkCount)) {
            currLinkCount = linkCount;
            if (segment != null) //o/w no need to update itemList. You probably just initialized LinkTextAdapter
                setItemList(Link.getLinkedTexts(segment, currLinkCount));
        }
    }
    public LinkFilter getCurrLinkCount() { return currLinkCount; }

}
