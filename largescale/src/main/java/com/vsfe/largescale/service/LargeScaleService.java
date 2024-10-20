package com.vsfe.largescale.service;

import com.vsfe.largescale.core.C4ThreadPoolExecutor;
import com.vsfe.largescale.domain.Account;
import com.vsfe.largescale.domain.Transaction;
import com.vsfe.largescale.domain.User;
import com.vsfe.largescale.model.PageInfo;
import com.vsfe.largescale.model.type.TransactionSearchOption;
import com.vsfe.largescale.repository.*;
import com.vsfe.largescale.util.C4QueryExecuteTemplate;
import com.vsfe.largescale.util.C4StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeScaleService {
    public static final int LIMIT_SIZE = 1000;

    /**
     * 일반적으로 ThreadPool을 Bean 으로 선언해서 사용하는 편인데, (요청이 들어올 때 마다 스레드풀이 생성되는 것을 방지하기 위함)
     * 학습 목적이므로 여기서는 그런 과정을 수행하지 않음.
     * CompletableFuture 에 대해 아시면 다른 방식으로도 가능합니다. (default ForkJoinPool 을 사용한 처리 가능)
     */
    private final C4ThreadPoolExecutor threadPoolExecutor = new C4ThreadPoolExecutor(8, 32);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<User> getUserInfo(int count) {
        return userRepository.findRecentCreatedUsers(count);
    }

    /**
     * 거래 내역을 가져오자
     * @param accountNumber
     * @param pageToken
     * @param option
     * @param size
     */
    public PageInfo<Transaction> getTransactions(String accountNumber, String pageToken, TransactionSearchOption option, int size) {
        if (pageToken == null) {
            return transactionRepository.findTransactionWithoutPageToken(accountNumber, option, size);
        } else {
            return transactionRepository.findTransactionWithPageToken(accountNumber, pageToken, option, size);
        }
    }

    public void validateAccountNumber(int pageSize) {
        // 다 들고 오는 건 무리 -> 쪼개서 들고오자
        // 1000건 단위로 들고 온다고 치면.. -> 2천만개 -> 2만번 쿼리를 때려서 부정합 데이터를 확인해주면 되겠다.
        // (병렬처리는 다음 스텝에서 진행)
        // 개발 중에 테스트 한다고 2만번을 다 때리면 너무 오랜 시간이 걸림
        // 개발 테스트 용으로, 초반 몇 페이지만 조회하고 말도록 만들 것이다.

        // pageSize = 3이라면, 3000건씩 들고 올 것이다.

        // 데이터 검색 -> 1000건
        // 1000건으로 검증
        // 1000건이 안 되면 컷 / 넘기면 쿼리 한 번 더
        // 반복문으로 때리자
        // 이 로직도 -> 공통화가 가능하고, 재사용이 충분히 가능하니까 이걸 utility 화 시킬 것이다.

        C4QueryExecuteTemplate.<Account>selectAndExecuteWithCursorAndPageLimit(pageSize, 1000,
                // 커서 페이징을 위한 selectFunction
                lastAccount -> accountRepository.findAccountByLastAccountId(lastAccount == null ? null : lastAccount.getId(), 1000),
                // 가져온 값을 가지고 뭘 할지
                accounts -> accounts.forEach(this::validateAccount)
        );
    }

    public void migrateData(int pageSize) {
        // 유저 정보를 들고와서
        // 유저 정보를 기반으로 account를 가져옴
        // account 정보를 기반으로 transaction을 가져옴

        threadPoolExecutor.init();

        C4QueryExecuteTemplate.<User>selectAndExecuteWithCursorAndPageLimit(pageSize, LIMIT_SIZE,
                // 위처럼 null로 하는 것을 더 권장하심
                lastUser -> userRepository.findUsersWithLastUserId(lastUser == null ? 0 : lastUser.getId(), 10000),
                users -> users.forEach(this::migrateUserInfo)
                );

        threadPoolExecutor.waitToEnd();
    }

    public void aggregateTransactionsWithSharding() {

    }

    private void validateAccount(Account account) {
        if (!account.validateAccountNumber()) {
            log.error("invalid accountNumber - accountNumber : {}", account.getAccountNumber());
        }
    }

    private void migrateUserInfo(User user) {
        var groupId = user.getGroupId();

        // 유저 정보를 기반으로 Account 를 가져옴
        C4QueryExecuteTemplate.<Account>selectAndExecuteWithCursor(LIMIT_SIZE,
                lastAccount -> accountRepository.findAccountByUserIdAndLastAccountId(user.getId(), lastAccount == null ? null : lastAccount.getId(), LIMIT_SIZE),
                accounts -> {
                    // account 삽입 - bulk insert
                    accountRepository.saveAll(groupId, accounts);
                    // transaction 조회 후 삽입
                    accounts.forEach(account -> transactionRepository.selectAndMigrate(account,
                            C4StringUtil.format("transaction_migration_greengreen_{}", groupId)
                            ));
                });

        // 일반적인 스레드풀로는
        // 예외가 터지든 안 터지든 끝까지 작업을 수행해야 함
        // 에러 터지면 로그로 띄우고 나중에 수동 추가하게..

        // Account 정보를 기반으로 Transaction 을 가져옴

        // Account를 어떻게 가져올 것인가?
        // Account를 어떻게 삽입할 것인가?
        //
    }
}
