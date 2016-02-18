package org.sefaria.sefaria.LinkElements;

import android.view.View;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Text;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.List;

/**
 * Created by nss on 2/9/16.
 */
public class LinkBiTextHolder extends LinkTextHolder {

    public SefariaTextView enTv;
    public SefariaTextView heTv;

    public LinkBiTextHolder(View v,List<Text> itemList, SuperTextActivity activity) {
        super( v,itemList,activity);
        enTv = (SefariaTextView) v.findViewById(R.id.en);
        heTv = (SefariaTextView) v.findViewById(R.id.he);
    }
}
