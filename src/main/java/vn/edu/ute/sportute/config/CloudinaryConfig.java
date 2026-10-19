package vn.edu.ute.sportute.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:${CLOUDINARY_CLOUD_NAME:}}")
    private String cloudName;

    @Value("${cloudinary.api-key:${CLOUDINARY_API_KEY:}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:${CLOUDINARY_API_SECRET:}}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", (cloudName != null && !cloudName.isBlank()) ? cloudName : "sporthub-ute");
        config.put("api_key", (apiKey != null && !apiKey.isBlank()) ? apiKey : "123456789012345");
        config.put("api_secret", (apiSecret != null && !apiSecret.isBlank()) ? apiSecret : "demo-api-secret");
        config.put("secure", "true");
        return new Cloudinary(config);
    }
}
