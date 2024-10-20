package com.vsfe.largescale.util;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class C4QueryExecuteTemplate {
    // 동작같은 것을 템플릿화 시켜서 핵심로직만 바꿔치기하고, 기타 등등은 감싸서 작업해줄 것이다.

    /**
     * Cursor 페이징을 적용하여 select를 수행한 후, 조회된 결과로 비즈니스 로직을 수행한다.
     * @param limit
     * @param selectFunction
     * @param resultConsumer
     * @param <T>
     */
    public static <T> void selectAndExecuteWithCursor(int limit, Function<T, List<T>> selectFunction, Consumer<List<T>> resultConsumer) {
        List<T> resultList = null;
        do {
            resultList = selectFunction.apply(resultList != null ? resultList.get(resultList.size() - 1) : null); // resultList가 비어있지 않으면 맨 마지막 값을 가져옴
            if (!resultList.isEmpty()) {
                resultConsumer.accept(resultList);
            }
        } while (resultList.size() >= limit);
    }

    /**
     * cursor 페이징을 적용하여 select를 수행한 후, 조회된 결과로 비즈니스 로직을 수행한다.
     * 단, iteration 횟수가 pageLimit에 도달한 경우 검색 결과와 상관 없이 중단한다.
     * @param pageLimit // 실행 횟수 제한 (음수인 경우, 제한 없음)
     * @param limit
     * @param selectFunction
     * @param resultConsumer
     * @param <T>
     */
    public static <T> void selectAndExecuteWithCursorAndPageLimit(int pageLimit, int limit, Function<T, List<T>> selectFunction, Consumer<List<T>> resultConsumer) {
        if (pageLimit < 0) {
            selectAndExecuteWithCursor(limit, selectFunction, resultConsumer);
            return;
        }

        var iterationCount = 0;
        List<T> resultList = null;
        do {
            resultList = selectFunction.apply(resultList != null ? resultList.get(resultList.size() - 1) : null); // resultList가 비어있지 않으면 맨 마지막 값을 가져옴
            if (!resultList.isEmpty()) {
                resultConsumer.accept(resultList);
            }
        } while (resultList.size() >= limit);
    }
}
