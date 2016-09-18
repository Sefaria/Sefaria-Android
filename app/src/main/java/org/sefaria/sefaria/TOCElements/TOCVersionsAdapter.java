package org.sefaria.sefaria.TOCElements;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * Created by nss on 9/12/16.
 */
public class TOCVersionsAdapter extends ArrayAdapter<TOCVersion> {

    private Context context;
    private int resId;
    private List<TOCVersion> items;

    public TOCVersionsAdapter(Context context,int resId, List<TOCVersion> items) {
        super(context,resId,items);
        this.context = context;
        this.resId = resId;
        this.items = items;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        return stupidGetView(position,view,parent);
    }

    public View getDropDownView(int position, View view, ViewGroup parent) {
        return stupidGetView(position,view,parent);
    }

    private View stupidGetView(int position, View view, ViewGroup parent) {
        TOCVersion item = items.get(position);

        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resId,parent,false);
        }

        SefariaTextView stv = (SefariaTextView) view.findViewById(R.id.tv);

        String text = item.getPrettyString();
        stv.setText(text);

        return view;
    }
}
