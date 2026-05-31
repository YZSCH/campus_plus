package com.campus.trade.user.repository;

import com.campus.trade.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByUsername(String username);
    Optional<User> findByPhoneAndRole(String phone, String role);
    Optional<User> findByUsernameAndRole(String username, String role);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);
    boolean existsByStudentId(String studentId);
    @Modifying @Query("UPDATE User u SET u.loginFailCount = u.loginFailCount + 1 WHERE u.id = :id")
    int incrementFailCount(@Param("id") Long id);
    @Modifying @Query("UPDATE User u SET u.loginFailCount = 0, u.lockTime = NULL WHERE u.id = :id")
    void resetFailCount(@Param("id") Long id);
    @Modifying @Query("UPDATE User u SET u.lockTime = :lockTime WHERE u.id = :id")
    void lockAccount(@Param("id") Long id, @Param("lockTime") LocalDateTime lockTime);
    
    // 管理员查询方法
    Optional<User> findByStudentId(String studentId);
    java.util.List<User> findByRole(String role);
    java.util.List<User> findByStatus(Integer status);
    java.util.List<User> findByRoleAndStatus(String role, Integer status);
    org.springframework.data.domain.Page<User> findByRole(String role, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<User> findByStatus(Integer status, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<User> findByRoleAndStatus(String role, Integer status, org.springframework.data.domain.Pageable pageable);
}
