package com.yandex.launcher.lint.app

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles.kt
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import com.yandex.launcher.lint.externaltheme.FileUtil
import junit.framework.TestCase
import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("UnstableApiUsage")
class ApplicationIdDetectorTest : LintDetectorTest() {

    private val cpuTrackerFile = "package com.yandex.launcher.app;\n" +
            "\n" +
            "public class CpuTracker {\n" +
            "\n" +
            "    private static class ThreadCpuInfo {\n" +
            "        final String name;\n" +
            "        final long utime;\n" +
            "        final long stime;\n" +
            "\n" +
            "        ThreadCpuInfo(String name, long utime, long stime) {\n" +
            "            this.name = name;\n" +
            "            this.utime = utime;\n" +
            "            this.stime = stime;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    private static class SystemCpuInfo {\n" +
            "        final long idleTime;\n" +
            "        final long activeTime;\n" +
            "\n" +
            "        SystemCpuInfo(long idleTime, long activeTime) {\n" +
            "            this.idleTime = idleTime;\n" +
            "            this.activeTime = activeTime;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    private static class CpuInfo {\n" +
            "        final ThreadCpuInfo threads;\n" +
            "        final SystemCpuInfo systemInfo;\n" +
            "\n" +
            "        CpuInfo(ThreadCpuInfo threads, SystemCpuInfo systemInfo) {\n" +
            "            this.threads = threads;\n" +
            "            this.systemInfo = systemInfo;\n" +
            "        }\n" +
            "    }\n" +
            "}"
    private val promoContractFile = "package com.yandex.launcher.app;\n" +
            "\n" +
            "import static com.yandex.launcher.app.BuildConfig.APPLICATION_ID;\n" +
            "\n" +
            "public class PromoContract {\n" +
            "\n" +
            "    String AUTHORITY = BuildConfig.APPLICATION_ID + \".promo\";\n" +
            "    String AUTHORITY_CONTENT = \"content://\" + AUTHORITY;\n" +
            "    String PROMO_HISTORY_PATH = \"history\";\n" +
            "    String TABLE_PROMO_HISTORY = \"history\";\n" +
            "\n" +
            "    interface Columns {\n" +
            "        String _ID = \"id\";\n" +
            "        /** TEXT **/\n" +
            "        String PROMO_ID = \"promo_id\";\n" +
            "        /** INTEGER **/\n" +
            "        String LAST_SHOWN_REALTIME = \"last_shown_realtime\";\n" +
            "        /** BOOLEAN **/\n" +
            "        String HIDDEN = \"hidden\";\n" +
            "        /** BOOLEAN **/\n" +
            "        String HIDDEN_FOREVER = BuildConfig.APPLICATION_ID + \"hidden_forever\";\n" +
            "    }\n" +
            "\n" +
            "    PromoContract() {\n" +
            "        System.console().printf(\"value\" + BuildConfig.APPLICATION_ID);\n" +
            "\n" +
            "        if (\"myString\".equals(BuildConfig.APPLICATION_ID)) {\n" +
            "            System.console().printf(\"value\");\n" +
            "            System.console().printf(\"com.yandex.launcher\");\n" +
            "            System.console().printf(\"com.yandex.launcher.my.provider\");\n" +
            "        }\n" +
            "\n" +
            "        \"My string substring\".substring(BuildConfig.APPLICATION_ID.length());\n" +
            "    }\n" +
            "}\n"
    private val buildConfigFile = "package com.yandex.launcher.app;\n" +
            "\n" +
            "public class BuildConfig {\n" +
            "\n" +
            "    public static final String APPLICATION_ID = \"com.yandex.launcher\";\n" +
            "}"

    fun testPositive() {
        lint().files(
            java(cpuTrackerFile)
        ).run().expectClean()
    }

    fun testNegative() {
        val expected =
            "src/com/yandex/launcher/app/BuildConfig.java:5: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "    public static final String APPLICATION_ID = \"com.yandex.launcher\";\n" +
                    "                               ~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:3: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "import static com.yandex.launcher.app.BuildConfig.APPLICATION_ID;\n" +
                    "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:7: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "    String AUTHORITY = BuildConfig.APPLICATION_ID + \".promo\";\n" +
                    "           ~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:7: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "    String AUTHORITY = BuildConfig.APPLICATION_ID + \".promo\";\n" +
                    "                       ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:21: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "        String HIDDEN_FOREVER = BuildConfig.APPLICATION_ID + \"hidden_forever\";\n" +
                    "               ~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:21: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "        String HIDDEN_FOREVER = BuildConfig.APPLICATION_ID + \"hidden_forever\";\n" +
                    "                                ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:25: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "        System.console().printf(\"value\" + BuildConfig.APPLICATION_ID);\n" +
                    "                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:27: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "        if (\"myString\".equals(BuildConfig.APPLICATION_ID)) {\n" +
                    "                              ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:29: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "            System.console().printf(\"com.yandex.launcher\");\n" +
                    "                                    ~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:30: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "            System.console().printf(\"com.yandex.launcher.my.provider\");\n" +
                    "                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/com/yandex/launcher/app/PromoContract.java:33: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]\n" +
                    "        \"My string substring\".substring(BuildConfig.APPLICATION_ID.length());\n" +
                    "                                        ~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "11 errors, 0 warnings"
        lint().files(
            java(buildConfigFile),
            java(cpuTrackerFile),
            java(promoContractFile)
        ).vital(false).run().expectErrorCount(11).expect(expected)
    }

    fun testNegativeKt() {
        lint()
            .files(
                kt("""
                        package com.yandex.launcher.app

                        import android.os.Parcel
                        import android.content.ComponentName

                        class InvalidKotlinRemoteObject(parcel: Parcel) : RemoteObject() {

                            val COMPONENT_NAME = ComponentName("com.yandex.launcher", "my package name")

                            override fun saveToParcel(parcel: Parcel, i: Int) {
                                System.console().printf("value" + BuildConfig.APPLICATION_ID);
                            }

                            companion object {

                                @JvmStatic
                                val COMPONENT_NAME = ComponentName("com.yandex.launcher", "UpdateVangaRatingJob")
                            }

                        }
                        """.trimIndent())
            )
            .issues(ApplicationIdDetector.ISSUE)
            .run()
            .expectErrorCount(3)
            .expect("""
src/com/yandex/launcher/app/InvalidKotlinRemoteObject.kt:8: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]
    val COMPONENT_NAME = ComponentName("com.yandex.launcher", "my package name")
        ~~~~~~~~~~~~~~
src/com/yandex/launcher/app/InvalidKotlinRemoteObject.kt:11: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]
        System.console().printf("value" + BuildConfig.APPLICATION_ID);
                                          ~~~~~~~~~~~~~~~~~~~~~~~~~~
src/com/yandex/launcher/app/InvalidKotlinRemoteObject.kt:17: Error: Don't reference BuildConfig.APPLICATION_ID (or com.yandex.launcher), it can break Yandex Launcher kit compatibility [YandexLauncherKitApplicationId]
        val COMPONENT_NAME = ComponentName("com.yandex.launcher", "UpdateVangaRatingJob")
            ~~~~~~~~~~~~~~
3 errors, 0 warnings
                    """.trimIndent())
    }

    override fun getDetector(): Detector {
        return ApplicationIdDetector()
    }

    override fun getIssues(): List<Issue> {
        return listOf(ApplicationIdDetector.ISSUE)
    }

    override fun getTestResource(relativePath: String, expectExists: Boolean): InputStream {
        try {
            return FileUtil.getResource(relativePath, javaClass.protectionDomain.codeSource)
        } catch (e: Exception) {
            TestCase.fail(e.message)
        }

        return ByteArrayInputStream("".toByteArray())
    }
}