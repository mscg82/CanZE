package lu.fisch.canze.actors;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by robertfisch on 30.12.2016.
 */

public class StoppableThread extends Thread {

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public StoppableThread(Runnable runnable) {
        super(runnable);
    }

    public void start() {
        stopped.getAndSet(false);
        super.start();
    }

    public void tryToStop() {
        stopped.getAndSet(true);
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
