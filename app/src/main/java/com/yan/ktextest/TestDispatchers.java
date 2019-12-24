package com.yan.ktextest;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Bevan
 * @since 2019-12-22 13:10.
 * Contact me: "https://github.com/genius158"
 */
public class TestDispatchers {
    public static IO IO = new IO();

    public static MAIN MAIN = new MAIN();
    private static DefaultExecutor defaultExecutor = new DefaultExecutor();


    static class IO extends TestDispatcher {
        public IO() {
            plusContext(this);
        }

        private AtomicLong atomicLong = new AtomicLong();
        private Executor executor = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()
                , new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("IO: " + atomicLong.getAndIncrement());
                return thread;
            }
        });

        @Override
        void dispatch(Runnable task) {
            executor.execute(task);
        }

        public void delay(final Runnable task, long delayMillis) {
            defaultExecutor.delayQueue.add(new RunnableTime(new Runnable() {
                @Override
                public void run() {
                    dispatch(task);
                }
            }, delayMillis));
        }
    }

    static class MAIN extends TestDispatcher {
        public MAIN() {
            plusContext(this);
        }

        Handler handler = new Handler(Looper.getMainLooper());

        @Override
        void dispatch(Runnable task) {
            handler.post(task);
        }

        public void delay(final Runnable task, long delayMillis) {
            handler.postDelayed(task, delayMillis);
        }
    }


    /**
     * 最简单的实现，没有处理添加、时序等问题
     */
    static class DefaultExecutor implements Runnable {
        LinkedBlockingQueue<RunnableTime> delayQueue = new LinkedBlockingQueue<>();

        public DefaultExecutor() {
            new Thread(this, "Default").start();
        }

        /**
         * delay的实现，一个死循环线程死，循环访问所有任务，当某个任务达到触发条件则执行
         * <p>
         * 协程的delay 默认是在 DefaultExecutor 上实现的，其也是的run是一个死循环，退不出去
         * 也就是说用了Default Dispatcher 那么就会存在一个不会退出且死循环的线程，
         * 这样真的好么,就不太懂了~~
         * <p>
         * override fun run() {
         * ...
         * try {
         * ...
         * // 死循环！！！
         * while (true) {}
         * } finally {
         * //一些变量清理
         * ...
         * }
         * }
         */
        @Override
        public void run() {
            while (true) {
                RunnableTime runnableTime = delayQueue.peek();
                if (runnableTime != null) {
                    if (runnableTime.time < System.currentTimeMillis()) {
                        runnableTime.run();
                        delayQueue.remove(runnableTime);
                    }
                }
                /* 一个死循环，任务为空的时候，仍然存在间隔1秒的运行 */
                LockSupport.parkNanos(this, processNextEvent());
            }
        }

        long processNextEvent() {
            long waitTime = 1000000000;
            RunnableTime runnableTime = delayQueue.peek();
            if (runnableTime != null) {
                waitTime = runnableTime.time - System.currentTimeMillis();
            }
            return waitTime;
        }
    }

    static class RunnableTime implements Runnable {
        Runnable runnable;
        long time;

        RunnableTime(Runnable runnable, long delayMillis) {
            this.runnable = runnable;
            this.time = delayMillis + System.currentTimeMillis();
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}

