package com.campus.trade.goods.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.service.FavoriteService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/toggle")
    public ApiResult<Boolean> toggle(@RequestParam Long goodsId, Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        Long userId = (Long) auth.getPrincipal();
        return favoriteService.toggle(userId, goodsId);
    }

    @GetMapping("/list")
    public ApiResult<List<Goods>> list(Authentication auth) {
        if (auth == null) return ApiResult.error(401, "未登录");
        Long userId = (Long) auth.getPrincipal();
        return favoriteService.list(userId);
    }

    @GetMapping("/check/{goodsId}")
    public ApiResult<Boolean> check(@PathVariable Long goodsId, Authentication auth) {
        if (auth == null) return ApiResult.success(false);
        Long userId = (Long) auth.getPrincipal();
        return favoriteService.isFavorited(userId, goodsId);
    }
}