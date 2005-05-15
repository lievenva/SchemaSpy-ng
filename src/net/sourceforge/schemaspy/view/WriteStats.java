package net.sourceforge.schemaspy.view;

import net.sourceforge.schemaspy.model.Table;

public class WriteStats {
    private int numTables;
    private int numViews;
    private boolean wroteImplied;
    private boolean wroteFocused;

    public void wroteTable(Table table) {
        if (table.isView())
            ++numViews;
        else
            ++numTables;
    }

    public void setWroteImplied(boolean wroteImplied) {
        this.wroteImplied = wroteImplied;
    }

    public void setWroteFocused(boolean wroteFocused) {
        this.wroteFocused = wroteFocused;
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

    public boolean wroteFocused() {
        return wroteFocused;
    }
}
