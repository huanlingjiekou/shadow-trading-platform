package com.shadowtradingplatform.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * stp-trade 模块启动类.
 *
 * <p>包路径：{@code com.shadowtradingplatform.trade}</p>
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.shadowtradingplatform.trade.**.mapper")
public class StpTradeApplication {

    public static void main(String[] args) {
        SpringApplication.run(StpTradeApplication.class, args);
        System.out.println("========== stp-trade 启动成功 ==========");
    }
}
