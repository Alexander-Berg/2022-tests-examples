package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.mapkit.extensions.toNativePoint
import ru.yandex.yandexmaps.multiplatform.mapkit.map.CameraPositionFactory
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases.helpers.KnownLocations

internal class MoveCameraTestCase : TestCaseBasedTest(name = "MoveCameraTestCase") {

    override fun TestCaseDsl.run() {
        perform("Set camera position") {
            pages.startScreen.map.setCameraPosition(
                CameraPositionFactory.createCameraPosition(
                    /* target = */ KnownLocations.YANDEX_CENTRAL_OFFICE.toNativePoint(),
                    /* zoom = */ 16.0f,
                    /* azimuth = */ 0.0f,
                    /* tilt = */ 0.0f
                )
            )
        }
    }

    override fun status() = Status.UNSTABLE

    override fun scopes(): List<Scope> = listOf()
}
