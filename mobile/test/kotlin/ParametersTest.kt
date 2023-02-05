import ru.yandex.yandexmaps.multiplatformmodulecreator.Parameters
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ParametersTest {

    @Test
    fun `parameters for module with - in name`() {
        val params = Parameters("test-module", File("~/"))
        checkParameters(params)
    }

    @Test
    fun `parameters for module with _ in name`() {
        val params = Parameters("Test_module", File("~/"))
        checkParameters(params)
    }

    @Test
    fun `parameters for module with space in name`() {
        val params = Parameters("Test Module", File("~/"))
        checkParameters(params)
    }

    fun checkParameters(params: Parameters) {
        assertEquals("test-module", params.moduleName)
        assertEquals("test.module", params.packageName)
        assertEquals("TestModule", params.capitalizeModuleName)

        assertEquals("test-module-sample-ios", params.iosSampleName)
        assertEquals("TestModuleSample", params.iosAppSpecName)
        assertEquals("TestModuleSample", params.iosTargetName)
        assertEquals("TestModuleSampleViewController", params.iosViewControllerName)
        assertEquals("TestModuleSampleViewControllerDeps", params.iosViewControllerDepsName)
        assertEquals("TestModuleAppDelegate", params.iosAppDelegateNname)
        assertEquals("TestModuleAppDeps", params.iosApplicationDepsName)
        assertEquals("TestModuleAppDepsScope", params.iosApplicationDepsScopeName)

        assertEquals("test-module-sample-android", params.androidSampleName)
        assertEquals("test.module.sample", params.androidApplicartionId)
        assertEquals("TestModuleActivity", params.androidActivityName)

        assertEquals(File("~/"), params.projectDir)
        assertEquals(File("~/multiplatform"), params.multiplatformDir)
        assertEquals(File("~/multiplatform/test-module"), params.moduleDir)

        assertEquals(File("~/multiplatform/test-module/test-module-sample-ios"), params.iosSampleDir)
        assertEquals(File("~/multiplatform/test-module/test-module-sample-android"), params.androidSampleDir)

        assertEquals(
            mapOf(
                "%CAPITALIZE_MODULE_NAME%" to "TestModule",
                "%MODULE_NAME%" to "test-module",
                "%PACKAGE_NAME%" to "test.module",
                "%CLASS_NAME%" to "TestModule",
                "%ANDROID_APPLICATION_ID%" to "test.module.sample",
                "%ANDROID_ACTIVITY_NAME%" to "TestModuleActivity",
                "%IOS_TARGET_NAME%" to "TestModuleSample",
                "%IOS_APP_SPEC_NAME%" to "TestModuleSample",
                "%IOS_VIEW_CONTROLLER_NAME%" to "TestModuleSampleViewController",
                "%IOS_VIEW_CONTROLLER_DEPS_NAME%" to "TestModuleSampleViewControllerDeps",
                "%IOS_APP_DELEGATE_NAME%" to "TestModuleAppDelegate",
                "%IOS_APPLICATION_DEPS_NAME%" to "TestModuleAppDeps",
                "%IOS_APPLICATION_DEPS_SCOPE_NAME%" to "TestModuleAppDepsScope"
            ),
            params.templateValues
        )
    }
}
