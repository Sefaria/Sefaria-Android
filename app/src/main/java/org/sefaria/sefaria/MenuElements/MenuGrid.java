package org.sefaria.sefaria.MenuElements;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.activities.MenuActivity;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.activities.TOCActivity;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Database;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nss on 9/11/15.
 */
public class MenuGrid extends LinearLayout {



    private static final int HOME_MENU_OVERFLOW_NUM = 8;

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

    private MenuButton moreMenuButton;

    public MenuGrid(Context context,int numColumns,MenuState menuState, boolean limitGridSize, Util.Lang lang) {
        super(context);
        this.menuState = menuState;
        this.context = context;
        this.numColumns = numColumns;
        this.limitGridSize = limitGridSize;

        init();
        setLang(lang);
    }

    private void init() {
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(10, 10, 10, 10);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        this.gridRoot = new LinearLayout(context);
        gridRoot.setOrientation(LinearLayout.VERTICAL);
        gridRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        gridRoot.setGravity(Gravity.CENTER);
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
        //ll.setGravity(Gravity.CENTER);
        ll.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
        for (int i = 0; i < numColumns; i++) {
            MenuButton menuButton = new MenuButton(context);
            ll.addView(menuButton);
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

    /**
     * add the section tabs for different text structures. For example in Genesis these can be:
     *  Chapters | Parshas | Commentary
     * @param nodeList
     */
    private void addTabsection(List<MenuNode> nodeList) {
        tabRoot = new LinearLayout(context);
        tabRoot.setOrientation(LinearLayout.HORIZONTAL);
        tabRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tabRoot.setGravity(Gravity.CENTER);
        this.addView(tabRoot, 0); //make sure it's on top

        int count = 0;
        for (MenuNode menuNode : nodeList) {
            //although generally this isn't necessary b/c the nodes come from menuState.getSections
            //this is used when rebuilding after memory dump and nodes come from setHasTabs()
            if (menuNode.getTitle(Util.Lang.EN).equals("Commentary") || (menuNode.getTitle(Util.Lang.EN).equals("Rif"))) {
                count++;
                continue;
            }
            if (count > 0 && count < nodeList.size()) {
                LayoutInflater inflater = (LayoutInflater)
                        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                inflater.inflate(R.layout.tab_divider_menu,tabRoot);
            }

            MenuButtonTab mbt = new MenuButtonTab(context,menuNode, menuState.getLang());
            mbt.setOnClickListener(tabButtonClick);
            tabRoot.addView(mbt);
            menuElementList.add(mbt);
            menuButtonTabList.add(mbt);
            count++;
        }
    }

    private MenuButton addElement(MenuNode menuNode, MenuNode sectionNode, LinearLayout ll, int childIndex) {

        ll.removeViewAt(childIndex);
        MenuButton mb = new MenuButton(context, menuNode, sectionNode, menuState.getLang());
        mb.setOnClickListener(menuButtonClick);
        mb.setOnLongClickListener(menuButtonLongClick);
        ll.addView(mb, childIndex);
        menuElementList.add(mb);

        return mb;


    }

    //adds the 'Other' button for home page
    private MenuButton addMoreButton(LinearLayout ll) {
        MenuNode moreNode = new MenuNode("More >","עוד >",null);
        moreMenuButton = new MenuButton(context,moreNode,null, menuState.getLang());
        moreMenuButton.setOnClickListener(moreButtonClick);
        ll.addView(moreMenuButton);

        menuElementList.add(moreMenuButton);

        return moreMenuButton;
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

        //thing that has subsections first (like Shulchan Arukh b/c the rest of Halacha
        for (int i = 0; i < sections.size(); i++) {
            addSubsection(sections.get(i),subsections.get(i),false);
        }

        if(sections.size()>0){
            MenuSubtitle ms = new MenuSubtitle(context,new MenuNode("","",null), Util.Lang.EN);
            menuElementList.add(ms);

            gridRoot.addView(ms);
        }

        addSubsection(null, nonSections, limitGridSize);

        if (getLang() == Util.Lang.HE) {
            flippedForHe = true;
            flipViews(true);
        }

    }

    public void setLang(Util.Lang lang) {
        menuState.setLang(lang);
        if ((lang == Util.Lang.HE && !flippedForHe) ||
                (lang == Util.Lang.EN && flippedForHe)) {

            flippedForHe = lang == Util.Lang.HE;
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

    public Util.Lang getLang() { return menuState.getLang(); }

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

    public OnLongClickListener menuButtonLongClick = new OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return menuClick(v,true);
        }
    };

    public OnClickListener menuButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            menuClick(v, false);
        }
    };

    private boolean menuClick(View v,boolean longClick){
        boolean goToTOC = longClick;
        longClick = false;
        MenuButton mb = (MenuButton) v;
        MenuState newMenuState = menuState.goForward(mb.getNode(), mb.getSectionNode());
        Intent intent;
        if (mb.isBook()) {
            Book book = null;
            try {
                if(!Settings.getUseAPI() && !Database.hasOfflineDB()){ //There's no DB //TODO make it work with API
                    Settings.setUseAPI(true);
                }
                book = new Book(newMenuState.getCurrNode().getTitle(Util.Lang.EN));
                if(goToTOC){
                    intent = TOCActivity.getStartTOCActivityIntent(context, book,null);
                    ///intent = new Intent(context, TOCActivity.class);
                    //intent.putExtra("currBook", book);
                    //intent.putExtra("lang", newMenuState.getLang());
                    context.startActivity(intent);
                    return true;
                }else {
                    SuperTextActivity.startNewTextActivityIntent(context,book,longClick);
                }
            } catch (Book.BookNotFoundException e) {
                Toast.makeText(context,"Sorry, book not found",Toast.LENGTH_SHORT).show();
            }

        }else {
            intent = new Intent(context, MenuActivity.class);
            Bundle options = null;
            if(longClick) {
                intent = MyApp.startNewTab(intent);
                options = ActivityOptionsCompat.makeCustomAnimation(context,R.animator.activity_zoom_in,R.animator.activity_zoom_out).toBundle();
            }
            intent.putExtra("menuState", newMenuState);
            intent.putExtra("hasSectionBack", mb.getSectionNode() != null);
            intent.putExtra("hasTabBack", hasTabs);


            if (longClick) {
                ActivityCompat.startActivityForResult((Activity)context, intent, 0, options);
            } else {
                ((Activity) context).startActivityForResult(intent, 0);
            }
        }

        return false;
    }



    public OnClickListener moreButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            v.setVisibility(View.GONE);
            for (MenuButton mb : overflowButtonList) {
                mb.setVisibility(View.VISIBLE);
            }
        }
    };

    public void closeMoreClick(){
        if(moreMenuButton != null)
            moreMenuButton.setVisibility(View.VISIBLE);
        for (MenuButton mb : overflowButtonList) {
            mb.setVisibility(View.GONE);
        }
    }

    public OnClickListener tabButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            MenuButtonTab mbt = (MenuButtonTab) v;
            for (MenuButtonTab tempMBT : menuButtonTabList) tempMBT.setActive(false);
            mbt.setActive(true);

            gridRoot.removeAllViews();
            menuState = menuState.goBack(false, false);
            menuState = menuState.goForward(mbt.getNode(), null);

            menuElementList = new ArrayList<>();
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
