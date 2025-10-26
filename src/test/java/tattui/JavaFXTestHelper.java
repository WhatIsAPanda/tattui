package tattui;

import javafx.application.Platform;
import java.util.concurrent.CountDownLatch;

public class JavaFXTestHelper {
    public static void initToolkit() {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}