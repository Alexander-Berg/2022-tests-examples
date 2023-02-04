package ru.yandex.whitespirit.it_tests;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Main {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SneakyThrows
    public static void main(String[] args) {
        val whiteSpiritBaseUrl = Context
                .getWhiteSpiritManager()
                .getServiceProvider()
                .getWhitespiritUrl();

        log.info("Service ready. You can access it at {}.\n" +
                "Press enter to clean up and exit...", whiteSpiritBaseUrl);
        System.in.read();
        log.info("Exiting...");
    }
}
