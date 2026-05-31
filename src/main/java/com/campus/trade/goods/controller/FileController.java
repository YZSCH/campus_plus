package com.campus.trade.goods.controller;

import com.campus.trade.common.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
    ));

    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    ));

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final int MAX_FILE_COUNT = 6;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/image")
    public ApiResult<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResult.error(400, "文件为空");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
            return ApiResult.error(400, "不支持的图片格式，仅支持 JPG/JPEG/PNG/GIF/BMP/WebP");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            return ApiResult.error(400, "不支持的图片格式，仅支持 JPG/JPEG/PNG/GIF/BMP/WebP");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ApiResult.error(400, "图片大小不能超过5MB");
        }

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String newFileName = UUID.randomUUID().toString() + ext;
            Path filePath = uploadPath.resolve(newFileName);
            file.transferTo(filePath.toFile());

            String url = "/uploads/" + newFileName;
            Map<String, Object> result = new HashMap<>();
            result.put("url", url);
            result.put("fileName", newFileName);
            return ApiResult.success(result);
        } catch (IOException e) {
            return ApiResult.error(500, "上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/images")
    public ApiResult<List<Map<String, Object>>> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ApiResult.error(400, "请选择要上传的图片");
        }
        if (files.size() > MAX_FILE_COUNT) {
            return ApiResult.error(400, "最多上传" + MAX_FILE_COUNT + "张图片");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            ApiResult<Map<String, Object>> singleResult = uploadImage(file);
            if (singleResult.getCode() != 200) {
                return ApiResult.error(singleResult.getCode(),
                        "第" + (results.size() + 1) + "张图片: " + singleResult.getMessage());
            }
            results.add(singleResult.getData());
        }

        return ApiResult.success(results);
    }
}