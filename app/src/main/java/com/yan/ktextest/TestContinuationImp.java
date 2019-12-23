package com.yan.ktextest;

/**
 * @author Bevan
 * @since 2019-12-23 10:19.
 * Contact me: "https://github.com/genius158"
 */
public abstract class TestContinuationImp extends TestContinuation {
    private final TestContinuation completion;
    private final TestContext context;

    public TestContinuationImp(TestContinuation completion, TestContext context) {
        this.completion = completion;
        this.context = context;
    }


    private TestContinuation intercepted;

    public final TestContinuation intercepted() {
        if (intercepted != null) {
            return intercepted;
        }
        intercepted = new TestDispatchedContinuation(this, context);
        onCreate(intercepted);
        return intercepted;
    }

    @Override
    public void resumeWith(Object result) {
        Object value;
        if ((value = invokeSuspend(result)) == State.COROUTINE_SUSPENDED) {
            return;
        }
        if (completion != null) {
            completion.resumeWith(value);
        }
    }

    abstract Object invokeSuspend(Object result);

    abstract void onCreate(TestContinuation testContinuation);
}
