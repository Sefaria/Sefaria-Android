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
public class LinkMonoTextHolder extends LinkTextHolder{

    public TextView monoTv;

    public LinkMonoTextHolder(View v,List<Text> itemList, SuperTextActivity activity) {
        super(v,itemList,activity);
        monoTv = (TextView) v.findViewById(R.id.monoTv);
    }
}
