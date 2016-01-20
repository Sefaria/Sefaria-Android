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
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 1/17/16.
 */

public class LinkTextAdapter extends RecyclerView.Adapter<LinkTextAdapter.LinkTextHolder> {

    private List<Text> itemList;
    private Context context;

    public class LinkTextHolder extends RecyclerView.ViewHolder {
        public TextView tv;
        public TextView verseNum;

        public LinkTextHolder(View v) {
            super(v);
            tv = (TextView) v.findViewById(R.id.tv);
            verseNum = (TextView) v.findViewById(R.id.verseNum);
        }
    }


    public LinkTextAdapter(Context context, List<Text> itemList) {
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
        holder.verseNum.setText(""+link.levels[0]);
        holder.tv.setText(link.heText);
        holder.tv.setTypeface(MyApp.getFont(MyApp.TAAMEY_FRANK_FONT));

    }

    @Override
    public int getItemCount() {
        return this.itemList.size();
    }

}
