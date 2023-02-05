package com.yandex.frankenstein.steps;

import com.yandex.frankenstein.CommandProcess;
import com.yandex.frankenstein.CommandRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestCoverageStepTest {

    private static final String DATA = "someString";

    private final String mEncoded = Base64.getEncoder().encodeToString(DATA.getBytes(Charset.defaultCharset()));

    private final CommandRunner mCommandRunner = mock(CommandRunner.class);
    private final CommandProcess mCommandProcess = mock(CommandProcess.class);

    @Test
    public void testEndTestCoverage() throws IOException {
        final String testName = "some_test_name";
        final String threadType = "some_type";
        final String uuid = "some_uuid";
        final String mLocalFileName = threadType + "." + uuid + ".good.exec";
        final String mFileName = testName + "." + threadType + "." + uuid + ".ec";

        final File outputDir = Files.createTempDirectory("prefix").toFile();
        final TestCoverageStep testCoverage = new TestCoverageStep(outputDir, testName, mCommandRunner);
        final Map<String, String> mResult = new HashMap<>();
        mResult.put(mLocalFileName, mEncoded);

        when(mCommandRunner.startSilently("collectCoverageData")).thenReturn(mCommandProcess);
        when(mCommandProcess.getResult()).thenReturn(mResult);

        testCoverage.after();
        final File outputFile = new File(outputDir, mFileName);
        final String result = FileUtils.readFileToString(outputFile, Charset.defaultCharset());
        assertThat(result).isEqualTo(DATA);
    }
}
