package org.sefaria.sefaria.menu;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.TextActivity;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 9/11/15.
 */
public class MenuGrid extends LinearLayout {

    private static final int HOME_MENU_OVERFLOW_NUM = 9;

    private Context context;
    private int numColumns;

    private MenuState menuState;
    private List<MenuButton> overflowButtonList;

    private List<MenuElement> menuElementList;
    private List<MenuButtonTab> menuButtonTabList;

    private boolean hasTabs; //does current page tabs (eg with Talmud)
    private boolean limitGridSize;

    //the LinearLayout which contains the actual grid of buttons, as opposed to the tabs
    //which is useful for destroying the page and switching to a new tab
    private LinearLayout gridRoot;
    private LinearLayout tabRoot;

    private boolean flippedForHe; //are the views flipped for hebrew

    public MenuGrid(Context context,int numColumns,MenuState menuState, boolean limitGridSize) {
        super(context);
        this.menuState = menuState;
        this.context = context;
        this.numColumns = numColumns;
        this.limitGridSize = limitGridSize;

        init();
    }

    private void init() {
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(10, 10, 10, 10);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        this.gridRoot = new LinearLayout(context);
        gridRoot.setOrientation(LinearLayout.VERTICAL);
        gridRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        this.addView(gridRoot);


        this.overflowButtonList = new ArrayList<>();
        this.menuElementList = new ArrayList<>();
        this.menuButtonTabList = new ArrayList<>();
        this.hasTabs = false;
        this.flippedForHe = false;


        buildPage();
    }


    private LinearLayout addRow() {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        for (int i = 0; i < numColumns; i++) {
            ll.addView(new MenuButton(context));
        }
        gridRoot.addView(ll);
        return ll;
    }

    public void addSubsection(MenuNode mainNode, List<MenuNode> subNodes, boolean limitGridSize) {
        if (subNodes.size() == 0) return;

        if (mainNode != null) {
            MenuSubtitle ms = new MenuSubtitle(context,mainNode, menuState.getLang());
            menuElementList.add(ms);
            gridRoot.addView(ms);
        }

        int currNodeIndex = 0;

        for (int i = 0; i <= Math.ceil(subNodes.size()/numColumns) && currNodeIndex < subNodes.size(); i++) {
            LinearLayout ll = addRow();
            for (int j = 0; j < numColumns && currNodeIndex < subNodes.size();  j++) {
                MenuButton mb = addElement(subNodes.get(currNodeIndex),mainNode, ll,j);
                if (currNodeIndex >= HOME_MENU_OVERFLOW_NUM-1 && limitGridSize) {
                    mb.setVisibility(View.GONE);
                    overflowButtonList.add(mb);
                }
                currNodeIndex++;
            }
            //add 'more' button in the row which was overflowed
            if (Math.floor(HOME_MENU_OVERFLOW_NUM/numColumns) == i+1 && limitGridSize)
                addMoreButton(ll);
        }
    }

    private void addTabsection(List<MenuNode> nodeList) {
        tabRoot = new LinearLayout(context);
        tabRoot.setOrientation(LinearLayout.HORIZONTAL);
        tabRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tabRoot.setGravity(Gravity.CENTER);
        this.addView(tabRoot, 0); //make sure it's on top

        int count = 0;
        for (MenuNode node : nodeList) {
            //although generally this isn't necessary b/c the nodes come from menuState.getSections
            //this is used when rebuilding after memory dump and nodes come from setHasTabs()
            if (node.getTitle(Util.EN).equals("Commentary")) {
                count++;
                continue;
            }
            if (count > 0 && count < nodeList.size()) {
                LayoutInflater inflater = (LayoutInflater)
                        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                inflater.inflate(R.layout.tab_divider_menu,tabRoot);
            }

            MenuButtonTab mbt = new MenuButtonTab(context,node, menuState.getLang());
            mbt.setOnClickListener(tabButtonClick);
            tabRoot.addView(mbt);
            menuElementList.add(mbt);
            menuButtonTabList.add(mbt);
            count++;
        }
    }

    private MenuButton addElement(MenuNode node, MenuNode sectionNode, LinearLayout ll, int childIndex) {
        ll.removeViewAt(childIndex);
        MenuButton mb = new MenuButton(context, node, sectionNode, menuState.getLang());
        mb.setOnClickListener(menuButtonClick);
        ll.addView(mb, childIndex);

        menuElementList.add(mb);

        return mb;


    }

    //adds the 'Other' button for home page
    private MenuButton addMoreButton(LinearLayout ll) {
        MenuNode moreNode = new MenuNode("More >","עוד >",null,null);
        MenuButton mb = new MenuButton(context,moreNode,null, menuState.getLang());
        mb.setOnClickListener(moreButtonClick);
        ll.addView(mb);

        menuElementList.add(mb);

        return mb;
    }

    public void buildPage() {

        List<MenuNode> sections = new ArrayList<>();
        List<List<MenuNode>> subsections = new ArrayList<>();
        List<MenuNode> nonSections = new ArrayList<>();

        menuState.getPageSections(sections, subsections, nonSections);
        if (menuState.hasTabs()) {
            hasTabs = true;

            addTabsection(nonSections);
            //default to the first tab in the list
            menuState = menuState.goForward(nonSections.get(0), null);
            menuButtonTabList.get(0).setActive(true);

            sections = new ArrayList<>();
            subsections = new ArrayList<>();
            nonSections = new ArrayList<>();
            menuState.getPageSections(sections, subsections, nonSections);
        }
        //MenuNode otherNode = new MenuNode("Other", "עוד", null, null);
        //if (sections.size() == 0) otherNode = null;
        addSubsection(null, nonSections, limitGridSize);

        for (int i = 0; i < sections.size(); i++) {
            addSubsection(sections.get(i),subsections.get(i),false);
        }

        if (getLang() == Util.HE) {
            flippedForHe = true;
            flipViews(true);
        }

    }

    public void setLang(int lang) {
        menuState.setLang(lang);
        if ((lang == Util.HE && !flippedForHe) ||
                (lang == Util.EN && flippedForHe)) {

            flippedForHe = lang == Util.HE;
            flipViews(true);
        }


        for (MenuElement me : menuElementList) {
            me.setLang(lang);
        }

    }

    //used when switching languages or building a hebrew page
    //reverse the order of every row in the grid
    private void flipViews(boolean flipTabsAlso) {

        int numChildren;
        if (tabRoot != null && flipTabsAlso) {
            numChildren = tabRoot.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                View tempView = tabRoot.getChildAt(numChildren - 1);
                tabRoot.removeViewAt(numChildren - 1);
                tabRoot.addView(tempView, i);
            }
        }

        for (int i = 0; i < gridRoot.getChildCount(); i++) {
            View v = gridRoot.getChildAt(i);
            if (v instanceof LinearLayout) {
                LinearLayout ll = (LinearLayout) v;
                numChildren = ll.getChildCount();
                for (int j = 0; j < numChildren - 1; j++) {
                    View tempView = ll.getChildAt(numChildren - 1);
                    ll.removeViewAt(numChildren - 1);
                    ll.addView(tempView, j);
                }


            }
        }
    }

    public int getLang() { return menuState.getLang(); }

    public boolean getHasTabs() { return hasTabs; }

    //used when you're rebuilding after memore dump
    //you need to make sure that you add the correct tabs
    public void setHasTabs(boolean hasTabs) {
        this.hasTabs = hasTabs;
        addTabsection(menuState.getCurrNode().getParent().getChildren());
    }

    public void goBack(boolean hasSectionBack, boolean hasTabBack) {
        menuState.goBack(hasSectionBack, hasTabBack);

    }

    public void goHome() {
        menuState.goHome();
    }

    public OnClickListener menuButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            MenuButton mb = (MenuButton) v;
            MenuState newMenuState = menuState.goForward(mb.getNode(), mb.getSectionNode());
            Intent intent;
            if (mb.isBook()) {
                intent = new Intent(context, TextActivity.class);
                //trick to destroy all activities beforehand
                //ComponentName cn = intent.getComponent();
                //Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
                intent.putExtra("menuState", newMenuState);
                context.startActivity(intent);

            }else {
                intent = new Intent(context, MenuActivity.class);
                intent.putExtra("menuState", newMenuState);
                intent.putExtra("hasSectionBack", mb.getSectionNode() != null);
                intent.putExtra("hasTabBack", hasTabs);


                ((Activity)context).startActivityForResult(intent, 0);
            }


        }
    };

    public OnClickListener moreButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setVisibility(View.GONE);
            for (MenuButton mb : overflowButtonList) {
                mb.setVisibility(View.VISIBLE);
            }

        }
    };

    public OnClickListener tabButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            MenuButtonTab mbt = (MenuButtonTab) v;
            for (MenuButtonTab tempMBT : menuButtonTabList) tempMBT.setActive(false);
            mbt.setActive(true);

            gridRoot.removeAllViews();
            menuState = menuState.goBack(false, false);
            menuState = menuState.goForward(mbt.getNode(), null);

            List<MenuNode> sections = new ArrayList<>();
            List<List<MenuNode>> subsections = new ArrayList<>();
            List<MenuNode> nonSections = new ArrayList<>();
            menuState.getPageSections(sections, subsections, nonSections);

            for (int i = 0; i < sections.size(); i++) {
                addSubsection(sections.get(i),subsections.get(i),false);
            }
            addSubsection(null, nonSections, false);

            //don't flip tabs b/c they're already flipped
            if (flippedForHe) flipViews(false);
        }
    };
}
