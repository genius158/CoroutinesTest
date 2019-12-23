package com.yan.ktextest;

/**
 * @author Bevan
 * @since 2019-12-22 10:02.
 * Contact me: "https://github.com/genius158"
 */
public class Delay {


    static State delay(final TestContinuation continuation, long delay) {
        if (delay == 0) {
            continuation.resumeWith(null);
            return null;
        }
        TestDispatchers.IO.delay(new Runnable() {
            @Override
            public void run() {
                continuation.resumeWith(null);
            }
        }, delay);
        return State.COROUTINE_SUSPENDED;
    }

}
