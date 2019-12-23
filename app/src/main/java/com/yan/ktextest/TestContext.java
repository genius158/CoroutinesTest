package com.yan.ktextest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Bevan
 * @since 2019-12-22 13:06.
 * Contact me: "https://github.com/genius158"
 */
public abstract class TestContext {
    protected static final String KEY_INTERCEPT = "intercept";
    protected Map<String, TestContext> contextMap = new HashMap<>();

    public abstract String key();

    public TestContext getDispatcher() {
        return contextMap.get(KEY_INTERCEPT);
    }

    public void plusContext(TestContext context) {
        contextMap.put(context.key(), context);
    }

}
