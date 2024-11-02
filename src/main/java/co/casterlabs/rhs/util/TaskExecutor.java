package co.casterlabs.rhs.util;

public interface TaskExecutor {

    /**
     * @implSpec The returned thread <b>MUST</b> already be started.
     * 
     * @implNote When reusing threads, you <b>MUST</b> clear the interrupt flag
     *           yourself.
     */
    public Thread execute(Runnable toRun, TaskUrgency urgency);

    public static enum TaskUrgency {
        /**
         * DELAYABLE tasks can be run whenever you have a chance to run them, such as
         * when sufficient threads are available.
         */
        DELAYABLE,

        /**
         * IMMEDIATE tasks <b>MUST</b> be run immediately. They <b>MUST NOT</b> be
         * queued for later execution.
         */
        IMMEDIATE
    }

}
