package ru.yandex.yandexmaps.multiplatform.uitesting.api

public interface VideoCapturer {
    public fun startCapture(): VideoCaptureSession
    public interface VideoCaptureSession {
        public fun stop(): MediaCapture?
    }
}
