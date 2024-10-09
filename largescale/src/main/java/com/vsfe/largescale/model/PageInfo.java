package com.vsfe.largescale.model;

import com.vsfe.largescale.util.C4PageTokenUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;

/**
 * 커서 페이징을 위한 정보
 * @param pageToken // 커서 정보 (다음 호출 시 사용)
 * @param data // 데이터 목록
 * @param hasNext // 다음 페이지 존재 여부
 * @param <T>
 */
public record PageInfo<T>(
        String pageToken, // pageToken을 String으로 박은 게 핵심!
        List<T> data,
        boolean hasNext
) {
    // size 20 이면 -> hasNext를 어떻게 판단할 수 있을까?
    // 1개 더 가져오면 됨

    public static <T> PageInfo<T> of(
            List<T> data,
            int expectedSize,
            Function<T, Object> firstPageTokenFunction, // input : T , output : 무언가  // firstPageTokenFunction에 getTransactionId를 가져올 것이다.
            Function<T, Object> secondPageTokenFunction
    ) {
        if (data.size() <= expectedSize) { // 1개 더 가져오기 때문에, expectedSize와 같아도 다음 값을 주지 않음
            return new PageInfo<>(null, data, false);
        }

        var lastValue = data.get(expectedSize - 1);
        var pageToken = C4PageTokenUtil.encodePageToken(Pair.of(
            firstPageTokenFunction.apply(lastValue),
                secondPageTokenFunction.apply(lastValue)
        ));

        return new PageInfo<>(pageToken, data.subList(0, expectedSize), true);
    }
}
