package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.Account;
import com.vsfe.largescale.util.C4StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
    private final AccountJpaRepository accountJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 주어진 lastAccountId를 활용하여, size만큼의 account 정보를 가져온다.
     * @param lastAccountId
     * @param size
     * @return
     */
    public List<Account> findAccountByLastAccountId(Integer lastAccountId, int size) {
        if (lastAccountId == null) {
            return accountJpaRepository.findAccount(size);
        }

        return accountJpaRepository.findAccountWithLastAccountId(lastAccountId, size);
    }

    public List<Account> findAccountByUserIdAndLastAccountId(Integer userId, Integer lastAccountId, int size) {
        if (lastAccountId == null) {
            return accountJpaRepository.findAccountByUserId(userId, size);
        }

        return accountJpaRepository.findAccountByUserIdWithLastUserId(userId, lastAccountId, size);
    }

    /**
     * account 데이터를 작성한다.
     * @param groupId
     * @param accounts
     */
    public void saveAll(int groupId, List<Account> accounts) {
        // bulk insert란?
        // 네트워크 트래픽을 여러 번 타지 않고, 묶음으로 바로 하는 것
        // JPA는 이를 지원하지 않고, saveAll의 경우에는 단건을 여러번 호출해서 진짜 많이 느림.
        // 따라서 벌크 인서트를 직접 만들어주는 것이 필요함

        var sql = C4StringUtil.format("""
                   INSERT INTO account_migration_greengreen_{} (account_id, account_number, user_id, account_type, memo, balance, create_date, recent_transaction_date)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, groupId);

        jdbcTemplate.batchUpdate(sql,
                accounts,
                accounts.size(),
                (PreparedStatement ps, Account account) -> {
                    ps.setInt(1, account.getId());
                    ps.setString(2, account.getAccountNumber());
                    ps.setInt(3, account.getUserId());
                    ps.setString(4, account.getAccountType().toString());
                    ps.setString(5, account.getMemo());
                    ps.setDouble(6, account.getBalance());
                    ps.setObject(7, account.getCreateDate());
                    ps.setObject(8, account.getRecentTransactionDate());
                }
        );
    }
}
