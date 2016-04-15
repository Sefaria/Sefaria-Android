package org.sefaria.sefaria.activities;

import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.sefaria.sefaria.MyApp;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.API;
import org.sefaria.sefaria.database.Node;
import org.sefaria.sefaria.database.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.sefaria.sefaria.MyApp.getRString;

/**
 * Created by LenJ on 4/15/2016.
 */
public class FindOnPage {
    final SuperTextActivity superTextActivity;

    public FindOnPage(final SuperTextActivity superTextActivity) {
        this.superTextActivity = superTextActivity;
        if (searchBox == null) {
            searchBox = (EditText) superTextActivity.findViewById(R.id.search_box);
            searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        superTextActivity.findOnPage.runFindOnPage(true);
                        return true;
                    }
                    return false;
                }
            });
            searchBox.requestFocus();
            searchBox.setTypeface(MyApp.getFont(MyApp.Font.QUATTROCENTO));
        }
    }

    protected void runFindOnPage(boolean directionForward) {
        superTextActivity.searchingTerm = superTextActivity.searchActionbar.getText();
        if (superTextActivity.searchingTerm.equals("")) {
            Toast.makeText(superTextActivity, getRString(R.string.enter_word_to_search), Toast.LENGTH_SHORT).show();
            return;
        }
        searchBox.clearFocus();
        this.directionForward = directionForward;
        if (!isWorking) {
            FindOnPageBackground findOnPageBackground = new FindOnPageBackground();
            findOnPageBackground.execute(superTextActivity.searchingTerm);
        } else {
            Log.d("FindOnPage", "isWorking");
        }
    }

    final Comparator<Text> textComparator = new Comparator<Text>() {
        public int compare(Text a, Text b) {
            return b.tid - a.tid;
        }
    };

    private void sort(List<Text> list) {
        int lastTid = 0;
        for (Text text : list) {
            if (text.tid < lastTid) {
                Collections.sort(list, textComparator);
                return;
            }
            lastTid = text.tid;
        }
        //if it's here that means that it's already sorted.. hey guess what? We just "sorted" in O(n)
    }

    private boolean isWorking;
    private boolean directionForward;
    private EditText searchBox;
    private List<Text> foundSearchList;
    private Set<Node> searchedNodes;
    private String lastSearchingTerm;
    private boolean finishedEverything = false;
    private int lastFoundTID = 0;
    private Snackbar snackbar;

    private class FindOnPageBackground extends AsyncTask<String, Void, Boolean> {

        private Node goingToNode = null;
        private Text goingToText = null;
        private Integer myTID = null;
        private boolean APIError = false;


        private boolean lookForAlreadyFoundWord(List<Text> list, boolean presort) {
            if (presort) {
                sort(list);
            }
            if (directionForward) {
                for (Text found : list) {
                    if (myTID <= found.tid) {
                        goingToNode = found.parentNode;
                        goingToText = found;
                        lastFoundTID = found.tid;
                        return true;
                    }
                }
            } else {
                for (int i = list.size() - 1; i >= 0; i--) {
                    Text found = list.get(i);
                    if (myTID >= found.tid) {
                        goingToNode = found.parentNode;
                        goingToText = found;
                        lastFoundTID = found.tid;
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isWorking = true;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.d("SuperTextAct", "starting FinOnPage.doBackground");
            if (Settings.getUseAPI()) {
                APIError = true;
                return false;
            }
            String term = params[0];
            if (superTextActivity.currText == null)
                return false;
            if (superTextActivity.currText.isChapter()) {
                try {
                    myTID = superTextActivity.currText.parentNode.getTexts().get(0).tid;
                } catch (API.APIException e) {
                    return false;
                }
            } else {
                myTID = superTextActivity.currText.tid;
                if (directionForward && lastFoundTID >= myTID && lastFoundTID - myTID < 4)
                    myTID = lastFoundTID + 1;
                else if (!directionForward && lastFoundTID <= myTID && myTID - lastFoundTID < 4)
                    myTID = lastFoundTID - 1;
                //if(term.equals(lastSearchingTerm) && searchedNodes.contains(currNode)) myTID++;
            }
            if (myTID == null || myTID == 0)
                return false;

            Log.d("findonpage", "myTID: " + myTID);


            if (lastSearchingTerm == null || !term.equals(lastSearchingTerm)) {
                foundSearchList = new ArrayList<>();
                searchedNodes = new HashSet<>();
                lastSearchingTerm = term;
                finishedEverything = false;
            }

            if (lookForAlreadyFoundWord(foundSearchList, true)) {
                return true;
            }

            List<Text> list = null;
            Node node = superTextActivity.currNode;//.getAncestorRoot().getFirstDescendant();
            Node startingNode = node;
            while (true) {
                try {
                    list = node.findWords(term);
                } catch (API.APIException e) {
                    //TODO handle better
                    e.printStackTrace();
                    APIError = true;
                    return false;
                }
                if (!searchedNodes.contains(node)) {
                    foundSearchList.addAll(list);
                    searchedNodes.add(node);
                }

                if (lookForAlreadyFoundWord(list, false))
                    return true;

                try {
                    if (directionForward)
                        node = node.getNextTextNode();
                    else
                        node = node.getPrevTextNode();
                    Log.d("findOnPage","Searching next (" + directionForward + ") node:" + node);
                } catch (Node.LastNodeException e) {
                    Log.d("findOnPage","Got lastNodeException");
                    if (directionForward) {
                        node = node.getAncestorRoot().getFirstDescendant();
                        myTID = 1;
                    } else {
                        node = node.getAncestorRoot().getLastDescendant();
                        myTID = Integer.MAX_VALUE;
                    }

                    Log.d("findonpage", "looping back around.. myTID:" + myTID);
                }
                if (startingNode.equals(node)) {
                    finishedEverything = true;
                    sort(foundSearchList);
                    return false;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            isWorking = false;
            if (success == false) {
                if(APIError)
                    Toast.makeText(superTextActivity,"Not available in Online mode",Toast.LENGTH_SHORT).show();
                if (finishedEverything)
                    Toast.makeText(superTextActivity, "Didn't find query in book", Toast.LENGTH_SHORT).show();
                return;
            }
            superTextActivity.postFindOnPageBackground();
            if (goingToNode != null) {
                Log.d("findOnPage", "node:" + goingToNode);
                superTextActivity.lastLoadedNode = null;
                superTextActivity.firstLoadedNode = goingToNode;
                superTextActivity.openToText = goingToText;
                if (superTextActivity.openToText != null) {
                    if(snackbar == null || !snackbar.isShown())
                        snackbar = Snackbar.make(superTextActivity.searchActionBarRoot, superTextActivity.openToText.getLocationString(superTextActivity.menuLang), Snackbar.LENGTH_SHORT);
                    else {
                        snackbar.setText(superTextActivity.openToText.getLocationString(superTextActivity.menuLang));
                    }
                    snackbar.show();
                }
                superTextActivity.init();
            }
        }
    }
}
