package org.sefaria.sefaria.database;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sefaria.sefaria.BilingualNode;
import org.sefaria.sefaria.SearchElements.SearchResultContainer;
import org.sefaria.sefaria.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by nss on 4/1/16.
 */
public class SearchAPI {
    private final static String SEARCH_URL = "http://search.sefaria.org/merged/_search/";

    private static Set<String> refSet;

    public static SearchResultContainer search(String query, boolean getFilters, List<String> appliedFilters, int pageNum, int pageSize) throws API.APIException {
        if (pageNum == 0) refSet = new HashSet<>();

        query = prepareQuery(query);
        String coreQuery = "\"query_string\": {" +
                "\"query\": \"" + query + "\"," +
                "\"default_operator\": \"AND\"," +
                "\"fields\": [\"content\"]" +
                "}";

        String jsonString =
                "{" +
                        "\"from\": " + (pageNum * pageSize) + "," +
                        "\"size\": " + pageSize + "," +
                        "\"sort\": [{" +
                            "\"order\": {}" +
                            "}]," +
                        "\"highlight\": {" +
                        "\"pre_tags\": [\"" + SearchingDB.BIG_BOLD_START + "\"]," +
                        "\"post_tags\": [\"" + SearchingDB.BIG_BOLD_END + "\"]," +
                        "\"fields\": {" +
                        "\"content\": {\"fragment_size\": 200}" +
                        "}" +
                        "}";

        if (getFilters) {
            jsonString += ",\"query\":{" +
                    coreQuery + "}" +
            ",\"aggs\":{ \"category\":{\"terms\": { \"field\":\"path\",\"size\":0}}}";
        } else if (appliedFilters == null || appliedFilters.size() == 0) {
            jsonString += ",\"query\":{" + coreQuery +"}";
        } else {
            String clauses = "[";
            for (int i = 0; i < appliedFilters.size(); i++) {
                String filterString = Util.regexpEscape(appliedFilters.get(i));

                //to account for Commentary and Commentary2...
                filterString = filterString.replace("Commentary","Commentary.*");

                clauses += "{\"regexp\":{" +
                        "\"path\":\"" + filterString + ".*\"}}";
                if (i != appliedFilters.size()-1) clauses += ",";
            }
            clauses += "]";
            jsonString += ",\"query\":{" +
                    "\"filtered\":{"+
                    "\"query\":{" + coreQuery + "}," +
                    "\"filter\":{" +
                    "\"or\":" + clauses + "}}}";
        }

        jsonString += "}";

        Log.d("YOYO",jsonString);
        Log.d("YOYO","GF = " + getFilters);
        String result = API.getDataFromURL(SEARCH_URL, jsonString, true, API.TimeoutType.REG);

        return getParsedResults(result,getFilters);
    }

    private static SearchResultContainer getParsedResults(String resultString, boolean getFilters) {
        SearchResultContainer searchResultContainer = null;
        JSONArray allFilters = new JSONArray();
        List<Segment> results = new ArrayList<>();

        int numResults = 0;
        JSONObject resultJson;
        try {
            resultJson = new JSONObject(resultString);
        } catch (JSONException e) {
            e.printStackTrace();
            return searchResultContainer;
        }


        if (getFilters) {
            try {
                allFilters = resultJson.getJSONObject("aggregations").getJSONObject("category").getJSONArray("buckets");
                Log.d("YOYO","ALLFILETER LEN " + allFilters.length());
            } catch (JSONException e) {
                e.printStackTrace();
                return searchResultContainer;
            }
        }

        try {

            numResults = resultJson.getJSONObject("hits").getInt("total");
            JSONArray hits = resultJson.getJSONObject("hits").getJSONArray("hits");
            for (int i = 0; i < hits.length(); i++) {
                JSONObject source = hits.getJSONObject(i).getJSONObject("_source");
                String id = hits.getJSONObject(i).getString("_id");
                String ref = source.getString("ref");
                if (refSet.contains(ref)) {
                    continue;
                } else {
                    refSet.add(ref);
                }

                String content = hits.getJSONObject(i).getJSONObject("highlight").getJSONArray("content").getString(0);
                String path = source.getString("path");
                String title = path.substring(path.lastIndexOf("/")+1);


                String heText = "";
                String enText = "";
                if (id.contains("[he]")) {
                    heText = content;
                } else /* if (id.contains("[en]") || "french" || whatever) */ {
                    enText = content;
                }
                int tempBid;
                try {
                    tempBid = Book.getBid(title);
                } catch (Book.BookNotFoundException e) {
                    tempBid = -1;
                }
                Log.d("title", title + " BID = " + tempBid);
                Segment segment = new Segment(enText, heText, tempBid, ref);
                //TODO deal with levels stuff
                results.add(segment);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new SearchResultContainer(results,numResults,allFilters);

    }

    /**
     *
     * @param filterNodes - all currently selected filters
     * @return - the minimum number of filter nodes required to represent those filters
     */
    public static List<BilingualNode> getMinFilterNodes(List<BilingualNode> filterNodes) {
        if (filterNodes.size() == 0) return filterNodes;

        Map<BilingualNode, Integer> parentMap = new HashMap<>();
        Set<BilingualNode> minNodesSet = new HashSet<>();

        for (BilingualNode node : filterNodes) {
            BilingualNode parent = node.getParent();
            if (parent == null) continue;
            if (!parentMap.containsKey(parent))
                parentMap.put(parent,1);
            else
                parentMap.put(parent,parentMap.get(parent)+1);
        }

        for (BilingualNode node : filterNodes) {
            BilingualNode parent = node.getParent();
            if (parent == null) continue;

            //2nd to last condition will keep filters more specific, in case you change your search
            //last condition will rule out root.
            boolean hasAllChildren = parent.getNumChildren() == parentMap.get(parent) && parent.getParent() != null;
            if (hasAllChildren && !minNodesSet.contains(parent))
                minNodesSet.add(parent);
            else if (!hasAllChildren)
                minNodesSet.add(node);
        }


        List<BilingualNode> minNodeList = new ArrayList<>(minNodesSet);
        Set<BilingualNode> filterSet = new HashSet<>(filterNodes);
        if (minNodesSet.equals(filterSet))
            return minNodeList;
        else
            return getMinFilterNodes(minNodeList);
    }

    private static String prepareQuery(String query) {
        //Replace internal quotes with gershaim.
        final Pattern r = Pattern.compile("(\\S)\"(\\S)");
        String yo = r.matcher(query).replaceAll("$1\u05f4$2");
        Log.d("QUERY",yo + " -> " + query);
        return yo;
    }

}
