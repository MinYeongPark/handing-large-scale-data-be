package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountJpaRepository extends JpaRepository<Account, Long> {

    // index : pk
    @Query("""
            SELECT a
            FROM Account a
            ORDER BY a.id
            LIMIT :size
            """)
    List<Account> findAccount(
            @Param("size") int size
    );

    // 페이징 쿼리
    // index : pk
    @Query("""
            SELECT a
            FROM Account a
            WHERE a.id > :lastAccountId
            ORDER BY a.id
            LIMIT :size
            """)
    List<Account> findAccountWithLastAccountId(
            @Param("lastAccountId") int lastAccountId,
            @Param("size") int size
    );
}
