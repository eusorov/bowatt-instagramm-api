package com.bowatt.instagramm.api.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
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

        return true;
    }

    private boolean reject(ConstraintValidatorContext context, String message) {
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }
}
