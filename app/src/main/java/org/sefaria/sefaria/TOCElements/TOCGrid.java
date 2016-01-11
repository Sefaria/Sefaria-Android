package org.sefaria.sefaria.TOCElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.TextActivity;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.activities.MenuActivity;
import org.sefaria.sefaria.layouts.AutoResizeTextView;
import org.sefaria.sefaria.menu.MenuButton;

import java.util.ArrayList;
import java.util.List;



public class TOCGrid extends LinearLayout {


    private Context context;
    private List<TOCNumBox> overflowButtonList;
    private List<TOCTab> TocTabList;
    private boolean hasTabs; //does current page tabs (eg with Talmud)

    //the LinearLayout which contains the actual grid of buttons, as opposed to the tabs
    //which is useful for destroying the page and switching to a new tab
    private TextView bookTitleView;
    private AutoResizeTextView currSectionTitleView;
    private LinearLayout tabRoot;
    private LinearLayout gridRoot;
    private Book book;
    private List<Node> tocNodesRoots;
    private Util.Lang lang;
    private TOCTab lastActivatedTab;

    private int numColumns = 7;

    public TOCGrid(Context context,Book book, List<Node> tocRoots, boolean limitGridSize, Util.Lang lang, String pathDefiningNode) {
        super(context);
        this.tocNodesRoots = tocRoots;
        this.context = context;
        //this.limitGridSize = limitGridSize;
        this.lang = lang;
        this.book = book;

        init(pathDefiningNode);
        setLang(lang);
    }

    private void init(String pathDefiningNode) {
        this.setOrientation(LinearLayout.VERTICAL);
        this.setPadding(10, 10, 10, 10);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));




        this.overflowButtonList = new ArrayList<>();
        this.TocTabList = new ArrayList<>();
        this.hasTabs = true;//lets assume for now... either with enough roots or with commentary


        bookTitleView = new TextView(context);
        bookTitleView.setTextSize(40);
        bookTitleView.setTextColor(Color.BLACK);
        bookTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bookTitleView.setGravity(Gravity.CENTER);
        int bookTitlepaddding =10;
        bookTitleView.setPadding(bookTitlepaddding, bookTitlepaddding, bookTitlepaddding, bookTitlepaddding);
        this.addView(bookTitleView,0);

        currSectionTitleView = new AutoResizeTextView(context);
        currSectionTitleView.setTextColor(getResources().getColor(R.color.toc_curr_section_title));
        currSectionTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int defaultTab = 0;
        try {
            Node node = book.getNodeFromPathStr(pathDefiningNode);
            defaultTab = node.getTocRootNum();
            String sectionTitle = node.getWholeTitle(lang); //TODO move lang to setLang
            currSectionTitleView.setText(sectionTitle);
            currSectionTitleView.setTextSize(40);
            int padding = 8;
            currSectionTitleView.setPadding(padding, padding, padding, padding);
        } catch (Node.InvalidPathException e) {
            currSectionTitleView.setHeight(0);
        }
        currSectionTitleView.setGravity(Gravity.CENTER);
        this.addView(currSectionTitleView, 1);

        tabRoot = makeTabSections(tocNodesRoots);
        this.addView(tabRoot,2);//It's the 2nd view starting with bookTitle and CurrSectionName

        this.gridRoot = new LinearLayout(context);
        gridRoot.setOrientation(LinearLayout.VERTICAL);
        gridRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        gridRoot.setGravity(Gravity.CENTER);
        this.addView(gridRoot,3);

        activateTab(defaultTab);


    }

    /* saving method in case switch  back to non-grid item
    private LinearLayout addRow(LinearLayout linearLayoutRoot) {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));


        for (int i = 0; i < numColumns; i++) {
            ll.addView(new TOCNumBox(context));
        }
        linearLayoutRoot.addView(ll);
        return ll;
    }
    */

    public void addNumGrid(List<Node> gridNodes,LinearLayout linearLayoutRoot) {

        //List<Integer> chaps = node.getChaps();
        if (gridNodes.size() == 0){
            Log.e("Node","Node.addNumGrid() never should have been called with 0 items");
            return;
        }


        //Log.d("grid", "NUM ROWS " + ((int) Math.ceil(gridNodes.size() / numColumns)));

        GridLayout gl = new GridLayout(context);
        gl.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        /**
         * This a hack to switch the order of boxes to go from right to left
         * as described in http://stackoverflow.com/questions/17830300/android-grid-view-place-items-from-right-to-left
         */
        if(lang == Util.Lang.HE) {
            //TODO make sure that this stuff still works (ei is called) when doing setLang()
            gl.setRotationY(180);
            gl.setRotationY(180);
        }

        gl.setRowCount((int) Math.ceil(gridNodes.size()/numColumns));
        gl.setColumnCount(numColumns);
        for (int j = 0; j <  gridNodes.size();  j++) {
            //This operation of creating a new view lots of times (for example, in Araab Turim) is causing it to go really slow
            TOCNumBox tocNumBox = new TOCNumBox(context,gridNodes.get(j), lang);
            if(lang == Util.Lang.HE) {//same hack as above, such that the letters look normal
                tocNumBox.setRotationY(180);
                tocNumBox.setRotationY(180);
            }
            gl.addView(tocNumBox);
        }
        linearLayoutRoot.addView(gl);

        /* saving method in case switch  back to non-grid item
        for (int i = 0; i <= Math.ceil(chaps.size()/numColumns) && currNodeIndex < chaps.size(); i++) {
            LinearLayout linearLayout = addRow(linearLayoutRoot);

            for (int j = 0; j < numColumns && currNodeIndex < chaps.size();  j++) {
                TOCNumBox tocNumBox = new TOCNumBox(context,chaps.get(currNodeIndex), node, lang);
                linearLayout.addView(tocNumBox);
                currNodeIndex++;
            }
        }*/
    }


    private void freshGridRoot(){
        if(gridRoot != null){
            gridRoot.removeAllViews();
        }
    }

    private void activateTab(int num) {
        if(num >= TocTabList.size()){
            num = 0;
        }
        TOCTab tocTab = TocTabList.get(num);
        activateTab(tocTab);
    }

    private void activateTab(TOCTab tocTab) {
        if(tocTab.getActive() && tocTab.getLang() == lang)
            return;
        for (TOCTab tempTocTab : TocTabList) {
            tempTocTab.setActive(false);
        }

        tocTab.setActive(true);

        freshGridRoot();
        Node root = tocTab.getNode();
        if(root != null) {
            displayTree(root, gridRoot, false);
        }else{
            List<Book> commentaries = book.getAllCommentaries();
            displayCommenaries(commentaries,gridRoot);

        }
    }

    private void displayCommenaries(List<Book> commentaries, LinearLayout linearLayout){
        LinearLayout rowLinearLayout = new LinearLayout(context);
        for(int i =0;i<commentaries.size();i++){
            if((i%2) == 0){
                rowLinearLayout = new LinearLayout(context);
                rowLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                if(lang == Util.Lang.HE)
                    rowLinearLayout.setGravity(Gravity.RIGHT);
                else
                    rowLinearLayout.setGravity(Gravity.LEFT);
                rowLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                linearLayout.addView(rowLinearLayout);
            }
            TOCCommentary tocCommentary = new TOCCommentary(context,commentaries.get(i),book,lang);
            rowLinearLayout.addView(tocCommentary);

        }
        return;
    }



    private void displayTree(Node node, LinearLayout linearLayout){
        displayTree(node, linearLayout, true);
    }
    private void displayTree(Node node, LinearLayout linearLayout, boolean displayLevel){
        TOCSectionName tocSectionName = new TOCSectionName(context, node, lang, displayLevel);
        linearLayout.addView(tocSectionName);
        if(lang == Util.Lang.HE) { //TODO make sure this is  still called at setLang()
            tocSectionName.setGravity(Gravity.RIGHT);
        }else {
            tocSectionName.setGravity(Gravity.LEFT);
        }
        List<Node> gridNodes = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            if(!child.isGridItem()) {
                if (gridNodes.size() > 0) {
                    //There's some gridsNodes that haven't been displayed yet
                    addNumGrid(gridNodes, tocSectionName);
                    gridNodes = new ArrayList<>();
                }
                displayTree(child, tocSectionName);
            }else{
                gridNodes.add(child);
            }
        }
        if (gridNodes.size() > 0) {
            //There's some gridsNodes that haven't been displayed yet
            addNumGrid(gridNodes, tocSectionName);
        }
    }

    private LinearLayout makeTabSections(List<Node> nodeList) {
        Log.d("TOCGrid", "makeTabSections started");
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        tabs.setGravity(Gravity.CENTER);


        Log.d("TOC", "nodeList.size(): " + nodeList.size());
        int numberOfTabs = nodeList.size();
        if(book.getAllCommentaries().size()>0)
            numberOfTabs++;
        for (int i=0;i<numberOfTabs;i++) {
            //ns comment from menu
            //although generally this isn't necessary b/c the nodes come from menuState.getSections
            //this is used when rebuilding after memory dump and nodes come from setHasTabs()
            //


            if(i > 0) { //skip adding the | (line) for the first item
                LayoutInflater inflater = (LayoutInflater)
                        context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

                inflater.inflate(R.layout.tab_divider_menu, tabs);
            }

            TOCTab tocTab;
            if(i<nodeList.size()){
                Node node = nodeList.get(i);
                tocTab = new TOCTab(context, node, lang);
            }else {
                tocTab = new TOCTab(context, lang);//for the last one it's just commentary tab
            }
            tocTab.setOnClickListener(tabButtonClick);
            tabs.addView(tocTab);
            TocTabList.add(tocTab);

        }

        Log.d("TOCGrid", "makeTabSections started");
        return tabs;
    }



    public void setLang(Util.Lang lang) {
        this.lang = lang;
        bookTitleView.setText(book.getTitle(lang));
        //TODO also setLang of all the Header feilds
        for (TOCTab tempTocTab : TocTabList) {
            if(tempTocTab.getActive()){
                activateTab(tempTocTab);
            }
        }

    }


    public Util.Lang getLang() { return lang; }


    /*
    //used when you're rebuilding after memore dump
    //you need to make sure that you add the correct tabs
    public void setHasTabs(boolean hasTabs) {
        this.hasTabs = hasTabs;
        addTabsections(tocRoots);
    }
    */
    public void goBack(boolean hasSectionBack, boolean hasTabBack) {
        //menuState.goBack(hasSectionBack, hasTabBack);

    }

    public void goHome() {
        ;//menuState.goHome();
    }

    public OnClickListener menuButtonClick = new OnClickListener() {
        @Override
        public void onClick(View v) {
            MenuButton mb = (MenuButton) v;
            //MenuState newMenuState = menuState.goForward(mb.getNode(), mb.getSectionNode());
            Intent intent;
            if (mb.isBook()) {
                intent = new Intent(context, TextActivity.class);
                //trick to destroy all activities beforehand
                //ComponentName cn = intent.getComponent();
                //Intent mainIntent = IntentCompat.makeRestartActivityTask(cn);
                //intent.putExtra("menuState", newMenuState)

                //jh
                //context.startActivity(intent);

            }else {
                intent = new Intent(context, MenuActivity.class);
                //intent.putExtra("menuState", newMenuState);
                intent.putExtra("hasSectionBack", mb.getSectionNode() != null);
                intent.putExtra("hasTabBack", hasTabs);


                ((Activity)context).startActivityForResult(intent, 0);
            }


        }
    };


    public OnClickListener tabButtonClick = new OnClickListener() {
        @Override
        public void onClick(View view) {
            TOCTab tocTab = (TOCTab) view;
            activateTab(tocTab);
        }
    };
}
