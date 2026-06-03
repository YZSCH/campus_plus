package com.campus.trade.pay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.campus.trade.common.ApiResult;
import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.repository.GoodsRepository;
import com.campus.trade.message.service.MessageService;
import com.campus.trade.order.entity.Order;
import com.campus.trade.order.service.OrderService;
import com.campus.trade.pay.dto.PayRequest;
import com.campus.trade.pay.dto.RefundRequest;
import com.campus.trade.pay.util.PayUtil;
import com.campus.trade.user.repository.UserRepository;
import com.campus.trade.wallet.service.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/alipay")
public class AlipayController {

    private static final Logger log = LoggerFactory.getLogger(AlipayController.class);

    @Autowired private PayUtil payUtil;
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired private OrderService orderService;
    @Autowired private WalletService walletService;
    @Autowired private MessageService messageService;
    @Autowired private GoodsRepository goodsRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/pay")
    public ApiResult<String> alipay(@RequestBody PayRequest payRequest) throws AlipayApiException {
        log.info("[支付宝] 收到支付请求，订单ID：{}", payRequest.getOrderId());

        Order order = orderService.findById(payRequest.getOrderId());
        if (order == null) return ApiResult.error(404, "订单不存在");
        if (!"unpaid".equals(order.getPayStatus())) return ApiResult.error(400, "该订单已支付或已取消");

        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String user = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String payId = time + user;

        orderService.updatePayId(order.getId(), payId);

        Float amount = order.getTotalPrice().floatValue();
        String orderNo = order.getOrderNo() == null ? "" : order.getOrderNo().trim();
        String content = payUtil.sendRequestToAlipay(payId, amount, "CampusTrade-" + orderNo);

        log.info("[支付宝] 订单 {} 发起支付，支付单号：{}", order.getId(), payId);
        return ApiResult.success(content);
    }

    @PostMapping("/pay/mock")
    public ApiResult<String> mockPay(@RequestBody PayRequest payRequest, Authentication auth) {
        log.info("[模拟支付] 收到支付请求，订单ID：{}", payRequest.getOrderId());

        Order order = orderService.findById(payRequest.getOrderId());
        if (order == null) return ApiResult.error(404, "订单不存在");
        if (!"unpaid".equals(order.getPayStatus())) return ApiResult.error(400, "该订单已支付或已取消");

        String subject = "商品";
        if (order.getGoodsId() != null) {
            Goods goods = goodsRepository.findById(order.getGoodsId()).orElse(null);
            if (goods != null && goods.getTitle() != null) {
                subject = goods.getTitle();
            }
        }

        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String payId = "MOCK_" + time + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        orderService.updatePayId(order.getId(), payId);

        Float amount = order.getTotalPrice().floatValue();

        String token = auth != null ? auth.getCredentials() != null ? auth.getCredentials().toString() : "" : "";

        BigDecimal balance = null;
        if (auth != null) {
            try {
                Long userId = Long.parseLong(auth.getName());
                var userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    balance = userOpt.get().getBalance();
                }
            } catch (Exception e) {
                log.warn("[模拟支付] 获取用户余额失败", e);
            }
        }

        String html = buildMockPayHtml(subject, payId, amount, order.getId(), token, balance);
        log.info("[模拟支付] 订单 {} 发起模拟支付，支付单号：{}", order.getId(), payId);
        return ApiResult.success(html);
    }

    @PostMapping("/mock/pay/confirm")
    public ApiResult<?> mockPayConfirm(@RequestBody Map<String, String> params, Authentication auth) {
        String payId = params.get("out_trade_no");
        String password = params.get("password");
        String orderIdStr = params.get("orderId");
        log.info("[模拟支付] 确认支付，支付单号：{}", payId);

        if (password == null || password.length() != 6) {
            return ApiResult.error(400, "支付密码错误");
        }
        if (orderIdStr == null || orderIdStr.isEmpty()) {
            return ApiResult.error(400, "订单ID不能为空");
        }

        Long orderId = Long.parseLong(orderIdStr);
        Long userId = auth != null ? Long.parseLong(auth.getName()) : null;
        if (userId == null) {
            return ApiResult.error(401, "请先登录");
        }

        Order order = orderService.findById(orderId);
        if (order == null) return ApiResult.error(404, "订单不存在");

        ApiResult<?> payResult = walletService.pay(orderId, userId, payId, order.getTotalPrice());
        if (payResult.getCode() != 200) {
            return payResult;
        }

        new Thread(() -> {
            try {
                Thread.sleep(800 + (long)(Math.random() * 1200));
                mockAsyncNotify(payId);
            } catch (Exception e) {
                log.error("[模拟支付] 异步通知失败", e);
            }
        }).start();

        return ApiResult.success("支付处理中");
    }

    private void mockAsyncNotify(String payId) {
        log.info("[模拟支付-异步通知] 发送异步通知，支付单号：{}", payId);

        Map<String, String> notifyParams = new HashMap<>();
        notifyParams.put("notify_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        notifyParams.put("notify_type", "trade_status_sync");
        notifyParams.put("notify_id", "mock_notify_" + System.currentTimeMillis());
        notifyParams.put("app_id", "2021000000000000");
        notifyParams.put("out_trade_no", payId);
        notifyParams.put("trade_status", "TRADE_SUCCESS");

        Order order = orderService.findOrderByPayId(payId);
        String realAmount = (order != null) ? order.getTotalPrice().toString() : "0.01";
        notifyParams.put("total_amount", realAmount);

        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    notifyParams,
                    payUtil.getAlipayPublicKey(),
                    payUtil.getCharset(),
                    payUtil.getSignType()
            );

            if (signVerified) {
                log.info("[模拟支付-异步通知] 验签通过，支付单号：{}", payId);
                orderService.updatePayStatus(payId, "paid", "paid");
                sendPaySuccessNotification(payId);
            } else {
                log.warn("[模拟支付-异步通知] 验签失败，支付单号：{}", payId);
            }
        } catch (Exception e) {
            log.error("[模拟支付-异步通知] 处理失败", e);
        }
    }

    @PostMapping("/mock/confirm")
    public String mockConfirm(@RequestParam Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        log.info("[模拟支付回调] 支付单号：{}", outTradeNo);

        orderService.updatePayStatus(outTradeNo, "paid", "paid");
        sendPaySuccessNotification(outTradeNo);

        log.info("[模拟支付回调] 支付成功，订单已更新：{}", outTradeNo);
        return buildMockSuccessHtml(outTradeNo);
    }

    private void sendPaySuccessNotification(String payId) {
        try {
            Order order = orderService.findOrderByPayId(payId);
            if (order == null) return;

            messageService.sendSystemMessage(order.getBuyerId(),
                    "支付成功",
                    "您的订单 " + order.getOrderNo() + " 已支付成功，等待卖家发货。",
                    order.getGoodsId());

            messageService.sendSystemMessage(order.getSellerId(),
                    "新订单已付款",
                    "您的商品订单 " + order.getOrderNo() + " 买家已付款，请尽快发货。",
                    order.getGoodsId());
        } catch (Exception e) {
            log.error("[通知] 发送支付成功通知失败", e);
        }
    }

    @GetMapping("/return")
    public String returns(@RequestParam Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        outTradeNo = outTradeNo == null ? "" : outTradeNo.trim();
        log.info("[支付回调-同步] 支付单号：{}", outTradeNo);

        String queryResult = payUtil.query(outTradeNo);
        if (queryResult == null) {
            return "<script>alert('查询支付结果失败');window.location.href='/';</script>";
        }

        Object tradeStatus = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonObject = OBJECT_MAPPER.readValue(queryResult, Map.class);
            Object o = jsonObject.get("alipay_trade_query_response");
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) o;
            tradeStatus = map.get("trade_status");
        } catch (Exception e) {
            log.error("解析支付宝返回结果失败", e);
            return "<script>alert('解析支付结果失败');window.location.href='/';</script>";
        }

        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            orderService.updatePayStatus(outTradeNo, "paid", "paid");
            sendPaySuccessNotification(outTradeNo);
            log.info("[支付回调] 支付成功：{}", outTradeNo);
            return "<script>window.location.href='/?payStatus=success';</script>";
        } else {
            log.info("[支付回调] 支付失败或未完成：{}", outTradeNo);
            return "<script>alert('支付失败或尚未完成，请稍后查看订单状态');window.location.href='/?payStatus=fail';</script>";
        }
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));

        log.info("[支付回调-异步] 收到通知：{}", params);

        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    payUtil.getAlipayPublicKey(),
                    payUtil.getCharset(),
                    payUtil.getSignType()
            );

            if (signVerified) {
                String outTradeNo = params.get("out_trade_no");
                String tradeStatus = params.get("trade_status");

                if ("TRADE_SUCCESS".equals(tradeStatus)) {
                    orderService.updatePayStatus(outTradeNo, "paid", "paid");
                    sendPaySuccessNotification(outTradeNo);
                    log.info("[支付回调-异步] 验签通过，支付成功：{}", outTradeNo);
                    return "success";
                }
            } else {
                log.warn("[支付回调-异步] 验签失败！");
            }
        } catch (Exception e) {
            log.error("[支付回调-异步] 处理异常", e);
        }
        return "fail";
    }

    @GetMapping("/query")
    public ApiResult<String> queryOrder(@RequestParam String outTradeNo) {
        String result = payUtil.query(outTradeNo);
        if (result == null) return ApiResult.error(500, "查询失败");
        return ApiResult.success(result);
    }

    @PostMapping("/refund")
    public ApiResult<String> aliPayRefund(@RequestBody RefundRequest refundRequest) throws AlipayApiException {
        log.info("[退款] 收到退款请求：{}", refundRequest);

        AlipayClient alipayClient = new DefaultAlipayClient(payUtil.getAlipayConfig());
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();

        model.setOutTradeNo(refundRequest.getPayId());
        model.setRefundAmount(refundRequest.getValue());
        model.setRefundReason(refundRequest.getReason() != null ? refundRequest.getReason() : "正常退款");
        request.setBizModel(model);

        AlipayTradeRefundResponse response = alipayClient.execute(request);
        log.info("[退款] 支付宝返回：{}", response.getBody());

        if (response.isSuccess()) {
            log.info("[退款] 退款成功：{}", refundRequest.getPayId());
            orderService.updateStatus(refundRequest.getOrderId(), "cancelled");
            return ApiResult.success("退款成功");
        } else {
            log.info("[退款] 退款失败：{}", refundRequest.getPayId());
            return ApiResult.error(500, "退款失败：" + response.getMsg());
        }
    }

    private String buildMockPayHtml(String subject, String payId, Float amount, Long orderId, String token, BigDecimal balance) {
        String baseUrl = "/api/alipay";
        String tokenScript = "";
        if (token != null && !token.isEmpty()) {
            tokenScript = "var AUTH_TOKEN='" + token + "';";
        } else {
            tokenScript = "var AUTH_TOKEN='';try{if(window.opener&&window.opener.localStorage){AUTH_TOKEN=window.opener.localStorage.getItem('token')||'';}}catch(e){}";
        }

        String balanceStr = balance != null ? String.format("%.2f", balance) : "0.00";
        boolean sufficient = balance != null && balance.compareTo(new BigDecimal(amount)) >= 0;
        String balanceColor = sufficient ? "#52c41a" : "#ff4d4f";
        String balanceTip = sufficient ? "余额充足" : "余额不足";

        return "<!DOCTYPE html><html><head><meta charset='utf-8'><title>模拟支付</title>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>"
                + "*{margin:0;padding:0;box-sizing:border-box}"
                + "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;background:#f0f2f5;color:#333}"
                + ".topbar{background:#1677ff;height:48px;display:flex;align-items:center;padding:0 24px;color:#fff;font-size:14px}"
                + ".topbar a{color:#fff;text-decoration:none;margin-right:24px;cursor:pointer}"
                + ".topbar .right{margin-left:auto;display:flex;gap:16px;align-items:center}"
                + ".container{max-width:800px;margin:24px auto;display:flex;gap:20px}"
                + ".pay-card{background:#fff;border-radius:12px;flex:1;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08)}"
                + ".pay-card-header{padding:20px 24px;border-bottom:1px solid #f0f0f0;display:flex;align-items:center;gap:12px}"
                + ".pay-card-header .icon{width:40px;height:40px;background:#1677ff;border-radius:50%;display:flex;align-items:center;justify-content:center;color:#fff;font-size:20px}"
                + ".pay-card-header h3{font-size:16px;font-weight:600}"
                + ".pay-card-body{padding:24px}"
                + ".order-info{background:#fafafa;border-radius:8px;padding:16px;margin-bottom:20px}"
                + ".order-info .row{display:flex;justify-content:space-between;padding:8px 0;font-size:14px}"
                + ".order-info .row .label{color:#999}.order-info .row .value{font-weight:500;color:#333}"
                + ".order-info .total{display:flex;justify-content:space-between;padding:12px 0 0;margin-top:8px;border-top:1px solid #eee;font-size:16px}"
                + ".order-info .total .amount{color:#ff4d4f;font-weight:700;font-size:20px}"
                + ".balance-info{background:#f6ffed;border-radius:8px;padding:12px 16px;margin-bottom:20px;border:1px solid #b7eb8f}"
                + ".balance-info .balance-row{display:flex;justify-content:space-between;align-items:center}"
                + ".balance-info .balance-label{font-size:14px;color:#666}"
                + ".balance-info .balance-value{font-size:18px;font-weight:700}"
                + ".balance-info .balance-tip{font-size:12px;margin-top:4px}"
                + ".pwd-area{margin-top:20px}"
                + ".pwd-area label{display:block;font-size:14px;color:#666;margin-bottom:8px}"
                + ".pwd-input{width:100%;padding:12px 16px;border:2px solid #e8e8e8;border-radius:8px;font-size:24px;letter-spacing:12px;text-align:center;outline:none;transition:border-color .3s}"
                + ".pwd-input:focus{border-color:#1677ff;box-shadow:0 0 0 2px rgba(22,119,255,.2)}"
                + ".btn-pay{width:100%;padding:14px;background:linear-gradient(135deg,#1677ff,#4096ff);color:#fff;border:none;border-radius:8px;font-size:16px;font-weight:600;cursor:pointer;transition:all .3s;margin-top:16px}"
                + ".btn-pay:hover{background:linear-gradient(135deg,#4096ff,#69b4ff)}"
                + ".btn-pay:disabled{background:#d9d9d9;cursor:not-allowed}"
                + ".order-card{background:#fff;border-radius:12px;width:280px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08)}"
                + ".order-card-header{background:#fafafa;padding:16px 20px;border-bottom:1px solid #f0f0f0;font-size:14px;font-weight:600;color:#333}"
                + ".order-card-body{padding:20px}"
                + ".order-card-body .info-row{display:flex;justify-content:space-between;padding:8px 0;font-size:13px;color:#666}"
                + ".order-card-body .info-row .val{color:#333;font-weight:500}"
                + ".loading-mask{display:none;position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,.5);z-index:9999;justify-content:center;align-items:center}"
                + ".loading-box{background:#fff;border-radius:16px;padding:40px;text-align:center;min-width:300px}"
                + ".loading-spinner{width:48px;height:48px;border:4px solid #f0f0f0;border-top-color:#1677ff;border-radius:50%;animation:spin 1s linear infinite;margin:0 auto 16px}"
                + "@keyframes spin{to{transform:rotate(360deg)}}"
                + ".loading-text{font-size:16px;color:#333;margin-bottom:8px}"
                + ".loading-tip{font-size:13px;color:#999}"
                + ".success-box{display:none;text-align:center;padding:40px}"
                + ".success-icon{width:80px;height:80px;background:#52c41a;border-radius:50%;display:inline-flex;align-items:center;justify-content:center;color:#fff;font-size:48px;animation:scaleIn .5s ease}"
                + "@keyframes scaleIn{from{transform:scale(0)}to{transform:scale(1)}}"
                + "</style></head><body>"
                + "<div class='topbar'>"
                + "  <a onclick='window.close()'>← 关闭</a>"
                + "  <span style='font-weight:600'>模拟支付</span>"
                + "  <div class='right'><span>⚠️ 模拟交易，非真实扣款</span></div>"
                + "</div>"
                + "<div class='container'>"
                + "<div class='pay-card'>"
                + "  <div class='pay-card-header'>"
                + "    <div class='icon'>💰</div>"
                + "    <div><h3>订单支付</h3><span style='font-size:12px;color:#999'>支付前请确认订单信息</span></div>"
                + "  </div>"
                + "  <div class='pay-card-body'>"
                + "    <div class='balance-info'>"
                + "      <div class='balance-row'>"
                + "        <span class='balance-label'>我的余额</span>"
                + "        <span class='balance-value' style='color:" + balanceColor + "'>¥ " + balanceStr + "</span>"
                + "      </div>"
                + "      <div class='balance-tip' style='color:" + balanceColor + "'>" + balanceTip + "</div>"
                + "    </div>"
                + "    <div class='order-info'>"
                + "      <div class='row'><span class='label'>商品</span><span class='value'>" + subject + "</span></div>"
                + "      <div class='row'><span class='label'>支付单号</span><span class='value' style='font-size:11px'>" + payId + "</span></div>"
                + "      <div class='total'><span>支付金额</span><span class='amount'>¥ " + String.format("%.2f", amount) + "</span></div>"
                + "    </div>"
                + "    <div class='pwd-area'>"
                + "      <label>请输入支付密码（6位数字）</label>"
                + "      <input class='pwd-input' id='pwdInput' type='password' maxlength='6' placeholder='******' oninput='checkPwd()' />"
                + "      <button class='btn-pay' id='btnPay' disabled onclick='doPay()'>确认支付 ¥ " + String.format("%.2f", amount) + "</button>"
                + "    </div>"
                + "  </div>"
                + "</div>"
                + "<div class='order-card'>"
                + "  <div class='order-card-header'>订单详情</div>"
                + "  <div class='order-card-body'>"
                + "    <div class='info-row'><span>商品</span><span class='val' style='font-size:12px'>" + subject + "</span></div>"
                + "    <div class='info-row'><span>创建时间</span><span class='val'>" + new SimpleDateFormat("MM-dd HH:mm").format(new Date()) + "</span></div>"
                + "    <div class='info-row'><span>订单状态</span><span class='val' style='color:#faad14'>待支付</span></div>"
                + "    <hr style='margin:12px 0;border:none;border-top:1px solid #f0f0f0'>"
                + "    <div class='info-row'><span style='font-weight:600;color:#333'>支付金额</span><span class='val' style='color:#ff4d4f;font-size:18px;font-weight:700'>¥ " + String.format("%.2f", amount) + "</span></div>"
                + "  </div>"
                + "</div>"
                + "</div>"
                + "<div class='loading-mask' id='loadingMask'>"
                + "  <div class='loading-box'>"
                + "    <div class='loading-spinner'></div>"
                + "    <div class='loading-text'>正在处理支付...</div>"
                + "    <div class='loading-tip'>请勿关闭页面</div>"
                + "  </div>"
                + "</div>"
                + "<div class='loading-mask' id='successMask'>"
                + "  <div class='loading-box'>"
                + "    <div class='success-box' style='display:block'>"
                + "      <div class='success-icon'>✓</div>"
                + "      <div style='font-size:18px;color:#52c41a;margin-top:16px;font-weight:600'>支付成功</div>"
                + "      <div style='font-size:14px;color:#999;margin-top:8px'>正在返回...</div>"
                + "    </div>"
                + "  </div>"
                + "</div>"
                + "<script>"
                + tokenScript
                + "function checkPwd(){"
                + "  var pwd=document.getElementById('pwdInput').value;"
                + "  document.getElementById('btnPay').disabled=pwd.length!==6;"
                + "}"
                + "function doPay(){"
                + "  var pwd=document.getElementById('pwdInput').value;"
                + "  if(pwd.length!==6){alert('请输入6位支付密码');return;}"
                + "  if(!AUTH_TOKEN){alert('登录已过期，请重新登录');return;}"
                + "  document.getElementById('loadingMask').style.display='flex';"
                + "  fetch('" + baseUrl + "/mock/pay/confirm',{"
                + "    method:'POST',"
                + "    headers:{'Content-Type':'application/json','Authorization':'Bearer '+AUTH_TOKEN},"
                + "    body:JSON.stringify({out_trade_no:'" + payId + "',password:pwd,orderId:" + orderId + "})"
                + "  }).then(function(r){return r.json()}).then(function(data){"
                + "    if(data.code===200){"
                + "      setTimeout(function(){"
                + "        document.getElementById('loadingMask').style.display='none';"
                + "        document.getElementById('successMask').style.display='flex';"
                + "        setTimeout(function(){window.close();},1500);"
                + "      },1200);"
                + "    } else {"
                + "      alert('支付失败：'+ (data.message||'未知错误'));"
                + "      document.getElementById('loadingMask').style.display='none';"
                + "    }"
                + "  }).catch(function(e){"
                + "    alert('支付异常，请重试');"
                + "    document.getElementById('loadingMask').style.display='none';"
                + "  });"
                + "}"
                + "document.getElementById('pwdInput').addEventListener('keypress',function(e){"
                + "  if(e.key==='Enter'&&!document.getElementById('btnPay').disabled){doPay();}"
                + "});"
                + "</script>"
                + "<script>"
                + "document.addEventListener('DOMContentLoaded',function(){document.getElementById('pwdInput').focus()});"
                + "</script></body></html>";
    }

    private String buildMockSuccessHtml(String outTradeNo) {
        return "<!DOCTYPE html><html><head><meta charset='utf-8'><title>支付成功</title>"
                + "<style>*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Microsoft YaHei',sans-serif;background:#f5f5f5;display:flex;justify-content:center;align-items:center;min-height:100vh}"
                + ".card{background:#fff;width:420px;border-radius:16px;box-shadow:0 4px 20px rgba(0,0,0,.1);overflow:hidden;text-align:center}"
                + ".success{background:linear-gradient(135deg,#52c41a,#73d13d);color:#fff;padding:48px 24px}.success .icon{font-size:72px;animation:scaleIn .6s ease}.success h2{font-size:24px;margin-top:12px;font-weight:600}"
                + ".body{padding:32px}.body p{color:#666;margin:8px 0;font-size:14px;line-height:1.6}"
                + ".btn{display:inline-block;padding:14px 48px;background:#1677ff;color:#fff;text-decoration:none;border-radius:10px;font-size:16px;font-weight:500;margin-top:20px;transition:all .3s;border:none;cursor:pointer}"
                + ".btn:hover{background:#4096ff;transform:translateY(-2px);box-shadow:0 4px 12px rgba(22,119,255,.4)}"
                + "@keyframes scaleIn{from{transform:scale(0) rotate(-180deg)}to{transform:scale(1) rotate(0)}}"
                + "</style></head><body>"
                + "<div class='card'>"
                + "<div class='success'><div class='icon'>✓</div><h2>支付成功</h2></div>"
                + "<div class='body'>"
                + "<p style='color:#333;font-size:16px;font-weight:500'>订单支付成功</p>"
                + "<p>支付单号：" + outTradeNo + "</p>"
                + "<p style='color:#999;font-size:13px'>资金已托管，卖家发货后将自动结算</p>"
                + "<a class='btn' href='/?payStatus=success'>返回首页</a>"
                + "</div></div></body></html>";
    }
}