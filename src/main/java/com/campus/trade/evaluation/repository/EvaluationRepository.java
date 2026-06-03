package com.campus.trade.evaluation.repository;

import com.campus.trade.evaluation.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Page<Evaluation> findByTargetUserIdAndStatusOrderByCreatedAtDesc(Long targetUserId, Integer status, Pageable pageable);
    Page<Evaluation> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);
    List<Evaluation> findByGoodsIdAndStatusOrderByCreatedAtDesc(Long goodsId, Integer status);
    boolean existsByOrderIdAndUserId(Long orderId, Long userId);
    Integer countByTargetUserIdAndStatus(Long targetUserId, Integer status);
    @Query("SELECT AVG(e.rating) FROM Evaluation e WHERE e.targetUserId = :targetUserId AND e.status = :status")
    Double getAverageRatingByTargetUserIdAndStatus(@Param("targetUserId") Long targetUserId, @Param("status") Integer status);
}
