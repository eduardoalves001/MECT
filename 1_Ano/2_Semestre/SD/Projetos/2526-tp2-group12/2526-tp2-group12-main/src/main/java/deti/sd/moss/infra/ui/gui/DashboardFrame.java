package deti.sd.moss.infra.ui.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import deti.sd.moss.core.manager.model.NodeState;
import deti.sd.moss.core.manager.model.StateReply;
import deti.sd.moss.core.manager.model.VolumeState;
import deti.sd.moss.core.object.model.ListReply;
import deti.sd.moss.core.object.model.ListRequest;
import deti.sd.moss.core.object.model.GetRequest;
import deti.sd.moss.core.object.model.PutRequest;
import deti.sd.moss.core.object.model.RemoveRequest;
import deti.sd.moss.infra.rpc.discovery.GrpcServiceDiscovery;

public class DashboardFrame extends JFrame {
    private static final int MAX_VOLUME_SIZE = 1 << 25;
    private static final int MAX_UPLOAD_SIZE = 4 * 1024 * 1024;
    private static final int MIN_REFRESH_SECONDS = 2;
    private static final int MAX_REFRESH_SECONDS = 5;

    private final JTextField managerField = new JTextField();
    private final JTextField objectField = new JTextField();
    private final JTextField bucketField = new JTextField();
    private final JTextField filterField = new JTextField();
    private final JLabel statusLabel = new JLabel("Idle");
    private final JButton uploadButton = new JButton("Upload...");
    private static final int DOWNLOAD_COLUMN_INDEX = 3;
    private static final int DELETE_COLUMN_INDEX = 4;

    private final DefaultTableModel nodeModel = tableModel(new String[] {"Node", "Status", "Last Seen"});
    private final DefaultTableModel volumeModel = tableModel(new String[] {"VID", "Node", "Used %", "Used", "Available", "Blobs", "Status"});
    private final DefaultTableModel objectModel = new DefaultTableModel(
            new String[] {"Key", "Size", "Timestamp", "Download", "Delete"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == DOWNLOAD_COLUMN_INDEX || column == DELETE_COLUMN_INDEX;
        }
    };
    private final TableRowSorter<DefaultTableModel> objectSorter = new TableRowSorter<>(objectModel);

    private final GrpcServiceDiscovery discovery = new GrpcServiceDiscovery();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService uploadExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService deleteExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean closed;
    private final int refreshSeconds;

    public DashboardFrame(String managerUrl, String objectUrl, String bucket, int refreshSeconds) {
        super("MOSS Dashboard");
        this.refreshSeconds = clampRefresh(refreshSeconds);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        buildUi(managerUrl, objectUrl, bucket);
        startPolling();
    }

    private void buildUi(String managerUrl, String objectUrl, String bucket) {
        managerField.setText(managerUrl);
        objectField.setText(objectUrl);
        bucketField.setText(bucket);

        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createTitledBorder("Connections"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        header.add(new JLabel("Manager"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        header.add(managerField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        header.add(new JLabel("Object"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        header.add(objectField, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.0;
        header.add(new JLabel("Bucket"), gbc);
        gbc.gridx = 5;
        gbc.weightx = 0.5;
        header.add(bucketField, gbc);

        JPanel clusterPanel = new JPanel(new BorderLayout());
        clusterPanel.setBorder(BorderFactory.createTitledBorder("Cluster"));
        JTable nodeTable = new JTable(nodeModel);
        JTable volumeTable = new JTable(volumeModel);
        JScrollPane nodeScroll = new JScrollPane(nodeTable);
        JScrollPane volumeScroll = new JScrollPane(volumeTable);

        JSplitPane clusterSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, nodeScroll, volumeScroll);
        clusterSplit.setResizeWeight(0.35);
        clusterPanel.add(clusterSplit, BorderLayout.CENTER);

        JPanel objectPanel = new JPanel(new BorderLayout());
        objectPanel.setBorder(BorderFactory.createTitledBorder("Objects"));
        JTable objectTable = new JTable(objectModel);
        objectTable.setRowSorter(objectSorter);
        objectSorter.setSortable(DOWNLOAD_COLUMN_INDEX, false);
        objectSorter.setSortable(DELETE_COLUMN_INDEX, false);
        setupDownloadColumn(objectTable);
        setupDeleteColumn(objectTable);
        JScrollPane objectScroll = new JScrollPane(objectTable);

        JPanel filterPanel = new JPanel(new BorderLayout(8, 0));
        filterPanel.add(new JLabel("Search"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);

        JPanel objectTopPanel = new JPanel(new BorderLayout(8, 0));
        objectTopPanel.add(filterPanel, BorderLayout.CENTER);
        objectTopPanel.add(uploadButton, BorderLayout.EAST);

        objectPanel.add(objectTopPanel, BorderLayout.NORTH);
        objectPanel.add(objectScroll, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, clusterPanel, objectPanel);
        mainSplit.setResizeWeight(0.55);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        setLayout(new BorderLayout(8, 8));
        add(header, BorderLayout.NORTH);
        add(mainSplit, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });

        uploadButton.addActionListener(e -> chooseAndUploadFile());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                closed = true;
                scheduler.shutdownNow();
                uploadExecutor.shutdownNow();
                downloadExecutor.shutdownNow();
                deleteExecutor.shutdownNow();
            }
        });
    }

    private void chooseAndUploadFile() {
        String objectUrl = objectField.getText().trim();
        String bucket = bucketField.getText().trim();
        if (objectUrl.isEmpty() || bucket.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Object address and bucket are required.", "Upload error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        int choice = chooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || !selected.isFile()) {
            JOptionPane.showMessageDialog(this, "Select a valid file.", "Upload error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selected.length() > MAX_UPLOAD_SIZE) {
            JOptionPane.showMessageDialog(this, "File exceeds 4 MiB upload limit.", "Upload error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String remotePath = (String) JOptionPane.showInputDialog(
            this,
            "Remote path / name for this object:",
            "Upload as",
            JOptionPane.PLAIN_MESSAGE,
            null,
            null,
            selected.getName());
        if (remotePath == null || remotePath.isBlank()) {
            return;
        }
        remotePath = remotePath.trim();

        final String finalObjectUrl = objectUrl;
        final String finalBucket = bucket;
        final String finalRemotePath = remotePath;
        final File finalSelected = selected;

        uploadButton.setEnabled(false);
        setStatus("Uploading " + finalRemotePath + "...");

        uploadExecutor.submit(() -> {
            try {
                byte[] data = Files.readAllBytes(finalSelected.toPath());
                var reply = discovery.getObject(finalObjectUrl).put(new PutRequest(finalBucket, finalRemotePath, data));
                if (reply.status() != 0) {
                    setStatus("Upload failed for " + finalRemotePath);
                    return;
                }

                setStatus("Upload completed for " + finalRemotePath);
                poll();
            } catch (Exception ex) {
                setStatus("Upload failed: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> uploadButton.setEnabled(true));
            }
        });
    }

    private void startPolling() {
        scheduler.scheduleAtFixedRate(this::poll, 0, refreshSeconds, TimeUnit.SECONDS);
    }

    private void poll() {
        if (closed) {
            return;
        }

        // Capture text field values on the EDT to respect Swing's single-thread rule.
        String[] fields = new String[3];
        try {
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                fields[0] = managerField.getText().trim();
                fields[1] = objectField.getText().trim();
                fields[2] = bucketField.getText().trim();
                latch.countDown();
            });
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        String managerUrl = fields[0];
        String objectUrl  = fields[1];
        String bucket     = fields[2];

        if (managerUrl.isEmpty() || objectUrl.isEmpty()) {
            setStatus("Missing manager or object address");
            return;
        }

        StateReply stateReply;
        ListReply listReply;
        try {
            stateReply = discovery.getManager(managerUrl).state();
        } catch (Exception e) {
            setStatus("Manager state failed: " + e.getMessage());
            return;
        }

        try {
            listReply = discovery.getObject(objectUrl).list(new ListRequest(bucket));
        } catch (Exception e) {
            setStatus("Object list failed: " + e.getMessage());
            return;
        }

        SwingUtilities.invokeLater(() -> {
            updateNodes(stateReply.nodes());
            updateVolumes(stateReply.volumes());
            updateObjects(listReply);
            setStatus("Updated " + Instant.now());
        });
    }

    private void updateNodes(List<NodeState> nodes) {
        nodeModel.setRowCount(0);
        for (NodeState node : nodes) {
            nodeModel.addRow(new Object[] {
                node.url(),
                node.online() ? "online" : "offline",
                node.lastSeen()
            });
        }
    }

    private void updateVolumes(List<VolumeState> volumes) {
        volumeModel.setRowCount(0);
        for (VolumeState volume : volumes) {
            int available = Math.max(0, Math.min(MAX_VOLUME_SIZE, volume.availableSize()));
            int used = Math.max(0, MAX_VOLUME_SIZE - available);
            String usedPct = formatPercent(used);
            String usedMiB = formatMiB(used);
            String availableMiB = formatMiB(available);
            String status = formatVolumeStatus(volume.status());

            volumeModel.addRow(new Object[] {
                volume.vid(),
                volume.nodeUrl(),
                usedPct,
                usedMiB,
                availableMiB,
                volume.fileCount(),
                status
            });
        }
    }

    private void updateObjects(ListReply reply) {
        objectModel.setRowCount(0);
        if (reply.status() != 0 || reply.objects() == null) {
            return;
        }
        for (ListReply.ObjectInfo info : reply.objects()) {
            objectModel.addRow(new Object[] {
                info.path(),
                formatBytes(info.size()),
                Instant.ofEpochMilli(info.timestamp()).toString(),
                "Save...",
                "Delete"
            });
        }
        applyFilter();
    }

    private void setupDownloadColumn(JTable objectTable) {
        objectTable.getColumnModel().getColumn(DOWNLOAD_COLUMN_INDEX)
            .setCellRenderer(new DownloadButtonRenderer());
        objectTable.getColumnModel().getColumn(DOWNLOAD_COLUMN_INDEX)
            .setCellEditor(new DownloadButtonEditor());
        objectTable.getColumnModel().getColumn(DOWNLOAD_COLUMN_INDEX).setMaxWidth(110);
    }

    private void downloadObject(String objectPath) {
        String objectUrl = objectField.getText().trim();
        String bucket = bucketField.getText().trim();
        if (objectUrl.isEmpty() || bucket.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Object address and bucket are required.", "Download error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(objectPath).getName().isEmpty()
            ? new File("download.bin")
            : new File(new File(objectPath).getName()));

        int choice = chooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destination = chooser.getSelectedFile();
        if (destination == null) {
            return;
        }

        setStatus("Downloading " + objectPath + "...");
        downloadExecutor.submit(() -> {
            try {
                var reply = discovery.getObject(objectUrl).get(new GetRequest(bucket, objectPath));
                if (reply.status() != 0) {
                    setStatus("Download failed for " + objectPath);
                    return;
                }
                Files.write(destination.toPath(), reply.data());
                setStatus("Downloaded to " + destination.getName());
            } catch (Exception ex) {
                setStatus("Download failed: " + ex.getMessage());
            }
        });
    }

    private void setupDeleteColumn(JTable objectTable) {
        objectTable.getColumnModel().getColumn(DELETE_COLUMN_INDEX)
            .setCellRenderer(new DeleteButtonRenderer());
        objectTable.getColumnModel().getColumn(DELETE_COLUMN_INDEX)
            .setCellEditor(new DeleteButtonEditor());
        objectTable.getColumnModel().getColumn(DELETE_COLUMN_INDEX).setMaxWidth(90);
    }

    private void deleteObject(String objectPath) {
        String objectUrl = objectField.getText().trim();
        String bucket = bucketField.getText().trim();
        if (objectUrl.isEmpty() || bucket.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Object address and bucket are required.", "Delete error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete '" + objectPath + "' from bucket '" + bucket + "'?",
            "Confirm deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setStatus("Deleting " + objectPath + "...");
        deleteExecutor.submit(() -> {
            try {
                var reply = discovery.getObject(objectUrl).remove(new RemoveRequest(bucket, objectPath));
                if (reply.status() != 0) {
                    setStatus("Delete failed for " + objectPath);
                    return;
                }
                setStatus("Deleted '" + objectPath + "'");
                poll();
            } catch (Exception ex) {
                setStatus("Delete failed: " + ex.getMessage());
            }
        });
    }

    private class DeleteButtonRenderer extends JButton implements TableCellRenderer {
        DeleteButtonRenderer() {
            setText("Delete");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value == null ? "Delete" : value.toString());
            return this;
        }
    }

    private class DeleteButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private final JButton button = new JButton("Delete");
        private String currentPath;

        DeleteButtonEditor() {
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            int modelRow = table.convertRowIndexToModel(row);
            Object pathValue = table.getModel().getValueAt(modelRow, 0);
            currentPath = pathValue == null ? null : pathValue.toString();
            button.setText(value == null ? "Delete" : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Delete";
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            if (currentPath != null && !currentPath.isEmpty()) {
                deleteObject(currentPath);
            }
        }
    }

    private class DownloadButtonRenderer extends JButton implements TableCellRenderer {
        DownloadButtonRenderer() {
            setText("Save...");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setText(value == null ? "Save..." : value.toString());
            return this;
        }
    }

    private class DownloadButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private final JButton button = new JButton("Save...");
        private String currentPath;

        DownloadButtonEditor() {
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            int modelRow = table.convertRowIndexToModel(row);
            Object pathValue = table.getModel().getValueAt(modelRow, 0);
            currentPath = pathValue == null ? null : pathValue.toString();
            button.setText(value == null ? "Save..." : value.toString());
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Save...";
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
            if (currentPath != null && !currentPath.isEmpty()) {
                downloadObject(currentPath);
            }
        }
    }

    private void applyFilter() {
        String query = filterField.getText().trim();
        if (query.isEmpty()) {
            objectSorter.setRowFilter(null);
            return;
        }
        objectSorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(query), 0));
    }

    private void setStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    private static DefaultTableModel tableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static String formatBytes(long bytes) {
        double value = bytes;
        String[] units = {"B", "KiB", "MiB", "GiB"};
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024.0;
            unit++;
        }
        return String.format("%.1f %s", value, units[unit]);
    }

    private static String formatMiB(long bytes) {
        double value = bytes / (1024.0 * 1024.0);
        return String.format("%.1f MiB", value);
    }

    private static String formatPercent(long usedBytes) {
        double pct = (double) usedBytes * 100.0 / MAX_VOLUME_SIZE;
        return String.format("%.1f%%", pct);
    }

    private static String formatVolumeStatus(int status) {
        return status > 0 ? "online" : "offline";
    }

    private static int clampRefresh(int seconds) {
        if (seconds < MIN_REFRESH_SECONDS) {
            return MIN_REFRESH_SECONDS;
        }
        if (seconds > MAX_REFRESH_SECONDS) {
            return MAX_REFRESH_SECONDS;
        }
        return seconds;
    }
}