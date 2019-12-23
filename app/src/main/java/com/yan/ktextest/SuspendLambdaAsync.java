package com.yan.ktextest;

import android.util.Log;

/**
 * @author Bevan
 * @since 2019-12-23 14:34.
 * Contact me: "https://github.com/genius158"
 */
class SuspendLambdaAsync extends SuspendLambda {
    int label;

    public SuspendLambdaAsync(TestContinuation suspendLambda, TestContext context) {
        super(suspendLambda, context);
    }

    private TestContinuation continuation;

    @Override
    void onCreate(TestContinuation testContinuation) {
        this.continuation = testContinuation;
    }

    @Override
    public Object invokeSuspend(Object result) {
        switch (label) {
            case 0:
                label = 1;
                State state = Delay.delay(continuation, 3000);
                if (state == State.COROUTINE_SUSPENDED) {
                    return state;
                }
            case 1:
                Log.e("launch withContext ", " 22222222 " + Thread.currentThread());
                return 1;
        }
        return null;
    }
}
