package org.sefaria.sefaria.LinkElements;

import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Link;
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 1/17/16.
 */

public class LinkTextAdapter extends RecyclerView.Adapter<LinkTextHolder> {

    private static final int BI_LINK_TEXT_VIEW_TYPE = 1;
    private static final int MONO_LINK_TEXT_VIEW_TYPE = 0;

    private List<Segment> itemList;
    private SuperTextActivity activity;
    private LinkFilter currLinkCount;
    private SefariaTextView noLinksTV;




    public LinkTextAdapter(SuperTextActivity context, List<Segment> itemList, SefariaTextView noLinksTV) {
        this.itemList = itemList;
        this.activity = context;
        this.noLinksTV = noLinksTV;
        if (itemList == null) itemList = new ArrayList<>();
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
        if (lang == Util.Lang.BI || itemList.get(position).getText(lang).length() == 0)
            return BI_LINK_TEXT_VIEW_TYPE;
        else return MONO_LINK_TEXT_VIEW_TYPE;
    }

    @Override
    public void onBindViewHolder(LinkTextHolder holder, int position) {
        Util.Lang lang = activity.getTextLang();

        Segment link = itemList.get(position);
        if (link.getText(lang).length() == 0) lang = Util.Lang.BI; //TODO noah, make this better.

        holder.title.setVisibility(View.VISIBLE);
        holder.title.setText(Html.fromHtml(link.getLocationString(Settings.getMenuLang())));
        holder.title.setFont(Settings.getMenuLang(),true,activity.getTextSize());
        holder.setPosition(position);

        //itemList might not have updated yet if you switched pesukim. make sure it's up to date
        if (!holder.getItemList().equals(itemList))
            holder.setItemList(itemList);

        if (holder instanceof LinkMonoTextHolder) {
            LinkMonoTextHolder monoHolder = (LinkMonoTextHolder) holder;

            String text;
            if (link.getText(lang).length() == 0)
                text = activity.getResources().getString(R.string.no_text);
            else
                text = link.getText(lang);

            monoHolder.monoTv.setText(Html.fromHtml(Util.getBidiString(text, lang)));
            monoHolder.monoTv.setFont(lang, true, activity.getTextSize());
            monoHolder.monoTv.setLangGravity(lang);

        } else if (holder instanceof LinkBiTextHolder) {
            LinkBiTextHolder biHolder = (LinkBiTextHolder) holder;

            biHolder.enTv.setVisibility(View.VISIBLE);
            biHolder.heTv.setVisibility(View.VISIBLE);

            String enText = link.getText(Util.Lang.EN);
            String heText = link.getText(Util.Lang.HE);

            if (enText.length() == 0)
                biHolder.enTv.setVisibility(View.GONE);
            else
                biHolder.enTv.setText(Html.fromHtml(Util.getBidiString(enText,Util.Lang.EN)));
            biHolder.enTv.setFont(Util.Lang.EN,true,activity.getTextSize());

            if (heText.length() == 0)
                biHolder.heTv.setVisibility(View.GONE);
            else
                biHolder.heTv.setText(Html.fromHtml(Util.getBidiString(heText,Util.Lang.HE)));
            biHolder.heTv.setFont(Util.Lang.HE,true,activity.getTextSize());
        }

    }

    @Override
    public int getItemCount() {
        try {
            return this.itemList.size();
        } catch (NullPointerException e) {
            return 0;
        }
    }

    public void setItemList(List<Segment> items) {
        itemList = items;
        if (itemList.size() == 0) {
            noLinksTV.setVisibility(View.VISIBLE);
        } else {
            noLinksTV.setVisibility(View.GONE);
        }
        notifyDataSetChanged();
    }

    public Segment getItem(int position) {
        return itemList.get(position);
    }

    //segment is used to update segment list
    public void setCurrLinkCount(LinkFilter linkCount, Segment segment) {
        //try not to update too often
        if (!linkCount.equals(currLinkCount)) {
            currLinkCount = linkCount;
            if (segment != null) //o/w no need to update itemList. You probably just initialized LinkTextAdapter
                try {
                    setItemList(Link.getLinkedTexts(segment, currLinkCount));
                } catch (API.APIException e) {
                    setItemList(new ArrayList<Segment>());
                    API.makeAPIErrorToast(activity);
                } catch (Exception e){
                    setItemList(new ArrayList<Segment>());
                    try{
                        Toast.makeText(activity,MyApp.getRString(R.string.error_getting_links),Toast.LENGTH_SHORT).show();
                    }catch (Exception e1){
                        e1.printStackTrace();
                    }
                }
        }
    }
    public LinkFilter getCurrLinkCount() { return currLinkCount; }

}
