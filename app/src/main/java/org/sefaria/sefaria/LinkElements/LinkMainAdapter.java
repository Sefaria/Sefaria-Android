package org.sefaria.sefaria.LinkElements;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
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
import org.sefaria.sefaria.database.LinkFilter;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * This adapter is used on the main link page to show the link counts for each category / book
 */
public class LinkMainAdapter extends RecyclerView.Adapter<LinkMainAdapter.LinkHolder> {

    private List<LinkFilter> itemList;
    private SuperTextActivity context;
    private Book book;
    private LinkFragment fragment;

    public class LinkHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public SefariaTextView tv;
        public View colorBar;
        public View catPadding;
        private Context context;
        public LinkHolder(View v, SuperTextActivity context) {
            super(v);
            v.setClickable(true);
            v.setOnClickListener(this);
            tv = (SefariaTextView) v.findViewById(R.id.tv);
            colorBar = v.findViewById(R.id.color_bar);
            catPadding = v.findViewById(R.id.cat_padding);
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            LinkFilter linkFilter = itemList.get(getAdapterPosition());
            LinkFragment.State tempState;
            if (linkFilter.getDepthType() == LinkFilter.DEPTH_TYPE.BOOK) {
                tempState = LinkFragment.State.BOOK;
            } else if(linkFilter.getDepthType() == LinkFilter.DEPTH_TYPE.ERR){
                return;
            }else {
                tempState = LinkFragment.State.CAT;
            }


            fragment.gotoState(tempState,fragment.getView(),linkFilter);
        }
    }


    public LinkMainAdapter(SuperTextActivity context, List<LinkFilter> itemList, Book book, LinkFragment fragment) {
        this.itemList = itemList;
        this.context = context;
        this.book = book;
        this.fragment = fragment;
    }

    @Override
    public LinkHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.link_category, null);
        LinkHolder linkHolder = new LinkHolder(layoutView,context);
        return linkHolder;
    }

    @Override
    public void onBindViewHolder(LinkHolder holder, int position) {
        LinkFilter linkCount = itemList.get(position);

        Util.Lang lang = Settings.getMenuLang();
        String bookTitle = linkCount.getGroupTitle(lang);
        holder.tv.setFont(lang,true);

        String fadedTextHexColor = String.format("#%06X", (0xFFFFFF & Util.getColor(context,R.attr.text_color_faded)));
        if (linkCount.getDepthType() == LinkFilter.DEPTH_TYPE.BOOK)  {
            if (linkCount.getCount() == 0) {
                holder.tv.setText(bookTitle);
                holder.tv.setTextColor(Util.getColor(context,R.attr.text_color_faded));
            } else {
                holder.tv.setText(Html.fromHtml(bookTitle + " <font color="+fadedTextHexColor+">(" + linkCount.getCount() + ")</font>"));
                holder.tv.setTextColor(Util.getColor(context, R.attr.text_color_main));
            }

            if (android.os.Build.VERSION.SDK_INT >= 14) {
                holder.tv.setAllCaps(false);
            }
            holder.colorBar.setVisibility(View.GONE);
            holder.catPadding.setVisibility(View.GONE);


        } else if(linkCount.getDepthType() == LinkFilter.DEPTH_TYPE.CAT || linkCount.getDepthType() == LinkFilter.DEPTH_TYPE.ALL ){ //ALL and CAT
            String text =  bookTitle.toUpperCase() + " " + Util.LINK_CAT_VERTICAL_LINE + " <font color="+fadedTextHexColor+">" + linkCount.getCount() + "</font>";
            holder.tv.setText(Html.fromHtml(text));
            holder.tv.setTextColor(Util.getColor(context, R.attr.text_color_main));
            /*if (android.os.Build.VERSION.SDK_INT >= 14) {//for older things it just will by non-capped (even though we can make a function to fix it, it's not worth it).
                holder.tv.setAllCaps(true);
            }*/
            holder.colorBar.setVisibility(View.VISIBLE);
            holder.catPadding.setVisibility(View.INVISIBLE); //just so it takes up space
            int color = MyApp.getCatColor(linkCount.getRealTitle(Util.Lang.EN));
            if (color != -1) {
                holder.colorBar.setBackgroundColor(context.getResources().getColor(color));
            }
        }
        else if(linkCount.getDepthType() == LinkFilter.DEPTH_TYPE.ERR ){ //ERR
            String text =  bookTitle;
            holder.tv.setText(Html.fromHtml(text));
            holder.tv.setTextColor(Util.getColor(context, R.attr.text_color_main));
            /*if (android.os.Build.VERSION.SDK_INT >= 14) {//for older things it just will by non-capped (even though we can make a function to fix it, it's not worth it).
                holder.tv.setAllCaps(true);
            }*/
            holder.colorBar.setVisibility(View.VISIBLE);
            holder.catPadding.setVisibility(View.INVISIBLE); //just so it takes up space
            int color = MyApp.getCatColor("ERROR");
            if (color != -1) {
                holder.colorBar.setBackgroundColor(context.getResources().getColor(color));
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public void setItemList(List<LinkFilter> items) {
        itemList = items;
        notifyDataSetChanged();
    }

    public LinkFilter getItem(int position) {
        return itemList.get(position);
    }

}
