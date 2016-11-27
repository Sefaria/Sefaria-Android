package org.sefaria.sefaria.database;

import java.util.List;

/**
 * Created by nss on 5/17/16.
 */
public class Section {

    private List<Segment> segmentList;
    private Segment headerSegment;
    private boolean isLoader;

    public Section(boolean isLoader) {
        this.isLoader = isLoader;
    }

    public Section(List<Segment> segmentList, Segment headerSegment) {
        this(segmentList, headerSegment,false);
    }

    public Section(List<Segment> segmentList, Segment headerSegment, boolean isLoader) {
        this.segmentList = segmentList;
        this.headerSegment = headerSegment;
    }

    public List<Segment> getSegmentList() { return segmentList; }
    public Segment getHeaderSegment() { return headerSegment; }
    public boolean getIsLoader() { return isLoader;}
}
