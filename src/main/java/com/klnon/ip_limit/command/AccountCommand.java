package com.klnon.ip_limit.command;

import com.klnon.ip_limit.PlayerDataManager;
import com.klnon.ip_limit.config.IpLimitConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AccountCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("account")
                // 设置账号
                .then(Commands.literal("set")
                        .then(Commands.argument("username", StringArgumentType.string())
                                .then(Commands.literal("main")
                                        .executes(context -> {
                                            String username = StringArgumentType.getString(context, "username");
                                            context.getSource().sendSuccess(Component.literal("DEBUG: 正在处理 setMainAccount，用户名: " + username), false);
                                            return handleSetAccount(context.getSource(), username, 0, false);
                                        }))))
                // 设置优先级（管理员）
                .then(Commands.literal("level")
                        .requires(source -> source.hasPermission(2))  // 仅管理员权限使用
                        .then(Commands.argument("username", StringArgumentType.string())
                                .then(Commands.argument("priority", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> {
                                            String username = StringArgumentType.getString(context, "username");
                                            int priority = IntegerArgumentType.getInteger(context, "priority");
                                            return handleSetAccount(context.getSource(), username, priority, true);
                                        }))))
                // 列出当前玩家的账号列表
                .then(Commands.literal("list")
                        .executes(context -> listAccounts(context.getSource()))));
    }

    // 处理设置账号的逻辑（合并管理员和普通玩家逻辑）
    private static int handleSetAccount(CommandSourceStack source, String username, int priority, boolean isAdmin) {
        source.sendSuccess(Component.literal("DEBUG: 处理账号请求，用户名: " + username), false);

        PlayerDataManager.PlayerData mainPlayerData = getMainPlayerData(source, username);
        if (mainPlayerData == null) {
            source.sendFailure(Component.literal("DEBUG: 无法获取主账号数据。"));
            return Command.SINGLE_SUCCESS;
        }

        // 获取配置中的小号数量限制，管理员不受限制
        int maxAltAccounts = isAdmin ? Integer.MAX_VALUE : IpLimitConfig.MAX_ALT_ACCOUNTS.get();
        source.sendSuccess(Component.literal("DEBUG: 配置的最大小号数量: " + maxAltAccounts), false);

        // 确定可用账号的范围（主账号 + maxAltAccounts 个小号）
        List<String> availableAccounts = getAvailableAccounts(mainPlayerData, maxAltAccounts);
        source.sendSuccess(Component.literal("DEBUG: 可用账号: " + availableAccounts), false);

        // 如果 priority 为 0，表示这是设置主账号
        if (priority == 0) {
            setMainAccountLogic(mainPlayerData, username);
            PlayerDataManager.savePlayerDataToFile();
            source.sendSuccess(Component.literal("DEBUG: 已设置 " + username + " 为主账号。"), true);
            return Command.SINGLE_SUCCESS;
        }

        // 如果 username 不在可用账号范围内，阻止操作
        if (!availableAccounts.contains(username)) {
            source.sendFailure(Component.literal("DEBUG: 账号 " + username + " 超出可用账号限制。"));
            return Command.SINGLE_SUCCESS;
        }

        // 否则处理优先级
        if (priority < 1 || priority > 100) {
            source.sendFailure(Component.literal("优先级必须在 1 到 100 之间。"));
            return Command.SINGLE_SUCCESS;
        }

        setAccountPriorityLogic(mainPlayerData, username, priority, availableAccounts.size());
        PlayerDataManager.savePlayerDataToFile();
        source.sendSuccess(Component.literal(username + " 的优先级已设置为 " + priority + "。"), true);

        return Command.SINGLE_SUCCESS;
    }

    // 列出当前玩家的主账号和子账号
    private static int listAccounts(CommandSourceStack source) throws CommandSyntaxException {
        return handleAccountListOrPriority(source, source.getPlayerOrException().getName().getString());
    }

    // 统一处理列出账号的逻辑
    private static int handleAccountListOrPriority(CommandSourceStack source, String targetPlayer) {
        source.sendSuccess(Component.literal("DEBUG: 处理账号请求，目标玩家: " + targetPlayer), false);

        PlayerDataManager.PlayerData mainPlayerData = getMainPlayerData(source, targetPlayer);
        if (mainPlayerData == null) {
            source.sendFailure(Component.literal("DEBUG: 无法获取主账号数据。"));
            return Command.SINGLE_SUCCESS;
        }

        // 获取配置中的小号数量限制
        int maxAltAccounts = IpLimitConfig.MAX_ALT_ACCOUNTS.get();
        source.sendSuccess(Component.literal("DEBUG: 配置的最大小号数量: " + maxAltAccounts), false);

        // 确定可用账号的范围（主账号 + maxAltAccounts 个小号）
        List<String> availableAccounts = getAvailableAccounts(mainPlayerData, maxAltAccounts);
        source.sendSuccess(Component.literal("DEBUG: 可用账号: " + availableAccounts), false);

        return listAvailableAccounts(source, availableAccounts);
    }

    // 处理列出可用账号的逻辑
    private static int listAvailableAccounts(CommandSourceStack source, List<String> availableAccounts) {
        StringBuilder accountList = new StringBuilder("主账号: " + availableAccounts.get(0) + "\n子账号:\n");

        for (int i = 1; i < availableAccounts.size(); i++) {
            accountList.append(" - ").append(availableAccounts.get(i)).append("\n");
        }

        source.sendSuccess(Component.literal(accountList.toString()), false);
        return Command.SINGLE_SUCCESS;
    }

    // 处理设定主账号的逻辑
    private static void setMainAccountLogic(PlayerDataManager.PlayerData mainPlayerData, String username) {
        mainPlayerData.accounts.remove(username);
        mainPlayerData.accounts.add(0, username);  // 将主账号放在首位
    }

    // 处理设定优先级的逻辑
    private static void setAccountPriorityLogic(PlayerDataManager.PlayerData mainPlayerData, String username, int priority, int availableSize) {
        mainPlayerData.accounts.remove(username);
        mainPlayerData.accounts.add(Math.min(priority, availableSize), username);  // 根据优先级插入
    }

    // 通用的获取主账号和主账号数据的方法，减少重复代码
    private static PlayerDataManager.PlayerData getMainPlayerData(CommandSourceStack source, String username) {
        String mainAccount = PlayerDataManager.getMainAccountFor(username);
        if (mainAccount == null) {
            source.sendFailure(Component.literal("该用户未关联到任何主账号！"));
            return null;
        }

        PlayerDataManager.PlayerData mainPlayerData = PlayerDataManager.getPlayerData(mainAccount);
        if (mainPlayerData == null) {
            source.sendFailure(Component.literal("主账号数据不存在！"));
            return null;
        }

        if (!mainPlayerData.accounts.contains(username)) {
            source.sendFailure(Component.literal("用户名 " + username + " 未关联到主账号 " + mainAccount + "。"));
            return null;
        }

        return mainPlayerData;
    }

    // 获取可用账号列表（主账号 + maxAltAccounts 个子账号）
    private static List<String> getAvailableAccounts(PlayerDataManager.PlayerData playerData, int maxAltAccounts) {
        List<String> availableAccounts = new ArrayList<>();

        if (!playerData.accounts.isEmpty()) {
            availableAccounts.add(playerData.accounts.get(0));  // 主账号
        }

        int availableAltCount = Math.min(maxAltAccounts, playerData.accounts.size() - 1);
        for (int i = 1; i <= availableAltCount; i++) {
            availableAccounts.add(playerData.accounts.get(i));
        }

        return availableAccounts;
    }
}
