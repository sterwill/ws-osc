package com.tinfig.wsosc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
}
