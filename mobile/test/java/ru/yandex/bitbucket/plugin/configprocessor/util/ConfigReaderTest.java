package ru.yandex.bitbucket.plugin.configprocessor.util;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.configprocessor.entity.Project;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ConfigReaderTest {
    private static final String CONFIG_PATH_ROOT = "ci.yaml";
    private static final String CONFIG_PATH_1 = "test/ci.yaml";
    private static final String CONFIG_PATH_2 = "test/test/ci.yaml";
    private static final String CONFIG_STRING = String.format("- %s\n- %s", CONFIG_PATH_1, CONFIG_PATH_2);
    private static final String PROJECT_1 = "name : testProject1\n"
            + "tcBuilds:\n"
            + " - name: Test Project 1 TCBuild Conf\n"
            + "   id: tc://main\n"
            + "   optional: false\n"
            + "   includedPaths:\n"
            + "    - path: testProject1/test/*\n"
            + "   excludedPaths:\n"
            + "    - path: \"*.xml\"\n"
            + "   includedTargetBranches:\n"
            + "    - branch: develop/*\n"
            + "   excludedTargetBranches:\n"
            + "    - branch: develop/feature/*\n";

    private static final String PROJECT_2 = "name : testProject2\n"
            + "tcBuilds:\n"
            + " - name: Test Project 2 TCBuild Conf\n"
            + "   id: tc://optional\n";

    @Mock
    private FileReader fileReader;

    private ConfigReader configReader;

    @Before
    public void init() throws FileNotFoundException {
        MockitoAnnotations.initMocks(this);
        configReader = new ConfigReader(fileReader);
        when(fileReader.readFile(CONFIG_PATH_ROOT)).thenReturn(CONFIG_STRING);
        when(fileReader.readFile(CONFIG_PATH_1)).thenReturn(PROJECT_1);
        when(fileReader.readFile(CONFIG_PATH_2)).thenReturn(PROJECT_2);
    }

    @Test
    public void loadRepositoryProjectList() throws FileNotFoundException {
        List<Project> res = configReader.loadRepositoryProjectList();
        Yaml yaml = new Yaml(new Constructor(Project.class));
        verify(fileReader).readFile(CONFIG_PATH_ROOT);
        verify(fileReader).readFile(CONFIG_PATH_1);
        verify(fileReader).readFile(CONFIG_PATH_2);
        Project project1 = yaml.load(PROJECT_1);
        Project project2 = yaml.load(PROJECT_2);
        assertEquals(Arrays.asList(project1, project2), res);
    }

}