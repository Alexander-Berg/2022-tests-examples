package com.yandex.launcher.lint.externaltheme;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ProgressiveJPEGDetectorTest extends LintDetectorTest {

    public void testPositive() {
        lint().files(
                file().copy("baseline.jpg=>res/drawable/baseline.jpg", this),
                file().copy("baseline.jpg=>assets/baseline.jpg", this)
        ).run().expectClean();
    }

    public void testNegative() {
        String expected = "assets/progressive.jpg: Error: Is a progressive jpeg, please convert to baseline [ProgressiveJPEG]\n" +
                "res/drawable/progressive.jpg: Error: Is a progressive jpeg, please convert to baseline [ProgressiveJPEG]\n" +
                "2 errors, 0 warnings\n";
        lint().files(
                file().copy("baseline.jpg=>res/drawable/baseline.jpg", this),
                file().copy("baseline.jpg=>assets/baseline.jpg", this),
                file().copy("progressive.jpg=>res/drawable/progressive.jpg", this),
                file().copy("progressive.jpg=>assets/progressive.jpg", this)
        ).vital(true).run().expectErrorCount(2).expect(expected);
    }

    @Override
    protected Detector getDetector() {
        return new ProgressiveJPEGDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Collections.singletonList(ProgressiveJPEGDetector.ISSUE);
    }

    @Override
    public InputStream getTestResource(String relativePath, boolean expectExists) {
        try {
            return FileUtil.getResource(relativePath, getClass().getProtectionDomain().getCodeSource());
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }
}