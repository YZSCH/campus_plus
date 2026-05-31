package com.campus.trade.admin.repository;

import com.campus.trade.admin.entity.Arbitration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 仲裁记录数据访问层
 */
@Repository
public interface ArbitrationRepository extends JpaRepository<Arbitration, Long> {
    
    /**
     * 根据订单ID查询仲裁记录
     */
    List<Arbitration> findByOrderId(Long orderId);
    
    /**
     * 查询用户发起的仲裁（作为申请人）
     */
    List<Arbitration> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
    
    /**
     * 查询用户被仲裁的记录（作为被申请人）
     */
    List<Arbitration> findByRespondentIdOrderByCreatedAtDesc(Long respondentId);
    
    /**
     * 分页查询进行中的仲裁
     */
    Page<Arbitration> findByStatusOrderByCreatedAtDesc(Integer status, Pageable pageable);
    
    /**
     * 分页查询所有仲裁记录
     */
    Page<Arbitration> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
