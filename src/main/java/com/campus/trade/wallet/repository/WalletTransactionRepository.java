package com.campus.trade.wallet.repository;

import com.campus.trade.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /** 根据订单ID和类型查询交易记录 */
    Optional<WalletTransaction> findByOrderIdAndType(Long orderId, String type);

    /** 根据支付单号查询交易记录 */
    Optional<WalletTransaction> findByPayId(String payId);

    /** 查询用户的交易记录（按时间倒序） */
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** 完成收款：将 PENDING 状态的 RECEIVE 交易更新为 COMPLETED，并增加用户余额 */
    @Modifying
    @Query("UPDATE WalletTransaction w SET w.status = :status WHERE w.orderId = :orderId AND w.type = :type")
    int updateStatus(@Param("orderId") Long orderId, @Param("type") String type, @Param("status") String status);
}
