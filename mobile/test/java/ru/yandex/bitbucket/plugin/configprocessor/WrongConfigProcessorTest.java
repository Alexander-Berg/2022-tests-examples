package ru.yandex.bitbucket.plugin.configprocessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.configprocessor.util.ConfigReader;
import ru.yandex.bitbucket.plugin.configprocessor.util.FileReader;
import ru.yandex.bitbucket.plugin.exception.WrongConfigException;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WrongConfigProcessorTest {
    private static final String CONFIG_PATH = "testPath";
    private static final String CHANGED_PATH = "root/testProject/src/test/util/test.xml";
    private static final Set<String> pathSet = Collections.singleton(CHANGED_PATH);
    private static final String CONFIG_STRING = String.format("- %s", CONFIG_PATH);
    private static final String BRANCH_NAME = "master";

    @Mock
    private FileReader fileReader;

    private ConfigProcessor configProcessor;

    @Before
    public void init() throws FileNotFoundException {
        MockitoAnnotations.initMocks(this);
        ConfigReader configReader = new ConfigReader(fileReader);
        configProcessor = new ConfigProcessor(configReader);
        when(fileReader.readFile("ci.yaml")).thenReturn(CONFIG_STRING);
    }

    @Test(expected = WrongConfigException.class)
    public void emptyConfig() throws FileNotFoundException {
        String project = "";
        when(fileReader.readFile(CONFIG_PATH)).thenReturn(project);
        configProcessor.getBuildInfoByChangingPath(pathSet, BRANCH_NAME);
    }

    @Test(expected = WrongConfigException.class)
    public void missedTcBuilds() throws FileNotFoundException {
        String project = "name: name";
        when(fileReader.readFile(CONFIG_PATH)).thenReturn(project);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, BRANCH_NAME);
        assertThat(buildSet, is(empty()));
    }

    @Test(expected = WrongConfigException.class)
    public void nullTcBuilds() throws FileNotFoundException {
        String project = "tcBuilds: null";
        when(fileReader.readFile(CONFIG_PATH)).thenReturn(project);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, BRANCH_NAME);
        assertThat(buildSet, is(empty()));
    }

    @Test(expected = WrongConfigException.class)
    public void emptyTcBuilds() throws FileNotFoundException {
        String project = "tcBuilds:";
        when(fileReader.readFile(CONFIG_PATH)).thenReturn(project);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, BRANCH_NAME);
        assertThat(buildSet, is(empty()));
    }

    @Test(expected = WrongConfigException.class)
    public void wrongFormatTcBuilds() throws FileNotFoundException {
        String project = "tcBuilds: \n" + "- fakeField: value";
        when(fileReader.readFile(CONFIG_PATH)).thenReturn(project);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, BRANCH_NAME);
        assertThat(buildSet, is(empty()));
    }

    @Test(expected = WrongConfigException.class)
    public void wrongFormatRootConfig() throws FileNotFoundException {
        String config = "fakeField";
        when(fileReader.readFile("ci.yaml")).thenReturn(config);
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(pathSet, BRANCH_NAME);
        assertThat(buildSet, is(empty()));
    }
}