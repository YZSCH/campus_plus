package com.campus.trade.evaluation.controller;

import com.campus.trade.common.ApiResult;
import com.campus.trade.evaluation.entity.Evaluation;
import com.campus.trade.evaluation.service.EvaluationService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final EvaluationService evaluationService;
    public EvaluationController(EvaluationService es) { evaluationService = es; }

    @PostMapping("/create")
    public ApiResult<Evaluation> create(@RequestBody Evaluation eval, Authentication auth) {
        eval.setUserId((Long) auth.getPrincipal());
        return evaluationService.create(eval);
    }

    @GetMapping("/user/{userId}")
    public ApiResult<Page<Evaluation>> listByUser(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return evaluationService.listByUser(userId, page, size);
    }

    @GetMapping("/goods/{goodsId}")
    public ApiResult<List<Evaluation>> listByGoods(@PathVariable Long goodsId) {
        return evaluationService.listByGoods(goodsId);
    }
}
