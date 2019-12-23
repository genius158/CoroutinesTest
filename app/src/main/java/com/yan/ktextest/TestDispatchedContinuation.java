package com.yan.ktextest;

/**
 * @author Bevan
 * @since 2019-12-23 11:47.
 * Contact me: "https://github.com/genius158"
 */
public class TestDispatchedContinuation extends TestContinuation implements Runnable {
    private TestContinuationImp completion;
    private TestContext context;

    public TestDispatchedContinuation(TestContinuationImp completion, TestContext context) {
        this.completion = completion;
        this.context = context;
    }

    private Object value;

    @Override
    public void resumeWith(Object result) {
        this.value = result;
        TestDispatcher dispatcher = (TestDispatcher) context.getDispatcher();
        if (dispatcher == null) {
            run();
        } else {
            dispatcher.dispatch(this);
        }
    }

    @Override
    public void run() {
        completion.resumeWith(value);
    }
}
