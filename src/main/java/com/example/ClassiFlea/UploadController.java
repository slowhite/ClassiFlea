package com.example.ClassiFlea;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class UploadController {

  private static final Path UPLOAD_DIR = Paths.get("./uploads"); // 物理目录

  @PostMapping(path = "/upload", consumes = "multipart/form-data")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
    try {
      Files.createDirectories(UPLOAD_DIR);

      String ext = "";
      String orig = file.getOriginalFilename();
      if (StringUtils.hasText(orig) && orig.contains(".")) {
        ext = orig.substring(orig.lastIndexOf('.'));
      }
      String filename = System.currentTimeMillis() + "-" + UUID.randomUUID().toString().replace("-", "") + ext;

      Path target = UPLOAD_DIR.resolve(filename);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
      }

      // 这个 URL 要和 WebConfig 里的前缀一致
      Map<String, Object> resp = new HashMap<>();
      resp.put("url", "/adminlte/uploads/" + filename);
      return ResponseEntity.ok(resp);
    } catch (Exception e) {
      e.printStackTrace();
      return ResponseEntity.badRequest().body("upload failed: " + e.getMessage());
    }
  }
}
