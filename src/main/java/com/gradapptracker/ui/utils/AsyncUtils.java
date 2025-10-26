package com.gradapptracker.ui.utils;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Utility to run background work using a shared ExecutorService and JavaFX
 * {@link Task} so callbacks are executed on the JavaFX Application Thread.
 */
public final class AsyncUtils {

    private static final ExecutorService EXEC = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()), new DaemonThreadFactory());

    private AsyncUtils() {
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        run(work, onSuccess, onError, null);
    }

    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError,
            Runnable onFinally) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };

        task.setOnSucceeded(evt -> {
            try {
                if (onSuccess != null)
                    onSuccess.accept(task.getValue());
            } finally {
                if (onFinally != null) {
                    try {
                        onFinally.run();
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            if (onError != null)
                onError.accept(ex == null ? new RuntimeException("Unknown error") : ex);
            if (onFinally != null) {
                try {
                    onFinally.run();
                } catch (Exception ignored) {
                }
            }
        });

        EXEC.submit(task);
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final ThreadFactory delegate = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = delegate.newThread(r);
            t.setDaemon(true);
            return t;
        }
    }
}
