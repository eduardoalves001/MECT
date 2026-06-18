package deti.sd.moss.app;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import deti.sd.moss.infra.ui.gui.DashboardFrame;

public class GuiNode implements Runnable {

    @Option(names = { "-m", "--manager" }, description = "Manager url", defaultValue = "localhost:4081")
    private String managerUrl;

    @Option(names = { "-o", "--object" }, description = "Object url", defaultValue = "localhost:4281")
    private String objectUrl;

    @Option(names = { "-b", "--bucket" }, description = "Bucket name", defaultValue = "sd")
    private String bucket;

    @Option(names = { "-r", "--refresh" }, description = "Refresh interval in seconds (2-5)", defaultValue = "3")
    private int refreshSeconds;

    @Override
    public void run() {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame(managerUrl, objectUrl, bucket, refreshSeconds);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    latch.countDown();
                }
            });
            frame.setVisible(true);
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GuiNode()).execute(args);
        System.exit(exitCode);
    }
}
