package com.vsfe.largescale.service;

import com.vsfe.largescale.domain.Account;
import com.vsfe.largescale.domain.Transaction;
import com.vsfe.largescale.domain.User;
import com.vsfe.largescale.model.PageInfo;
import com.vsfe.largescale.model.type.TransactionSearchOption;
import com.vsfe.largescale.repository.*;
import com.vsfe.largescale.util.C4QueryExecuteTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LargeScaleService {
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

    public void aggregateTransactions() {

    }

    public void aggregateTransactionsWithSharding() {

    }

    private void validateAccount(Account account) {
        if (!account.validateAccountNumber()) {
            log.error("invalid accountNumber - accountNumber : {}", account.getAccountNumber());
        }
    }
}
