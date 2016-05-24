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

    public static List<Text> search(String query,int pageNum,int pageSize) throws API.APIException {
        if (pageNum == 0) refSet = new HashSet<>();

        List<Text> resultList = new ArrayList<>();
        String jsonString =
                "{" +
                        "\"from\": " + (pageNum * pageSize) + "," +
                        "\"size\": " + pageSize + "," +
                        "\"sort\": [{" +
                            "\"order\": {}" +
                            "}]," +
                        "\"query\": {" +
                        "\"query_string\": {" +
                        "\"query\": \"" + query + "\"," +
                        "\"default_operator\": \"AND\"," +
                        "\"fields\": [\"content\"]" +
                        "}" +
                        "}," +
                        "\"highlight\": {" +
                        "\"pre_tags\": [\"<big><b>\"]," +
                        "\"post_tags\": [\"</b></big>\"]," +
                        "\"fields\": {" +
                        "\"content\": {\"fragment_size\": 200}" +
                        "}" +
                        "}" +
                        "}";
        String result = API.getDataFromURL(SEARCH_URL, jsonString,true,false);
        resultList = getParsedResults(result);
        return resultList;
    }

    private static List<Text> getParsedResults(String resultString) {
        List<Text> resultList = new ArrayList<>();
        try {
            JSONObject resultJson = new JSONObject(resultString);
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
}
