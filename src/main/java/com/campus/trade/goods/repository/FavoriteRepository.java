package com.campus.trade.goods.repository;

import com.campus.trade.goods.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndGoodsId(Long userId, Long goodsId);
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserIdAndGoodsId(Long userId, Long goodsId);
    boolean existsByUserIdAndGoodsId(Long userId, Long goodsId);
}