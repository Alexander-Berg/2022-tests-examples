package ru.yandex.whitespirit.it_tests.utils;

import io.restassured.response.Response;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.function.Supplier;

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.awaitility.Awaitility.await;

@UtilityClass
@Slf4j
public class Utils {
    public static InputStream getResourceAsStream(String filename) {
        val classLoader = Utils.class.getClassLoader();
        return classLoader.getResourceAsStream(filename);
    }

    @SneakyThrows
    public static <T> T executeWithAttempts(Supplier<T> supplier, int attempts, Duration delay) {
        return await().pollInterval(delay)
                .atMost(delay.multipliedBy(attempts+1))
                .ignoreException(AssertionError.class)
                .until(supplier::get, x -> true);
    }

    @SneakyThrows
    public static <T> T executeWithAttempts(Supplier<T> supplier, int attempts) {
        return executeWithAttempts(supplier, attempts, Duration.ofSeconds(10));
    }

    @SneakyThrows
    public static File inputStreamToFile(InputStream inputStream, String filename) {
        val file = File.createTempFile(filename, "");
        copy(inputStream, file.toPath(), REPLACE_EXISTING);
        return file;
    }

    public static void checkResponseCode(Response response) {
        val code = response.getStatusCode();
        if (code / 100 != 2) {
            throw new IllegalStateException("Bad response code=" + code + ".");
        }
    }

    public static String generateRNM(String inn, String sn) {
        val rnm10 = "6666006666";
        val concatenated = rnm10 + padWithZeros(inn, 12) + padWithZeros(sn, 20)
                .replace(' ', '0');
        val crc16 = computeCRC16(concatenated.getBytes());
        return rnm10 + padWithZeros(Integer.toString(crc16), 6);
    }

    private static String padWithZeros(String initialString, int expectedLength) {
        return String.format("%" + expectedLength + 's', initialString)
                .replace(' ', '0');
    }

    /**
     * Compute the CRC16-CCIIT for array of bytes.
     * See https://introcs.cs.princeton.edu/java/61data/CRC16CCITT.java
     */
    private static int computeCRC16(final byte[] bytes) {
        int crc = 0xffff;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }

    public static String getMySecret(String serial) {
        return "xxx_oke_" + serial.substring(serial.length() - 4);
    }
}
