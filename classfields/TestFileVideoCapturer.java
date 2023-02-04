package ru.auto.ara.core.feature.calls;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import ru.auto.util.L;

import org.webrtc.CapturerObserver;
import org.webrtc.JavaI420Buffer;
import org.webrtc.Logging;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TestFileVideoCapturer implements VideoCapturer {
    private static final String TAG = "FileVideoCapturer";
    private final TestFileVideoCapturer.VideoReader videoReader;
    private final Timer timer = new Timer();
    private CapturerObserver capturerObserver;
    private final TimerTask tickTask = new TimerTask() {
        public void run() {
            TestFileVideoCapturer.this.tick();
        }
    };

    public TestFileVideoCapturer(String assetPath) throws IOException {
        try {
            this.videoReader = new TestFileVideoCapturer.VideoReaderY4M(assetPath);
        } catch (IOException e) {
            L.e(TAG, e);
            throw e;
        }
    }

    public void tick() {
        VideoFrame videoFrame = this.videoReader.getNextFrame();
        this.capturerObserver.onFrameCaptured(videoFrame);
        videoFrame.release();
    }

    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context applicationContext, CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    public void startCapture(int width, int height, int framerate) {
        this.timer.schedule(this.tickTask, 0L, (long) (1000 / framerate));
    }

    public void stopCapture() throws InterruptedException {
        this.timer.cancel();
    }

    public void changeCaptureFormat(int width, int height, int framerate) {
    }

    public void dispose() {
        this.videoReader.close();
    }

    public boolean isScreencast() {
        return false;
    }

    private interface VideoReader {
        VideoFrame getNextFrame();

        void close();
    }

    private static class VideoReaderY4M implements TestFileVideoCapturer.VideoReader {
        private static final int FRAME_DELIMETER_LENGTH = "FRAME".length() + 1;
        private final int frameWidth;
        private final int frameHeight;
        private final long videoStart;
        private final RandomAccessFile mediaFile;
        private final FileChannel mediaFileChannel;

        public VideoReaderY4M(String assetPath) throws IOException {
            InputStream inputStream = InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(assetPath);
            File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
            File tempFile;
            try {
                // create independent cache for each instance to avoid multi stream collisions.
                tempFile = new File(cacheDir, "cached_" + hashCode() + ".y4m");
                try (OutputStream output = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4 * 1024];
                    int read;

                    while ((read = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }

                    output.flush();
                }
            } finally {
                inputStream.close();
            }

            this.mediaFile = new RandomAccessFile(tempFile.getAbsoluteFile(), "r");
            this.mediaFileChannel = this.mediaFile.getChannel();
            StringBuilder builder = new StringBuilder();

            while (true) {
                int readByte = this.mediaFile.read();
                if (readByte == -1) {
                    throw new RuntimeException("Found end of assetPath before end of header for assetPath: " + assetPath);
                }

                if (readByte == 10) {
                    this.videoStart = this.mediaFileChannel.position();
                    String header = builder.toString();
                    String[] headerTokens = header.split("[ ]");
                    int w = 0;
                    int h = 0;
                    String colorSpace = "";
                    String[] var8 = headerTokens;
                    int var9 = headerTokens.length;

                    for (int var10 = 0; var10 < var9; ++var10) {
                        String tok = var8[var10];
                        char c0 = tok.charAt(0);
                        switch (c0) {
                            case 'C':
                                colorSpace = tok.substring(1);
                                break;
                            case 'H':
                                h = Integer.parseInt(tok.substring(1));
                                break;
                            case 'W':
                                w = Integer.parseInt(tok.substring(1));
                        }
                    }

                    Logging.d("VideoReaderY4M", "Color space: " + colorSpace);
                    if (!colorSpace.equals("420") && !colorSpace.equals("420mpeg2")) {
                        throw new IllegalArgumentException("Does not support any other color space than I420 or I420mpeg2");
                    }

                    if (w % 2 != 1 && h % 2 != 1) {
                        this.frameWidth = w;
                        this.frameHeight = h;
                        L.d(TAG, "frame dim: (" + w + ", " + h + ")");
                        return;
                    }

                    throw new IllegalArgumentException("Does not support odd width or height");
                }

                builder.append((char) readByte);
            }
        }

        public VideoFrame getNextFrame() {
            long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
            JavaI420Buffer buffer = JavaI420Buffer.allocate(this.frameWidth, this.frameHeight);
            ByteBuffer dataY = buffer.getDataY();
            ByteBuffer dataU = buffer.getDataU();
            ByteBuffer dataV = buffer.getDataV();

            try {
                ByteBuffer frameDelim = ByteBuffer.allocate(FRAME_DELIMETER_LENGTH);
                if (this.mediaFileChannel.read(frameDelim) < FRAME_DELIMETER_LENGTH) {
                    this.mediaFileChannel.position(this.videoStart);
                    if (this.mediaFileChannel.read(frameDelim) < FRAME_DELIMETER_LENGTH) {
                        throw new RuntimeException("Error looping video");
                    }
                }

                String frameDelimStr = new String(frameDelim.array(), Charset.forName("US-ASCII"));
                if (!frameDelimStr.equals("FRAME\n")) {
                    throw new RuntimeException("Frames should be delimited by FRAME plus newline, found delimter was: '" + frameDelimStr + "'");
                }

                frameDelim.clear();

                this.mediaFileChannel.read(dataY);
                this.mediaFileChannel.read(dataU);
                this.mediaFileChannel.read(dataV);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return new VideoFrame(buffer, 0, captureTimeNs);
        }

        public void close() {
            try {
                this.mediaFile.close();
            } catch (IOException var2) {
                Logging.e(TAG, "Problem closing file", var2);
            }

        }
    }
}
