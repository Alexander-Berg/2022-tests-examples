package ru.yandex.yandexmaps.tools.bump

import com.nhaarman.mockito_kotlin.calls
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import ru.yandex.yandexmaps.tools.bump.di.Cmd
import java.nio.file.Path
import kotlin.test.BeforeTest
import kotlin.test.Test

private val mapsDir = Path.of("some_maps_dir")

private val cmd: Cmd = mock()

class PodsTests {

    private lateinit var pods: Pods

    @BeforeTest
    fun beforeTest() {
        pods = Pods(
            cmd = cmd,
            diff = mock(),
            fs = mock(),
            projects = setOf(PodsProject.KARTOGRAPH, PodsProject.MAPS, PodsProject.NAVI),
            mapsProjectDir = mapsDir,
        )
    }

    @Test
    fun `ensures pod update correctness`() {
        pods.ensurePodfileLockHasDiffAfter {
            pods.install()
        }

        val naviDir = mapsDir.resolve("../../navi/client/yandexnavi.ios")
        verify(cmd, times(1)).executeOrThrow("./Podinstall.sh", workingDirectory = naviDir)

        fun verifyMapsProject(path: Path) {
            inOrder(cmd) {
                verify(cmd, calls(1)).executeOrThrow(mapsDir.resolve("../tools/ios/buildscripts/env/install_ruby.sh"), path)
                verify(cmd, calls(1)).executeOrThrow(mapsDir.resolve("../tools/ios/buildscripts/env/install_gems.sh"), "-s", path)
                verify(cmd, calls(1)).executeOrThrow(
                    "rvm", "in", path, "do", "bundle", "exec", "pod", "install",
                    "--repo-update", "--project-directory=$path"
                )
            }
        }
        verifyMapsProject(mapsDir.resolve("ios"))
        verifyMapsProject(mapsDir.resolve("multiplatform/kartograph/kartograph-app-ios"))
    }
}
