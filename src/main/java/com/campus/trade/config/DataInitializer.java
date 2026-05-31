package com.campus.trade.config;

import com.campus.trade.goods.entity.Goods;
import com.campus.trade.goods.repository.GoodsRepository;
import com.campus.trade.message.entity.Message;
import com.campus.trade.message.repository.MessageRepository;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final GoodsRepository goodsRepository;
    private final BCryptPasswordEncoder encoder;
    private final MessageRepository messageRepository;

    public DataInitializer(UserRepository ur, GoodsRepository gr, BCryptPasswordEncoder e,
                          MessageRepository mr) {
        userRepository = ur; goodsRepository = gr; encoder = e;
        messageRepository = mr;
    }

    @Override
    public void run(String... args) {
        // 给所有已有用户补发系统欢迎消息（如果还没有系统消息的话）
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            long msgCount = messageRepository.countByToUserIdAndIsRead(u.getId(), 0);
            long total = messageRepository.findByToUserIdOrderByCreatedAtDesc(u.getId()).size();
            if (total == 0) {
                // 该用户还没有任何系统消息，发送欢迎消息
                sendWelcomeMessage(u.getId());
            }
        }

        if (!userRepository.existsByUsername("admin")) {
            var admin = userRepository.save(User.builder().username("admin")
                    .passwordHash(encoder.encode("admin123456")).phone("13800000000")
                    .studentId("ADMIN001").realName("管理员").department("信息中心")
                    .role("admin").authStatus(1).creditScore(100)
                    .balance(new BigDecimal("300.00")).build());
            sendWelcomeMessage(admin.getId());
            log.info("Admin created: admin / admin123456 (balance: 300)");
        }
        if (!userRepository.existsByUsername("alice")) {
            var u = userRepository.save(User.builder().username("alice")
                    .passwordHash(encoder.encode("abc123456")).phone("13812345678")
                    .studentId("2024001").realName("爱丽丝").department("计算机学院")
                    .role("user").authStatus(1).creditScore(95)
                    .balance(new BigDecimal("200.00")).build());
            goodsRepository.save(Goods.builder().userId(u.getId()).title("高等数学第七版 九成新")
                    .description("考研用书，几乎全新，只有前几页有笔记").category("教材教辅")
                    .price(new BigDecimal("25.00")).contactPhone("13812345678").status("onsale").build());
            goodsRepository.save(Goods.builder().userId(u.getId()).title("惠普计算器 HP-39GS")
                    .description("工科必备，功能完好，带原装保护壳").category("电子数码")
                    .price(new BigDecimal("60.00")).contactPhone("13812345678").status("onsale").build());
            sendWelcomeMessage(u.getId());
            log.info("Test user created: alice / abc123456 (balance: 200)");
        }
        if (!userRepository.existsByUsername("bob")) {
            var u = userRepository.save(User.builder().username("bob")
                    .passwordHash(encoder.encode("abc123456")).phone("13987654321")
                    .studentId("2024112").realName("鲍勃").department("经济管理学院")
                    .role("user").authStatus(0).creditScore(100)
                    .balance(new BigDecimal("200.00")).build());
            goodsRepository.save(Goods.builder().userId(u.getId()).title("全新雅思真题集 剑18")
                    .description("全新未做，随书附赠听力光盘").category("教材教辅")
                    .price(new BigDecimal("80.00")).contactPhone("13987654321").status("onsale").build());
            sendWelcomeMessage(u.getId());
            log.info("Test user created: bob / abc123456 (balance: 200)");
        }
    }

    /** 发送系统欢迎消息 */
    private void sendWelcomeMessage(Long userId) {
        Message msg = Message.builder()
                .fromUserId(0L)
                .toUserId(userId)
                .content("【系统通知】欢迎使用校园二手交易平台！如有任何问题，请联系客服。")
                .msgType(1)
                .isRead(0)
                .build();
        messageRepository.save(msg);
    }
}