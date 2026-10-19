package vn.edu.ute.sportute.integration.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    private static final List<String> ALLOWED_IMAGE_MIME = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Upload ảnh lên Cloudinary với kiểm tra quyền, định dạng MIME và kích thước
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File tải lên không được để trống!");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file vượt quá giới hạn 10MB!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Định dạng file không hợp lệ! Chỉ chấp nhận JPG, PNG, WEBP, GIF.");
        }

        // Kiểm tra xem có cấu hình cloud name thật hay dùng local fallback
        if (cloudName != null && !cloudName.isBlank() && !cloudName.equals("sporthub-ute")) {
            try {
                Map<String, Object> params = ObjectUtils.asMap(
                        "folder", "sporthub/" + (folder != null ? folder : "services"),
                        "resource_type", "image",
                        "overwrite", true
                );
                Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
                String secureUrl = (String) uploadResult.get("secure_url");
                log.info("Upload ảnh lên Cloudinary thành công: {}", secureUrl);
                return secureUrl;
            } catch (Exception e) {
                log.error("Lỗi khi upload Cloudinary: {}. Chuyển sang lưu trữ local dự phòng.", e.getMessage());
            }
        }

        // Local Storage Fallback
        return saveLocalFile(file.getBytes(), file.getOriginalFilename(), folder != null ? folder : "services");
    }

    /**
     * Upload hóa đơn PDF lên đám mây Cloudinary
     */
    public String uploadPdf(byte[] pdfBytes, String invoiceCode) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("Nội dung PDF rỗng!");
        }

        if (cloudName != null && !cloudName.isBlank() && !cloudName.equals("sporthub-ute")) {
            try {
                Map<String, Object> params = ObjectUtils.asMap(
                        "folder", "sporthub/invoices",
                        "public_id", "Invoice_" + invoiceCode,
                        "resource_type", "raw",
                        "overwrite", true
                );
                Map<?, ?> uploadResult = cloudinary.uploader().upload(pdfBytes, params);
                String secureUrl = (String) uploadResult.get("secure_url");
                log.info("Lưu trữ hóa đơn PDF lên Cloudinary thành công: {}", secureUrl);
                return secureUrl;
            } catch (Exception e) {
                log.error("Lỗi lưu PDF lên Cloudinary: {}", e.getMessage());
            }
        }

        return saveLocalFile(pdfBytes, "Invoice_" + invoiceCode + ".pdf", "invoices");
    }

    /**
     * Upload ảnh đại diện khách hàng lên đám mây Cloudinary theo customerId
     */
    public String uploadAvatar(byte[] imageBytes, String customerId) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Dữ liệu ảnh đại diện rỗng!");
        }

        if (cloudName != null && !cloudName.isBlank() && !cloudName.equals("sporthub-ute")) {
            try {
                Map<String, Object> params = ObjectUtils.asMap(
                        "folder", "sporthub/avatars",
                        "public_id", customerId,
                        "resource_type", "image",
                        "overwrite", true,
                        "invalidate", true
                );
                Map<?, ?> uploadResult = cloudinary.uploader().upload(imageBytes, params);
                String secureUrl = (String) uploadResult.get("secure_url");
                log.info("[CLOUDINARY] Upload avatar thành công cho khách hàng {}: {}", customerId, secureUrl);
                return secureUrl;
            } catch (Exception e) {
                log.error("[CLOUDINARY] Lỗi upload avatar lên Cloudinary cho {}: {}. Chuyển sang lưu local dự phòng.", customerId, e.getMessage());
            }
        }

        return null;
    }

    /**
     * Xóa ảnh trên Cloudinary theo public ID
     */
    public boolean deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) return false;
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equalsIgnoreCase((String) result.get("result"));
        } catch (Exception e) {
            log.error("Không thể xóa ảnh Cloudinary publicId {}: {}", publicId, e.getMessage());
            return false;
        }
    }

    private String saveLocalFile(byte[] data, String originalName, String folder) throws IOException {
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString().substring(0, 8) + ext;

        Path targetDir = Paths.get("src/main/resources/static/uploads/" + folder);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetPath = targetDir.resolve(fileName);
        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.write(data);
        }

        // Đồng bộ sang target/classes
        Path runtimeDir = Paths.get("target/classes/static/uploads/" + folder);
        if (!Files.exists(runtimeDir)) {
            Files.createDirectories(runtimeDir);
        }
        Files.copy(targetPath, runtimeDir.resolve(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        log.info("Lưu file cục bộ thành công: /uploads/{}/{}", folder, fileName);
        return "/uploads/" + folder + "/" + fileName;
    }
}
