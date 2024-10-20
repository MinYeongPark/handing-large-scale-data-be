package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.Account;
import com.vsfe.largescale.domain.Transaction;
import com.vsfe.largescale.model.PageInfo;
import com.vsfe.largescale.model.type.TransactionSearchOption;
import com.vsfe.largescale.util.C4PageTokenUtil;
import com.vsfe.largescale.util.C4StringUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TransactionRepository {
    private final TransactionJpaRepository transactionJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * pageToken을 포함하지 않고 커서 페이징 쿼리를 호출한다.
     * @param accountNumber
     * @param option
     * @param count
     * @return
     */
    public PageInfo<Transaction> findTransactionWithoutPageToken(
            String accountNumber,
            TransactionSearchOption option,
            int count
    ) {
        // 컴파일러에게 타입을 위임해서 타입을 추론하게 해줌
        var data = switch (option) {
            case SENDER -> transactionJpaRepository.findTransactionBySenderAccount(accountNumber, count + 1);
            case RECEIVER -> transactionJpaRepository.findTransactionByReceiverAccount(accountNumber, count + 1);
            case ALL -> mergeAllOptions(
                    transactionJpaRepository.findTransactionBySenderAccount(accountNumber, count + 1),
                    transactionJpaRepository.findTransactionByReceiverAccount(accountNumber, count + 1),
                    count + 1
            );
        };

        // ALL의 경우 : sender인 경우와 receiver인 경우를 모두 가져옴
        // 두개를 합쳐서, 얘를 transaction data 기준으로 내림차순 정렬, id 기준으로 오름차순 정렬하면,
        // 가장 최근 거래 count 건이 나옴이 보장된다.
        // 꼭 쿼리 내에서 한방에 가져옲 필요 x

        return PageInfo.of(
                data, count,
                Transaction::getTransactionDate,
                Transaction::getId
        );
    }

    /**
     * pageToken을 포함하여 커서 페이징 쿼리를 호출한다.
     * @param accountNumber
     * @param pageToken
     * @param option
     * @param count
     * @return
     */
    public PageInfo<Transaction> findTransactionWithPageToken(
            String accountNumber,
            String pageToken,
            TransactionSearchOption option,
            int count
    ) {
        // pageToken 파싱해서, 날짜와 id를 가져옴
        // 그거 기반으로 위와 동일하게 쿼리 때림
        // 그 결과로 pageInfo 만들어주고 반환하면 됨

        var pageData = C4PageTokenUtil.decodePageToken(pageToken, Instant.class, Integer.class);
        var transactionDate = pageData.getLeft();
        var transactionId = pageData.getRight();
        var data = switch (option) {
            case SENDER -> transactionJpaRepository.findTransactionBySenderAccountWithPageToken(accountNumber, transactionDate, transactionId, count + 1);
            case RECEIVER -> transactionJpaRepository.findTransactionByReceiverAccountWithPageToken(accountNumber, transactionDate, transactionId,count + 1);
            case ALL -> mergeAllOptions(
                    transactionJpaRepository.findTransactionBySenderAccountWithPageToken(accountNumber, transactionDate, transactionId, count + 1),
                    transactionJpaRepository.findTransactionByReceiverAccountWithPageToken(accountNumber, transactionDate, transactionId,count + 1),
                    count + 1
            );
        };

        return PageInfo.of(
                data, count,
                Transaction::getTransactionDate,
                Transaction::getId
        );
    }

    /**
     * 두 결과를 합쳐서, 데이터를 정렬 조건에 맞춰 count개 만큼 가져온다.
     * @param senderResult
     * @param receiverResult
     * @param count
     * @return
     */
    private List<Transaction> mergeAllOptions(
            List<Transaction> senderResult,
            List<Transaction> receiverResult,
            int count
    ) {
        return ListUtils.union(senderResult, receiverResult).stream()
                .sorted(
                        Comparator.comparing(Transaction::getTransactionDate).reversed() // 1차 정렬로는 TransactionDate로 DESC 정렬
                                .thenComparing(Transaction::getId) // 2차 정렬로는 id로 정렬해주겠다
                )
                .limit(count)
                .toList();
    }

    public void selectAndMigrate(Account account, String destTableName) {
        var sql = C4StringUtil.format("""
            INSERT INTO {} (transaction_id, sender_account, receiver_account, sender_swift_code, receiver_swift_code, sender_name, receiver_name, amount, memo, transaction_date)
            (SELECT transaction_id, sender_account, receiver_account, sender_swift_code, receiver_swift_code, sender_name, receiver_name, amount, memo, transaction_date FROM transaction t
            WHERE t.sender_account = '{}' OR t.receiver_account = '{}')
            """, destTableName, account.getAccountNumber(), account.getAccountNumber());

        jdbcTemplate.execute(sql);
    }
}
