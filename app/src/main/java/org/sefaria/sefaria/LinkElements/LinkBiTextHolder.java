package org.sefaria.sefaria.LinkElements;

import android.view.View;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 2/9/16.
 */
public class LinkBiTextHolder extends LinkTextHolder {

    public TextView enTv;
    public TextView heTv;

    public LinkBiTextHolder(View v,List<Text> itemList, SuperTextActivity activity) {
        super( v,itemList,activity);
        enTv = (TextView) v.findViewById(R.id.en);
        heTv = (TextView) v.findViewById(R.id.he);
    }
}
