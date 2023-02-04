package ru.auto.ara.core.mocks_and_stubbs.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.frame.ByteBufferFrameManager
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.size.Size
import ru.auto.ara.core.utils.FileTestUtils.readAssetImage
import ru.auto.ara.core.utils.getPanoramaFile
import ru.auto.core_ui.ui.camera.CameraDelegate
import ru.auto.core_ui.ui.camera.ICameraDelegate
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Method

class TestCameraDelegate(
    cameraViewInflater: () -> CameraView,
    config: ICameraDelegate.Config,
    private val onChangedFrame: (Frame) -> Unit = {}
) : ICameraDelegate {

    var showValidCar: Boolean = true

    private val frameData: ByteArray by lazy {
        val bitmap = readAssetImage(CAR_PHOTO_FILE_NAME)
        getNV21(FRAME_WIDTH, FRAME_HEIGHT, bitmap)
    }

    private val frame: Frame by lazy {
        val constructor: Constructor<Frame> = Frame::class.java.declaredConstructors[0] as Constructor<Frame>
        constructor.isAccessible = true
        constructor.newInstance(ByteBufferFrameManager(2, null))
    }

    private val setFrameContent: Method by lazy {
        Frame::class.java.getDeclaredMethod(
            "setContent",
            Object::class.java,
            Long::class.java,
            Int::class.java,
            Int::class.java,
            Size::class.java,
            Int::class.java
        ).apply {
            isAccessible = true
        }
    }

    private val frameSize: Size by lazy { Size(FRAME_WIDTH, FRAME_HEIGHT) }

    private val cameraDelegate = CameraDelegate(
        cameraViewInflater = cameraViewInflater,
        config = config,
        onChangedFrame = {
            setFrameContent(frame, frameData, it.time, 0, 0, frameSize, ImageFormat.NV21)
            onChangedFrame(frame)
        }
    )

    override fun startPreview() {
        cameraDelegate.startPreview()
    }

    override fun open() {
        // Not implemented.
    }

    override fun close() {
        cameraDelegate.close()
    }

    override fun destroy() {
        cameraDelegate.destroy()
    }

    override fun startRecording(
        file: File,
        onRecordingStarted: () -> Unit,
        onRecordingEnded: () -> Unit,
        onError: () -> Unit
    ) {
        cameraDelegate.startRecording(
            file = file,
            onRecordingStarted = onRecordingStarted,
            onRecordingEnded = onRecordingEnded,
            onError = onError
        )
    }

    override fun stopRecording(onVideoReady: (file: File) -> Unit, onError: () -> Unit) {
        cameraDelegate.stopRecording (
            {
                onVideoReady(getPanoramaFile())
            },
            onError
        )
    }

    override fun takePicture(onSuccess: (Bitmap) -> Unit, onError: (CameraException) -> Unit) {
        onSuccess(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    override fun onConfigurationChanged() {
        cameraDelegate.onConfigurationChanged()
    }

    companion object {
        private const val FRAME_WIDTH = 1280
        private const val FRAME_HEIGHT = 960
        private const val CAR_PHOTO_FILE_NAME = "panorama/exterior/car.jpg"
    }

}
