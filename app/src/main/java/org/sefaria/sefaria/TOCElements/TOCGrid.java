package org.sefaria.sefaria.TOCElements;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.activities.CtsTextActivity;
import org.sefaria.sefaria.activities.SuperTextActivity;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.activities.MenuActivity;
import org.sefaria.sefaria.layouts.AutoResizeTextView;
import org.sefaria.sefaria.MenuElements.MenuButton;
import org.sefaria.sefaria.layouts.SefariaTextView;

import java.util.ArrayList;
import java.util.List;



public class TOCGrid extends LinearLayout {


    private Context context;
    private List<TOCNumBox> overflowButtonList;
    private List<TOCTab> TocTabList;
    private boolean hasTabs; //does current page tabs (eg with Talmud)

    //the LinearLayout which contains the actual grid of buttons, as opposed to the tabs
    //which is useful for destroying the page and switching to a new tab
    private SefariaTextView bookTitleView;
    private AutoResizeTextView currSectionTitleView;
    private Spinner versionsDropdown;
    private LinearLayout tabRoot;
    private LinearLayout gridRoot;
    private Book book;
    private List<Node> tocNodesRoots;
    private Util.Lang lang;
    private TOCTab lastActivatedTab;
    private String pathDefiningNode;

    private boolean flippedForHe;

    private double regularColumnCount = 0.0;



    public TOCGrid(Context context, Book book, List<Node> tocRoots, boolean limitGridSize, Util.Lang lang, String pathDefiningNode) {
        super(context);
        this.tocNodesRoots = tocRoots;
        this.context = context;
        //this.limitGridSize = limitGridSize;
        this.lang = lang;
        this.book = book;
        this.pathDefiningNode = pathDefiningNode;

        init();
    }

    private void init() {
        this.setOrientation(LinearLayout.VERTICAL);

        int sidePadding = Math.round(Util.dpToPixels(context.getResources().getDimension(R.dimen.main_margin_lr)));
        this.setPadding(sidePadding/2, 10, sidePadding/2, 100);
        this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        this.flippedForHe = false;



        this.overflowButtonList = new ArrayList<>();
        this.TocTabList = new ArrayList<>();
        this.hasTabs = true;//lets assume for now... either with enough roots or with commentary
        int positionNum = 0;

        if(book.isTalmudBavli()) {
            SefariaTextView williamDTalumd = MenuGrid.getWilliamDTalumd(context, 45, 65);
            this.addView(williamDTalumd, positionNum++);
        }

        bookTitleView = new SefariaTextView(context);
        bookTitleView.setFont(lang, true, 25);
        bookTitleView.setTextColor(Util.getColor(context, R.attr.text_color_main));
        bookTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        bookTitleView.setGravity(Gravity.CENTER);
        final int bookTitlepaddding = 10;
        bookTitleView.setPadding(0, 2*bookTitlepaddding, 0, bookTitlepaddding/2);
        this.addView(bookTitleView, positionNum++);

        AutoResizeTextView bookCategoryView = new AutoResizeTextView(context);
        bookCategoryView.setTextColor(getResources().getColor(R.color.toc_curr_section_title));
        bookCategoryView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        bookCategoryView.setText(book.getCategories());
        bookCategoryView.setFont(lang, true, 20);
        final int padding = 6;
        bookCategoryView.setPadding(0, 0, 0, padding);
        bookCategoryView.setGravity(Gravity.CENTER);
        this.addView(bookCategoryView, positionNum++);




        currSectionTitleView = new AutoResizeTextView(context);
        currSectionTitleView.setTextColor(getResources().getColor(R.color.toc_curr_chap));
        currSectionTitleView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        currSectionTitleView.setGravity(Gravity.CENTER);
        this.addView(currSectionTitleView, positionNum++);

        if(book.isTalmudBavli()) {
            View bookVersionInfo = LayoutInflater.from(context).inflate(R.layout.book_version_info, null);
            this.addView(bookVersionInfo, positionNum++);
        }

        View dummySpace = new View(context);
        dummySpace.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,Math.round(Util.dpToPixels(20))));
        this.addView(dummySpace, positionNum++);

        //Alt versions dropdown menu
        versionsDropdown = new Spinner(context,Spinner.MODE_DROPDOWN);
        //versionsDropdown.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        //versionsDropdown.setGravity(Gravity.CENTER);
        this.addView(versionsDropdown, positionNum++);




        int defaultTab = setCurrSectionText();

        //ADD GREY DIVIDER
        View divideer = new View(context);
        LinearLayout.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, 1);
        lp.setMargins(0,Math.round(Util.dpToPixels(30)), 0,Math.round(Util.dpToPixels(20)));
        divideer.setLayoutParams(lp);
        divideer.setBackgroundColor(Color.parseColor("#CCCCCC"));
        this.addView(divideer, positionNum++);

        tabRoot = makeTabSections(tocNodesRoots);
        this.addView(tabRoot, positionNum++);//It's the 3nd view starting with bookTitle and CurrSectionName

        this.gridRoot = new LinearLayout(context);
        gridRoot.setOrientation(LinearLayout.VERTICAL);
        gridRoot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        this.addView(gridRoot, positionNum++);

        TocTabList.get(defaultTab).setActive(true);//set it true, such that the setLang function will start the right tab


        if (getLang() == Util.Lang.HE) {
            flippedForHe = true;
            flipViews();
        }
        setLang(lang);
    }
    /*
    // /* saving method for SDK VERSION < 14 //
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


    private double getRegularColumnCount(){
        if(regularColumnCount == 0.0){
            Point size = MyApp.getScreenSize();
            regularColumnCount = (size.x)/48.0;
            if(regularColumnCount < 1.0)
                regularColumnCount = 1.0;
        }
        return regularColumnCount;
    }

    public void addNumGrid(List<Node> gridNodes,LinearLayout linearLayoutRoot, int depth) {
        //List<Integer> chaps = node.getChaps();
        if (gridNodes.size() == 0){
            Log.e("Node","Node.addNumGrid() never should have been called with 0 items");
            return;
        }




        GridLayout gl = new GridLayout(context);
        //GridLayout gl = new GridLayout(context);
        gl.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        /**
         * This is a hack to switch the order of boxes to go from right to left
         * as described in http://stackoverflow.com/questions/17830300/android-grid-view-place-items-from-right-to-left
         */
        if (lang == Util.Lang.HE) {
            //TODO make sure that this stuff still works (ei is called) when doing setLang()
            gl.setRotationY(180);
            gl.setRotationY(180);
        }

        double numColumns = getRegularColumnCount();
        if(depth > 0) depth--;
        numColumns -= (int) (depth*1);
        if(numColumns <1) numColumns = 1.0;

        gl.setColumnCount((int) numColumns);
        gl.setRowCount((int) Math.ceil(gridNodes.size() / numColumns));


        for (int j = 0; j < gridNodes.size(); j++) {
            //This operation of creating a new view lots of times (for example, in Araab Turim) is causing it to go really slow
            TOCNumBox tocNumBox = new TOCNumBox(context, gridNodes.get(j), lang);
            if (lang == Util.Lang.HE) {//same hack as above, such that the letters look normal
                tocNumBox.setRotationY(180);
                tocNumBox.setRotationY(180);
            }
            gl.addView(tocNumBox);
        }
        linearLayoutRoot.addView(gl);

    /*{
        //Old SDK (looks not as good, but doesn't matter as much)
        int currNodeIndex = 0;
        for (int i = 0; i <= Math.ceil(gridNodes.size() / numColumns) && currNodeIndex < gridNodes.size(); i++) {
            LinearLayout linearLayout = addRow(linearLayoutRoot);

            for (int j = 0; j < numColumns && currNodeIndex < gridNodes.size(); j++) {
                TOCNumBox tocNumBox = new TOCNumBox(context, gridNodes.get(currNodeIndex), lang);
                final int padding = 3;
                linearLayout.setPadding(padding,padding,padding,padding);
                linearLayout.addView(tocNumBox);
                currNodeIndex++;
            }
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
        for (TOCTab tempTocTab : TocTabList) {
            tempTocTab.setActive(false);
        }
        Log.d("TOCGrid","activating tab");
        tocTab.setActive(true);

        freshGridRoot();
        Node root = tocTab.getNode();
        if(root != null) {
            displayTree(root, gridRoot, false);
        }else{
            List<Book> commentaries = book.getAllCommentaries();
            displayCommentaries(commentaries, gridRoot);

        }
    }

    private void displayCommentaries(List<Book> commentaries, LinearLayout linearLayout){
        LinearLayout rowLinearLayout = null;
        final int columnNum = 2;
        for(int i =0;i<commentaries.size();i++){
            if((i%columnNum) == 0){
                rowLinearLayout = new LinearLayout(context);
                rowLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
                //final int padding = 4;
                //rowLinearLayout.setPadding(padding,padding,padding,padding);
                if(lang == Util.Lang.HE)
                    rowLinearLayout.setGravity(Gravity.RIGHT);
                else
                    rowLinearLayout.setGravity(Gravity.LEFT);
                //TODO make it such that the hebrew goes in the right side first
                rowLinearLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                linearLayout.addView(rowLinearLayout);
            }
            TOCCommentary tocCommentary = new TOCCommentary(context,commentaries.get(i),book,lang);
            rowLinearLayout.addView(tocCommentary);
        }
        if(commentaries.size() %columnNum != 0){ //odd number of
            TOCCommentary tocCommentary = new TOCCommentary(context,null,null,Util.Lang.EN);
            tocCommentary.setVisibility(INVISIBLE);
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

        if (lang == Util.Lang.HE) { //TODO make sure this is  still called at setLang()
            tocSectionName.setGravity(Gravity.RIGHT);
        } else {
            tocSectionName.setGravity(Gravity.LEFT);
        }

        List<Node> gridNodes = new ArrayList<>();
        for (int i = 0; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            if(!child.isGridItem()) {
                if (gridNodes.size() > 0) {
                    //There's some gridsNodes that haven't been displayed yet
                    addNumGrid(gridNodes, tocSectionName.getChildrenView(),node.getDepth());
                    gridNodes = new ArrayList<>();
                }
                displayTree(child, tocSectionName.getChildrenView());
            }else{
                gridNodes.add(child);
            }
        }
        if (gridNodes.size() > 0) {
            //There's some gridsNodes that haven't been displayed yet
            tocSectionName.setSubGravity(Gravity.CENTER);//this will make it that the gridItems will be centered on the page (say for all chaps in Genesis, but other things will be left or right
            addNumGrid(gridNodes, tocSectionName.getChildrenView(), node.getDepth());
        }
        if(displayLevel && node.getDepth()>=2){
            tocSectionName.setDisplayingChildren(false);
        }

    }

    private LinearLayout makeTabSections(List<Node> nodeList) {
        //Log.d("TOCGrid", "makeTabSections started");
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        tabs.setPadding(0,0,0,30);

        tabs.setGravity(Gravity.CENTER);


        //Log.d("TOC", "nodeList.size(): " + nodeList.size());
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

                inflater.inflate(R.layout.tab_divideer_menu, tabs);
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

        //if(numberOfTabs == 1) tabs.setVisibility(View.INVISIBLE);//removed so that people know what the structure is a bit better (seeing siman)
        return tabs;
    }



    public void setLang(Util.Lang lang) {
        this.lang = lang;
        /*
        if(lang == Util.Lang.HE){
            gridRoot.setGravity(Gravity.RIGHT);
        }else{
            gridRoot.setGravity(Gravity.LEFT);
        }*/
        gridRoot.setGravity(Gravity.CENTER);
        bookTitleView.setText(book.getTitle(lang));
        bookTitleView.setFont(lang, true, 25);
        setCurrSectionText();
        //TODO also setLang of all the Header feilds
        if ((lang == Util.Lang.HE && !flippedForHe) ||
                (lang == Util.Lang.EN && flippedForHe)) {

            flippedForHe = lang == Util.Lang.HE;
            flipViews();
        }
        for (TOCTab tempTocTab : TocTabList) {
            if(tempTocTab.getActive()){
                activateTab(tempTocTab);
            }
            tempTocTab.setLang(lang);
        }

    }

    private void flipViews() {
        if (tabRoot != null) {
            int numChildren = tabRoot.getChildCount();
            for (int i = 0; i < numChildren; i++) {
                View tempView = tabRoot.getChildAt(numChildren - 1);
                tabRoot.removeViewAt(numChildren - 1);
                tabRoot.addView(tempView, i);
            }
        }
    }

    private void addAlternateTextVersions(Node node){
        if (Downloader.getNetworkStatus() == Downloader.ConnectionType.NONE) { //if there's no internet don't even try to display the versions// otherwise it might use cache and get confusing
            versionsDropdown.setVisibility(View.GONE);
            return;
        }

        if(Build.VERSION.SDK_INT >= 23) {
            versionsDropdown.setBackground(MyApp.getContext().getDrawable(R.drawable.alternate_version_dropdown_spinner));
        }
        try {
            // Alternate versions
            JSONObject textData = new JSONObject(node.getTextFromAPIData(API.TimeoutType.SHORT));
            JSONArray versions = textData.getJSONArray("versions");
            final List<TOCVersion> versionList = new ArrayList<>();
            TOCVersion tocVersion = node.getTextVersion();
            if (tocVersion != null)
                versionList.add(tocVersion);
            versionList.add(new TOCVersion());
            for (int i = 0; i < versions.length(); i++) {
                JSONObject version = versions.getJSONObject(i);
                versionList.add(new TOCVersion(version.getString("versionTitle"), version.getString("language")));
            }
            //final String [] items = new String[] {"test","bob","sam"};
            TOCVersionsAdapter adapter = new TOCVersionsAdapter(context, R.layout.toc_versions_adapter_item, versionList);
            versionsDropdown.setAdapter(adapter);
            versionsDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                private boolean veryFirstTime = true;

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        if(Build.VERSION.SDK_INT >= 23) {
                            TypedValue typedValue = new TypedValue();
                            Resources.Theme theme = context.getTheme();
                            theme.resolveAttribute(R.attr.text_color_english, typedValue, true);
                            ((TextView) parent.getChildAt(0)).setTextColor(typedValue.data);//set color of button item
                        }
                        return;

                    }
                    Toast.makeText(context, "Version: " + versionList.get(position).getPrettyString(), Toast.LENGTH_SHORT).show();
                    try {
                        Node newVersionNode = book.getNodeFromPathStr(pathDefiningNode);
                        newVersionNode.setTextVersion(versionList.get(position));
                        SuperTextActivity.startNewTextActivityIntent(context, book, null, newVersionNode, false, null, -1);
                    } catch (Node.InvalidPathException e) {
                        e.printStackTrace();
                    } catch (API.APIException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (API.APIException e) {
            e.printStackTrace();
        }
    }

    private int setCurrSectionText() {
        int padding = 6;
        int defaultTab = 0;
        try {
            Node node = book.getNodeFromPathStr(pathDefiningNode);
            defaultTab = node.getTocRootNum();
            String sectionTitle = node.getWholeTitle(lang, true, false); //TODO move lang to setLang
            currSectionTitleView.setText(sectionTitle);
            currSectionTitleView.setFont(lang, false, 20);
            currSectionTitleView.setPadding(0, 4 * padding, 0, padding);

            addAlternateTextVersions(node);

        }catch(Node.InvalidPathException e){
            currSectionTitleView.setHeight(0);
        }catch(API.APIException e){
            Toast.makeText(context, MyApp.getRString(R.string.problem_internet), Toast.LENGTH_SHORT).show();
        }

        return defaultTab;
    }


    public Util.Lang getLang() { return lang; }


    /*
    //used when you're rebuilding after memory dump
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
                intent = new Intent(context, CtsTextActivity.class);
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
