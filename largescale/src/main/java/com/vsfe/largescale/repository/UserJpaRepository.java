package com.vsfe.largescale.repository;

import com.vsfe.largescale.domain.User;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long> {
    // index : user_idx05 (create_date-1, user_id) // 이렇게 인덱스 어떤 것에 의한 것인지 적어주면 좋음
    @Query("""
        SELECT u
        FROM User u
        ORDER BY u.createDate DESC, u.id ASC
        LIMIT :count      
    """)
    List<User> findRecentCreatedUsers(@Param("count") int count);
    // 정렬 기준을 createDate만 넣어주면, 데이터가 많아질 때 문제가 생김!
    // -> create_date가 같은 경우가 있을 수 있음.
    // 따라서 정렬 기준을 하나 더 넣어주어야 함

    // index: pk
    @Query("""
        SELECT u
        FROM User u
        WHERE u.id > :lastUserId
        ORDER BY u.id
        LIMIT :count
    """)
    List<User> findUsersWithLastUserId(
            @PositiveOrZero @Param("lastUserId") int lastUserId,
            @Positive @Param("count") int count
    );
}

