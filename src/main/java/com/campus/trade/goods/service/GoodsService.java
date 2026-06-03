package com.campus.trade.goods.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.repository.GoodsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GoodsService {
    private final GoodsRepository goodsRepository;
    public GoodsService(GoodsRepository gr) { goodsRepository = gr; }

    public ApiResult<Page<Goods>> list(int page, int size, String category, String keyword) {
        var p = PageRequest.of(page, size);
        Page<Goods> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = goodsRepository.search(keyword, p);
        } else if (category != null && !category.isEmpty()) {
            result = goodsRepository.findByCategoryAndStatusOrderByCreatedAtDesc(category, "onsale", p);
        } else {
            result = goodsRepository.findByStatusOrderByCreatedAtDesc("onsale", p);
        }
        return ApiResult.success(result);
    }

    public ApiResult<Goods> detail(Long id) {
        return goodsRepository.findById(id).map(ApiResult::success)
                .orElse(ApiResult.error(404, "商品不存在"));
    }

    public ApiResult<Goods> create(Goods goods) {
        goods.setStatus("pending");
        return ApiResult.success(goodsRepository.save(goods));
    }

    public ApiResult<List<Goods>> myList(Long userId) {
        return ApiResult.success(goodsRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100)).getContent());
    }

    /**
     * 查看待审核商品列表（管理员用）
     */
    public ApiResult<Page<Goods>> listPending(int page, int size) {
        return ApiResult.success(goodsRepository.findByStatusOrderByCreatedAtDesc("pending", PageRequest.of(page, size)));
    }

    /**
     * 审核商品（管理员用）
     * @param id 商品ID
     * @param approved true=通过, false=拒绝
     */
    public ApiResult<Goods> review(Long id, boolean approved) {
        return goodsRepository.findById(id).map(goods -> {
            goods.setStatus(approved ? "onsale" : "rejected");
            return ApiResult.success(goodsRepository.save(goods));
        }).orElse(ApiResult.error(404, "商品不存在"));
    }

    /**
     * 查看在售商品列表
     */
    public ApiResult<Page<Goods>> listOnSale(int page, int size) {
        return ApiResult.success(goodsRepository.findByStatusOrderByCreatedAtDesc("onsale", PageRequest.of(page, size)));
    }

    /**
     * 下架商品
     */
    public ApiResult<String> delist(Long id) {
        return goodsRepository.findById(id).map(goods -> {
            goods.setStatus("delisted");
            goodsRepository.save(goods);
            return ApiResult.success("已下架");
        }).orElse(ApiResult.error(404, "商品不存在"));
    }
}
