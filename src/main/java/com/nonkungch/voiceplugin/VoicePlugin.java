package com.nonkungch.voiceplugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;

public class VoicePlugin extends JavaPlugin {

    // 💡 ตั้งค่า: URL ของ Node.js Voice Stream Server
    private final String VOICE_SERVER_API = "http://localhost:8080/register_voice_user";
    // 💡 การจัดเก็บ Token: ในโค้ดจริง ควรใช้ Map/Database หรือ API ภายนอก
    private final java.util.Map<String, String> playerTokens = new java.util.HashMap<>();

    @Override
    public void onEnable() {
        // 1. สร้างไฟล์ HTML Client
        createClientHtmlFile("voice_client_app.html");

        // 2. ลงทะเบียนคำสั่ง
        getCommand("linkvoice").setExecutor(new VoiceLinkCommand(this));

        // 3. เริ่ม Task อัปเดตตำแหน่งทุก 1 วินาที
        startLocationUpdateTask();

        getLogger().info("VoiceChatPlugin: เริ่มทำงานและพร้อมใช้งานแล้ว!");
    }

    // 💡 ฟังก์ชันสำหรับคัดลอกไฟล์ HTML ออกจาก Jar
    private void createClientHtmlFile(String fileName) {
        File targetFile = new File(getDataFolder(), fileName);
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        if (!targetFile.exists()) {
            try (InputStream inputStream = getResource(fileName)) {
                if (inputStream == null) {
                    getLogger().severe("❌ ไม่พบไฟล์ " + fileName + " ในทรัพยากร Jar!");
                    return;
                }
                Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                getLogger().info("✅ สร้างไฟล์ " + fileName + " ที่: " + targetFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().severe("❌ เกิดข้อผิดพลาดในการคัดลอกไฟล์ " + fileName + ": " + e.getMessage());
            }
        }
    }
    
    // 💡 Task สำหรับอัปเดตตำแหน่งผู้เล่นที่ลงทะเบียนแล้ว
    private void startLocationUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    String token = playerTokens.get(player.getName());
                    // ส่งข้อมูลเฉพาะผู้เล่นที่มี Token (ลงทะเบียนแล้ว)
                    if (token != null) { 
                        org.bukkit.Location loc = player.getLocation();
                        sendLocationUpdate(
                            player.getName(), token, 
                            loc.getX(), loc.getY(), loc.getZ()
                        );
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 20L, 20L); // ทุก 1 วินาที
    }
    
    // 💡 ฟังก์ชันส่งข้อมูลตำแหน่งไปยัง Voice Stream Server
    private void sendLocationUpdate(String username, String token, double x, double y, double z) {
        try {
            URL url = new URL(VOICE_SERVER_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = String.format(
                "{\"username\": \"%s\", \"token\": \"%s\", \"x\": %f, \"y\": %f, \"z\": %f}",
                username, token, x, y, z
            );
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                getLogger().warning("ไม่สามารถอัปเดตตำแหน่ง Voice Server ได้: " + conn.getResponseCode());
            }

        } catch (Exception e) {
            getLogger().severe("❌ ไม่สามารถเชื่อมต่อ Voice Server ได้: " + e.getMessage());
        }
    }
    
    // 💡 Getters/Setters สำหรับ Token (ใช้ใน VoiceLinkCommand)
    public void setPlayerToken(String username, String token) {
        playerTokens.put(username, token);
    }
    public String getPlayerToken(String username) {
        return playerTokens.get(username);
    }
                }
