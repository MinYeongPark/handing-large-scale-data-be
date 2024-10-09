package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final UserJpaRepository userJpaRepository;

    // 팁 : JpaRepository를 상속하는 인터페이스의 이름은 AccountJpaRepository 로 하고,
    // AccountRepository 안에 AccountJpaRepository를 담아서 진행하심
    // 직접적으로 이용하면 조금 제약이 있을 때가 있음

    /**
     * 최신 유저의 목록을 가져온다. (count 만큼)
     * "최신"의 조건을 바꿔가면서 쿼리를 날려보자.
     * @param count
     * @return
     */
    public List<User> findRecentCreatedUsers(int count) {
        return userJpaRepository.findRecentCreatedUsers(count);
    }
}
