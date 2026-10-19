package vn.edu.ute.sportute.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.TimeUnit;

@Controller
public class FaviconController {

    @GetMapping(value = "/favicon.ico")
    @ResponseBody
    public ResponseEntity<Resource> faviconIco() {
        Resource resource = new ClassPathResource("static/favicon.ico");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/x-icon"))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }

    @GetMapping(value = "/favicon.png")
    @ResponseBody
    public ResponseEntity<Resource> faviconPng() {
        Resource resource = new ClassPathResource("static/images/sporthub-logo-single.png");
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(resource);
    }
}
