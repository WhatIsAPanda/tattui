package app.bus;

import java.util.function.Consumer;

public interface WorkspaceBus {
    <T> AutoCloseable subscribe(Class<T> type, Consumer<T> handler);
    void publish(Object event);
}
