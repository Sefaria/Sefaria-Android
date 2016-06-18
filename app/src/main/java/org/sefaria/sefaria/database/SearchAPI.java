package org.sefaria.sefaria.database;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by nss on 4/1/16.
 */
public class SearchAPI {
    private final static String SEARCH_URL = "http://search.sefaria.org:788/merged/_search/";

    private static int numResults;
    private static Set<String> refSet;
    private static JSONArray allFilters;

    public static List<Text> search(String query,boolean getFilters,ArrayList<String> appliedFilters, int pageNum,int pageSize) throws API.APIException {
        if (pageNum == 0) refSet = new HashSet<>();

        List<Text> resultList = new ArrayList<>();

        getFilters = getFilters || allFilters == null; //if you lost your filters, get them back

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
                        "\"pre_tags\": [\"<big><b>\"]," +
                        "\"post_tags\": [\"</b></big>\"]," +
                        "\"fields\": {" +
                        "\"content\": {\"fragment_size\": 200}" +
                        "}" +
                        "}";

        if (getFilters) {
            jsonString += ",\"query\":{" +
                    coreQuery + "}" +
            ",\"aggs\":{ \"category\":{\"terms\": { \"field\":\"path\",\"size\":0}}}";
        } else if (appliedFilters == null || true) {
            jsonString += ",\"query\":{" + coreQuery +"}";
        } else {
            //TODO apply filters
        }

        jsonString += "}";

        String result = API.getDataFromURL(SEARCH_URL, jsonString,true,false);
        resultList = getParsedResults(result,getFilters);

        return resultList;
    }

    private static List<Text> getParsedResults(String resultString, boolean getFilters) {
        List<Text> resultList = new ArrayList<>();

        JSONObject resultJson;
        try {
            resultJson = new JSONObject(resultString);
        } catch (JSONException e) {
            e.printStackTrace();
            return resultList;
        }


        if (getFilters) {
            try {
                allFilters = resultJson.getJSONObject("aggregations").getJSONObject("category").getJSONArray("buckets");
            } catch (JSONException e) {
                e.printStackTrace();
                return resultList;
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

                Log.d("title", title);
                String heText = "";
                String enText = "";
                if (id.contains("[he]")) {
                    heText = content;
                } else /* if (id.contains("[en]") || "french" || whatever) */ {
                    enText = content;
                }

                Text text = new Text(enText, heText, Book.getBid(title), ref);
                //TODO deal with levels stuff
                resultList.add(text);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return resultList;

    }

    public static int getNumResults() { return numResults; }
    public static JSONArray getAllFilters() { return allFilters;}
}
