package com.klnon.ip_limit;

import com.klnon.ip_limit.command.AccountCommand;
import com.klnon.ip_limit.config.IpLimitConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

@Mod("ip_limit")
public class IpLimitMod {
    public static final String MODID = "ip_limit";

    public IpLimitMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        // 自定义配置路径
        Path configPath = Paths.get("playerlimit");
        createDirectoryIfNotExists(configPath);  // 确保目录存在

        // 注册自定义配置文件路径
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IpLimitConfig.getConfig(), configPath.resolve("ip_limit.toml").toString());

        // 注册设置事件
        bus.addListener(this::setup);

    }

    private void setup(final FMLCommonSetupEvent event) {
        // 在 mod 加载时加载玩家数据
        PlayerDataManager.loadPlayerDataFromFile();

        // 注册服务器启动事件
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        // 注册登录处理事件
        MinecraftForge.EVENT_BUS.register(PlayerLoginHandler.class);
        // 注册命令
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        AccountCommand.register(event.getDispatcher());
    }

    // 在服务器启动时执行操作（可选）
    private void onServerStarting(ServerStartingEvent event) {
        // 服务器启动时的操作
        System.out.println("小号限制正在启动...");
    }

    // 检查并创建目录
    private void createDirectoryIfNotExists(Path path) {
        File directory = new File("config/" + path.toString());
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (result) {
                System.out.println("成功创建目录: " + directory.getPath());
            } else {
                System.err.println("无法创建目录: " + directory.getPath());
            }
        }
    }
}
