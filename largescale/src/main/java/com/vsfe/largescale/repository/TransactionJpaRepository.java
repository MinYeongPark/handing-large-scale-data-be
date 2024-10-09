package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionJpaRepository extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.senderAccount = :account
        ORDER BY t.transactionDate DESC, t.id ASC
        LIMIT :limit
    """)
    List<Transaction> findTransactionBySenderAccount(
            @Param("account") String account,
            @Param("limit") int limit
    );

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverAccount = :account
        ORDER BY t.transactionDate DESC, t.id ASC
        LIMIT :limit
    """)
    List<Transaction> findTransactionByReceiverAccount(
            @Param("account") String account,
            @Param("limit") int limit
    );

    // or 조건을 타더라도 똑같은 1개의 인덱스를 타는 경우에는 인덱스 방식으로 쿼리를 조회한다.
    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.senderAccount = :account
        AND ((t.transactionDate < :transactionDate) OR 
             (t.transactionDate = :transactionDate AND t.id > :id))
        ORDER BY t.transactionDate DESC, t.id ASC
        LIMIT :limit        
    """)
    List<Transaction> findTransactionBySenderAccountWithPageToken(
            @Param("account") String account,
            @Param("transactionDate") Instant transactionDate, // 이전에 조회한 마지막 값의 날짜 -> 우리는 이것보다 더 예전 것을 들고와야 한다.
            @Param("id") int id,
            @Param("limit") int limit
    );

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.receiverAccount = :account
        AND ((t.transactionDate < :transactionDate) OR 
             (t.transactionDate = :transactionDate AND t.id > :id))
        ORDER BY t.transactionDate DESC, t.id ASC
        LIMIT :limit        
    """)
    List<Transaction> findTransactionByReceiverAccountWithPageToken(
            @Param("account") String account,
            @Param("transactionDate") Instant transactionDate, // 이전에 조회한 마지막 값의 날짜 -> 우리는 이것보다 더 예전 것을 들고와야 한다.
            @Param("id") int id,
            @Param("limit") int limit
    );
}
