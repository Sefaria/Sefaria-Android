package org.sefaria.sefaria.LinkElements;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

/**
 * This adapter is used on the main link page to show the link counts for each category / book
 */
public class LinkMainAdapter extends RecyclerView.Adapter<LinkMainAdapter.LinkHolder> {

    private List<LinkCount> itemList;
    private SuperTextActivity context;
    private Book book;
    private LinkFragment fragment;

    public class LinkHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView tv;
        public View colorBar;
        public View catPadding;
        private Context context;
        public LinkHolder(View v,SuperTextActivity context) {
            super(v);
            v.setClickable(true);
            v.setOnClickListener(this);
            tv = (TextView) v.findViewById(R.id.tv);
            colorBar = v.findViewById(R.id.color_bar);
            catPadding = v.findViewById(R.id.cat_padding);
            this.context = context;
        }

        @Override
        public void onClick(View v) {
            LinkCount linkCount = itemList.get(getAdapterPosition());
            LinkFragment.State tempState;
            if (linkCount.getDepthType() == LinkCount.DEPTH_TYPE.BOOK) {
                tempState = LinkFragment.State.BOOK;
            } else {
                tempState = LinkFragment.State.CAT;
            }


            fragment.gotoState(tempState,fragment.getView(),linkCount);
        }
    }


    public LinkMainAdapter(SuperTextActivity context, List<LinkCount> itemList, Book book, LinkFragment fragment) {
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
        LinkCount linkCount = itemList.get(position);

        holder.tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));

        Util.Lang lang = Settings.getMenuLang();
        String bookTitle = linkCount.getSlimmedTitle(book, lang);

        if (linkCount.getDepthType() == LinkCount.DEPTH_TYPE.BOOK)  {
            if (linkCount.getCount() == 0) {
                holder.tv.setText(bookTitle);
                holder.tv.setTextColor(Color.parseColor("#999999"));
            } else {
                holder.tv.setText(bookTitle + " (" + linkCount.getCount() + ")");
                holder.tv.setTextColor(context.getResources().getColor(android.R.color.black));
            }

            if (android.os.Build.VERSION.SDK_INT >= 14) {
                holder.tv.setAllCaps(false);
            }
            holder.colorBar.setVisibility(View.GONE);
            holder.catPadding.setVisibility(View.GONE);


        } else { //ALL and CAT
            holder.tv.setText(bookTitle + " " + Util.LINK_CAT_VERICAL_LINE + " " + linkCount.getCount());
            holder.tv.setTextColor(context.getResources().getColor(android.R.color.black));
            if (android.os.Build.VERSION.SDK_INT >= 14) {//for older things it just will by non-capped (even though we can make a function to fix it, it's not worth it).
                holder.tv.setAllCaps(true);
            }
            holder.colorBar.setVisibility(View.VISIBLE);
            holder.catPadding.setVisibility(View.INVISIBLE); //just so it takes up space
            int color = MyApp.getCatColor(linkCount.getRealTitle(Util.Lang.EN));
            if (color != -1) {
                holder.colorBar.setBackgroundColor(context.getResources().getColor(color));
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public void setItemList(List<LinkCount> items) {
        itemList = items;
        notifyDataSetChanged();
    }

    public LinkCount getItem(int position) {
        return itemList.get(position);
    }

}
