package com.yan.ktextest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class MainActivity : AppCompatActivity() {

//    @InternalCoroutinesApi
//    class Dispatch : MainCoroutineDispatcher(), Delay {
//        override val immediate: MainCoroutineDispatcher
//            get() = this
//
//        override fun dispatch(context: CoroutineContext, block: Runnable) {
//            Schedulers.io().scheduleDirect(block)
//        }
//
//        override fun scheduleResumeAfterDelay(
//            timeMillis: Long,
//            continuation: CancellableContinuation<Unit>
//        ) {
//            val block = Runnable {
//                with(continuation) { resumeUndispatched(Unit) }
//            }
//            Schedulers.io().scheduleDirect(block, timeMillis, TimeUnit.MILLISECONDS)
//        }
//
//    }

    @InternalCoroutinesApi
       override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        SuspendLambdaMain(TestDispatchers.MAIN).intercepted().resumeWith(null)

        GlobalScope.launch(Dispatchers.Main) {
            Log.e("launch withContext ", " 1111111 " + Thread.currentThread())
            val intValue = async(context = Dispatchers.IO) {
                delay(3000)
                Log.e("launch withContext ", " 22222222 " + Thread.currentThread())
                1
            }
            val value = intValue.await()
            Log.e("launch withContext", "value  " + value + "    " + Thread.currentThread())
        }
    }
}
