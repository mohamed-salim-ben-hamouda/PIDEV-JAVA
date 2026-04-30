package com.pidev.utils.hackthon;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pidev.utils.EnvConfig;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudinaryUtil {
    private static final String CLOUD_NAME = EnvConfig.get("CLOUDINARY_CLOUD_NAME");
    private static final String API_KEY = EnvConfig.get("CLOUDINARY_API_KEY");
    private static final String API_SECRET = EnvConfig.get("CLOUDINARY_API_SECRET");
    private static final String UPLOAD_PRESET = EnvConfig.get("CLOUDINARY_UPLOAD_PRESET");

    private static Cloudinary cloudinary;

    static {
        if (CLOUD_NAME == null || API_KEY == null || API_SECRET == null || UPLOAD_PRESET == null
                || CLOUD_NAME.isBlank() || API_KEY.isBlank() || API_SECRET.isBlank() || UPLOAD_PRESET.isBlank()) {
            throw new IllegalStateException("Missing Cloudinary keys in .env");
        }
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,
                "api_key", API_KEY,
                "api_secret", API_SECRET,
                "secure", true
        ));
    }

    public static String upload(File file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.asMap(
                "upload_preset", UPLOAD_PRESET
        ));
        return (String) uploadResult.get("secure_url");
    }
}
