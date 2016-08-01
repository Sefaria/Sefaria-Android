package org.sefaria.sefaria.SearchElements;

import android.app.Activity;
import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;

import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.SearchAPI;
import org.sefaria.sefaria.layouts.ListViewCheckBox;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by nss on 6/27/16.
 */
public class SearchFilterAdapter extends ArrayAdapter<BilingualNode> {

    private Context context;
    private int resourceId;
    private List<BilingualNode> nodes;
    private boolean[] isCheckedArray;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;
    private View.OnClickListener onCheckBoxClick;
    private boolean isMaster;
    private SearchFilterAdapter slaveAdapter;
    private int masterPosition; //pos of item in master list which is controlling slave list
    private Util.Lang systemLang;


    public SearchFilterAdapter(Context context, int resourceId, List<BilingualNode> objects, CompoundButton.OnCheckedChangeListener checkedChangeListener, View.OnClickListener onCheckBoxClick, SearchFilterAdapter slaveAdapter) {
        super(context,resourceId,objects);

        this.context = context;
        this.resourceId = resourceId;
        this.nodes = objects;
        this.isCheckedArray = new boolean[objects.size()];
        this.onCheckedChangeListener = checkedChangeListener;
        this.onCheckBoxClick = onCheckBoxClick;
        this.slaveAdapter = slaveAdapter;
        this.isMaster = slaveAdapter != null;
        this.masterPosition = -1;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        SearchFilterNode node = (SearchFilterNode) nodes.get(position);
        SearchFilterHolder searchFilterHolder;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater)
                    context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(resourceId,null);

            searchFilterHolder = new SearchFilterHolder();
            searchFilterHolder.tv = (SefariaTextView) view.findViewById(R.id.tv);
            searchFilterHolder.checkBox = (ListViewCheckBox) view.findViewById(R.id.checkbox);
            searchFilterHolder.arrowLeft = (ImageView) view.findViewById(R.id.arrow_left);
            searchFilterHolder.arrowRight = (ImageView) view.findViewById(R.id.arrow_right);
            searchFilterHolder.checkBox.setOnCheckedChangeListener(onCheckedChangeListener);
            searchFilterHolder.checkBox.setOnClickListener(onCheckBoxClick);
            searchFilterHolder.checkBox.setAdapter(this);

            view.setTag(searchFilterHolder);
        } else {
            searchFilterHolder = (SearchFilterHolder) view.getTag();
        }
        String fadedTextHexColor = String.format("#%06X", (0xFFFFFF & Util.getColor(context,R.attr.text_color_faded)));
        searchFilterHolder.tv.setText(Html.fromHtml(node.getTitle(Settings.getMenuLang()) + " <font color="+fadedTextHexColor+">(" + node.getCount() + ")</font>"));
        searchFilterHolder.checkBox.setPosition(position);
        searchFilterHolder.checkBox.setChecked(isCheckedArray[position]);

        if (slaveAdapter != null && slaveAdapter.getMasterPosition() == position) {
            if (Settings.getMenuLang() == Util.Lang.EN) {
                searchFilterHolder.arrowRight.setVisibility(View.VISIBLE);
                searchFilterHolder.arrowLeft.setVisibility(View.GONE);
            } else {
                searchFilterHolder.arrowLeft.setVisibility(View.VISIBLE);
                searchFilterHolder.arrowRight.setVisibility(View.GONE);
            }
        } else {
            searchFilterHolder.arrowLeft.setVisibility(View.GONE);
            searchFilterHolder.arrowRight.setVisibility(View.GONE);
        }
        return view;
    }

    static class SearchFilterHolder {
        SefariaTextView tv;
        ListViewCheckBox checkBox;
        ImageView arrowRight;
        ImageView arrowLeft;
    }

    public void clearAndAdd(List<BilingualNode> objects) {
        clear();
        addAll(objects);
        isCheckedArray = new boolean[objects.size()];
    }

    public void clearAndAdd(List<BilingualNode> objects, List<BilingualNode> currChecked) {
        clearAndAdd(objects);
        for(BilingualNode node : currChecked) {
            int index = objects.indexOf(node);
            if (index != -1) {
                isCheckedArray[index] = true;
            }
        }
    }

    /**
     *
     * @param objects
     * @param isChecked - default value to fill array with
     */
    public void clearAndAdd(List<BilingualNode> objects, boolean isChecked) {
        clearAndAdd(objects);
        for (int i = 0; i < isCheckedArray.length; i++) {
            isCheckedArray[i] = isChecked;
        }
    }

    @Override
    public void addAll(Collection<? extends BilingualNode> collection) {
        List<SearchFilterNode> searchFilterNodes = (List<SearchFilterNode>)collection;
        Collections.sort(searchFilterNodes);
        super.addAll(searchFilterNodes);
    }

    public void setIsCheckedAtPos(boolean isChecked, int position) {
        isCheckedArray[position] = isChecked;
    }

    public boolean getIsCheckedAtPos(int position) {
        return isCheckedArray[position];
    }

    public boolean getIsMaster() { return isMaster; }

    public boolean getIsMasterPosition(int position) {
        return slaveAdapter != null && slaveAdapter.getMasterPosition() == position;
    }

    public void setSlaveCheckBoxes(boolean isChecked) {
        if (slaveAdapter != null) {
            for (int i = 0; i < slaveAdapter.getCount(); i++) {
                slaveAdapter.setIsCheckedAtPos(isChecked,i);
            }
            slaveAdapter.notifyDataSetChanged();
        }

    }

    public int getMasterPosition() { return masterPosition; }
    public void setMasterPosition(int masterPosition) { this.masterPosition = masterPosition; }


}
