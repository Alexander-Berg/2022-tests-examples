package com.yandex.frankenstein.steps;

import com.yandex.frankenstein.CommandRunner;
import com.yandex.frankenstein.TestCoverageRegistry;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public final class TestCoverageStep implements Step {

    @NotNull private final File mOutputDir;
    @NotNull private final String mTestName;
    @NotNull private final CommandRunner mCommandRunner;

    public TestCoverageStep(@NotNull final File outputDir, @NotNull final String testName) {
        this(outputDir, testName, new CommandRunner("TestCoverage"));
    }

    TestCoverageStep(@NotNull final File outputDir, @NotNull final String testName,
                     @NotNull final CommandRunner commandRunner) {
        mOutputDir = outputDir;
        mTestName = testName;
        mCommandRunner = commandRunner;

        TestCoverageRegistry.registerInstance(this);
    }

    @Override
    public void before() {}

    @Override
    public void after() {
        collect();
    }

    public void collect() {
        try {
            mOutputDir.mkdirs();
            final Map<String, String> result = mCommandRunner.startSilently("collectCoverageData").getResult();
            result.forEach((final String key, final String value) -> {
                try {
                    final String coverageFileName = mTestName + "." + key.replace("good.exec", "ec");
                    final File coverageFile = new File(mOutputDir, coverageFileName);
                    FileUtils.writeByteArrayToFile(coverageFile, Base64.getDecoder().decode(value));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (final RuntimeException e) {
            if (e.getCause() instanceof TimeoutException == false) {
                throw e;
            }
        }
    }
}
