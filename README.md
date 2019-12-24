## kotlin 协程执行过程java代码化，让我们来了解其中的奥秘
ps:写文章不是强项，有什么问题看[代码事例](https://github.com/genius158/CoroutinesTest)~~

### 开始
```
GlobalScope.launch(Dispatchers.Main) {
    Log.e("launch withContext ", " 1111111 " + Thread.currentThread())
    val intValue = async(context = Dispatchers.IO) {
        delay(3000)
        1
        Log.e("launch withContext ", " 22222222 " + Thread.currentThread())
    }
    val value = intValue.await()
    Log.e("launch withContext", "value  " + value + "    " + Thread.currentThread())
 
```
以上是一段最常用的标准代码样例，编译后这整段代码都会提到，kotlin自动生成的类里面,类似[SuspendLambdaMain](https://github.com/genius158/CoroutinesTest/blob/master/app/src/main/java/com/yan/ktextest/SuspendLambdaMain.java)

```
class SuspendLambdaMain extends SuspendLambda {
    //1.每执行一个case，label都会被置为下一个case对应的int值
    int label;
    public SuspendLambdaMain(TestContext dispatch) {
        super(null, dispatch);
    }
    //2.协程为我们分装的执行类，把执行和线程切换包装在一起
    private TestContinuation continuation;

    @Override
    void onCreate(TestContinuation testContinuation) {
        this.continuation = testContinuation;
    }

    @Override
    public final Object invokeSuspend(Object result) {
        int i = this.label;
        if (i == 0) {
            // 这里把label置为了1，也就是else if里的执行条件达成
            this.label = 1;
            Log.e("launch withContext ", " 1111111 " + Thread.currentThread());
            
            //3.async的模类
            new SuspendLambdaAsync(continuation, TestDispatchers.IO).intercepted().resumeWith(null);
            //为了方便，这里直接判断挂起状态，实际会根据是否执行完毕，来放回对应的值
            return State.COROUTINE_SUSPENDED;
        } else if (i == 1) {
            int value = (int) result;
            Log.e("launch withContext", "value  " + value + "    " + Thread.currentThread());
        }

        return Unit.INSTANCE;
    }
}

```
1处label是一个标记位，执行invokeSuspend的第一个case(也就是 if(i==0))代码段后，label变为1，当invokeSuspend再
次执行的时候，就会执行第二个case，所以通过label也就实现了我们launch里逐行调用对应的代码
<br/>
<br/>
2处，TestContinuation的作用就是把context（可以理解为线程切换帮助类）和对应的代码执行类结合起来，能够实现逻辑跑在指定的线程
<br/>
<br/>
接下来我们看3处对应生成的类[SuspendLambdaAsync](https://github.com/genius158/CoroutinesTest/blob/master/app/src/main/java/com/yan/ktextest/SuspendLambdaAsync.java)

```
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
                //4.delay的实现
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

```
SuspendLambdaAsync的实现和SuspendLambdaMain没什么大的区别
<br/>
4处也就是一个延时的实现，我们之后再看
<br/>
<br/>
我们先来看看launch原本的位置的代码模拟
```
SuspendLambdaMain(TestDispatchers.MAIN).intercepted().resumeWith(null)
```
重点方法intercepted ，看SuspendLambdaMain的父类
```
public abstract class TestContinuationImp extends TestContinuation<Object> {
    //5.本类的执行者，本类执行结束，会通过completion恢复执行者的执行，也就是回调到之前执行过的invokeSuspend的下一个case
    private final TestContinuation completion;
    //线程切换相关
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
        //6.执行和线程切换组合类
        intercepted = new TestDispatchedContinuation(this, context);
        // 这里回传执行和线程切换组合类
        onCreate(intercepted);
        return intercepted;
    }

    @Override
    public void resumeWith(Object result) {
        Object value;
        //7.判断执行状态，需要挂起直接return
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

```
5处completion，也就是那个调用自己的对象，当自己执行完，completion会继续执行，事例只有成功的情况，当然还有异常和取消
<br/>
6处线程执行和切换类的包装
<br/>
7.判断执行需不需要挂起，挂起直接return
<br/>
<br/>
接下来，我们看看线程切换实现类的模拟[TestDispatchers](https://github.com/genius158/CoroutinesTest/blob/master/app/src/main/java/com/yan/ktextest/TestDispatchers.java)
<br/>重点看一下IO切换实现
```
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
    //8.delay的具体实现
    public void delay(final Runnable task, long delayMillis) {
        defaultExecutor.delayQueue.add(new RunnableTime(new Runnable() {
            @Override
            public void run() {
                dispatch(task);
            }
        }, delayMillis));
    }
}
```
线程切换就没什么好说的，主线程dispatch这是直接通过handle，都是常规操作
<br/>
8处就是延时的实现也就是注释4处具体的实现，延时在不同的环境下，可以有不同的实现，默认都是通过协程的DefaultExecutor实现
<br/>看看Default的模拟类
```
static class DefaultExecutor implements Runnable {
    LinkedBlockingQueue<RunnableTime> delayQueue = new LinkedBlockingQueue<>();
    public DefaultExecutor() {
        new Thread(this, "Default").start();
    }
    @Override
    public void run() {
       //9.循环执行任务
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
```
延时执行就更没什么秘密了，内部维护类似handle的messageQueue的那套东西，当然还有任务添加，执行时序调整等内容我没有去写，
因为这部分本来就可以看到源码，就不做过多解释了
<br/>
9处，是看到目前比较不理解的地方，这是个死循环，协程内部就是个死循环，且这个实现又不像messageQueue，在没有任务的时候，
这个循环仍然以一定时间间隔在跑，打上断点就可以看到。有谁了解，可以提点一下哇~~

### coroutines vs rxjava
从实现上来讲，协程并没什么特别的优势，所谓挂起，也只是让代码能顺序执行，支撑起这些的还是线程、线程切换和回调这些东西。
<br/>
从写法上讲，协程确实带了了全新的体验，但这一切都需要编译器的支持，写的代码和最终的样子差距很大，这一点可能在反破解、混淆上可能有点好处，
不过看源码实现上也会相对的显得吃力点，同时这也是一些人不喜欢用kotlin、Lambda的原因，自己写写还好，但是看别人写的，接手别人的代码，
熟悉起来肯定比不过java。
<br/>
rxajva的Subject，也是比较常用的，且强大的，虽然协程也有channel,但是必须写在协程的作用域，使用体验上和java的Subject还是有不小的差距
<br/>
<br/>
rxjava 链式调用它真的就不香了么？各种操作符就不方便了么？
<br/>
rxjava则是纯代码形式，不存在什么黑魔法，和大多数java库一样，源码阅读上还算是简单明了的。

链式写法，链的太深可能会照成crash不好找，但是
对于它所带来的优势比，这完全可以接受，不然rx相关的库为啥这么多呢。



