package ru.yandex.vertis;

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.artifacts.ArtifactsWatcher;
import jetbrains.buildServer.util.EventDispatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ru.yandex.vertis.bean.Bean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.io.FileMatchers.anExistingFile;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.vertis.RerunConstant.FAILED;
import static ru.yandex.vertis.RerunConstant.RUN_TYPE;
import static ru.yandex.vertis.SaveFailedAdapter.testToRerun;
import static ru.yandex.vertis.bean.Config.buildConfig;

public class SaveFailedTest {

    private static final String WORKING_DIR = "working";
    private static final String FAILED_TESTS_LIST = "ru.tests.SimpleTest#shouldBeBroken," +
            "ru.tests.SimpleTest#shouldBeFailed," +
            "ru.tests.StringParameterizedTest#stringParameterizedTest[i = 2],";
    private static final String FAILED_TESTS_LIST_WITH_RERUN = "ru.tests.SimpleTest#shouldBeFailed," +
            "ru.tests.StringParameterizedTest#stringParameterizedTest[i = 2],";

    private AgentRunningBuild build;
    private BuildRunnerContext runner;
    private SaveFailedAdapter saveFailedAdapter;

    private File wrkDir;

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void buildWithRerunFeature() throws IOException {
        saveFailedAdapter =
                new SaveFailedAdapter(mock(EventDispatcher.class), mock(ArtifactsWatcher.class));

        build = mock(AgentRunningBuild.class);
        when(build.getBuildTempDirectory()).thenReturn(tmpFolder.getRoot());
        when(build.getBuildLogger()).thenReturn(mock(BuildProgressLogger.class));
        when(build.getBuildFeatures()).thenReturn(Collections.singletonList(Bean.agentBuildFeature(RUN_TYPE)));
        when(build.getSharedBuildParameters()).thenReturn(Bean.buildParametersSystem(buildConfig()));
        when(build.getBuildTypeExternalId()).thenReturn("myType");

        wrkDir = tmpFolder.newFolder(WORKING_DIR);

        runner = mock(BuildRunnerContext.class);
        when(runner.getWorkingDirectory()).thenReturn(wrkDir);
    }

    @Test
    public void shouldCreateEmptyFileIfDidntExistAnyReports() throws IOException {
        saveFailedAdapter.beforeRunnerStart(runner);
        saveFailedAdapter.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
        shouldSeeFile();
    }

    @Test
    public void shouldSaveFailedTestsToFile() throws Exception {
        addFailedTestReport(wrkDir, "TEST-Report.xml");

        saveFailedAdapter.beforeRunnerStart(runner);
        saveFailedAdapter.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
        shouldSeeFile(FAILED_TESTS_LIST);
    }

    @Test
    public void shouldCreteEmptyFileIfAllTestsPassed() throws Exception {
        addFailedTestReport(wrkDir, "TEST-ReportPASSED.xml");

        saveFailedAdapter.beforeRunnerStart(runner);
        saveFailedAdapter.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
        shouldSeeFile();
    }

    @Test
    public void shouldNotWriteResultWhenPassedThenFailed() throws Exception {
        List<Path> paths = newArrayList(
                addFailedTestReport(wrkDir, "TEST-ReportRERUNPASSED.xml"),
                addFailedTestReport(wrkDir, "TEST-Report.xml"));
        assertThat("", testToRerun(paths), equalTo(FAILED_TESTS_LIST_WITH_RERUN));
    }

    @Test
    public void shouldNotWriteResultWhenFailedThenPassed() throws Exception {
        List<Path> paths = newArrayList(
                addFailedTestReport(wrkDir, "TEST-Report.xml"),
                addFailedTestReport(wrkDir, "TEST-ReportRERUNPASSED.xml"));
        assertThat("", testToRerun(paths), equalTo(FAILED_TESTS_LIST_WITH_RERUN));
    }

    @Test
    public void shouldNotWriteResultWhenErrorThenError() throws Exception {
        List<Path> paths = newArrayList(
                addFailedTestReport(wrkDir, "TEST-Report.xml"),
                addFailedTestReport(wrkDir, "TEST-ReportRERUNERROR.xml"));
        assertThat("", testToRerun(paths), equalTo(FAILED_TESTS_LIST));
    }

    @Test
    public void shouldNotWriteResultWhenFailedThenFailed() throws Exception {
        List<Path> paths = newArrayList(
                addFailedTestReport(wrkDir, "TEST-Report.xml"),
                addFailedTestReport(wrkDir, "TEST-ReportRERUNFAILED.xml"));
        assertThat("", testToRerun(paths), equalTo(FAILED_TESTS_LIST));
    }


    @Test
    public void shouldSearchResultsRecursively() throws Exception {
        addFailedTestReport(tmpFolder.newFolder(WORKING_DIR, "subFolder"), "TEST-Report.xml");

        saveFailedAdapter.beforeRunnerStart(runner);
        saveFailedAdapter.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
        shouldSeeFile(FAILED_TESTS_LIST);
    }

    @Test
    public void shouldNotSearchAnyResultsInEmptyFolder() throws Exception {
        saveFailedAdapter.beforeRunnerStart(runner);
        saveFailedAdapter.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
        shouldSeeFile();
    }

    @Test
    public void shouldParseParameterizedTest() throws Exception {
        addFailedTestReport(tmpFolder.newFolder(WORKING_DIR, "subFolder"), "TEST-ParameterizedTest.xml");

        saveFailedAdapter.beforeRunnerStart(runner);
        saveFailedAdapter.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
        shouldSeeFile("ru.vertis.Test#test1[0]," +
                "ru.vertis.Test#test1[1]," +
                "ru.vertis.Test#test1[2]," +
                "ru.vertis.Test#test2[0]," +
                "ru.vertis.Test#test2[1]," +
                "ru.vertis.Test#test2[2],");
    }

    private void shouldSeeFile(String... data) throws IOException {
        assertThat("Создался файл с тестами", tmpFolder.getRoot().toPath().resolve(FAILED).toFile(),
                anExistingFile());
        List<String> lines = Files.readAllLines(tmpFolder.getRoot().toPath().resolve(FAILED));
        assertThat("В файле нужная строчка", lines, is(asList(data)));
    }

    private Path addFailedTestReport(File dir, String fileName) throws Exception {
        File report = new File(dir, fileName);
        byte[] testData = Files.readAllBytes(new File(this.getClass().getClassLoader()
                .getResource(format("surefire/%s", fileName)).toURI()).toPath());
        Files.write(report.toPath(), testData);
        return report.toPath();
    }
}
