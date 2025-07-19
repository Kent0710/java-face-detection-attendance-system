package dlsl.project;

import org.opencv.core.Core;

public class Main {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void main(String[] args) {
        System.out.println("OpenCV version: " + Core.VERSION);

        // Run the face detector app
        FaceDetector.run();
    }
}