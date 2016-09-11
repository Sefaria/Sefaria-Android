package org.sefaria.sefaria.SearchElements;

import org.json.JSONArray;
import org.sefaria.sefaria.database.Text;

import java.util.List;

/**
 * Created by nss on 7/20/16.
 */
public class SearchResultContainer {

    private List<Text> results;
    private int numResults; //not the same as results.size(). this the total num results, not the paginated results
    private JSONArray allFilters; //filters associated with query

    public SearchResultContainer(List<Text> results, int numResults, JSONArray allFilters) {
        this.results = results;
        this.numResults = numResults;
        this.allFilters = allFilters;
    }

    public List<Text> getResults() { return results; }
    public int getNumResults() { return numResults; }
    public JSONArray getAllFilters() { return allFilters; }
    public void setAllFilters(JSONArray allFilters) { this.allFilters = allFilters; }
}
