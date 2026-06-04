package com.campus.trade.goods.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.service.GoodsService;
import com.campus.trade.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {
    private final GoodsService goodsService;
    private final UserRepository userRepository;
    public GoodsController(GoodsService gs, UserRepository ur) { goodsService = gs; userRepository = ur; }

    @GetMapping("/list")
    public ApiResult<Page<Goods>> list(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String keyword) {
        return goodsService.list(page, size, category, keyword);
    }

    @GetMapping("/{id}")
    public ApiResult<Goods> detail(@PathVariable Long id) { return goodsService.detail(id); }

    @PostMapping("/create")
    public ApiResult<Goods> create(@RequestBody Goods goods, Authentication auth) {
        if (auth == null) return ApiResult.error(401, "请先登录");
        Long userId = (Long) auth.getPrincipal();
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) return ApiResult.error(401, "用户不存在");
        if (user.getAuthStatus() == null || user.getAuthStatus() != 1)
            return ApiResult.error(403, "请先完成校园认证");
        if (user.getCreditScore() == null || user.getCreditScore() < 80)
            return ApiResult.error(403, "信用分低于80分，无法发布商品");
        goods.setUserId(userId);
        return goodsService.create(goods);
    }

    @GetMapping("/mine")
    public ApiResult<List<Goods>> myGoods(Authentication auth) {
        if (auth == null) return ApiResult.error(401, "请先登录");
        return goodsService.myList((Long) auth.getPrincipal());
    }

    // ==================== 管理员功能 ====================

    /**
     * 查看待审核商品列表（管理员用）
     */
    @GetMapping("/pending")
    public ApiResult<Page<Goods>> pending(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "50") int size) {
        return goodsService.listPending(page, size);
    }

    /**
     * 审核商品（管理员用）
     * @param goodsId 商品ID
     * @param approved true=通过, false=拒绝
     */
    @PostMapping("/review")
    public ApiResult<Goods> review(@RequestParam Long goodsId, @RequestParam boolean approved) {
        return goodsService.review(goodsId, approved);
    }

    // ==================== 其他功能 ====================

    /**
     * 查看在售商品列表
     */
    @GetMapping("/onsale")
    public ApiResult<Page<Goods>> onsale(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size) {
        return goodsService.listOnSale(page, size);
    }

    /**
     * 下架商品
     */
    @PostMapping("/delist")
    public ApiResult<String> delist(@RequestParam Long goodsId) {
        return goodsService.delist(goodsId);
    }
}