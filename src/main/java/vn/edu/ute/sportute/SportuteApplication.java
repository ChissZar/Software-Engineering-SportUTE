package vn.edu.ute.sportute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class SportuteApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(SportuteApplication.class, args);
    }

    /**
     * Tự động đọc file .env ở thư mục gốc (nếu có) và nạp vào System Properties
     * Giúp thuận tiện cấu hình email, mật khẩu mà không cần cài thêm thư viện ngoài.
     */
    private static void loadDotEnvIfPresent() {
        File envFile = new File(".env");
        if (!envFile.exists() || !envFile.isFile()) {
            envFile = new File(System.getProperty("user.dir"), ".env");
        }

        if (envFile.exists() && envFile.isFile()) {
            System.out.println("[SPORTHUB .ENV] Tìm thấy file cấu hình tại: " + envFile.getAbsolutePath());
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String val = line.substring(idx + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                        System.setProperty(key, val);
                        boolean isSecret = key.toLowerCase().contains("password") || key.toLowerCase().contains("secret");
                        System.out.println("[SPORTHUB .ENV] Đã nạp: " + key + " = " + (isSecret ? "******" : val));
                    }
                }
            } catch (Exception e) {
                System.err.println("[SPORTHUB .ENV] Lỗi đọc file .env: " + e.getMessage());
            }
        } else {
            System.out.println("[SPORTHUB .ENV] Không tìm thấy file .env tại thư mục làm việc.");
        }
    }
}
