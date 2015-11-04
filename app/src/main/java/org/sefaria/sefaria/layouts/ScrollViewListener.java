package org.sefaria.sefaria.layouts;

import org.sefaria.sefaria.layouts.ScrollViewExt;

public interface ScrollViewListener {
    void onScrollChanged(ScrollViewExt scrollView,
                         int x, int y, int oldx, int oldy);
}
