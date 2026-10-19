package vn.edu.ute.sportute.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.ute.sportute.integration.cloudinary.CloudinaryStorageService;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerUserDataService {

    private final CloudinaryStorageService cloudinaryStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File dataDir = new File("uploads");
    private final File avatarsDir = new File("uploads/avatars");
    private final File favoritesFile = new File("uploads/favorites.json");
    private final File avatarsFile = new File("uploads/avatars.json");

    private final Map<String, Set<String>> customerFavorites = new ConcurrentHashMap<>();
    private final Map<String, String> customerAvatars = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs();
            }
            if (favoritesFile.exists()) {
                Map<String, List<String>> loaded = objectMapper.readValue(favoritesFile, new TypeReference<Map<String, List<String>>>() {});
                if (loaded != null) {
                    loaded.forEach((k, v) -> customerFavorites.put(k, new LinkedHashSet<>(v)));
                }
                log.info("[USER DATA] Loaded favorites for {} customers from disk", customerFavorites.size());
            }
            if (avatarsFile.exists()) {
                Map<String, String> loadedAvatars = objectMapper.readValue(avatarsFile, new TypeReference<Map<String, String>>() {});
                if (loadedAvatars != null) {
                    customerAvatars.putAll(loadedAvatars);
                }
                log.info("[USER DATA] Loaded avatars for {} customers from disk", customerAvatars.size());
            }
        } catch (Exception e) {
            log.error("[USER DATA] Failed to load data from disk: {}", e.getMessage());
        }
    }

    public synchronized List<String> getFavorites(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> favs = customerFavorites.get(customerId);
        if (favs == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(favs);
    }

    public synchronized List<String> toggleFavorite(String customerId, String courtId) {
        if (customerId == null || customerId.isBlank() || courtId == null || courtId.isBlank()) {
            return Collections.emptyList();
        }
        Set<String> favs = customerFavorites.computeIfAbsent(customerId, k -> new LinkedHashSet<>());
        if (favs.contains(courtId)) {
            favs.remove(courtId);
        } else {
            favs.add(courtId);
        }
        persistFavoritesToDisk();
        return new ArrayList<>(favs);
    }

    public synchronized boolean isFavorite(String customerId, String courtId) {
        if (customerId == null || courtId == null) return false;
        Set<String> favs = customerFavorites.get(customerId);
        return favs != null && favs.contains(courtId);
    }

    private synchronized void persistFavoritesToDisk() {
        try {
            if (!dataDir.exists()) dataDir.mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(favoritesFile, customerFavorites);
        } catch (Exception e) {
            log.error("[USER DATA] Failed to persist favorites to disk: {}", e.getMessage());
        }
    }

    private synchronized void persistAvatarsToDisk() {
        try {
            if (!dataDir.exists()) dataDir.mkdirs();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(avatarsFile, customerAvatars);
        } catch (Exception e) {
            log.error("[USER DATA] Failed to persist avatars to disk: {}", e.getMessage());
        }
    }

    public String saveAvatar(String customerId, String base64DataUrl) {
        if (customerId == null || customerId.isBlank() || base64DataUrl == null || base64DataUrl.isBlank()) {
            return null;
        }
        try {
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs();
            }
            String base64Image = base64DataUrl;
            if (base64DataUrl.contains(",")) {
                base64Image = base64DataUrl.substring(base64DataUrl.indexOf(",") + 1);
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // 1. Lưu bản sao cục bộ để backup & stream offline
            File targetFile = new File(avatarsDir, customerId + ".png");
            try (FileOutputStream fos = new FileOutputStream(targetFile)) {
                fos.write(imageBytes);
            }

            // 2. Tải lên Cloudinary đám mây nếu có cấu hình
            String cloudUrl = null;
            try {
                cloudUrl = cloudinaryStorageService.uploadAvatar(imageBytes, customerId);
            } catch (Exception e) {
                log.warn("[CLOUDINARY] Upload avatar thất bại, dùng fallback local: {}", e.getMessage());
            }

            String effectiveUrl;
            if (cloudUrl != null && !cloudUrl.isBlank()) {
                effectiveUrl = cloudUrl;
            } else {
                effectiveUrl = "/customer/api/avatar/" + customerId + "?t=" + System.currentTimeMillis();
            }

            customerAvatars.put(customerId, effectiveUrl);
            persistAvatarsToDisk();

            log.info("[USER DATA] Đã lưu avatar cho khách hàng {}: {}", customerId, effectiveUrl);
            return effectiveUrl;
        } catch (Exception e) {
            log.error("[USER DATA] Lỗi lưu avatar cho khách hàng {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    public String getAvatarUrl(String customerId) {
        if (customerId == null || customerId.isBlank()) return null;
        String url = customerAvatars.get(customerId);
        if (url != null && !url.isBlank()) {
            return url;
        }
        File file = new File(avatarsDir, customerId + ".png");
        if (file.exists() && file.length() > 0) {
            return "/customer/api/avatar/" + customerId + "?t=" + file.lastModified();
        }
        return null;
    }

    public byte[] getAvatarBytes(String customerId) {
        if (customerId == null || customerId.isBlank()) return null;
        File file = new File(avatarsDir, customerId + ".png");
        if (!file.exists()) return null;
        try {
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasAvatar(String customerId) {
        if (customerId == null || customerId.isBlank()) return false;
        if (customerAvatars.containsKey(customerId)) return true;
        File file = new File(avatarsDir, customerId + ".png");
        return file.exists() && file.length() > 0;
    }
}
