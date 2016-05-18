package org.sefaria.sefaria.database;

import java.util.List;

/**
 * Created by nss on 5/17/16.
 */
public class Section {

    private List<Text> textList;
    private Text headerText;
    private boolean isLoader;

    public Section(boolean isLoader) {
        this.isLoader = isLoader;
    }

    public Section(List<Text> textList, Text headerText) {
        this(textList,headerText,false);
    }

    public Section(List<Text> textList, Text headerText, boolean isLoader) {
        this.textList = textList;
        this.headerText = headerText;
    }

    public List<Text> getTextList() { return textList; }
    public Text getHeaderText() { return headerText; }
    public boolean getIsLoader() { return isLoader;}
}
