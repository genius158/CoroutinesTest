package com.yan.ktextest;

/**
 * @author Bevan
 * @since 2019-12-23 11:50.
 * Contact me: "https://github.com/genius158"
 */
public abstract class TestDispatcher extends TestContext {

    abstract void dispatch(Runnable task);

    @Override
    public String key() {
        return TestContext.KEY_INTERCEPT;
    }
}
