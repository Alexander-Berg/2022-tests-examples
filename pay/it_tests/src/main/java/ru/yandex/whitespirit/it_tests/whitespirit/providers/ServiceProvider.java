package ru.yandex.whitespirit.it_tests.whitespirit.providers;

public interface ServiceProvider {
    String getWhitespiritUrl();
    String getHudsuckerUrl();
    void onStart();
    void onShutdown();
}
