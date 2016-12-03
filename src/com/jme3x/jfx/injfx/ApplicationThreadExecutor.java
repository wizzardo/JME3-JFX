package com.jme3x.jfx.injfx;

import rlib.util.ArrayUtils;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;
import rlib.util.array.ConcurrentArray;

/**
 * The executor for executing tasks in application thread.
 *
 * @author JavaSaBr
 */
public class ApplicationThreadExecutor {

    private static final ApplicationThreadExecutor INSTANCE = new ApplicationThreadExecutor();

    public static ApplicationThreadExecutor getInstance() {
        return INSTANCE;
    }

    /**
     * The list of waiting tasks.
     */
    private final ConcurrentArray<Runnable> waitTasks;

    /**
     * THe list of tasks to execute.
     */
    private final Array<Runnable> execute;

    public ApplicationThreadExecutor() {
        this.waitTasks = ArrayFactory.newConcurrentAtomicARSWLockArray(Runnable.class);
        this.execute = ArrayFactory.newArray(Runnable.class);
    }

    /**
     * Add the task to execute.
     *
     * @param task the new task.
     */
    public void addToExecute(final Runnable task) {
        ArrayUtils.runInWriteLock(waitTasks, task, Array::add);
    }

    /**
     * Execute the waiting tasks.
     */
    public void execute() {
        if (waitTasks.isEmpty()) return;
        ArrayUtils.runInWriteLock(waitTasks, execute, ArrayUtils::move);
        try {
            execute.forEach(Runnable::run);
        } finally {
            execute.clear();
        }
    }
}
