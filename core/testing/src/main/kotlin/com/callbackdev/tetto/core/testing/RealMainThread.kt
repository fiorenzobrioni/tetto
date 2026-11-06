package com.callbackdev.tetto.core.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.concurrent.Executors

/**
 * `Dispatchers.Main` on a real thread of its own, for a ViewModel's test on real threads (a real
 * database, real files, real time), where a test scheduler would not do: [start] it in `@Before`,
 * and [stop] it first thing in `@After`, before the database is closed, with every ViewModel the
 * test made.
 *
 * A ViewModel's flows outlive the test's last check: a write invalidates Room's tables, the flow
 * the ViewModel shares (`stateIn`, kept alive a while after its last collector) reads them again
 * on Room's threads and comes back to Main to emit. Main reset at that moment fails the test,
 * "Dispatchers.Main is used concurrently with setting it", however right everything it checked
 * was. So [stop] cancels each ViewModel's work and waits for it to end, Main still in place, and
 * only then resets Main: nothing is left to come back to it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RealMainThread {
    private val thread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    fun start() {
        Dispatchers.setMain(thread)
    }

    fun stop(vararg viewModels: ViewModel) {
        runBlocking {
            viewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        }
        Dispatchers.resetMain()
        thread.close()
    }
}
