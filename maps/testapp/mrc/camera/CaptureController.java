package com.yandex.maps.testapp.mrc.camera;

import android.util.Log;

/**
 * This class runs capture loop and sends picture requests to cameraManager
 * depending on current captureMode
 */
public class CaptureController {
    private static final String TAG = "CameraCaptureController";

    private static final int MIN_CAPTURE_PERIOD_MS = 750;
    private class CaptureMode {
        CaptureMode(boolean isOn, long periodMillis) {
            this.isOn = isOn;
            this.periodMillis = periodMillis;
        }
        boolean isOn;
        long periodMillis;
    }

    private final CameraManager cameraManager;

    private final ConcurrentVar<CaptureMode> captureMode
            = new ConcurrentVar<>(new CaptureMode(false, 0L));

    private final RepeatedTask takePictureTask;

    private Thread captureLoopThread = null;

    private volatile boolean canceled = false;

    // For backward compatibility
    public void setCaptureMode(boolean isCaptureOn) {
        captureMode.set(new CaptureMode(isCaptureOn, MIN_CAPTURE_PERIOD_MS));
    }

    public void setCaptureMode(boolean isCaptureOn, long capturePeriodMs) {
        captureMode.set(new CaptureMode(isCaptureOn, capturePeriodMs));
    }

    public CaptureController(final CameraManager cameraManager) {
        this.cameraManager = cameraManager;
        takePictureTask = new RepeatedTask(new Runnable() {
            @Override
            public void run() {
                cameraManager.takePicture();
            }
        });
    }

    public void start() {
        if (captureLoopThread != null) {
            Log.w(TAG, "CameraCaptureController has been already started");
            return;
        }

        canceled = false;
        captureLoopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "Start photo capture loop");

                    while (!canceled) {
                        // Blocks till a new value is available
                        CaptureMode mode = captureMode.take();

                        takePictureTask.cancel();
                        if (mode.isOn) {
                            long period = Math.max(mode.periodMillis, MIN_CAPTURE_PERIOD_MS);

                            long currentTime = System.currentTimeMillis();
                            long lastCaptureTime = takePictureTask.lastRunTimeMillis();
                            long initialDelay = 0;
                            if (lastCaptureTime > 0) {
                                initialDelay = Math.max(0, period - (currentTime - lastCaptureTime));
                            }
                            takePictureTask.run(initialDelay, period);
                        }
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "Camera thread interrupted");
                }
            }
        });
        captureLoopThread.start();
    }

    public void finish() {
        canceled = true;
        captureMode.set(new CaptureMode(false, 0L));
        try {
            if (takePictureTask != null) {
                takePictureTask.cancel();
            }
            if (captureLoopThread != null) {
                captureLoopThread.interrupt();
                captureLoopThread.join();
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "captureLoopThread interrupted");
        } finally {
            captureLoopThread = null;
        }
    }
}
