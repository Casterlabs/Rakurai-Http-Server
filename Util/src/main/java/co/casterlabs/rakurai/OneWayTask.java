package co.casterlabs.rakurai;

import org.jetbrains.annotations.Nullable;

import lombok.NonNull;

public class OneWayTask<T> {
    private ReturningTask<T> task;
    private boolean completed;

    private Exception exception;
    private T result;

    public OneWayTask(@NonNull ReturningTask<T> task) {
        this.task = task;
    }

    public @Nullable T get() throws Exception {
        if (this.completed) {
            if (this.exception == null) {
                return this.result;
            } else {
                throw this.exception;
            }
        } else {
            this.completed = true;

            try {
                return this.result = this.task.invoke();
            } catch (Exception e) {
                this.exception = e;
                throw e;
            }
        }
    }

    public static interface ReturningTask<T> {

        public T invoke() throws Exception;

    }

}
