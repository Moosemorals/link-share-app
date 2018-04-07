package com.moosemorals.linkshare;

// Based on https://stackoverflow.com/a/6312491/195833
final class AsyncResult<T> {
    private T result;
    private Exception error;

    AsyncResult(T result) {
        this.result = result;
    }

    AsyncResult(Exception error) {
        this.error = error;
    }

    boolean isSuccess() {
        return result != null;
    }

    T getResult() {
        return result;
    }

    Exception getError() {
        return error;
    }
}

