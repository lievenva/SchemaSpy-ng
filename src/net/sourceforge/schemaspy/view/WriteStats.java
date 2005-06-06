package net.sourceforge.schemaspy.view;

import net.sourceforge.schemaspy.model.Table;

/**
 * Simple ugly hack that provides details of what was written.
 */
public class WriteStats {
    private int numTables;
    private int numViews;
    private boolean wroteImplied;
    private boolean wroteTwoDegrees;

    public void wroteTable(Table table) {
        if (table.isView())
            ++numViews;
        else
            ++numTables;
    }

    public void setWroteImplied(boolean wroteImplied) {
        this.wroteImplied = wroteImplied;
    }

    public void setWroteTwoDegrees(boolean wroteFocused) {
        this.wroteTwoDegrees = wroteFocused;
    }

    public int getNumTablesWritten() {
        return numTables;
    }

    public int getNumViewsWritten() {
        return numViews;
    }

    public boolean wroteImplied() {
        return wroteImplied;
    }

    public boolean wroteTwoDegrees() {
        return wroteTwoDegrees;
    }
}
