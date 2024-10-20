package com.vsfe.largescale.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 마이그레이션 처리를 위한 ThreadPoolExecutor
 * 기본 제공 ThreadPoolExecutor가 충족하지 못하는 아래 조건들을 위해 커스텀을 수행함.
 * - 모든 작업이 완료될 때까지 대기할 수 있어야 한다.
 * - 예외 발생 시, 중간에 작업이 중단되지 않고 로깅으로 처리가 가능해야 한다. (후처리를 위함)
 * - 모든 작업이 수행된 후, Hold한 예외를 throw한다.
 */
@RequiredArgsConstructor
@Slf4j
public class C4ThreadPoolExecutor implements Executor {

    private final int threadCount;
    private final int queueSize; // linkedListBlockingQueue를 쓰기 때문에 사실 큐 사이즈는 큰 의미 없음
    private ThreadPoolExecutor threadPoolExecutor; // 내부적으로 포함하도록 진행함 (상속 x)
    private RuntimeException exception = null;

    public void init() {
        // 쓰레드풀은 초기화가 필요함

        // 쓰레드풀은 무거움.
        // 따라서 최대한 늦게 init하는 게 필요함

        if (threadPoolExecutor != null) {
            return; // 이미 호출한 적이 있으면 리턴
        }

        // queueSize를 설정한 이유?
        // 처음에 메모리를 그만큼 미리 확보해줌
        // 그 이상은 추가로 더 할당해줌
        // 근데 미미한 차이이긴 함

        threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueSize),
                ((r, executor) -> {
                    try {
                        executor.getQueue().put(r); // 어떻게든 추가해줌!
                    } catch (InterruptedException e) {
                        log.error(e.toString(), e);
                        Thread.currentThread().interrupt();
                    }
                } )
                );
    }

    @Override
    public void execute(Runnable command) {
        if (isInvalidState()) {
            return;
        }

        threadPoolExecutor.execute(() -> {
            try {
                command.run();
            } catch (RuntimeException e) {
                log.error(e.toString(), e);
                exception = e;
            }
        });
    }

    public void waitToEnd() {
        if (isInvalidState()) {
            return;
        }

        threadPoolExecutor.shutdown();
        while(true) {
            try {
                if (threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                log.error(e.toString(), e);
                Thread.currentThread().interrupt();
            }
        }

        threadPoolExecutor = null; // 너의 운명은 끝났다.

        if (exception != null) {
            throw exception;
        }
    }

    private boolean isInvalidState() {
        return threadPoolExecutor == null || threadPoolExecutor.isTerminating() || threadPoolExecutor.isTerminated();
    }
}
