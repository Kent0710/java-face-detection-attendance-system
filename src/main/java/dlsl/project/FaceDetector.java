package dlsl.project;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

public class FaceDetector {
    private static final String CASCADE_PATH = "resources/haarcascade_frontalface_default.xml";
    private static final String SNAPSHOT_DIR = "snapshots";
    private static final int FRAME_WIDTH = 640;
    private static final int FRAME_HEIGHT = 480;

    private static JFrame window;
    private static JLabel videoPanel;
    private static VideoCapture camera;
    private static CascadeClassifier faceCascade;

    // Volatile means to always read directly from memory
    // Do not cached the value
    private static volatile boolean isCameraRunning = false;

    public static void run() {
        SwingUtilities.invokeLater(FaceDetector::createAndShowGUI);
    }

    private static CascadeClassifier loadCascadeClassifier() {
        CascadeClassifier classifier = new CascadeClassifier(CASCADE_PATH);
        if (classifier.empty()) {
            throw new IllegalStateException("Failed to load cascade  model from path: " + CASCADE_PATH);
        }
        System.out.println("Cascade model successfully loaded.");
        return classifier;
    }

    private static void createAndShowGUI() {
        // Load classifier first
        // If classifier first, the system itself will fail
        // Prevent unnecessary GUI initialization if the main logic fails
        faceCascade = loadCascadeClassifier();

        // Create window
        window = new JFrame("Face Detection - Press Q to Exit");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.setSize(FRAME_WIDTH, FRAME_HEIGHT);

        // Create video panel feed
        // Where the camera will show
        videoPanel = new JLabel();
        window.add(videoPanel, BorderLayout.CENTER);

        // JPanel for the buttons
        JPanel bottomButtonsPanel = getBottomButtonsPanel();

        window.add(bottomButtonsPanel, BorderLayout.SOUTH);

        // Key listener for "Q" to quit
        InputMap inputMap = window.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = window.getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("Q"), "quit");
        actionMap.put("quit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Reverting isCameraRunning is done inside stopCameraFeed()
                stopCameraFeed();
            }
        });

        window.setVisible(true);
        window.setFocusable(true);
        window.requestFocusInWindow();
    }

    private static JPanel getBottomButtonsPanel() {
        JPanel bottomButtonsPanel = new JPanel();

        JButton startButton = new JButton("Start camera");
        startButton.addActionListener(e -> {
            if (!isCameraRunning) {
                startCameraFeed();
            } else {
                System.out.println("Camera is already running.");
            }
        });

        JButton endButton = new JButton("Stop camera");
        endButton.addActionListener(e -> {
            if (isCameraRunning) {
                closeCameraFeed();
            } else {
                System.out.println("Camera is not running.");
            }
        });

        SnapshotButton snapshotButton = new SnapshotButton("Take snapshot", () -> isCameraRunning, SNAPSHOT_DIR, ()-> camera, () ->window);

        // Add the start and stop button to the bottom panel
        bottomButtonsPanel.add(startButton);
        bottomButtonsPanel.add(endButton);
        bottomButtonsPanel.add(snapshotButton.getSnapshotButton());
        return bottomButtonsPanel;
    }

    private static void detectAndDrawFaces(Mat frame) {
        MatOfRect faces = new MatOfRect();
        faceCascade.detectMultiScale(frame, faces);

        for (Rect rect : faces.toArray()) {
            Imgproc.rectangle(
                    frame,
                    new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(0, 255, 0),
                    2
            );
        }
    }

    private static void startCameraFeed() {
        isCameraRunning = true;
        System.out.println("Attempting to start camera feed...");

        camera = new VideoCapture(0);
        if (!camera.isOpened()) {
            Utils.showError("ERR at line 81 in FaceDetector.java. Message: camera failed to open.");
            isCameraRunning = false;
            return;
        }

        System.out.println("Camera feed is now open.");
        System.out.println("Now starting camera thread...");

        Thread cameraThread = getCameraThread();
        cameraThread.start();

        System.out.println("Camera thread is now active.");
    }

    private static Thread getCameraThread() {
        Thread cameraThread = new Thread(() -> {
            Mat frame = new Mat();

            while (isCameraRunning) {
                if (!camera.read(frame) || frame.empty()) {
                    continue;
                }
                detectAndDrawFaces(frame);
                BufferedImage image = Utils.matToBufferedImage(frame);
                SwingUtilities.invokeLater(() -> {
                    videoPanel.setIcon(new ImageIcon(image));
                    videoPanel.repaint();
                });
            }

            camera.release();
            System.out.println("Camera released.");
        });

        cameraThread.setDaemon(true);
        return cameraThread;
    }

    private static void stopCameraFeed() {
        isCameraRunning = false;
        System.out.println("Camera feed stopped.");
        if (window != null) {
            window.dispose();
            System.out.println("Window disposed.");
        }
        System.out.println("Stopped by user.");
    }

    private static void closeCameraFeed() {
        System.out.println("Attempting to stop camera feed...");

        isCameraRunning = false;

        if (camera != null && camera.isOpened()) {
            camera.release();
        }

        // Remove the last displayed image
        SwingUtilities.invokeLater(() -> {
            videoPanel.setIcon(null);
            videoPanel.repaint();
        });

        System.out.println("Camera feed stopped and panel cleared.");
    }
}