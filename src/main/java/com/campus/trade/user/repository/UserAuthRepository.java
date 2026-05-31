package com.campus.trade.user.repository;

import com.campus.trade.user.entity.UserAuth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByUserId(Long userId);
    boolean existsByUserIdAndStatus(Long userId, Integer status);
    Page<UserAuth> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);
    Page<UserAuth> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
