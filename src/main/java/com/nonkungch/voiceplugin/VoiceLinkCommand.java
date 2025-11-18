package com.nonkungch.voiceplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class VoiceLinkCommand implements CommandExecutor {

    // 💡 ตั้งค่า: URL ของ Web Server API สำหรับยืนยันโค้ด
    private final String WEB_VERIFY_URL = "http://localhost:8080/verify_code";
    private final VoicePlugin plugin;

    public VoiceLinkCommand(VoicePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "คำสั่งนี้สำหรับผู้เล่นเท่านั้น!");
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(ChatColor.YELLOW + "วิธีใช้: /linkvoice <code>");
            player.sendMessage(ChatColor.YELLOW + "รับโค้ด 6 หลักได้จาก: " + ChatColor.AQUA + "http://localhost:8080/");
            return true;
        }

        String inputCode = args[0];
        
        // 💡 ส่งโค้ดไปยืนยันแบบ Asynchronous
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            VerificationResult result = verifyCodeWithWebServer(player.getName(), inputCode);
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (result.success) {
                    // บันทึก Token ที่ได้รับ
                    plugin.setPlayerToken(player.getName(), result.token); 
                    
                    player.sendMessage(ChatColor.GREEN + "✅ เชื่อมต่อ Voice Chat สำเร็จ!");
                    player.sendMessage(ChatColor.YELLOW + "นำ Token นี้ไปใส่ใน Web Client:");
                    player.sendMessage(ChatColor.AQUA + "Token: " + ChatColor.WHITE + result.token);
                    player.sendMessage(ChatColor.GRAY + "Client App: http://localhost:8080/voice_client_app.html");
                } else {
                    player.sendMessage(ChatColor.RED + "❌ " + result.message);
                }
            });
        });
        return true;
    }
    
    private VerificationResult verifyCodeWithWebServer(String playerName, String code) {
        try {
            URL url = new URL(WEB_VERIFY_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = String.format("{\"username\": \"%s\", \"code\": \"%s\"}", playerName, code);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // อ่าน Token จาก Response
                try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
                    String responseBody = scanner.useDelimiter("\\A").next();
                    // 💡 ต้องใช้ JSON Library เพื่อ Parse Token จริง
                    // ในตัวอย่างนี้ เราจะใช้การ Parse แบบง่าย: สมมติว่า Token อยู่ใน response
                    String token = responseBody.substring(responseBody.indexOf("voice_token\":\"") + 14, responseBody.indexOf("\"}"));

                    return new VerificationResult(true, "Token ได้รับแล้ว", token);
                }
            } else {
                return new VerificationResult(false, "โค้ดไม่ถูกต้องหรือหมดอายุ", null);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("เกิดข้อผิดพลาดในการเชื่อมต่อ Web Server: " + e.getMessage());
            return new VerificationResult(false, "ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ได้", null);
        }
    }
    
    // คลาสสำหรับเก็บผลลัพธ์
    private static class VerificationResult {
        public final boolean success;
        public final String message;
        public final String token;
        public VerificationResult(boolean success, String message, String token) {
            this.success = success;
            this.message = message;
            this.token = token;
        }
    }
              }
