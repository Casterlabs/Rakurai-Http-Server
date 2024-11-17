package co.casterlabs.rhs.util;

public interface TaskExecutor {

    /**
     * @implSpec The returned thread <b>MUST</b> already be started.
     * 
     * @implNote When reusing threads, you <b>MUST</b> clear the interrupt flag
     *           yourself.
     */
    public Task execute(Runnable toRun);

    public interface Task {

        public void interrupt();

        public void waitFor() throws InterruptedException;

        public boolean isAlive();

    }

}
