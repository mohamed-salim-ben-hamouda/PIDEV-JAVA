package com.pidev.utils.hackthon;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CloudinaryUtil {
    private static final String CLOUD_NAME = "dvs4zymiq";
    private static final String API_KEY = "xxxxxxxxxxxxxxx";
    private static final String API_SECRET = "xxxxxxxxxx";
    private static final String UPLOAD_PRESET = "hackthon";

    private static Cloudinary cloudinary;

    static {
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
