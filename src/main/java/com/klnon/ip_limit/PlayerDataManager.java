package com.klnon.ip_limit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class PlayerDataManager {
    private static final String DIRECTORY_PATH = "config/playerlimit";
    private static final String FILE_PATH = DIRECTORY_PATH + "/player_data.json";
    private static final Logger LOGGER = LogManager.getLogger();
    private static Map<String, PlayerData> playerDataMap = new HashMap<>();
    private static final Map<String, String> ipToMainAccountMap = new HashMap<>(); // IP 对应的主账号

    public static class PlayerData {
        public List<String> accounts = new ArrayList<>(); // 维护有序的账号列表
        public List<String> ips = new ArrayList<>();

        public PlayerData(String mainAccount, String ip) {
            this.accounts.add(mainAccount); // 主账号第一个
            this.ips.add(ip);
        }
    }

    // 检查并创建目录
    private static void createDirectoryIfNotExists() {
        File directory = new File(DIRECTORY_PATH);
        if (!directory.exists()) {
            boolean result = directory.mkdirs();
            if (result) {
                LOGGER.info("成功创建目录: " + DIRECTORY_PATH);
            } else {
                LOGGER.error("无法创建目录: " + DIRECTORY_PATH);
            }
        }
    }

    // 添加或更新玩家数据
    public static void addOrUpdatePlayer(String playerName, String ip) {
        String mainAccount = ipToMainAccountMap.computeIfAbsent(ip, k -> playerName);

        // 如果该 IP 没有被记录为主账号，设定当前玩家为主账号

        // 获取或创建主账号的玩家数据
        PlayerData mainAccountData = playerDataMap.get(mainAccount);
        if (mainAccountData == null) {
            mainAccountData = new PlayerData(mainAccount, ip);
            playerDataMap.put(mainAccount, mainAccountData);
        }

        // 更新主账号的数据：添加当前登录账号和新的 IP 地址
        if (!mainAccountData.accounts.contains(playerName)) {
            mainAccountData.accounts.add(playerName);  // 按顺序添加账号
        }

        if (!mainAccountData.ips.contains(ip)) {
            mainAccountData.ips.add(ip);               // 记录新的IP
        }

        // 更新 IP 对主账号的映射
        ipToMainAccountMap.put(ip, mainAccount);

        // 保存数据
        savePlayerDataToFile();
    }

    // 获取玩家数据 (根据 playerName 查找)
    public static PlayerData getPlayerData(String playerName) {
        // 遍历所有主账号数据，找到包含该用户名的 PlayerData
        for (Map.Entry<String, PlayerData> entry : playerDataMap.entrySet()) {
            if (entry.getValue().accounts.contains(playerName)) {
                return entry.getValue();  // 返回该用户名所在的 PlayerData
            }
        }
        return null; // 如果没有找到，返回 null
    }

    // 获取玩家数据 (根据 IP 查找)
    public static PlayerData getPlayerDataByIp(String ip) {
        String mainAccount = ipToMainAccountMap.get(ip); // 通过 IP 获取主账号名称
        if (mainAccount == null) {
            return null; // 如果没有找到与该 IP 关联的主账号，返回 null
        }
        return playerDataMap.get(mainAccount); // 返回主账号的数据
    }

    // 获取主账号名称 (根据小号查找)
    public static String getMainAccountFor(String username) {
        // 遍历所有玩家数据，找到包含该用户名的主账号
        for (Map.Entry<String, PlayerData> entry : playerDataMap.entrySet()) {
            if (entry.getValue().accounts.contains(username)) {
                return entry.getKey();  // 返回主账号名称
            }
        }
        return null;  // 如果没有找到，返回 null
    }

    // 保存玩家数据到文件
    public static void savePlayerDataToFile() {
        createDirectoryIfNotExists(); // 确保目录存在

        try (FileWriter writer = new FileWriter(FILE_PATH)) {
            Gson gson = new Gson();
            gson.toJson(playerDataMap, writer);
            LOGGER.info("玩家数据成功保存到文件: " + FILE_PATH);
        } catch (IOException e) {
            LOGGER.error("无法保存玩家数据到文件: " + FILE_PATH, e);
        }
    }

    // 从文件加载玩家数据
    public static void loadPlayerDataFromFile() {
        createDirectoryIfNotExists(); // 确保目录存在

        File file = new File(FILE_PATH);
        if (!file.exists()) {
            LOGGER.warn("数据文件不存在: " + FILE_PATH + "，将创建新的文件。");
            return;
        }

        try (FileReader reader = new FileReader(FILE_PATH)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, PlayerData>>() {}.getType();
            playerDataMap = gson.fromJson(reader, type);
            LOGGER.info("玩家数据成功从文件加载: " + FILE_PATH);
        } catch (IOException e) {
            LOGGER.error("无法从文件加载玩家数据: " + FILE_PATH, e);
        }
    }
}
