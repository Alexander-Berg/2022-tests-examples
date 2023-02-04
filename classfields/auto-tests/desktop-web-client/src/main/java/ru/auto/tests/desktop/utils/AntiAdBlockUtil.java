package ru.auto.tests.desktop.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class AntiAdBlockUtil {

    private final static String IP = "127.0.0.1";
    private final static String ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7";
    public final static String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/68.0.3440.106 Safari/537.36 QATeamAutoTests";

    private AntiAdBlockUtil() {
    }

    public static String generateAntiAdBlockCookie(String aabPartnerKey) {
        long timestamp = (new Date()).getTime() / 1000;
        String ip = hashString(IP);
        String hashedUserAgent = hashString(USER_AGENT);
        String acceptLanguage = hashString(ACCEPT_LANGUAGE);

        String data = format("%s\t%s\t%s\t%s", timestamp, ip, hashedUserAgent, acceptLanguage);

        return encrypt(data, aabPartnerKey);
    }

    private static String hashString(String str) {
        char[] chars = str.toCharArray();
        int hashedString = 0;
        for (int i = 0; i < str.length(); i++) {
            hashedString += chars[i] * (i + 1);
        }
        return String.valueOf(hashedString);
    }

    private static String encrypt(String data, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.substring(0, 16).getBytes(UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            int padLength = (Math.floorDiv(data.length(), 16) + 1) * 16;
            String text = format("%-" + padLength + "s", data);

            return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Can't generate anti AdBlock cookie", e);
        }
    }
}
