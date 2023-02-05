package ru.yandex.yandexmaps.multiplatform.uitesting.api.pageobjects

public enum class OrientationMode {
    PORTRAIT,
    LANDSCAPE;
}

public interface DevicePage {
    public fun setOrientation(mode: OrientationMode)
}
