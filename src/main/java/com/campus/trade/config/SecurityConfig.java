package com.campus.trade.config;

import com.campus.trade.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                // 公开访问的静态资源和页面
                .antMatchers("/", "/index.html", "/favicon.ico").permitAll()
                .antMatchers("/css/**", "/js/**", "/img/**").permitAll()
                // 上传文件可公开访问
                .antMatchers("/uploads/**").permitAll()
                // 用户认证相关（注册、登录、发送验证码）
                .antMatchers("/api/user/register", "/api/user/login", "/api/user/send-code").permitAll()
                // 支付宝支付回调（同步返回和异步通知）
                .antMatchers("/api/alipay/return", "/api/alipay/notify").permitAll()
                // 商品列表和详情可以匿名访问
                .antMatchers("/api/goods/list", "/api/goods/**").permitAll()
                // 获取评价评分可以公开访问
                .antMatchers("/api/evaluation/rating/**").permitAll()
                // 文件上传需要登录
                .antMatchers("/api/upload/**").authenticated()
                .antMatchers("/api/favorite/**").authenticated()  // 收藏功能
                .antMatchers("/api/order/**").authenticated()  // 订单功能
                .antMatchers("/api/message/**").authenticated()  // 消息功能
                .antMatchers("/api/wallet/**").authenticated()  // 钱包功能
                .antMatchers("/api/evaluation/**").authenticated()  // 评价功能
                // 管理员接口
                .antMatchers("/api/admin/**").hasRole("admin")
                // 其他所有请求需要认证
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}