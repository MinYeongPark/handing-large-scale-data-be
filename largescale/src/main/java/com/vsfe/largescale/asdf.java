package com.vsfe.largescale;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.vsfe.largescale.util.C4StringUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class asdf {
    public static AtomicInteger data = new AtomicInteger(); // 하드웨어 도움을 통한 원자적 연산 -> 해당 연산을 할 때 다른 연산이 끼어들지 못하도록 하는 것
    private static Random random = new Random(); // Random은 패턴화가 가능해서, 시큐어랜덤 같은 것을 써서 확실하게 랜덤을 가져와야 함!!

    public static void main(String[] args) {
        // thread pool
        var threadCount = 10;
        var queueSize = 100;
        var threadPoolExecutor = new ThreadPoolExecutor(threadCount, queueSize,
                0L, // KeepAliveTime : 시간을 얼마나 할 것인지
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
                // LinkedBlockingQueue는 내부적으로 linkedList를 쓰고 있어서, 데이터가 무한으로 쌓일 수 있음
                // 따라서 모든 요청을 전부 수행할 수 있는 threadPool !
        );
        var startTime = Instant.now();

        for (int i = 0; i < 10001; i++) {
            int aa = i;
            threadPoolExecutor.execute(() -> {
                // Runnable 타입 들어가는데, runnable : 아무것도 안 받고 아무것도 리턴을 안 하는 함수형 인터페이스
                System.out.println(C4StringUtil.format("Hello from {}", aa));
                try {
                    Thread.sleep(random.nextInt(10)); // 랜덤으로 지연되게 함
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                add(aa);
            });
        }

        threadPoolExecutor.shutdown(); // 쓰레드풀 종료 상태로 바꿔야 끝남

        while (true) {
            try {
                if (threadPoolExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException e) {
                System.out.println("으어");
            }
        }

        System.out.println(C4StringUtil.format("total - {}", data));
        System.out.println(C4StringUtil.format("total time - {}", ChronoUnit.MILLIS.between(startTime, Instant.now())));
    }

    public static void add(int i) {
        // synchronized : 뮤텍스 락을 획득하고 이 안에 들어옴
        // 근데 락 을 걸면 조금 성능이 안 좋아질 수 있음
        data.addAndGet(1);
    }
}
