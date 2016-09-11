package org.sefaria.sefaria.layouts;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;

/**
 * Created by nss on 6/29/16.
 */
public class ListViewCheckBox extends CheckBox {

    private Context context;
    private int position; //current position of the checkBox in the listview
    private ArrayAdapter adapter;

    public ListViewCheckBox(Context context) {
        super(context);
        init(context);
    }

    public ListViewCheckBox(Context context, AttributeSet attributeSet) {
        super(context,attributeSet);
        init(context);
    }

    public ListViewCheckBox(Context context, AttributeSet attributeSet, int defStyle) {
        super(context,attributeSet,defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = context;
    }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position;}

    public ArrayAdapter getAdapter() { return adapter;}
    public void setAdapter(ArrayAdapter adapter) { this.adapter = adapter;}

}
