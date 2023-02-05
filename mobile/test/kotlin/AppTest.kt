import impl.Abi
import impl.MemoryAddress
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Ignore("Sanity tests to run manually")
class AppTest {

    @Test
    fun `should process Logcat stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "logcat_stacktrace_input.txt",
            outputFilename = "logcat_stacktrace_output.txt",
            providedTargetAbi = Abi.ARM_V7,
        )
    }

    @Test
    fun `should process Google Play Console stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "gpc_stacktrace_input.txt",
            outputFilename = "gpc_stacktrace_output.txt",
            providedTargetAbi = Abi.ARM_V7,
        )
    }

    @Test
    fun `should NOT process Firebase stacktrace`() { // Because of missing address offsets
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "firebase_stacktrace_input.txt",
            outputFilename = "firebase_stacktrace_output.txt",
            providedTargetAbi = Abi.ARM_V7,
        )
    }

    @Test
    fun `should process AppMetrica stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "appmetrica_stacktrace_input.txt",
            outputFilename = "appmetrica_stacktrace_output.txt",
            providedTargetAbi = Abi.ARM_V7,
        )
    }

    @Test
    fun `should process armeabi-v7a stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "armv7_stacktrace_input.txt",
            outputFilename = "armv7_stacktrace_output.txt",
            providedTargetAbi = Abi.ARM_V7,
        )
    }

    @Test
    fun `should process arm64-v8a stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "armv8_stacktrace_input.txt",
            outputFilename = "armv8_stacktrace_output.txt",
            providedTargetAbi = Abi.ARM_V8,
        )
    }

    @Test
    fun `should process x86 stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "x86_stacktrace_input.txt",
            outputFilename = "x86_stacktrace_output.txt",
            providedTargetAbi = Abi.X86,
        )
    }

    @Test
    fun `should process x86_64 stacktrace`() {
        launchGenericTest(
            mapsMobileVersion = "2020121119.7714926-navi",
            inputFilename = "x86_64_stacktrace_input.txt",
            outputFilename = "x86_64_stacktrace_output.txt",
            providedTargetAbi = Abi.X86_64,
        )
    }

    private fun launchGenericTest(
        mapsMobileVersion: String,
        inputFilename: String,
        outputFilename: String,
        providedTargetAbi: Abi? = null,
    ) {
        val output = ByteArrayOutputStream()

        launch(
            openInput = { resource("$mapsMobileVersion/$inputFilename") },
            output = output,
            seekSymbolOffsets = true,
            symbolsPaths = emptyList(),
            mapsMobileVersion = mapsMobileVersion,
            providedTargetAbi = providedTargetAbi,
            globalAddressOffset = MemoryAddress.ZERO,
        )

        val expected = resource("$mapsMobileVersion/$outputFilename").readToString()
        val actual = output.toString()

        Assert.assertEquals(expected, actual)
    }

    private fun resource(name: String): InputStream {
        return requireNotNull(javaClass.classLoader.getResourceAsStream(name)) {
            "Failed to find resource '$name'"
        }
    }

    private fun InputStream.readToString(): String {
        return String(readBytes())
    }
}
