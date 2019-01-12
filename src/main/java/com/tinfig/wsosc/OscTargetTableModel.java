package com.tinfig.wsosc;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OscTargetTableModel extends AbstractTableModel {
    private static final String[] COLUMN_NAMES = {"Address", "Port"};
    private final List<OscTarget> data = new ArrayList<>();

    public String getColumnName(int col) {
        return COLUMN_NAMES[col].toString();
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return Integer.class;
            default:
                throw new IllegalStateException();
        }
    }

    public int getRowCount() {
        return data.size();
    }

    public Object getValueAt(int row, int col) {
        OscTarget target = data.get(row);
        switch (col) {
            case 0:
                return target.address;
            case 1:
                return target.port;
            default:
                throw new IllegalStateException();
        }
    }

    public boolean isCellEditable(int row, int col) {
        return true;
    }

    public void setValueAt(Object value, int row, int col) {
        OscTarget target = data.get(row);
        switch (col) {
            case 0:
                target.address = (String) value;
                break;
            case 1:
                target.port = (Integer) value;
                break;
            default:
                throw new IllegalStateException();
        }
        fireTableCellUpdated(row, col);
    }

    public List<OscTarget> getData() {
        return data;
    }

    public void add(OscTarget target) {
        data.add(target);
        fireTableDataChanged();
    }

    public void delete(int[] rows) {
        // Sort them backwards, then remove them from the data model
        Arrays.stream(rows)
                .mapToObj(i -> (Integer) i)
                .sorted((i1, i2) -> Integer.compare(i2, i1))
                .forEach(i -> {
                    if (i < data.size()) {
                        data.remove((int) i);
                    }
                });
        fireTableDataChanged();
    }
}
