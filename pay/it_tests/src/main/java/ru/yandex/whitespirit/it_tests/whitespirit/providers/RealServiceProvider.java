package ru.yandex.whitespirit.it_tests.whitespirit.providers;

import lombok.Value;

@Value
public class RealServiceProvider implements ServiceProvider {
    String whitespiritUrl;
    String hudsuckerUrl;

    @Override
    public void onStart() {}

    @Override
    public void onShutdown() {}
}
