package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccountRepository {
    private final AccountJpaRepository accountJpaRepository;

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
}
