package org.sefaria.sefaria.layouts;

/**
 * Created by nss on 9/12/16.
 */

import android.widget.Checkable;

/**
 * Extension to Checkable interface with addition "indeterminate" state
 * represented by <code>getState()</code>. Value meanings:
 *   null = indeterminate state
 *   true = checked state
 *   false = unchecked state
 */
public interface IndeterminateCheckable extends Checkable {

    void setState(Boolean state);
    Boolean getState();
}