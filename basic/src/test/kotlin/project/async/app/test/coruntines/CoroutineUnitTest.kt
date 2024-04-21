package project.async.app.test.coruntines

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.slf4j.LoggerFactory
import project.async.app.external.FetchService
import java.lang.System.currentTimeMillis
import java.lang.Thread.currentThread
import java.lang.Thread.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.allOf
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.cancellation.CancellationException
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger(CoroutineUnitTest::class.java)

class CoroutineUnitTest {

    private lateinit var fetchService: FetchService

    @BeforeEach
    fun setUp() {
        fetchService = FetchService()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    @DisplayName("Coroutine에서 delay를 통해 시간을 지연할 수 있다.")
    fun testCoroutineDelay() = runTest {
        val startTime = currentTime
        val job = launch {
            delay(ONE_SECONDS)
            assertEquals(ONE_SECONDS, currentTime - startTime)
        }

        job.join()
        assertEquals(ONE_SECONDS, currentTime - startTime)
    }

    @Test
    @DisplayName("await 키워드를 통해 비동기 연산을 할 수 있다.")
    fun asyncSumTest() = runTest {
        val deferred = async {
            delay(ONE_SECONDS)
            return@async 5 + 5
        }

        val result = deferred.await()
        assertEquals(10, result)
    }

    @Test
    @DisplayName("비동기 연산은 순서가 바뀔 수 있다.")
    fun asyncOrderTest() = runTest {
        launch {
            delay(ONE_SECONDS)
            log.info("B")
        }
        log.info("A")
    }

    @Test
    @DisplayName("비동기 연산을 두 개 이상 실행해서 결과를 알 수 있다.")
    fun multiAsyncTest() = runTest {
        val simpleHtmlPage = async {
            fetchService.fetchSimpleHtmlPage()
        }
        val thymeleafPage = async {
            fetchService.fetchThymeLeafPage()
        }

        val result = simpleHtmlPage.await() != null && thymeleafPage.await() != null

        assertTrue(result)
    }

    @Test
    @DisplayName("CompletableFuture를 사용해 데이터를 비동기로 받아올 수 있다.")
    fun completableFutureAsyncTest() = runTest {
        val futures = mutableListOf<CompletableFuture<String>>()

        repeat(100) {
            val future = CompletableFuture.supplyAsync {
                fetchService.fetchThymeLeafPage()!!
            }
            futures.add(future)
        }

        allOf(*futures.toTypedArray()).join()

        val results = futures.map { it.get() }
        assertTrue(results.size == 100)
    }

    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    @DisplayName("Coroutine을 사용해 데이터를 비동기로 받아온 후, 한 번에 결과를 받을 수 있다.")
    fun coroutineWithSuspendTest() = runTest {
        val deferred = List(1_000) {
            async {
                fetchService.fetchThymeLeafPageWithSuspend()
            }
        }

        val results = deferred.awaitAll()

        assertTrue(results.size == 1_000)
    }

    /**
     * awaitAll이 있기 때문에 확실한 병렬 처리는 아니다.
     * */
    @Test
    @Timeout(value = 100, unit = TimeUnit.SECONDS)
    @DisplayName("Coroutine을 사용해 데이터를 비동기로, 병렬처럼 가져올 수 있다.")
    fun coroutineAsyncParallelTest() = runTest {
        val start = currentTimeMillis()

        val simpleHtmlPageDeferred = List(100) {
            async {
                fetchService.fetchSimpleHtmlPageWithSuspend()
            }
        }

        val simpleHtmlPageResult = simpleHtmlPageDeferred.awaitAll()

        val elapsedTimeInSeconds = (currentTimeMillis() - start) / 1_000.0
        log.info("elapsedTime:$elapsedTimeInSeconds")
        assertEquals(100, simpleHtmlPageResult.size)
    }

    @Test
    @DisplayName("Coroutine 테스트에서 Mock을 사용할 수 있다.")
    fun coroutineMockTest() = runTest {
        val fetchService = mockk<FetchService>()

        coEvery { fetchService.fetchSimpleHtmlPageWithSuspend() } returns "Simple Html"

        val result = fetchService.fetchSimpleHtmlPageWithSuspend()

        assertEquals("Simple Html", result)
    }

    @Test
    @DisplayName("Coroutine에서 스코프를 사용할 수 있다. 순서는 스코프 순서를 지킨다.")
    fun coroutineScopeTest() = runTest {
        val fetchService = mockk<FetchService>()
        coEvery { fetchService.fetchSimpleHtmlPageWithSuspend() } returns "Simple Html"
        coEvery { fetchService.fetchThymeLeafPageWithSuspend() } returns "ThymeLeaf Html"

        launch {
            log.info(fetchService.fetchThymeLeafPageWithSuspend())
            coroutineScope {
                launch {
                    log.info(fetchService.fetchSimpleHtmlPageWithSuspend())
                }
            }
        }
    }

    @Test
    @DisplayName("Thread vs Coroutine 속도 비교.")
    fun drawDotWithTreadTest() {
        drawDotWithThread()
        sleep(3_000)
    }

    @Test
    @DisplayName("Thread vs Coroutine 속도 비교.")
    fun drawDotWithCoroutineTest(): Unit = runBlocking {
        drawDotWithCoroutine()
        delay(3_000)
    }

    @Test
    @Disabled
    @DisplayName("실행중인 프로세스가 종료되면 Coroutine도 함께 종료된다.")
    fun coroutineForceFinishedWithProcessExit(): Unit = runBlocking {
        launch {
            repeat(100) { i ->
                log.info("$i")
                delay(10)
            }
        }

        log.info("Exit.")
        delay(100)
        exitProcess(1)
    }

    @Test
    @DisplayName("Coroutine을 사용할 때, timeout을 설정할 수 있다.")
    fun coroutineTestWithTimeout(): Unit = runBlocking {
        try {
            withTimeout(500L) {
                launch {
                    repeat(100) { i ->
                        log.info("$i")
                        delay(100L)
                    }
                }
            }
        } catch (ex: TimeoutCancellationException) {
            log.error("Timeout.")
        }
    }

    @Test
    @DisplayName("timeout을 설정하면 시간이 지나면 작업이 취소된다.")
    fun testCoroutineCancellation() = runBlocking {
        val job = launch {
            try {
                repeat(1_000) { i ->
                    log.info("Job:$i ------x>")
                    delay(500L)
                }
            } catch (ex: CancellationException) {
                log.info("Job cancelled.")
            }
        }

        delay(1_000L)
        job.cancelAndJoin()
        log.info("Job cancelled.")
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testCoroutineWithRunTest() = runTest {
        val job = launch {
            repeat(1_000) { i ->
                log.info("Job:$i ------x>")
                delay(500L)
            }
        }

        advanceTimeBy(1_000L)
        job.cancelAndJoin()
        log.info("Job cancelled and joined.")
    }

    @Test
    @DisplayName("코루틴 취소 시 NonCancellable이 실행된다.")
    fun coroutineJobCancelWithContextTest() = runBlocking {
        val job = launch {
            try {
                repeat(10) { i ->
                    log.info("Job:$i ------x>")
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) {
                    delay(1_000L)
                    log.info("Job canceled.")
                }
            }
        }

        delay(1_000L)
        job.cancelAndJoin()
    }

    @Test
    @DisplayName("withTimeoutOrNull을 사용하면, 작업 시간이 초과되면 Null을 반환한다.")
    fun withTimeoutOrNullTest() = runBlocking {
        val result = withTimeoutOrNull(1_000L) {
            repeat(10) { i ->
                log.info("Job:$i ------x>")
                delay(300L)
            }
            "Finished."
        }

        assertNull(result)
    }

    @Test
    @DisplayName("Coroutine은 하나가 실패하면 나머지에도 영향을 미친다.")
    fun coroutineExceptionTest(): Unit = runBlocking {
        val deferred1 = async {
            try {
                delay(1_000)
                throw IllegalArgumentException("Exception")
            } catch (ex: IllegalArgumentException) {
                null
            }
        }

        val deferred2 = async {
            delay(500)
            "Success"
        }

        deferred2.await()
        assertNull(deferred1.await())
    }

    @Test
    @OptIn(DelicateCoroutinesApi::class)
    @DisplayName("부모 Coroutine을 취소하면 자식 Coroutine도 함께 취소된다.")
    fun parentCoroutineCancelTest() = runBlocking {
        val parentJob = GlobalScope.launch { parentCoroutine() }
        delay(3_000)
        parentJob.cancel()
    }

    @Test
    @DisplayName("자식 Coroutine만 취소하면, 부모 Coroutine은 계속해서 실행된다.")
    fun childCoroutineCancelTest() = runBlocking {
        parentCoroutineWithCancel()
    }

    @Test
    @DisplayName("Coroutine이 다른 Dispatchers에서 실행되면, 다른 쓰레드에서 실행된다.")
    fun coroutineThreadTest() = runTest {
        launch(Dispatchers.Default) {
            log.info("Coroutine 1: ${currentThread().id}")
            delay(1_000)
        }
        launch(Dispatchers.Default) {
            log.info("Coroutine 2: ${currentThread().id}")
            delay(1_000)
        }
        log.info("Main test coroutine: ${currentThread().id}")
    }

    companion object {
        private const val ONE_SECONDS = 1_000L
    }
}

fun main() = runBlocking {
    launchA()
    launchB()

    delay(500L)
    log.info("Hello World!")
}

private suspend fun launchA() = coroutineScope {
    launch {
        log.info("launch A Start")
        delay(1_000L)
        log.info("launch A End")
    }
}

private suspend fun launchB() = coroutineScope {
    launch {
        log.info("launch B Start")
        delay(1_000L)
        log.info("launch B End")
    }
}

fun drawDotWithThread() = runBlocking {
    thread {
        repeat(100_000) {
            log.info("$it .")
        }
    }
}

fun drawDotWithCoroutine() = runBlocking {
    launch {
        repeat(100_000) {
            log.info("$it .")
        }
    }
}

suspend fun childCoroutine() {
    try {
        log.info("[Child] Start.")
        delay(3_000L)
        log.info("[Child] Finished.")
    } catch (ex: CancellationException) {
        log.error("[Child] Cancelled.")
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun parentCoroutine() {
    try {
        log.info("[Parent] Start.")
        val childJob = GlobalScope.launch { childCoroutine() }
        delay(1_000)
        childJob.cancel()
        delay(1_000)
        log.info("[Parent] Finished.")
    } catch (ex: CancellationException) {
        log.error("[Parent] Cancelled")
    }
}

suspend fun childCoroutineWithCancel() {
    try {
        log.info("[Child] Start.")
        delay(3_000)
        log.info("[Child] Finished.")
    } catch (ex: CancellationException) {
        log.error("[Child] Cancelled.")
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun parentCoroutineWithCancel() {
    try {
        log.info("[Parent] Start.")
        val childJob = GlobalScope.launch { childCoroutineWithCancel() }
        delay(2_000L)
        childJob.cancel()
        delay(3_000L)
        log.info("[Parent] Finished.")
    } catch (ex: CancellationException) {
        log.error("[Parent] Cancelled.")
    }
}
