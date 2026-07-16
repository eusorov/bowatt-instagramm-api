package com.bowatt.instagramm.api.web.validation;

import com.bowatt.instagramm.api.web.ImageContentType;
import com.bowatt.instagramm.api.web.ImageUploadException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

public class ImageFileValidator implements ConstraintValidator<ValidImageFile, MultipartFile> {

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (file == null || file.isEmpty()) {
            return reject(context, "Image file is required");
        }

        if (file.getContentType() == null) {
            return reject(context, "Image content type is required");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            return reject(context, "Image original filename is required");
        }

        if (filename.length() > 255) {
            return reject(context, "Original filename must be at most 255 characters");
        }

        try {
            verifyImageContentType(file);
            return true;
        } catch (ImageUploadException ex) {
            return reject(context, ex.getMessage());
        }
    }

    public static ImageContentType verifyImageContentType(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Tika tika = new Tika();
            String mimeType = tika.detect(inputStream, file.getOriginalFilename());

            return ImageContentType.fromMediaType(mimeType, true).get();
        } catch (IOException e) {
            throw new ImageUploadException("Wrong content Type");
        }
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
