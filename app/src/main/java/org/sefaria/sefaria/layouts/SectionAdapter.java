package org.sefaria.sefaria.layouts;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 11/29/15.
 */
public class SectionAdapter extends ArrayAdapter<Text> {

    private Context context;
    private List<Text> texts;

    public SectionAdapter(Context context, int resource, List<Text> objects) {
        super(context,resource,objects);
        this.context = context;
        this.texts = objects;
        Log.d("adapter","TEXT LEN " + texts.size());
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        Text segment = texts.get(position);
        Log.d("adapter","POS " + position);
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.adapter_text_mono,null);
        }
        TextView tv = (TextView) view.findViewById(R.id.mono);
        tv.setText(Html.fromHtml(segment.heText));

        return view;
    }
}
