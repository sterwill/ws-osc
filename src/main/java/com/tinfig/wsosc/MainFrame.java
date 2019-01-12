package com.tinfig.wsosc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class MainFrame extends JFrame implements Logger {
    private static final String PREF_OSC_TARGETS = "oscTargets";
    private static final String PREF_COLUMN_WIDTHS = "columnWidths";

    private JTable tableOscTargets;
    private JTextArea textAreaLogs;
    private JPanel rootPanel;
    private JButton applyButton;
    private JButton buttonAddOscTarget;
    private JButton buttonDeleteOscTarget;
    private JProgressBar progressBarProcessing;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Preferences prefs = Preferences.userNodeForPackage(MainFrame.class);
    private final OscTargetTableModel tableModel;
    private final AtomicBoolean closing = new AtomicBoolean();
    private final Timer progressBarCancelTimer;
    private BridgeThread bridgeThread;
    private TimerTask progressBarCancelTimerTask;

    public MainFrame() {
        super("WebSocket to OSC Bridge");

        // UI
        tableModel = new OscTargetTableModel();
        setContentPane(rootPanel);
        textAreaLogs.setFont(new Font("monospaced", Font.PLAIN, 12));
        textAreaLogs.setAutoscrolls(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        tableOscTargets.setModel(tableModel);
        progressBarProcessing.setMinimum(0);
        progressBarProcessing.setMaximum(1);
        pack();

        // Prefs (updates UI)
        loadPrefs();

        // Listeners
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closing.set(true);
                stopBridgeThread();
                savePrefs();
                e.getWindow().dispose();
            }
        });

        applyButton.addActionListener(e -> {
            savePrefs();
            restartBridgeThread();
        });

        buttonAddOscTarget.addActionListener(e -> {
            tableModel.add(new OscTarget("localhost", 1234));
        });

        buttonDeleteOscTarget.setEnabled(false);
        buttonDeleteOscTarget.addActionListener(e -> {
            tableModel.delete(tableOscTargets.getSelectedRows());
        });

        tableOscTargets.getSelectionModel().addListSelectionListener(e -> {
            buttonDeleteOscTarget.setEnabled(tableOscTargets.getSelectedRowCount() > 0);
        });

        progressBarCancelTimer = new Timer();

        startBridgeThread();
    }

    private void loadPrefs() {
        String oscTargetsString = prefs.get(PREF_OSC_TARGETS, null);
        if (oscTargetsString != null) {
            try {
                List<OscTarget> oscTargets = objectMapper.readValue(oscTargetsString, new TypeReference<List<OscTarget>>() {
                });
                tableModel.getData().clear();
                tableModel.getData().addAll(oscTargets);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String columnWidthsString = prefs.get(PREF_COLUMN_WIDTHS, null);
        if (columnWidthsString != null) {
            try {
                setColumnWidths(objectMapper.readValue(columnWidthsString, Integer[].class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void savePrefs() {
        try {
            prefs.put(PREF_OSC_TARGETS, objectMapper.writeValueAsString(tableModel.getData()));
            prefs.put(PREF_COLUMN_WIDTHS, objectMapper.writeValueAsString(getColumnWidths()));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        try {
            prefs.sync();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    private void restartBridgeThread() {
        if (bridgeThread != null) {
            stopBridgeThread();
        }
    }

    private void startBridgeThread() {
        bridgeThread = new BridgeThread(tableModel.getData(), this::onMessageProcessed, this::onBridgeThreadShutdown, this::log);
        bridgeThread.start();
    }

    private void stopBridgeThread() {
        if (bridgeThread != null) {
            bridgeThread.shutdownBridge();
        }
    }

    private void onBridgeThreadShutdown() {
        boolean crashed = bridgeThread.isCrashed();
        bridgeThread = null;
        if (!closing.get()) {
            if (crashed) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            startBridgeThread();
        }
    }

    private void onMessageProcessed() {
        SwingUtilities.invokeLater(() -> {
            if (progressBarCancelTimerTask != null) {
                progressBarCancelTimerTask.cancel();
            }
            progressBarCancelTimer.purge();

            progressBarCancelTimerTask = new TimerTask() {
                @Override
                public void run() {
                    progressBarProcessing.setValue(0);
                    progressBarCancelTimerTask = null;
                }
            };
            progressBarProcessing.setValue(1);
            progressBarCancelTimer.schedule(progressBarCancelTimerTask, 200);
        });
    }

    public void log(String msg) {
        SwingUtilities.invokeLater(() -> textAreaLogs.append(msg + "\n"));
    }

    public int[] getColumnWidths() {
        return Collections.list(tableOscTargets.getColumnModel().getColumns()).stream()
                .mapToInt(TableColumn::getPreferredWidth)
                .toArray();
    }

    public void setColumnWidths(Integer[] widths) {
        List<TableColumn> cols = Collections.list(tableOscTargets.getColumnModel().getColumns());
        for (int i = 0; i < Math.min(widths.length, cols.size()); i++) {
            cols.get(i).setPreferredWidth(widths[i]);
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(2, 1, new Insets(8, 8, 8, 8), -1, -1));
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setDividerLocation(276);
        splitPane1.setOrientation(0);
        rootPanel.add(splitPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(600, 400), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("OSC Targets");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        tableOscTargets = new JTable();
        scrollPane1.setViewportView(tableOscTargets);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttonAddOscTarget = new JButton();
        buttonAddOscTarget.setText("Add Target");
        panel3.add(buttonAddOscTarget, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonDeleteOscTarget = new JButton();
        buttonDeleteOscTarget.setText("Delete Target");
        panel3.add(buttonDeleteOscTarget, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        applyButton = new JButton();
        applyButton.setText("Apply Settings");
        panel3.add(applyButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setRightComponent(panel4);
        final JLabel label2 = new JLabel();
        label2.setText("Log Messages");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel4.add(scrollPane2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textAreaLogs = new JTextArea();
        textAreaLogs.setEditable(false);
        Font textAreaLogsFont = UIManager.getFont("TextArea.font");
        if (textAreaLogsFont != null) textAreaLogs.setFont(textAreaLogsFont);
        textAreaLogs.setLineWrap(true);
        scrollPane2.setViewportView(textAreaLogs);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        progressBarProcessing = new JProgressBar();
        panel5.add(progressBarProcessing, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(40, 20), new Dimension(40, 20), new Dimension(40, 20), 1, false));
        final JLabel label3 = new JLabel();
        label3.setText("Processing WebSocket Message");
        panel5.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel5.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        label1.setLabelFor(scrollPane1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
