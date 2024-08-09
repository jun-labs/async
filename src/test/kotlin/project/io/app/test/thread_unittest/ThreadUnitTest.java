package project.io.app.test.thread_unittest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("[UnitTest] Thread 단위 테스트")
class ThreadUnitTest {

    private static final Logger log = LoggerFactory.getLogger(ThreadUnitTest.class);

    @Test
    @DisplayName("쓰레드가 Interrupt 당하면 상태가 변한다.")
    void whenInterruptedThenThreadStatusShouldBeChanged() {
        final MyThread thread = new MyThread();
        thread.start();
        thread.interrupt();
        assertTrue(thread.isInterrupted());
    }

    @Test
    @DisplayName("쓰레드가 작업을 양보하며, 작업시간이 더 오래걸릴 수 있다.")
    void whenYieldCalledThenThreadShouldYield() throws InterruptedException {
        final YieldingThread yieldingThread = new YieldingThread();
        final BusyThread busyThread = new BusyThread();

        final long yieldStartTime = System.nanoTime();
        yieldingThread.start();
        yieldingThread.join();
        final long yieldEndTime = System.nanoTime();

        final long busyStartTime = System.nanoTime();
        busyThread.start();
        busyThread.join();
        final long busyEndTime = System.nanoTime();

        assertTrue((yieldEndTime - yieldStartTime) > busyEndTime - busyStartTime);
    }

    static class MyThread extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    log.info("Run.");
                    sleep(1_000);
                } catch (InterruptedException ex) {
                    System.out.println("Interrupted.");
                    break;
                }
            }
        }
    }

    static class YieldingThread extends Thread {
        private int executionCount = 0;

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                executionCount++;
                log.info("YieldingThread execution count: {}", executionCount);
                Thread.yield();
            }
        }
    }

    static class BusyThread extends Thread {
        private int executionCount = 0;

        @Override
        public void run() {
            for (int i = 0; i < 100; i++) {
                executionCount++;
                log.info("BusyThread execution count: {}", executionCount);
            }
        }
    }
}
