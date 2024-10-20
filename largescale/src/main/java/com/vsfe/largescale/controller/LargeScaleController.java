package com.vsfe.largescale.controller;

import com.vsfe.largescale.domain.Transaction;
import com.vsfe.largescale.domain.User;
import com.vsfe.largescale.model.PageInfo;
import com.vsfe.largescale.model.type.TransactionSearchOption;
import com.vsfe.largescale.service.LargeScaleService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 예시 중심의 세미나이므로,
 * 실제 서비스와는 달리 사용하는 API만 Controller 형태로 구성했습니다.
 */
@RestController
@RequestMapping("/service")
@RequiredArgsConstructor
public class LargeScaleController {
    private final LargeScaleService largeScaleService;

    /**
     * Step 1. 기본적인 쿼리의 최적화를 수행해 봅시다.
     */
    @GetMapping("/user-info")
    public List<User> getUserInfo(@RequestParam @Positive @Max(100) int count) { // @Positive : 값을 양수로 제한함
        // 원래는 엔티티 직접 주면 안 됨!
        return largeScaleService.getUserInfo(count);
    }

    /**
     * Step 2. 페이징을 활용한 쿼리 최적화 방식에 대해 고민해 봅시다.
     */
    @GetMapping("/get-transactions")
    public PageInfo<Transaction> getTransactions(
            @RequestParam @NotEmpty String accountNumber,
            @RequestParam(required = false) String pageToken,
            // 커서는 첫번째 요청에는 존재할 수 없음 -> null
            // 따라서 (required = false) 로 설정함
            @RequestParam @NotNull TransactionSearchOption option,
            @RequestParam @Positive @Max(100) int count

            ) {
        // 거래 내역을 가져오자.
        // 특정 사용자에 대한 거래 내역을 들고와야 함
        // 근데 account 테이블은 2000만건임
        // 따라서 step1 user(500만건)보다 더 세세하게 쿼리를 짜야 한다!
        // 페이징 방식 : 오프셋 / 커서
        // 스프링에서 페이징하면 Pageable, Page 이런 것을 쓰는데,
        // 이게 실은 위험함. 스프링에서 제공하는 페이징은 오프셋방식임.
        // 오프셋 방식 : 특정 위치부터, 몇 개를 들고 오겠다
        // 커서 방식  이 값 이후에 몇 개를 들고 오겠다
        // 아니 일반적인 경우는 다 offset 쓰는 것 아님?
        // 데이터를 물리적으로 분산하거나, 앞의 조건에 의해 필터링되는 값이 많지 않은 경우 (ex : 네이버카페)
        // 네이버 카페는 글이 많을 수 있어도 카페, 게시판 별로는 게시글이 적은 편임.
        // 일반적인 커뮤니티의 경우에는 초반 몇 페이지만 정상적으로 보여주고
        // 이후에는 강제적으로 커서 페이징으로 전환됨
        // 따라서 우리도 커서 페이징으로 할 것이다.

        // 트랜잭션 테이블을 보면 pk와 transaction_Date가 순서가 동일하지 않음.
        // 일반적으로 pk가 순서를 보장해주면 안 되는 경우들이 있다.
        // 그리고 특정 계좌에 대해 조회하고 싶은데, sender_Account에 있을 수도 있고, receiver_account에 있을 수도 있음
        // 컬럼 or 컬럼 쿼리로 해야 하는데 그러면 인덱스를 타지 않음

        // 우리는 커서 기반 페이징을 할 것이다.
        // 우리는 Spring 기반의 페이징을 사용할 수 없음 -> 내부적으로 offset을 쓰고 있음


        // 커서는 첫번째 요청에는 존재할 수 없음 -> null
        return largeScaleService.getTransactions(accountNumber, pageToken, option, count);
    }

    /**
     * Step 3. Full Scan 을 수행해야 하는 로직은 어떻게 수행해야 할까요?
     */
    @GetMapping("/validate-account")
    public void validateAccountNumber(@RequestParam int pageSize) { // pageSize가 음수면 모든 데이터를 다 풀스캔하도록 진행할 것이다.
        // Full Scan : 테이블 전체 조회
        // Account 테이블(계좌번호)의 account_number는 검증 로직이 포함되어 있는데,
        // 계좌를 하나하나 찾아서 잘못된 계좌번호를 찾는 작업을 진행해볼 것이다.
        // 2000만개를 네트워크 트래픽 다 들고와서 검증하는 게 가능할까? -> 할 짓이 아님.

        largeScaleService.validateAccountNumber(pageSize);
    }

    /**
     * Step 4. 병렬 처리를 사용한 마이그레이션 작업 수행
     */
    @GetMapping("/migrate-data")
    public void migrateData(@RequestParam int pageSize) {
        // 테이블의 데이터를 2개의 테이블로 나누어줌 -> 샤딩
        // 샤딩을 하면, 내 데이터가 0번, 1번에 있는지 알 길이 없긴 함
        // -> 따라서 user 테이블에 group_id 컬럼을 추가해두셨는데, 여기에 0 or 1 이 있어서,
        // 해당 번호대로 찾아가서 테이블을 뒤져보게 할 수 있음.

        // 우리의 목표 : 데이터를 진짜로 분산시켜야 한다.
        largeScaleService.migrateData(pageSize);
    }

    /**
     * Step 5. 데이터를 샤딩한다면 어떻게 될까요?
     */
    public void aggregateTransactionsWithSharding() {

    }
}
