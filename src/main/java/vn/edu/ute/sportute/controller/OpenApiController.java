package vn.edu.ute.sportute.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class OpenApiController {

    @GetMapping(value = "/v3/api-docs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getApiDocs() {
        try {
            ClassPathResource resource = new ClassPathResource("openapi.json");
            String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("{\"error\": \"Không thể tải OpenAPI specification: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping(value = {"/swagger-ui.html", "/swagger-ui", "/swagger-ui/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String getSwaggerUi() {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>SportHub UTE - Tài liệu API Swagger / OpenAPI 3.0</title>
              <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui.css" />
              <link rel="preconnect" href="https://fonts.googleapis.com">
              <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
              <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">
              <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
              <style>
                body {
                  margin: 0;
                  padding: 0;
                  font-family: 'Plus Jakarta Sans', sans-serif !important;
                  background-color: #f8fafc;
                }
                .sporthub-header {
                  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
                  color: #fff;
                  padding: 16px 28px;
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  box-shadow: 0 4px 20px rgba(0,0,0,0.15);
                  border-bottom: 2px solid #10b981;
                }
                .sporthub-brand {
                  display: flex;
                  align-items: center;
                  gap: 14px;
                }
                .sporthub-brand img {
                  height: 40px;
                  border-radius: 8px;
                }
                .sporthub-title {
                  font-size: 20px;
                  font-weight: 800;
                  letter-spacing: -0.5px;
                  color: #f8fafc;
                }
                .sporthub-badge {
                  background: rgba(16, 185, 129, 0.2);
                  color: #10b981;
                  border: 1px solid #10b981;
                  padding: 4px 10px;
                  border-radius: 20px;
                  font-size: 12px;
                  font-weight: 700;
                }
                .sporthub-links a {
                  color: #94a3b8;
                  text-decoration: none;
                  font-weight: 600;
                  font-size: 14px;
                  margin-left: 20px;
                  transition: color 0.2s;
                }
                .sporthub-links a:hover {
                  color: #10b981;
                }
                /* Swagger UI customization */
                .swagger-ui .topbar { display: none !important; }
                .swagger-ui .info { margin: 25px 0 !important; }
                .swagger-ui .info .title { font-family: 'Plus Jakarta Sans', sans-serif !important; font-weight: 800 !important; }
                .swagger-ui .btn.authorize { background-color: #10b981; border-color: #10b981; color: white; }
                .swagger-ui .btn.authorize svg { fill: white; }
                .swagger-ui .opblock.opblock-post { border-color: #10b981; background: rgba(16, 185, 129, 0.05); }
                .swagger-ui .opblock.opblock-post .opblock-summary-method { background: #10b981; }
                .swagger-ui .opblock.opblock-get { border-color: #0284c7; background: rgba(2, 132, 199, 0.05); }
                .swagger-ui .opblock.opblock-get .opblock-summary-method { background: #0284c7; }
              </style>
            </head>
            <body>
              <div class="sporthub-header">
                <div class="sporthub-brand">
                  <div style="width:36px; height:36px; background:#10b981; border-radius:10px; display:flex; align-items:center; justify-content:center; color:white; font-size:18px;">
                    <i class="fa-solid fa-bolt"></i>
                  </div>
                  <div>
                    <span class="sporthub-title">SportHub UTE</span>
                    <span style="color:#94a3b8; font-size:14px; margin-left:8px;">OpenAPI 3.0 / Swagger UI</span>
                  </div>
                  <span class="sporthub-badge">v1.0.0 Active</span>
                </div>
                <div class="sporthub-links">
                  <a href="/admin/bookings"><i class="fa-solid fa-calendar-check me-1"></i> Quản lý Đặt Sân</a>
                  <a href="/admin/services"><i class="fa-solid fa-mug-hot me-1"></i> Quản lý F&B POS</a>
                  <a href="/v3/api-docs" target="_blank"><i class="fa-solid fa-code me-1"></i> JSON Spec</a>
                </div>
              </div>
              <div id="swagger-ui"></div>
              <script src="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui-bundle.js" charset="UTF-8"></script>
              <script src="https://unpkg.com/swagger-ui-dist@5.11.0/swagger-ui-standalone-preset.js" charset="UTF-8"></script>
              <script>
                window.onload = function() {
                  window.ui = SwaggerUIBundle({
                    url: "/v3/api-docs",
                    dom_id: '#swagger-ui',
                    deepLinking: true,
                    presets: [
                      SwaggerUIBundle.presets.apis,
                      SwaggerUIStandalonePreset
                    ],
                    plugins: [
                      SwaggerUIBundle.plugins.DownloadUrl
                    ],
                    layout: "BaseLayout"
                  });
                };
              </script>
            </body>
            </html>
            """;
    }
}
