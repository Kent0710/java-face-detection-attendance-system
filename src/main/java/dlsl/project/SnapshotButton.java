package dlsl.project;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class SnapshotButton {
    private final JButton SNAPSHOT_BUTTON;
    private final String SNAPSHOT_DIR;
    private final Supplier<VideoCapture>  cameraSupplier;
    private final Supplier<JFrame> windowSupplier;

    public SnapshotButton(String text, Supplier<Boolean> isCameraRunningSupplier, String snapshotDir, Supplier<VideoCapture> cameraSupplier, Supplier<JFrame> windowSupplier) {
        this.SNAPSHOT_BUTTON = new JButton(text);
        this.SNAPSHOT_DIR = snapshotDir;
        this.cameraSupplier = cameraSupplier;
        this.windowSupplier = windowSupplier;

        SNAPSHOT_BUTTON.addActionListener(e -> {
            if (isCameraRunningSupplier.get()) {
                takeSnapshot();
            } else {
                System.out.println("Camera must be running to take snapshot.");
            }
        });
    }

    public JButton getSnapshotButton() {
        return this.SNAPSHOT_BUTTON;
    }

    private void takeSnapshot() {
        Mat frame = new Mat();

        if (!cameraSupplier.get().read(frame) || frame.empty()) {
            System.err.println("ERR at line 127 in FaceDetector.java. Message: frame can not be captured.");
            Utils.showError("Failed to capture frame");
            return;
        }

        String label = JOptionPane.showInputDialog(windowSupplier.get(), "Enter name: ");

        if (label == null || label.trim().isEmpty()) {
            System.err.println("ERR at line 135 in FaceDetector.java. Message: user did not provide a name for the snapshot");
            Utils.showError("Snapshot person must have a name.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                windowSupplier.get(),
                "Save snapshot for \"" + label + "\"?",
                "Confirm snapshot",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            JOptionPane.showMessageDialog(null, "Snapshot not saved.", "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String dirPath = this.SNAPSHOT_DIR + "/" + label.trim();
        File dir = new File(dirPath);

        if (!dir.exists()) {
            boolean isDirCreated = dir.mkdirs();

            if (!isDirCreated) {
                System.err.println("ERR at line 162 in FaceDetector.java. Message: failed to create directory");
                Utils.showError("Something went wrong with directory. Try again.");
            }
        }

        BufferedImage image = Utils.matToBufferedImage(frame);
        String filename = dirPath + "/snapshot_" + System.currentTimeMillis() + ".png";

        try {
            ImageIO.write(image, "png", new File(filename));
            JOptionPane.showMessageDialog(null, "Snapshot saved to " + filename, "SUCCESS", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            Utils.showError("Failed to save snapshot: " + e.getMessage());
        }
    }
}
