package com.campus.trade.pay.util;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import org.springframework.stereotype.Component;



/**
 * 支付宝支付工具类
 * 封装支付宝电脑网站支付（pageExecute）和交易查询
 */
@Component
public class PayUtil {

    // ==================== ⚠️ 请填写你的支付宝沙箱配置 ====================

    /** 应用ID（从支付宝沙箱控制台获取：https://open.alipay.com/develop/sandbox/app） */
    private final String APP_ID = "9021000164621913";

    /** 应用私钥（PEM格式，使用支付宝密钥工具生成，注意换行符为 \n） */
    private final String APP_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCPQYjwpyznfnILeJpVGJGGA0F9aOgafnwxh86mim4UbXzL/n2UMD/xYyMDzclGwue7VNDV9SOzKmBh7QODoznYGFwh5ZOEyPplIUGvjpRsgb1OxC1i01vNWxoNWQNp3Kst9J//sxVKadJePNRpcJGGgHJ8o7Ar+HdBNuG1wXeEkIr2h8ldsA7MbfvJsiIbFBK+87n3I3N0Ycrp/AqkgWY4zlG7DJF9+9cs+0WHMkLv++oK5x5cEJUIzGCBiLrVV4OkOGHLaraw3UmrPOKeZNrALrCPZXX9Pn1ICLUQdduGaok+npJXZiEtsotU2Nlv99PIPd1BfXKORWmh+mDkTdVpAgMBAAECggEASnUmhuZtOXkIL5wkoxwc7wmcLWGsWbDqPhg3OWpz4pwxpBmHDdGyOnVripuYBPZi2YQw8zPxtTrOU7eL/wyEkSPAJ0Ia1YZcVtndOTEjRe59BYaDjH0MlwTF2rAsTQaL1lvyGUXsR/wOgjqdsJv4l28FknDFrWT8HuI2mHCQ/U4YQKAn5cdBFjFo+GXF2NHXg9h8vJwZH/TK5DD9hA9HKv8QzmUI1NDIv4W0243dvYJtFZU/kp3WIuE+3nlBU9r9KuPz+zcc4/+7TVl6ZTPSOibevDny1OxDEUksWUeOw1x4gcWuL2wkUDu1pPFLetVJc7iGnlWWCFuKjuceKgWUsQKBgQDDK6QW9fgEwd5Z9FZsMqhdvS38U8CSS7AToHxd3/0mClsp4zW/RPMCgmLSMNZGoY5PVCTLRG+9UHYoD3cESWKYcH165eZmcAiswqLqmsXz+91a7wPLkPNAR3NrtoXiUt22nKXSOMefbnLkGuV8H5Pms4QOJ2bLssI/Z1q8VOdPxQKBgQC757c9JicU4A6oXadMq+5Q8MmTqdu7BHhdzs8u81UFjRNXmb9VZjKsNzq42JrSAaI3PxKNHOTBI16WTRO4oK6ai/ncMXe3fDDPhlh7FEA31ix8pWtUh41G5WPriDftJ5dYb8ROxx0o51f1kBnivjVXzQF1hqAnASNZxMSn9MOFVQKBgDNJlpdKD+EAKTQuVz+bmE751cnHIIXRSfX+aHAq2lVSx14cjRU0Qz/Xj6x4lPZ+oe0KyZh45Xw0Vrh0R+xidHxmozLWk1M8AauUgkdwYLCa8TJiOPcXojS0EgquLkSpTgYc6B50OVEWLy7uOnwBuFYZuowVopfI4+7RdDClhxuZAoGBAKSEiGa9lNIuKic3eFck4Y90gskKmxvvGgPoInYVDofBsQGt11vKEuu9n2hgHY1paMG+M1vhxFdlxFcMPQwVg0pE7AriNNMtpP7NvYJlhKRenKy1mHd2Bzffd9Csf2rgkx4XHWVr6Bf8FYMnG8Lujn3FPFXN/UKpOG5MXXJAVz/RAoGBAI5lqxdwy5gk8Veo7E3z0OCUEM+U62EAKVMcG3N35wFR+WM9jUx6M1Icozr6Gr9LZ4F/JXGzw9FXzpEntJ00K8t/5nxquTg7N0h6BqqwF3LIe6oJFg5ukIZNOAP6IcZ0EvZqnCxDSU25XkRjwWz7Ji+vAiFVG0ZBMDKaaQV9pQmA";

    /** 支付宝公钥（从沙箱控制台复制） */
    private final String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAygoQu6ZOVw9BJboBTp615PeWQ/npm0CKVCRpffWf43T4DqiqB1NUD4+mtl3BozS361iK49mxdNPUCNPVxXYR8lS7uEcT11JL3l4uuzdA85oyLsjoP/YrWMdMd/yzuzR02d/szuG6GdVmDO1ZJl0Gj0KwJVguhlMDz72x+zgLfRcFqVd8GvnnIKFmyvDW901p9UiwUFQZdnlkim2F/2Uiw1+fsyIn1jeWwppSusX1rzcZK1KHNprJyf5hU5ppNO2cMcmrtoeHj1kyOg05e19nm8RryGSr4RNj70ed/wcYMJOxM/1EG1gUJrdoa+Nlv28EkclHTWYke+ioTnUYnKJmPQIDAQAB";

    /** 字符编码 */
    private final String CHARSET = "UTF-8";

    /** 沙箱网关地址 */
    private final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    /** 返回格式 */
    private final String FORMAT = "JSON";

    /** 签名方式 */
    private final String SIGN_TYPE = "RSA2";

    /** 支付宝异步通知路径（⚠️ 必须为公网地址，支付宝服务器主动回调，不能用 localhost） */
    private final String NOTIFY_URL = "http://s27e4da7.natappfree.cc/api/alipay/notify";

    /** 支付宝同步跳转路径（支付完成后浏览器跳转，可以改用 localhost） */
    private final String RETURN_URL = "http://localhost:8080/api/alipay/return";

    private AlipayClient alipayClient = null;

    // ==================== 支付 ====================

    /**
     * 发起支付宝电脑网站支付
     *
     * @param outTradeNo  商户订单号（唯一）
     * @param totalAmount 支付金额（元）
     * @param subject     商品名称/订单标题
     * @return 支付宝返回的 HTML 表单字符串，前端直接输出即可跳转支付宝
     * @throws AlipayApiException 支付宝 API 异常
     */
    public String sendRequestToAlipay(String outTradeNo, Float totalAmount, String subject) throws AlipayApiException {
        // 去除可能的首尾空格，避免支付宝报 SYSTEM_ERROR
        outTradeNo = outTradeNo == null ? "" : outTradeNo.trim();
        subject = subject == null ? "" : subject.trim();
        // 初始化 AlipayClient
        alipayClient = new DefaultAlipayClient(GATEWAY_URL, APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);

        // 构造请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(RETURN_URL);
        alipayRequest.setNotifyUrl(NOTIFY_URL);

        // 金额格式化为两位小数（支付宝要求）
        String amountStr = String.format("%.2f", totalAmount);

        // 业务参数（不加 time_expire，让支付宝用默认超时）
        String bizContent = "{\"out_trade_no\":\"" + outTradeNo + "\","
                + "\"total_amount\":\"" + amountStr + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}";

        System.out.println("[支付宝] ========== 支付请求参数 ==========");
        System.out.println("[支付宝] out_trade_no: [" + outTradeNo + "]");
        System.out.println("[支付宝] total_amount: " + amountStr);
        System.out.println("[支付宝] subject: [" + subject + "]");
        System.out.println("[支付宝] bizContent: " + bizContent);

        alipayRequest.setBizContent(bizContent);

        // 请求支付宝
        String result = alipayClient.pageExecute(alipayRequest).getBody();
        // 沙箱环境有时在签名参数里用 http，强制替换为 https 避免 504 错误
        result = result.replace("http://openapi-sandbox.dl.alipaydev.com", "https://openapi-sandbox.dl.alipaydev.com");
        result = result.replace("http://s27e4da7.natappfree.cc", "https://s27e4da7.natappfree.cc");
        System.out.println("[支付宝] 支付请求返回：" + result);
        return result;
    }

    // ==================== 查询订单 ====================

    /**
     * 通过商户订单号查询支付宝交易状态
     *
     * @param outTradeNo 商户订单号
     * @return 支付宝返回的 JSON 响应体
     */
    public String query(String outTradeNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        request.setBizContent(bizContent.toString());

        AlipayTradeQueryResponse response = null;
        String body = null;
        try {
            response = alipayClient.execute(request);
            body = response.getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return null;
        }
        if (response.isSuccess()) {
            System.out.println("[支付宝] 查询调用成功");
        } else {
            System.out.println("[支付宝] 查询调用失败");
        }
        return body;
    }

    // ==================== 获取配置（供退款等场景使用） ====================

    public AlipayConfig getAlipayConfig() {
        AlipayConfig alipayConfig = new AlipayConfig();
        alipayConfig.setServerUrl(GATEWAY_URL);
        alipayConfig.setAppId(APP_ID);
        alipayConfig.setPrivateKey(APP_PRIVATE_KEY);
        alipayConfig.setFormat(FORMAT);
        alipayConfig.setAlipayPublicKey(ALIPAY_PUBLIC_KEY);
        alipayConfig.setCharset(CHARSET);
        alipayConfig.setSignType(SIGN_TYPE);
        return alipayConfig;
    }

    public AlipayClient getAlipayClient() {
        return alipayClient;
    }

    // ==================== Getter（供验签等场景使用） ====================

    public String getAlipayPublicKey() { return ALIPAY_PUBLIC_KEY; }
    public String getCharset() { return CHARSET; }
    public String getSignType() { return SIGN_TYPE; }
}
