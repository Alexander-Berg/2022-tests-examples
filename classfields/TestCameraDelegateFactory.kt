package ru.auto.ara.core.mocks_and_stubbs.camera

import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.frame.Frame
import ru.auto.core_ui.ui.camera.ICameraDelegate
import ru.auto.core_ui.ui.camera.ICameraDelegateFactory

class TestCameraDelegateFactory : ICameraDelegateFactory {

    override fun create(
        cameraViewInflater: () -> CameraView,
        config: ICameraDelegate.Config,
        onChangedFrame: (Frame) -> Unit
    ): ICameraDelegate = TestCameraDelegate(
        cameraViewInflater = cameraViewInflater,
        config = config,
        onChangedFrame = onChangedFrame
    )

}
