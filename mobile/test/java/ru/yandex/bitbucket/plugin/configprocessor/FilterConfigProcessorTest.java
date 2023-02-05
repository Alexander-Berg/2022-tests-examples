package ru.yandex.bitbucket.plugin.configprocessor;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.configprocessor.entity.Project;
import ru.yandex.bitbucket.plugin.configprocessor.util.ConfigReader;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import ru.yandex.bitbucket.plugin.configprocessor.util.FileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(JUnitParamsRunner.class)
public class FilterConfigProcessorTest {
    private static final String CONFIG_DIRECTORY = "src/test/resources/ru/yandex/bitbucket/plugin/configprocessor";
    private static final String MAIN_CONFIG_1 = CONFIG_DIRECTORY + "/configProcessingTest_1.yaml";
    private static final String MAIN_CONFIG_2 = CONFIG_DIRECTORY + "/configProcessingTest_2.yaml";
    private static final String BRANCH_CONFIG = CONFIG_DIRECTORY + "/filterBranchesTest.yaml";
    private static final String PATH_CONFIG = CONFIG_DIRECTORY + "/filterPathsTest.yaml";
    private static final String BRANCH_ROOT_CONFIG = String.format("- %s", BRANCH_CONFIG);
    private static final String PATH_ROOT_CONFIG = String.format("- %s", PATH_CONFIG);

    @Mock
    private FileReader fileReader;

    private ConfigProcessor configProcessor;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        ConfigReader configReader = new ConfigReader(fileReader);
        configProcessor = new ConfigProcessor(configReader);
    }

    private List<ConfigTCBuild> mockAndGetExpectedResult(String configPath, String rootConfig) throws FileNotFoundException {
        File configFile = new File(configPath);
        InputStream configStream = new FileInputStream(configFile);
        String configString = new BufferedReader(new InputStreamReader(configStream))
                .lines().collect(Collectors.joining("\n"));
        when(fileReader.readFile("ci.yaml")).thenReturn(rootConfig);
        when(fileReader.readFile(configPath)).thenReturn(configString);

        InputStream configStreamCopy = new FileInputStream(configFile);
        Yaml yaml = new Yaml(new Constructor(Project.class));
        Project project = yaml.load(configStreamCopy);
        return project.getTcBuilds();
    }

    @Test
    public void multipleConfigsAllBuilds() throws IOException {
        String rootConfig = String.format("- %s\n- %s", MAIN_CONFIG_1, MAIN_CONFIG_2);
        List<ConfigTCBuild> res = mockAndGetExpectedResult(MAIN_CONFIG_1, rootConfig);
        res.addAll(mockAndGetExpectedResult(MAIN_CONFIG_2, rootConfig));
        String path = "root/testProject/src/test/util/test.txt";
        Set<String> pathSet = Collections.singleton(path);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, "master");
        assertEquals(new HashSet<>(res), buildSet);
    }

    @Test
    @Parameters({
            "bugfix/test"
    })
    public void filterByBranchPositive(String branchName) throws IOException {
        List<ConfigTCBuild> res = mockAndGetExpectedResult(BRANCH_CONFIG, BRANCH_ROOT_CONFIG);
        String path = "test.txt";
        Set<String> pathSet = Collections.singleton(path);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, branchName);
        assertEquals(Collections.singleton(res.get(0)), buildSet);
    }

    @Test
    @Parameters({
            "develop/feature/test",
            "bugfix/feature/test",
            "develop/test",
            "bugfix/test/custom"
    })
    public void filterByBranchNegative(String branchName) throws IOException {
        mockAndGetExpectedResult(BRANCH_CONFIG, BRANCH_ROOT_CONFIG);
        String path = "test.txt";
        Set<String> pathSet = Collections.singleton(path);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, branchName);
        assertThat(buildSet, is(empty()));
    }

    @Test
    @Parameters({
            "common/test",
            "common/test/changed.sxml"
    })
    public void filterByPathPositive(String path) throws IOException {
        List<ConfigTCBuild> res = mockAndGetExpectedResult(PATH_CONFIG, PATH_ROOT_CONFIG);
        Set<String> pathSet = Collections.singleton(path);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, "branch");
        assertEquals(Collections.singleton(res.get(0)), buildSet);
    }

    @Test
    @Parameters({
            "common/doc/test",
            "util/doc/test",
            "util/test",
            "common/test/changed.xml"
    })
    public void filterByPathNegative(String path) throws IOException {
        mockAndGetExpectedResult(PATH_CONFIG, PATH_ROOT_CONFIG);
        Set<String> pathSet = Collections.singleton(path);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, "branch");
        assertThat(buildSet, is(empty()));
    }
}