package app.bus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class SimpleWorkspaceBus implements WorkspaceBus {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    @Override
    public <T> AutoCloseable subscribe(Class<T> type, Consumer<T> handler) {
        listeners.computeIfAbsent(type, k -> Collections.synchronizedList(new ArrayList<>())).add(handler);
        return () -> listeners.getOrDefault(type, List.of()).remove(handler);
    }

    @Override
    public void publish(Object event) {
        if (event == null) return;
        var list = listeners.getOrDefault(event.getClass(), List.of());
        for (var l : List.copyOf(list)) {
            @SuppressWarnings("unchecked")
            Consumer<Object> c = (Consumer<Object>) l;
            c.accept(event);
        }
    }
}
