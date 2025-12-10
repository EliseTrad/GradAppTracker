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

    /**
     * Execute background work and handle callbacks on the JavaFX Application
     * Thread.
     * 
     * @param <T>       the return type of the background work
     * @param work      the callable to execute in background thread
     * @param onSuccess callback executed on JavaFX thread if work succeeds,
     *                  receives the result
     * @param onError   callback executed on JavaFX thread if work fails, receives
     *                  the exception
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        run(work, onSuccess, onError, null);
    }

    /**
     * Execute background work with callbacks and a finally block on the JavaFX
     * Application Thread.
     * 
     * @param <T>       the return type of the background work
     * @param work      the callable to execute in background thread
     * @param onSuccess callback executed on JavaFX thread if work succeeds,
     *                  receives the result
     * @param onError   callback executed on JavaFX thread if work fails, receives
     *                  the exception
     * @param onFinally runnable always executed on JavaFX thread after success or
     *                  error
     */
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

    /**
     * Thread factory that creates daemon threads for background work.
     * Daemon threads don't prevent JVM shutdown.
     */
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
