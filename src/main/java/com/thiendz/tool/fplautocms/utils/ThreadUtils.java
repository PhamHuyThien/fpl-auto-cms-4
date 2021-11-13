package com.thiendz.tool.fplautocms.utils;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadUtils extends ThreadPoolExecutor {

    private boolean exec;
    private boolean stopExecute;
    private List<? extends Runnable> runnableList;

    public ThreadUtils(List<? extends Runnable> runnableList, int maxThread) {
        super(maxThread, maxThread, 10, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000000));
        this.runnableList = runnableList;
    }

    public void execute() {
        if (exec) {
            return;
        }
        exec = true;
        for (Runnable runnable : this.runnableList) {
            if (stopExecute)
                break;
            if (runnable == null)
                continue;
            execute(runnable);
            sleep(1L);
        }
        shutdown();
    }

    public void await() {
        try {
            awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
        }
    }

    public boolean isExecute() {
        return exec;
    }

    public void stopExecute() {
        stopExecute = true;
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }
}
