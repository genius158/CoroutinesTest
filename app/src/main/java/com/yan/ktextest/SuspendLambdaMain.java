package com.yan.ktextest;

import android.util.Log;

import kotlin.Unit;

/**
 * GlobalScope.launch(dispatch) {
 * val intValue = async(start = CoroutineStart.LAZY, context = Dispatchers.IO) {
 * delay(3000)
 * 1
 * }
 * val value = intValue.await()
 * Log.e("launch", "value  " + value)
 * }
 */

class SuspendLambdaMain extends SuspendLambda {
    /**
     * 没执行一个case，label都会被置为下一个case对应的int值
     */
    private int label;

    public SuspendLambdaMain(TestContext dispatch) {
        super(null, dispatch);
    }

    /**
     * 协程为我们分装的执行类，把执行和线程切换包装在一起
     */
    private TestContinuation continuation;

    @Override
    void onCreate(TestContinuation testContinuation) {
        this.continuation = testContinuation;
    }

    @Override
    public final Object invokeSuspend(Object result) {
        int i = this.label;
        if (i == 0) {
            this.label = 1;
            Log.e("launch withContext ", " 1111111 " + Thread.currentThread());
            new SuspendLambdaAsync(continuation, TestDispatchers.IO).intercepted().resumeWith(null);
            return State.COROUTINE_SUSPENDED;
        } else if (i == 1) {
            int value = (int) result;
            Log.e("launch withContext", "value  " + value + "    " + Thread.currentThread());
        }

        return Unit.INSTANCE;
    }
}
