package com.klnon.ip_limit.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class IpLimitConfig {
    // 定义静态的配置构建器和配置规范
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec SPEC;

    // 定义具体的配置项，最大允许的小号数量
    public static final ForgeConfigSpec.IntValue MAX_ALT_ACCOUNTS;

    // 初始化配置项
    static {
        // 创建配置文件的段落
        BUILDER.push("ip_limit_config");

        // 定义最大小号数量的配置项，默认为3，最小为1，最大为100
        MAX_ALT_ACCOUNTS = BUILDER
                .comment("最大允许的小号数量。")
                .defineInRange("maxAltAccounts", 3, 1, 100);

        // 结束配置段落
        BUILDER.pop();

        // 构建配置规范
        SPEC = BUILDER.build();
    }

    // 提供获取配置的方法
    public static ForgeConfigSpec getConfig() {
        return SPEC;
    }
}
