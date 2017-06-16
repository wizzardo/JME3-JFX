package com.jme3x.jfx.injfx;

import org.jetbrains.annotations.NotNull;

import com.ss.rlib.util.ArrayUtils;
import com.ss.rlib.util.array.Array;
import com.ss.rlib.util.array.ArrayFactory;
import com.ss.rlib.util.array.ConcurrentArray;

/**
 * The executor for executing tasks in application thread.
 *
 * @author JavaSaBr
 */
public class ApplicationThreadExecutor {

    private static final ApplicationThreadExecutor INSTANCE = new ApplicationThreadExecutor();

    /**
     * Gets instance.
     *
     * @return the instance
     */
    @NotNull
    public static ApplicationThreadExecutor getInstance() {
        return INSTANCE;
    }

    /**
     * The list of waiting tasks.
     */
    @NotNull
    private final ConcurrentArray<Runnable> waitTasks;

    /**
     * THe list of tasks to execute.
     */
    @NotNull
    private final Array<Runnable> execute;

    /**
     * Instantiates a new Application thread executor.
     */
    public ApplicationThreadExecutor() {
        this.waitTasks = ArrayFactory.newConcurrentAtomicARSWLockArray(Runnable.class);
        this.execute = ArrayFactory.newArray(Runnable.class);
    }

    /**
     * Add the task to execute.
     *
     * @param task the new task.
     */
    public void addToExecute(@NotNull final Runnable task) {
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
