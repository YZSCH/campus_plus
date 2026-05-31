package com.campus.trade.admin.repository;

import com.campus.trade.admin.entity.Violation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 违规行为数据访问层
 */
@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {
    
    /**
     * 根据用户ID查询违规记录
     */
    List<Violation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * 分页查询违规记录（支持按状态筛选）
     */
    Page<Violation> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);
    
    /**
     * 查询所有违规记录（按创建时间倒序）
     */
    Page<Violation> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * 根据违规类型查询
     */
    List<Violation> findByViolationTypeOrderByCreatedAtDesc(String violationType);
}
