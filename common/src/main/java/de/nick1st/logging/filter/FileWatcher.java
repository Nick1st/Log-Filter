package de.nick1st.logging.filter;

import java.io.File;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileWatcher extends Thread {
    private final File file;
    private final Runnable runnable;
    private AtomicBoolean stop = new AtomicBoolean(false);

    public FileWatcher(File file, Runnable runnable) {
        this.file = file;
        this.runnable = runnable;
    }

    public boolean isStopped() { return stop.get(); }
    public void stopThread() { stop.set(true); }

    public synchronized void doOnChange() {
        runnable.run();
    }

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            Path path = file.toPath().getParent();
            path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            while (!isStopped()) {
                WatchKey key;
                try { key = watcher.poll(25, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { return; }
                if (key == null) { Thread.yield(); continue; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(file.getName())) {
                        doOnChange();
                    }
                    boolean valid = key.reset();
                    if (!valid) { break; }
                }
                Thread.yield();
            }
        } catch (Exception e) {
            Constants.LOG.error(e.getMessage(), e);
        }
    }
}