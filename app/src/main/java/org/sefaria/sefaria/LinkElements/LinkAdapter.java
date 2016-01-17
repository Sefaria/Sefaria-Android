package org.sefaria.sefaria.LinkElements;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Link;

import java.util.List;

public class LinkAdapter extends RecyclerView.Adapter<LinkAdapter.LinkHolder> {

    private List<Link.LinkCount> itemList;
    private Context context;
    private Book book;

    public class LinkHolder extends RecyclerView.ViewHolder {
        public TextView tv;
        public View colorBar;
        public LinkHolder(View v) {
            super(v);
            tv = (TextView) v.findViewById(R.id.tv);
            colorBar = v.findViewById(R.id.color_bar);
        }
    }


    public LinkAdapter(Context context, List<Link.LinkCount> itemList, Book book) {
        this.itemList = itemList;
        this.context = context;
        this.book = book;
    }

    @Override
    public LinkHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.link_category, null);
        LinkHolder linkHolder = new LinkHolder(layoutView);
        return linkHolder;
    }

    @Override
    public void onBindViewHolder(LinkHolder holder, int position) {
        Link.LinkCount linkCount = itemList.get(position);
        holder.tv.setText(linkCount.getSlimmedTitle(book, Util.Lang.EN) + " (" + linkCount.getCount() + ")");
        holder.tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));

        if (linkCount.getDepthType() == Link.LinkCount.DEPTH_TYPE.CAT) {
            holder.colorBar.setVisibility(View.VISIBLE);
            int color = MyApp.getCatColor(linkCount.getRealTitle(Util.Lang.EN));
            if (color != -1) {
                holder.colorBar.setBackgroundColor(context.getResources().getColor(color));
            }
        } else {
            holder.colorBar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

    public void setItemList(List<Link.LinkCount> items) {
        itemList = items;
        notifyDataSetChanged();
    }

    public Link.LinkCount getItem(int position) {
        return itemList.get(position);
    }
}
