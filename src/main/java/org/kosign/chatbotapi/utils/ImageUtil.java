package org.kosign.chatbotapi.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;
import java.util.stream.Collectors;

public class ImageUtil {

    public static String getImageUrl(String baseUrl, String imageUrl) {
        if(StringUtils.isBlank(imageUrl)) return "";
        return baseUrl + "/" + imageUrl;
    }

    public static String getImageDownloadUrl(String baseUrl, String fileName) {
        if(StringUtils.isBlank(fileName)) return "";
        return baseUrl + "/download?fileName=" + fileName;
    }

    public static String getImageUrlWithFallback(String baseUrl, String imageUrl) {
        if(StringUtils.isBlank(imageUrl)) return baseUrl + "/user-default.png";
        return baseUrl + "/" + imageUrl;
    }
    public static String convertImageUrlToBase64(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        try (InputStream inputStream = url.openStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(imageBytes);
        }
    }
    public static String convertSvgUrlToBase64(String svgUrl) throws Exception {
        URL url = new URL(svgUrl);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String svgContent = reader.lines().collect(Collectors.joining("\n"));
            byte[] svgBytes = svgContent.getBytes("UTF-8");
            String base64Svg = Base64.getEncoder().encodeToString(svgBytes);
            return  base64Svg;
        }
    }
}
