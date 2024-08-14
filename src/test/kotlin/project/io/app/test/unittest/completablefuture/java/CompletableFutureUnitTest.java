package project.io.app.test.unittest.completablefuture.java;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.anyOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import project.io.app.external.FetchService;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@DisplayName("[UnitTest] CompletableFuture 단위 테스트")
class CompletableFutureUnitTest {

    private static final Logger log = LoggerFactory.getLogger(CompletableFutureUnitTest.class);

    private FetchService fetchService;

    @BeforeEach
    void setUp() {
        fetchService = new FetchService();
    }

    @Test
    @DisplayName("CompletedFuture를 사용해 비동기로 결과를 받아올 수 있다.")
    void whenUseCompletableFutureWithAsyncThenResultShouldBeNotNull() {
        completedFuture(fetchService.fetchThymeLeafPage())
            .thenApply(result -> {
                assertNotNull(result);
                return result;
            });
    }

    @Test
    @DisplayName("allOf를 사용하면 모든 결과를 기다린다.")
    void whenAllOfThenAllResultShouldBeWaited() {
        final CompletableFuture<String> future1 = supplyAsync(() -> "Task 1 result");
        final CompletableFuture<String> future2 = supplyAsync(() -> "Task 2 result");
        final CompletableFuture<String> future3 = supplyAsync(() -> "Task 3 result");

        final CompletableFuture<Void> combinedFuture = allOf(future1, future2, future3);
        combinedFuture.join();

        assertTrue(future1.isDone());
        assertTrue(future2.isDone());
        assertTrue(future3.isDone());
    }

    @Test
    @DisplayName("CompletableFuture anyOf를 사용하여 첫 번째로 완료된 작업의 결과를 얻을 수 있다.")
    void whenAnyOfThenReturnsFirstCompletedFuture() {
        final CompletableFuture<String> future1 = supplyAsync(() -> {
            try {
                Thread.sleep(3_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Task 1 result";
        });

        final CompletableFuture<String> future2 = supplyAsync(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Task 2 result";
        });

        final CompletableFuture<String> future3 = supplyAsync(() -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Task 3 result";
        });

        final CompletableFuture<Object> anyOfFuture = anyOf(future1, future2, future3);
        final String result = (String) anyOfFuture.join();

        assertEquals("Task 2 result", result);
    }

    @Test
    @DisplayName("supplyAsync를 사용할 경우, join을 통해 결과를 받아온다.")
    void whenSupplyAsyncThenResultShouldBeFetchedByJoin() {
        final CompletableFuture<String> resultFuture = supplyAsync(() -> fetchService.fetchThymeLeafPage())
            .thenApply(data -> {
                assertNotNull(data);
                return data;
            });

        assertNotNull(resultFuture.join());
    }

    @Test
    @DisplayName("호출 결과를 받아오면, join 메서드를 여러 번 호출해도, 실제 호출 횟수는 오르지 않는다.")
    void whenFetchFirstResultThenAfterJoinShouldNotBeApplied() {
        final FetchService spy = Mockito.spy(fetchService);
        given(spy.fetchThymeLeafPage()).willReturn("Hello World");

        final CompletableFuture<String> completableFuture = supplyAsync(spy::fetchThymeLeafPage);
        final CompletableFuture<String> result = completableFuture.thenApply(value -> value);
        for (int index = 0; index < 100_000; index++) {
            result.join();
        }

        verify(spy, Mockito.times(1)).fetchThymeLeafPage();
    }

    @Test
    @DisplayName("호출 결과를 매 번 새로 생성하면, join 메서드를 호출한 만큼 호출 횟수가 오른다.")
    void whenFetchFirstResultButUseNewResultThenJoinShouldBeApplied() {
        final FetchService spy = Mockito.spy(fetchService);
        given(spy.fetchThymeLeafPage()).willReturn("Hello World");

        for (int index = 0; index < 100; index++) {
            CompletableFuture<String> completableFuture = supplyAsync(spy::fetchThymeLeafPage);
            completableFuture.thenApply(value -> value);
        }

        verify(spy, Mockito.atLeast(1)).fetchThymeLeafPage();
    }

    @Test
    @DisplayName("join을 사용하면 ExecutionException 또는 InterruptedException이 발생한다.")
    void whenJoinExecutionExceptionOrInterruptedExceptionShouldBeHappen() throws ExecutionException, InterruptedException {
        final CompletableFuture<String> failedFuture = supplyAsync(() -> {
            throw new RuntimeException("Exception");
        });

        try {
            failedFuture.join();
        } catch (CompletionException ex) {
            log.error("ex:{}", ex.getMessage());
        }

        try {
            failedFuture.get();
        } catch (ExecutionException | InterruptedException ex) {
            log.error("ex:{}", ex.getMessage());
        }

        final CompletableFuture<String> timeoutFuture = supplyAsync(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Result";
        });

        try {
            timeoutFuture.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            log.error("ex:{}", ex.toString());
        }
    }

    @Test
    @DisplayName("get을 사용하면 ExecutionException 또는 InterruptedException이 발생한다.")
    void whenGetExecutionExceptionOrInterruptedExceptionShouldBeHappen() throws ExecutionException, InterruptedException {
        final CompletableFuture<String> failedFuture = supplyAsync(() -> {
            throw new RuntimeException("Exception");
        });

        try {
            failedFuture.join();
        } catch (CompletionException ex) {
            log.error("ex:{}", ex.getMessage());
        }

        try {
            failedFuture.get();
        } catch (ExecutionException | InterruptedException ex) {
            log.error("ex:{}", ex.getMessage());
        }

        final CompletableFuture<String> timeoutFuture = supplyAsync(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Result";
        });

        try {
            timeoutFuture.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            log.error("ex:{}", ex.toString());
        }
    }

    @Test
    @DisplayName("exceptionally 메서드를 통해, 예외를 컨트롤할 수 있다.")
    void whenExceptionallyThenCanControlException() {
        final CompletableFuture<String> future = supplyAsync(() -> {
            fetchService.throwException();
            return "Expected result";
        }).exceptionally(ex -> "Exception");

        String result = future.join();
        assertEquals("Exception", result);
    }

    @Test
    @DisplayName("handle 메서드를 통해, 예외를 컨트롤할 수 있다.")
    void whenHandleThenCanControlException() {
        final CompletableFuture<String> result = supplyAsync(() -> {
            fetchService.throwException();
            return "Expected";
        }).handle((res, ex) -> {
            if (ex != null) {
                return "Exception";
            }
            return res;
        });

        assertEquals("Exception", result.join());
    }

    @Test
    @DisplayName("CompletableFuture 연산에서 handle과 exceptionally를 동시에 사용할 수 있다.")
    void whenUseBothHandleAndExceptionallyThenResultShouldBeControlled() {
        final CompletableFuture<String> futureResult = supplyAsync((Supplier<String>) () -> {
            try {
                fetchService.throwException();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
            return null;
        }).handle((res, ex) -> {
                if (ex != null) {
                    return "Exception";
                }
                return res;
            })
            .exceptionally(ex -> {
                log.error("exception:{}", ex.getMessage());
                return null;
            });

        final String result = futureResult.join();
        assertEquals("Exception", result);
    }

    @Test
    @DisplayName("whenComplete를 사용하면 예외를 처리할 수 있다.")
    void whenCompleteShouldHandleResultOrException() {
        final CompletableFuture<String> failedFuture = supplyAsync(() -> {
            throw new RuntimeException("Failure");
        });

        final CompletableFuture<String> resultFutureWithException = failedFuture.whenComplete((res, exception) -> {
            assertNotNull(exception, "Exception should not be null");
            assertEquals("Failure", exception.getMessage());
        });

        assertThrows(CompletionException.class, resultFutureWithException::join);
    }

    @Test
    @DisplayName("CompletableFuture에서 발생한 예외는 CompletionException의 자손이다.")
    void whenCompletableFutureExceptionThenShouldBeInstanceOfCompletionException() {
        final CompletableFuture<String> future = supplyAsync(() -> {
            throw new RuntimeException("Test Exception");
        });

        assertThrows(CompletionException.class, future::join);
    }
}

