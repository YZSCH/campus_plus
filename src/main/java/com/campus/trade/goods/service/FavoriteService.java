package com.campus.trade.goods.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.goods.entity.Favorite;
import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.repository.FavoriteRepository;
import com.campus.trade.goods.repository.GoodsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final GoodsRepository goodsRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, GoodsRepository goodsRepository) {
        this.favoriteRepository = favoriteRepository;
        this.goodsRepository = goodsRepository;
    }

    @Transactional
    public ApiResult<Boolean> toggle(Long userId, Long goodsId) {
        if (favoriteRepository.existsByUserIdAndGoodsId(userId, goodsId)) {
            favoriteRepository.deleteByUserIdAndGoodsId(userId, goodsId);
            return ApiResult.success(false);
        } else {
            Favorite favorite = Favorite.builder()
                    .userId(userId)
                    .goodsId(goodsId)
                    .build();
            favoriteRepository.save(favorite);
            return ApiResult.success(true);
        }
    }

    public ApiResult<List<Goods>> list(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Long> goodsIds = favorites.stream().map(Favorite::getGoodsId).collect(Collectors.toList());
        List<Goods> goods = goodsRepository.findAllById(goodsIds);
        goods.sort((a, b) -> {
            int idxA = goodsIds.indexOf(a.getId());
            int idxB = goodsIds.indexOf(b.getId());
            return Integer.compare(idxA, idxB);
        });
        return ApiResult.success(goods);
    }

    public ApiResult<Boolean> isFavorited(Long userId, Long goodsId) {
        boolean favorited = favoriteRepository.existsByUserIdAndGoodsId(userId, goodsId);
        return ApiResult.success(favorited);
    }
}