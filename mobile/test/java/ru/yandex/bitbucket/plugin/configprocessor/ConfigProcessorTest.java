package ru.yandex.bitbucket.plugin.configprocessor;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.configprocessor.util.ConfigReader;
import ru.yandex.bitbucket.plugin.configprocessor.util.FileReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


public class ConfigProcessorTest {
    private static final String PROJECT_CONFIG_PATH = "src/test/resources/ru/yandex/bitbucket/plugin/configprocessor/buildInfoSortTest.yaml";
    private static final String ROOT_CONFIG_STRING = String.format("- %s", PROJECT_CONFIG_PATH);

    @Mock
    private FileReader fileReader;

    private ConfigProcessor configProcessor;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        ConfigReader configReader = new ConfigReader(fileReader);
        configProcessor = new ConfigProcessor(configReader);

        File configFile = new File(PROJECT_CONFIG_PATH);
        InputStream configStream = new FileInputStream(configFile);
        String configString = new BufferedReader(new InputStreamReader(configStream))
                .lines().collect(Collectors.joining("\n"));
        when(fileReader.readFile("ci.yaml")).thenReturn(ROOT_CONFIG_STRING);
        when(fileReader.readFile(PROJECT_CONFIG_PATH)).thenReturn(configString);
    }

    @Test
    public void buildInfoSortTest() throws FileNotFoundException {
        Set<ConfigTCBuild> buildSet = configProcessor.getBuildInfoByChangingPath(Collections.singleton("path"), "branch");
        List<ConfigTCBuild> buildList = new ArrayList<>(buildSet);
        assertEquals("First Name 2", buildList.get(0).getName());
        assertEquals("First Name 1", buildList.get(1).getName());
        assertEquals("Last Name", buildList.get(2).getName());
    }
}