package co.casterlabs.rhs.util;

public interface TaskExecutor {

    /**
     * @implSpec The returned thread <b>MUST</b> already be started.
     * 
     * @implNote When reusing threads, you <b>MUST</b> clear the interrupt flag
     *           yourself.
     */
    public Task execute(Runnable toRun, TaskType type);

    public interface Task {

        public void interrupt();

        public void waitFor() throws InterruptedException;

        public boolean isAlive();

    }

    public static enum TaskType {
        /**
         * LIGHT_IO tasks can be run whenever you have a chance to run them, such as
         * when sufficient threads are available. "Virtual threads" are recommended for
         * this type.
         */
        LIGHT_IO,

        /**
         * MEDIUM_IO tasks <b>MUST</b> be run immediately. They <b>MUST NOT</b> be
         * queued for later execution. "Virtual threads" are recommended for this type.
         */
        MEDIUM_IO,

        /**
         * HEAVY_IO tasks <b>MUST</b> be run immediately. They <b>MUST NOT</b> be queued
         * for later execution. "Platform threads" are recommended for this type.
         */
        HEAVY_IO
    }

}
