package com.ecotvy.application.port.out;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStoragePort {
    String uploadImage(MultipartFile file, String userId);
    void deleteImage(String imageUrl);
    byte[] resizeImage(byte[] imageBytes, int maxWidth, int maxHeight);
}

