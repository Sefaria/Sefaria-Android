package org.sefaria.sefaria.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.Dialog.DialogManager2;
import org.sefaria.sefaria.Dialog.DialogNoahSnackbar;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.SearchElements.SearchActionbar;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.TextElements.TextChapterHeader;
import org.sefaria.sefaria.TextElements.TextMenuBar;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Downloader;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Section;
import org.sefaria.sefaria.database.Segment;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.MenuElements.MenuNode;

import java.util.ArrayList;
import java.util.List;

import static org.sefaria.sefaria.MyApp.getRString;

/**
 * Created by nss on 1/5/16.
 */
public abstract class SuperTextActivity extends FragmentActivity {
    public enum TextEnums {
        NEXT_SECTION, PREV_SECTION
    }

    public enum LoadSectionResult {
        LAST_NODE, API_EXCEPTION
    }

    public static int SEGMENT_SELECTOR_LINE_FROM_TOP = 150; //pixels from top of layout
    public static int MAX_LINK_FRAG_SNAP_DISTANCE = 230;
    protected static int PREV_DELAY_TIME = 1700; //ms from initTime
    protected static int LINK_FRAG_ANIM_TIME = 300; //ms
    protected static CharSequence CONTEXT_MENU_COPY_TITLE = "Copy";
    protected static CharSequence CONTEXT_MENU_SEND_CORRECTION = "Report Mistake";
    protected static CharSequence CONTEXT_MENU_SHARE = "Share";
    protected static CharSequence CONTEXT_MENU_VISIT = "View on Site";
    protected static CharSequence CONTEXT_MENU_PIN = "Pin Segment";
    protected static CharSequence CONTEXT_MENU_SHORTCUT = "Add to Home Screen";
    public static final int PREV_CHAP_DRAWN = 234234;
    public static final int TOC_CHAPTER_CLICKED_CODE = 3456;
    protected static final int LOAD_PIXEL_THRESHOLD = 500; //num pixels before the bottom (or top) of a segment after (before) which the next (previous) segment will be loaded

    protected static final String LINK_FRAG_TAG = "LinkFragTag";
    protected static final String HOME_FRAG_TAG = "HomeFragTag";

    protected boolean isTextMenuVisible;
    protected LinearLayout textMenuRoot;


    protected TextMenuBar textMenuBar;
    protected Book book;
    protected List<TextChapterHeader> textChapterHeaders;

    protected Node firstLoadedNode = null;
    protected Node currNode; // Node which you're currently up to in scrollView
    protected Segment currSegment;
    protected Node lastLoadedNode;
    protected Segment openToSegment;
    private int textNum;

    protected Util.Lang menuLang;
    protected Util.Lang textLang = null;
    protected boolean isCts;
    protected int colorTheme;
    protected boolean isSideBySide;

    protected float textSize;
    protected boolean isLoadingSection; //to make sure multiple sections don't get loaded at once
    protected boolean isLoadingInit; //true while init is loading. previous loads should not take place until after isLoadingInit is false
    protected long initTime;

    //fragment vars
    protected DrawerLayout drawerLayout;
    protected LinkFragment linkFragment;
    protected HomeFragment homeFragment;
    /**
     * hacky boolean so that if there's a problem with the on create, the subclasses know not to continue with init (after they call super.onCreate)
     */
    protected boolean goodOnCreate = false;
    private CustomActionbar customActionbar;

    protected long openedNewBookTime = 0; //this is used for Analytics for when someone first goes to a book
    protected int openedNewBookType = 0;
    protected boolean reportedNewBookBack = false;
    protected boolean reportedNewBookTOC = false;
    protected boolean reportedNewBookScroll = false;
    private static final int NO_HASH_NODE = -1;

    private static boolean firstTimeOpeningAppThisSession = true;

    private Boolean loadedTextUsingAPI = null;

    protected LinearLayout searchActionBarRoot;
    protected SearchActionbar searchActionbar;
    protected String searchingTerm;
    protected FindOnPage findOnPage;

    //protected GestureDetectorCompat textGestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SuperTextActi", "onCreate");

        /*
        if(!Database.hasOfflineDB() && Downloader.getNetworkStatus() == Downloader.ConnectionType.NONE){
            Toast.makeText(this,"No internet connection or Offline Library",Toast.LENGTH_SHORT).show();
        }
        */

        if (Settings.getIsFirstTimeOpened()) {
            MyApp.isFirstTimeOpened = true;
            DialogManager2.showDialog(this, DialogManager2.DialogPreset.FIRST_TIME_OPEN);
            Settings.setIsFirstTimeOpened(false);
        }

        if(firstTimeOpeningAppThisSession){
            firstTimeOpeningAppThisSession = false;
            if(!MyApp.isFirstTimeOpened)
                Database.dealWithStartupDatabaseStuff(this);
            Database.checkAndSwitchToNeededDB(this);
        }

        int nodeHash = getValuesFromIntent(savedInstanceState);
        getAllNeededLocationVariables(nodeHash);

        if(linkFragment == null){//LINK FRAGMENT
            linkFragment = new LinkFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.linkRoot, linkFragment,LINK_FRAG_TAG);
            fragmentTransaction.commit();
        }

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.homeFragmentRoot, homeFragment,HOME_FRAG_TAG);
            fragmentTransaction.commit();
        }


        //These vars are specifically initialized here and not in init() so that they don't get overridden when coming from TOC
        //defaults
        isCts = Settings.getIsCts(book);
        isSideBySide = Settings.getIsSideBySide();
        colorTheme = Settings.getTheme();
        setTheme(colorTheme);



        textSize = Settings.getDefaultFontSize();
        //end defaults
        isLoadingInit = false;
        menuLang = Settings.getMenuLang();
        if(customActionbar != null)//it's already been set
            customActionbar.setLang(menuLang);

        if (book != null || book.equals(Book.dummyBook))
            Settings.setLastBook(book.title);

        //textGestureDetector = new GestureDetectorCompat(this,new TextGestureDetector());

        goodOnCreate = true;
    }

    private int getValuesFromIntent(Bundle savedInstanceState){
        Intent intent = getIntent();
        int nodeHash;
        if (savedInstanceState != null) {//it's coming back after it cleared the activity from ram
            linkFragment = (LinkFragment) getSupportFragmentManager().getFragment(savedInstanceState,LINK_FRAG_TAG);
            homeFragment = (HomeFragment) getSupportFragmentManager().getFragment(savedInstanceState,HOME_FRAG_TAG);
            nodeHash = savedInstanceState.getInt("nodeHash", NO_HASH_NODE);
            book = savedInstanceState.getParcelable("currBook");
            openToSegment = savedInstanceState.getParcelable("incomingLinkText");
            searchingTerm = savedInstanceState.getString("searchingTerm");
            textNum = savedInstanceState.getInt("textNum",-1);
        }else{
            nodeHash = intent.getIntExtra("nodeHash", NO_HASH_NODE);
            book = intent.getParcelableExtra("currBook");
            openToSegment = intent.getParcelableExtra("incomingLinkText");
            searchingTerm = intent.getStringExtra("searchingTerm");
            textNum = intent.getIntExtra("textNum",-1);
        }
        return nodeHash;
    }


    private boolean getAllNeededLocationVariables(int nodeHash){
            if (nodeHash != NO_HASH_NODE) {
                firstLoadedNode = Node.getSavedNode(nodeHash);
            }

            if(firstLoadedNode != null && book != null && firstLoadedNode.getBid() != book.bid) {
                if(MyApp.DEBUGGING)
                    Toast.makeText(this,"Wrong book with node:" + book.bid + " --" + firstLoadedNode.getBid(),Toast.LENGTH_SHORT);
                GoogleTracker.sendEvent(GoogleTracker.CATEGORY_RANDOM_ERROR,"Wrong book with node:" + book.bid + " --" + firstLoadedNode.getBid());
                book = null;
            }

            if (book == null) {
                try {
                    if (firstLoadedNode != null) {
                        book = Book.getByBid(firstLoadedNode.getBid());
                    } else if (openToSegment != null) {
                        book = Book.getByBid(openToSegment.bid);
                    } else {
                        book = new Book(Settings.getLastBook());
                    }
                } catch (Book.BookNotFoundException e) {
                    Toast.makeText(this,MyApp.getRString(R.string.error_getting_book),Toast.LENGTH_SHORT).show();
                    GoogleTracker.sendException(e,"SuperText Book Prob");
                    e.printStackTrace();
                    book = book.dummyBook;
                    return false;
                }
            }
            Settings.RecentTexts.addRecentText(book.title);

            if (firstLoadedNode == null && openToSegment != null) {
                try {
                    firstLoadedNode = openToSegment.getNodeFromText(book);
                } catch (Book.BookNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this,MyApp.getRString(R.string.error_getting_book),Toast.LENGTH_SHORT).show();
                    GoogleTracker.sendException(e,"SuperText Book Prob");
                    firstLoadedNode = Node.dummyNode;
                    return false;
                } catch (API.APIException e) {
                    e.printStackTrace();
                    API.makeAPIErrorToast(this);
                    firstLoadedNode = Node.dummyNode;
                    return false;
                }
            }

            Settings.BookSettings bookSettings = Settings.BookSettings.getSavedBook(book);
            textLang = bookSettings.lang;
            if (firstLoadedNode == null && bookSettings.node != null) { //opening previously opened book
                firstLoadedNode = bookSettings.node;
                textNum = bookSettings.textNum;
            }

            if (firstLoadedNode == null) {
                List<Node> TOCroots = null;
                try {
                    TOCroots = book.getTOCroots();
                } catch (API.APIException e) {
                    API.makeAPIErrorToast(this);
                    firstLoadedNode = Node.dummyNode;
                    return false;
                }
                if (TOCroots.size() == 0) {
                    Toast.makeText(this, MyApp.getRString(R.string.unable_to_load_toc_for_book), Toast.LENGTH_SHORT).show();
                    return false;
                }
                Node root = TOCroots.get(0);
                firstLoadedNode = root.getFirstDescendant();//was getting first segment true);
                GoogleTracker.sendEvent(GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION, "Opened New Book");
                openedNewBookTime = System.currentTimeMillis();
            }
            if(firstLoadedNode != null) //put in saved textVersion
                firstLoadedNode.setTextVersion(bookSettings.textVersion);

            if (openToSegment == null && textNum > -1) {
                try {
                    openToSegment = firstLoadedNode.getTexts().get(textNum);
                }catch (IndexOutOfBoundsException e1) {
                    Log.e("SuperTextAct", e1.getMessage());
                } catch (API.APIException e) {
                    e.printStackTrace();
                }
            }
        return true;
    }



    protected boolean veryFirstTime = true;
    @Override
    protected void onResume() {
        super.onResume();
        GoogleTracker.sendScreen("SuperTextActivity");
        if(!goodOnCreate)
            return;

        if(Settings.getTheme() != colorTheme){
            restartActivity();
            return;
        }

        textLang = Settings.BookSettings.getSavedBook(book).lang;

        GoogleTracker.sendEvent(GoogleTracker.CATEGORY_NEW_TEXT, book.title, Settings.lang2Int(textLang));
        if(!veryFirstTime) {
            setMenuLang(Settings.getMenuLang());
            homeFragment.setLang(Settings.getMenuLang());
        }else
            veryFirstTime = false;

        if(loadedTextUsingAPI == null)
            loadedTextUsingAPI = Settings.getUseAPI();
        else if(loadedTextUsingAPI != Settings.getUseAPI()) {
            restartActivity();
        }


        DialogNoahSnackbar.checkCurrentDialog(this, (ViewGroup) this.findViewById(R.id.dialogNoahSnackbarRoot));
        //if(drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.LEFT)){
        //    drawerLayout.closeDrawer(Gravity.LEFT);
        //}

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Settings.BookSettings.setSavedBook(book, currNode, currSegment, textLang);//data also saved with home click
        outState.putParcelable("currBook", book);
        getSupportFragmentManager().putFragment(outState, LINK_FRAG_TAG, linkFragment);
        getSupportFragmentManager().putFragment(outState, HOME_FRAG_TAG, homeFragment);
    }


    /**
     * used for pressing on link
     * @param context
     * @param segment
     */
    public static void startNewTextActivityIntent(Context context, Segment segment, boolean openNewTask){
        startNewTextActivityIntent(context, null, segment, null, openNewTask, null, -1);
    }


    /**
     * used for coming in from Menu
     * @param context
     * @param book
     */
    public static void startNewTextActivityIntent(Context context, Book book, boolean openNewTask){
        startNewTextActivityIntent(context, book, null, null, openNewTask, null, -1);
    }

    protected void createShortcut(Segment segment){
        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(this, SplashScreenActivity.class);// makeNewTextActivityIntent(this, null, segment, null, false, null, -1);
        try {
            shortcutIntent.putExtra("url", segment.getURL(true));
        } catch (Book.BookNotFoundException e) {
            e.printStackTrace();
        }

        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        String title = Util.ellipsis(segment.getLocationString(Settings.getSystemLang()), 16, 19);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.sefaria_icon));

        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        //addIntent.putExtra("duplicate", false);  //may it's already there so don't duplicate
        sendBroadcast(addIntent);
    }

    private static Intent makeNewTextActivityIntent(Context context, Book book, Segment segment, Node node, boolean openNewTask, String searchingTerm, int textNum) {
        Intent intent;

        //Open CtsTextActivity if the current book can be cts, and your settings are cts
        Util.Lang bookLang;
        if (book != null)
            bookLang = Settings.BookSettings.getSavedBook(book).lang;
        else
            bookLang = Settings.getDefaultTextLang();

        if (Settings.getIsCts() && bookLang != Util.Lang.BI && canBeCts(book)) {
            intent = new Intent(context, CtsTextActivity.class);
        } else {
            intent = new Intent(context, SepTextActivity.class);
        }

        if(book != null)
            intent.putExtra("currBook", book);
        if(segment != null)
            intent.putExtra("incomingLinkText", segment);
        if(node != null){
            node.log();
            Node.saveNode(node);
            intent.putExtra("nodeHash",node.hashCode());
        }
        if(searchingTerm != null){
            intent.putExtra("searchingTerm",searchingTerm);
        }
        if(textNum>-1){
            intent.putExtra("textNum",textNum);
        }

        if(openNewTask){
            intent = MyApp.startNewTab(intent);
        }
        return intent;
    }

    public static void startNewTextActivityIntent(Context context, Book book, Segment segment, Node node, boolean openNewTask, String searchingTerm, int textNum) {
        Intent intent = makeNewTextActivityIntent(context, book, segment, node, openNewTask, searchingTerm, textNum);
        context.startActivity(intent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        comingFromTOC(intent);
    }

    protected void init(){



        isLoadingInit = true;
        textChapterHeaders = new ArrayList<>();

        isTextMenuVisible = false;
        textMenuRoot = (LinearLayout) findViewById(R.id.textMenuRoot);
        textMenuBar = new TextMenuBar(SuperTextActivity.this,textMenuBtnClick, canBeCts(book));
        textMenuBar.setState(textLang, isCts, isSideBySide, colorTheme);
        textMenuRoot.addView(textMenuBar);
        textMenuRoot.setVisibility(View.GONE);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.OK,R.string.CANCEL) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                //Log.d("SuperTextAct", "draw closed");
                setMenuLang(Settings.getMenuLang());
                homeFragment.onHomeFragClose();
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //Log.d("SuperTextAct", "draw opened");
                Settings.BookSettings.setSavedBook(book, currNode, currSegment, textLang);//save this book b/f home frag so that this book is in recent list
                homeFragment.onHomeFragOpen();
            }

            /*
            not using b/c opening doesn't account for homeClick and closing doesn't account for clicking on side grey
            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                if (newState == DrawerLayout.STATE_DRAGGING) {
                    if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                        //closing
                        Log.d("SuperTextAct", "draw closing");
                    } else {
                        //opening
                        Log.d("SuperTextAct", "draw opening");
                    }
                }
            }*/
        };

        drawerLayout.setDrawerListener(actionBarDrawerToggle);


        //linkDraggerView = (LinkDraggerView) findViewById(R.id.link_dragger);

        //this specifically comes before menugrid, b/c in tabs it menugrid does funny stuff to currnode
        if (customActionbar == null) {
            MenuNode menuNode = new MenuNode("Error (if you see this)","Error (if you see this)",null); //TODO possibly replace this object with a more general bilingual node
            int catColor = book.getCatColor();
            if(Settings.getUseAPI()|| true) //findOnPae temp removed for all versions
                searchClick = null;
            backClick = null;
            homeLongClick = null;

            customActionbar = new CustomActionbar(this, menuNode, menuLang,homeClick,homeLongClick, null,searchClick,titleClick,menuClick,backClick,null,catColor,true,false, book.isTalmudBavli()); //TODO.. I'm not actually sure this should be lang.. instead it should be MENU_LANG from Util.S
            LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
            abRoot.addView(customActionbar);
            customActionbar.setLang(menuLang);
        }

        setCurrNode(firstLoadedNode);
    }

    private void comingFromTOC(Intent intent){
        //lang = (Util.Lang) data.getSerializableExtra("lang"); TODO you might need to set lang here if user can change lang in TOC
        int nodeHash = intent.getIntExtra("nodeHash", -1);
        textNum = -1; //This is so that it jumps to the beginning of the Node
        //TODO it might want to try to keep the old loaded sections and just go scroll to it if the new section is already loaded
        getAllNeededLocationVariables(nodeHash);
        lastLoadedNode = null;
        init();
    }

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("SuperTextAct", "onActivityResult");
        if (data != null) {
            //you're returning to segment page b/c chapter was clicked in toc
            if (requestCode == TOC_CHAPTER_CLICKED_CODE) {
                comingFromTOC(data);
            }
        }
    }
    */

    public InputMethodManager getInputMethodManager(){
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Database.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if(openedNewBookTime >0 && !reportedNewBookBack){
            long time = (System.currentTimeMillis() - openedNewBookTime);
            if(time <10000) {
                String category;
                if(reportedNewBookScroll || reportedNewBookTOC)
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                else
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                GoogleTracker.sendEvent(category, "Back Pressed", time);
            }
            reportedNewBookBack = true;
        }

        Settings.BookSettings.setSavedBook(book, currNode, currSegment, textLang);
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT);
        }else if(isTextMenuVisible)
            toggleTextMenu();
        else if (linkFragment != null && linkFragment.getIsOpen()) {
            if (linkFragment.getCurrState() == LinkFragment.State.MAIN) {
                View linkRoot = findViewById(R.id.linkRoot);
                AnimateLinkFragClose(linkRoot);
            } else { //In CAT or BOOK state
                linkFragment.gotoState(LinkFragment.State.MAIN, linkFragment.getView(), null);
            }

        } else if(getFindOnPageIsOpen()){
            //searchActionBarRoot.removeAllViews();
            findOnPageClose();
        } else {
            //super.onBackPressed();
            setResult(RESULT_CANCELED);
            finish();
        }
    }



    protected boolean getFindOnPageIsOpen(){
        return searchActionBarRoot != null && searchActionBarRoot.getChildCount()>0;
    }

    protected void findOnPageClose(){
        if(findOnPage != null){
            findOnPage.hideShowKeyboard(false,0);
        }
        searchActionBarRoot.removeAllViews();
    }

    @Override
    public boolean onSearchRequested() {
        super.onSearchRequested();
        if(findOnPage == null) findOnPage = new FindOnPage(SuperTextActivity.this);
        findOnPage.runFindOnPage(false);
        return true;
    }

    protected Segment getSectionHeaderText(TextEnums dir){
        Node node = null;
        if(dir == TextEnums.NEXT_SECTION) {
            node = lastLoadedNode;
        } else if (dir == TextEnums.PREV_SECTION){
            node = firstLoadedNode;
        }
        return new Segment(node);
    }



    protected void toggleTextMenu() {
        textMenuRoot.bringToFront();
        customActionbar.bringToFront();
        if (isTextMenuVisible) {
            //textMenuRoot.setVisibility(View.GONE);
            AnimateLinkTMBClose(textMenuRoot);
        } else {
            //textMenuRoot.setVisibility(View.VISIBLE);
            AnimateLinkTMBOpen(textMenuRoot);
        }
        isTextMenuVisible = !isTextMenuVisible;
    }

    View.OnLongClickListener homeLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            Settings.BookSettings.setSavedBook(book, currNode, currSegment, textLang);
            //MyApp.homeClick(SuperTextActivity.this, true,false);
            return true;
        }
    };

    View.OnClickListener findOnPageCloseClick = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            findOnPageClose();
        }
    };

    protected abstract void postFindOnPageBackground();

    View.OnClickListener findOnPageUpClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onSearchRequested();
        }
    };

    View.OnClickListener findOnPageDownClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(findOnPage == null) findOnPage = new FindOnPage(SuperTextActivity.this);
            findOnPage.runFindOnPage(true);
        }
    };


    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //Log.d("TextAct", "here");
            if(searchActionbar == null) {
                searchActionbar = new SearchActionbar(SuperTextActivity.this, findOnPageCloseClick, null,findOnPageUpClick,findOnPageDownClick,book.getCatColor(), getRString(R.string.search) + " " + book.getTitle(menuLang), null);
            }
            if(searchActionBarRoot == null)
                searchActionBarRoot = (LinearLayout) findViewById(R.id.searchBarRoot);
            searchActionBarRoot.removeAllViews();//in case you some how click on the search button while the search thing is already open (see if the old bar is visible through the search bar)
            searchActionBarRoot.addView(searchActionbar);
            searchActionbar.requestFocus();

            if(findOnPage == null)
                findOnPage = new FindOnPage(SuperTextActivity.this);

            if(linkFragment.getIsOpen()){
                View linkRoot = findViewById(R.id.linkRoot);
                AnimateLinkFragClose(linkRoot);
            }
        }
    };



    View.OnClickListener titleClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Settings.BookSettings.setSavedBook(book, currNode, currSegment, textLang);//data also saved with home click
            if(openedNewBookTime >0 && !reportedNewBookTOC){
                String category;
                if(reportedNewBookScroll || reportedNewBookBack)
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION_2;
                else
                    category = GoogleTracker.CATEGORY_OPEN_NEW_BOOK_ACTION;
                GoogleTracker.sendEvent(category,"Opening TOC", (System.currentTimeMillis() - openedNewBookTime));
                reportedNewBookTOC = true;
            }
            Intent intent = TOCActivity.getStartTOCActivityIntent(SuperTextActivity.this, book,firstLoadedNode);
            startActivityForResult(intent, TOC_CHAPTER_CLICKED_CODE);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleTextMenu();
        }
    };

    View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    View.OnClickListener textMenuBtnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            boolean updatedTextSize = false;
            switch (v.getId()) {
                case R.id.en_btn:
                    setTextLang(Util.Lang.EN);
                    break;
                case R.id.he_btn:
                    setTextLang(Util.Lang.HE);
                    break;
                case R.id.bi_btn:
                    setTextLang(Util.Lang.BI);
                    break;
                case R.id.cts_btn:
                    setIsCts(true);
                    break;
                case R.id.sep_btn:
                    setIsCts(false);
                    break;
                case R.id.sbs_btn:
                    setIsSideBySide(true);
                    break;
                case R.id.tb_btn:
                    setIsSideBySide(false);
                    break;
                case R.id.white_btn:
                    setColorTheme(R.style.SefariaTheme_White);
                    break;
                case R.id.grey_btn:
                    setColorTheme(R.style.SefariaTheme_Grey);
                    break;
                case R.id.black_btn:
                    setColorTheme(R.style.SefariaTheme_Black);
                    break;
                case R.id.small_btn:


                    if (textSize > getResources().getDimension(R.dimen.min_text_font_size)) {
                        incrementTextSize(false);
                        updatedTextSize = true;
                    }
                    break;
                case R.id.big_btn:
                    if (textSize < getResources().getDimension(R.dimen.max_text_font_size)) {
                        incrementTextSize(true);
                        updatedTextSize = true;
                    }
                    break;
            }
            textMenuBar.setState(textLang, isCts, isSideBySide,colorTheme);
            if (!updatedTextSize) toggleTextMenu();
        }
    };


    public Util.Lang getTextLang() { return textLang; }

    public Util.Lang getMenuLang() { return menuLang; }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
        Settings.setDefaultFontSize(textSize);
    }

    protected void incrementTextSize(boolean isIncrement){
        float increment = getResources().getDimension(R.dimen.text_font_size_increment);
        float tempTextSize = textSize;
        if (isIncrement) tempTextSize  += increment;
        else tempTextSize -= increment;
        setTextSize(tempTextSize);
    }

    //return the currently selected segment, as determined by the link fragment
    public Segment getCurrLinkSegment() { return linkFragment.getSegment(); }
    public boolean getFragmentIsOpen() { return linkFragment.getIsOpen(); }

    protected void setMenuLang(Util.Lang menuLang){
        this.menuLang = menuLang;
        customActionbar.setTitleText(currNode.getMenuBarTitle(book, menuLang), menuLang, true, true);
        linkFragment.notifyDataSetChanged();
    }

    public Book getBook() { return book; }
    public boolean getIsCts(){ return isCts;}

    public static boolean canBeCts(Book book) {
        boolean isCtsText = false;
        if (book != null) {
            try {
                final String[] CTS_TEXT_CATS = {"Talmud"};
                for (String ctsText : CTS_TEXT_CATS) {
                    isCtsText = ctsText.equals(book.categories[0]);
                    if (isCtsText) {
                        break;
                    }
                }
            }catch (Exception e){ //could throw if categories doesn't have the right stuff
                e.printStackTrace();
            }
        }
        return isCtsText;
    }

    public boolean getIsSideBySide() { return isSideBySide; }
    public int getColorTheme() { return colorTheme; }
    private void setColorTheme(int colorTheme) {
        Settings.setTheme(colorTheme);
        restartActivity();
    }

    protected void restartActivity(){
        Settings.BookSettings.setSavedBook(book, currNode, currSegment, textLang);
        finish();
        startNewTextActivityIntent(this, book, currSegment, currNode, false, searchingTerm, -1);
    }

    protected abstract void setTextLang(Util.Lang textLang);

    protected void setIsCts(boolean isCts) {
        setIsCts(isCts,false);
    }

    protected void setIsCts(boolean isCts, boolean forceRestart) {
        if (isCts != this.isCts || forceRestart) {
            Settings.setIsCts(isCts);
            restartActivity();
        }
    }
    protected void setIsSideBySide(boolean isSideBySide){
        this.isSideBySide = isSideBySide;
        Settings.setIsSideBySide(isSideBySide);
    }

    protected abstract void jumpToText(Segment segment);
    protected abstract void updateFocusedSegment();

    protected void setCurrNode(Node node){
        setCurrNode(node,null);
    }
    protected  void setCurrNode(Segment segment) {
        Node node;
        if (segment == null) node = null;
        else node = segment.parentNode;
        setCurrNode(node, segment);
    }

    protected void setCurrNode(Section section) {
        Node node;
        Segment segment;
        if (section == null || section.getSegmentList() == null || section.getSegmentList().size() == 0) {
            node = null;
            segment = null;
        } else {
            segment = section.getSegmentList().get(0);
            node = segment.parentNode;
        }

        setCurrNode(node, segment);
    }

    private void setCurrNode(Node node, Segment segment){
        currSegment = segment;
        if(node == null) return;
        if(currNode != node){
            currNode = node;
            customActionbar.setTitleText(currNode.getMenuBarTitle(book, menuLang), menuLang, true, true);
        }
    }


    protected List<Segment> loadSection(TextEnums dir) throws API.APIException, Node.LastNodeException {
        Node newNode = null;
        if (dir == TextEnums.NEXT_SECTION) {
            if (lastLoadedNode == null) { //this is the initial load
                newNode = firstLoadedNode;
            } else {
                newNode = lastLoadedNode.getNextTextNode();
            }
            lastLoadedNode = newNode;
        } else if (dir == TextEnums.PREV_SECTION) {
            if (firstLoadedNode == null) {
                newNode = lastLoadedNode;
            } else {
                newNode = firstLoadedNode.getPrevTextNode();
            }
            firstLoadedNode = newNode;
        }
        //if at last node throws LastNodeException


        List<Segment> textsList;
        if(newNode == null){//This error occurs when using API and the book no longer exists in Sefaria (it could also happen other times we don't know about)
            //TODO add error segment into the list.
            //Node.dummyNode;
            return new ArrayList<>();
        }
        textsList = newNode.getTexts();
        newNode.findWords(searchingTerm);
        return textsList;
        //} catch (API.APIException e) {
    }




    //-----
    //TEXT MENU BAR
    //-----
    protected void AnimateLinkTMBOpen(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {v.setVisibility(View.VISIBLE);}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                v.clearAnimation();
                //onFinishLinkFragOpen();
            }

        });

    }

    protected void AnimateLinkTMBClose(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, -1.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                v.clearAnimation();
                v.setVisibility(View.GONE);
            }

        });

    }


    //-----
    //LINK FRAGMENT
    //-----

    public int getLinkFragMaxHeight() {
        return findViewById(R.id.root).getHeight()-findViewById(R.id.actionbarRoot).getHeight();
    }


    protected abstract void onFinishLinkFragOpen();
    protected abstract void onStartLinkFragClose();

    //Thank you Farhan Shah! https://stackoverflow.com/questions/20323628/android-layout-animations-from-bottom-to-top-and-top-to-bottom-on-imageview-clic

    protected void AnimateLinkFragOpen(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                1.0f, Animation.RELATIVE_TO_SELF, 0.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {

                v.setVisibility(View.VISIBLE);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                v.clearAnimation();

                onFinishLinkFragOpen();
                linkFragment.setDontUpdate(false);
                linkFragment.setIsOpen(true);

                //linkDraggerView.setVisibility(View.VISIBLE);
            }

        });

    }

    public void AnimateLinkFragClose(final View v) {
        Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 1.0f);

        slide.setDuration(LINK_FRAG_ANIM_TIME);
        slide.setFillAfter(true);
        slide.setFillEnabled(true);
        v.startAnimation(slide);

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                //linkDraggerView.setVisibility(View.GONE);

                linkFragment.setIsOpen(false);
                linkFragment.setDontUpdate(true);

                onStartLinkFragClose();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                v.clearAnimation();

                v.setVisibility(View.GONE);

                if (v.getHeight() < MAX_LINK_FRAG_SNAP_DISTANCE) {
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) v.getLayoutParams();
                    params.height = MAX_LINK_FRAG_SNAP_DISTANCE;
                    v.setLayoutParams(params);
                }

            }

        });
    }


    View.OnClickListener homeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            drawerLayout.openDrawer(Gravity.LEFT);
        }
    };

    /*
    ONCLICK LISTENERS
     */
    public void settingsClick(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void aboutClick(View v) {
        String url = "https://www.sefaria.org/about";
        MyApp.openURLInBrowser(this, url);
    }

    public void donateClick(View v){
        String url = "https://www.sefaria.org/donate";
        MyApp.openURLInBrowser(this, url);
    }

    public void feedbackClick(View v) {


        String email = "android@sefaria.org";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android App Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, getEmailHeader());
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String [] {email});
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    public static String getEmailHeader(){
        return  "App Version: " + BuildConfig.VERSION_NAME + " ("  + BuildConfig.VERSION_CODE + ")" + "\n"
                + "Online Library Version: " + Util.convertDBnum(Database.getVersionInDB(true)) + "\n"
                + "Offline Library Version: " + Util.convertDBnum(Database.getVersionInDB(false)) + "\n"
                + "Using " + (Settings.getUseAPI()? "Online":"Offline") + " Library" + "\n"
                + GoogleTracker.randomID + "\n"
                + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")" + "\n"
                +"\n\n\n";
    }

    public void siteClick(View v){
        String url = "https://www.sefaria.org";
        MyApp.openURLInBrowser(this, url);
    }

    public void recentMoreClick(View v){
        Intent intent = new Intent(this, RecentsActivity.class);
        startActivity(intent);
    }

    public void reloadClick(View v){

        if(Downloader.getHasInternet()) {
            //Toast.makeText(SuperTextActivity.this, "Reloading texts", Toast.LENGTH_SHORT).show();
            restartActivity();
        }else{
            Toast.makeText(SuperTextActivity.this, MyApp.getRString(R.string.problem_internet), Toast.LENGTH_SHORT).show();
        }
    }


    /*@Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        float newTextSize = TextGestureDetector.zoomZoom(event,textSize);
        if (newTextSize > -1f) {
            setTextSize(newTextSize);
        }

        if (TextGestureDetector.getMode() == TextGestureDetector.Mode.ZOOM) {
            Log.d("zoom","YUMMY");
            return true;
        } else {
            Log.d("zoom","NOT YUMMY");
            return super.dispatchTouchEvent(event);
        }
        //textGestureDetector.onTouchEvent(event);



    }*/


    //FIND ON PAGE


}
