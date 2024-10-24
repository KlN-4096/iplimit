package com.klnon.ip_limit;

import com.klnon.ip_limit.config.IpLimitConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.net.InetSocketAddress;

@Mod.EventBusSubscriber(modid = IpLimitMod.MODID)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 获取玩家对象
        ServerPlayer player = (ServerPlayer) event.getEntity();
        String playerName = player.getName().getString(); // 获取玩家名称

        // 获取远程地址，解析出IP
        InetSocketAddress address = (InetSocketAddress) player.connection.getConnection().getRemoteAddress();
        String playerIp = address.getAddress().getHostAddress(); // 获取玩家的纯 IP 地址

        // 更新玩家数据（包括其 IP 和可能的关联小号）
        PlayerDataManager.addOrUpdatePlayer(playerName, playerIp);

        // 获取与该 IP 关联的主账号数据，而不是通过 playerName
        PlayerDataManager.PlayerData playerData = PlayerDataManager.getPlayerDataByIp(playerIp); // 修改为通过 IP 查找

        // 如果 playerData 为 null，记录错误并退出
        if (playerData == null) {
            player.connection.disconnect(Component.literal("无法获取玩家数据。请联系管理员！"));
            return;
        }

        // 获取配置中的小号数量限制
        int maxAltAccounts = IpLimitConfig.MAX_ALT_ACCOUNTS.get();

        // 确保主账号总是可以登录
        if (!playerData.accounts.contains(playerName)) {
            player.connection.disconnect(Component.literal("未能正确获取玩家账号信息。请联系管理员！"));
            return;
        }

        // 假设主账号是第一个登录的玩家
        String mainAccount = playerData.accounts.get(0);

        // 获取子账号数量（不包含主账号）
        int altAccountCount = playerData.accounts.size() - 1;

        // 检查当前登录的玩家是否是主账号
        if (!playerName.equals(mainAccount)) {
            // 如果是小号，按顺序允许前 maxAltAccounts 个小号登录
            if (altAccountCount > maxAltAccounts) {
                // 检查当前小号是否在允许登录的范围之外
                int accountIndex = playerData.accounts.indexOf(playerName);
                if (accountIndex > maxAltAccounts) {
                    player.connection.disconnect(Component.literal("你的小号数量已超出限制，无法登录此小号！"));
                }
            }
        }

        // 如果是主账号或者子账号在允许的范围内，允许继续登录
    }
}
