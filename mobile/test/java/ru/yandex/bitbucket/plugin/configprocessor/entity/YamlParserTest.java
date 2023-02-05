package ru.yandex.bitbucket.plugin.configprocessor.entity;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(JUnitParamsRunner.class)
public class YamlParserTest {
    private static final String PROJECT_1 = "tcBuilds: \n"
            + " - optional: null \n"
            + "   includedPaths: null \n"
            + "   excludedPaths: null \n"
            + "   includedTargetBranches: null \n"
            + "   excludedTargetBranches: null \n";

    private Project readYaml(InputStream inputStream) {
        Yaml yaml = new Yaml(new Constructor(Project.class));
        return yaml.load(inputStream);
    }

    private InputStream readFile(String path) throws FileNotFoundException {
        File file = new File(path);
        return new FileInputStream(file);
    }

    @Test(expected = ConstructorException.class)
    @Parameters({
            "name: null",
            "tcBuilds: null",
            "tcBuilds: \n - name: null",
            "tcBuilds: \n - id: null"
    })
    public void nullNonnullParameters(String projectConfig) {
        readYaml(new ByteArrayInputStream(projectConfig.getBytes()));
    }

    @Test
    public void parseYamlDefaultValues() {
        Project project = readYaml(new ByteArrayInputStream(PROJECT_1.getBytes()));
        assertNull(project.getName());
        List<ConfigTCBuild> buildList = project.getTcBuilds();
        assertEquals(1, buildList.size());
        ConfigTCBuild build = buildList.get(0);
        assertNull(build.getName());
        assertNull(build.getId());
        assertFalse(build.getOptional());
        assertThat(build.getIncludedPaths(), is(empty()));
        assertThat(build.getExcludedPaths(), is(empty()));
        assertThat(build.getIncludedTargetBranches(), is(empty()));
        assertThat(build.getExcludedTargetBranches(), is(empty()));
    }

    @Test
    public void parseYamlAllFields() throws FileNotFoundException {
        InputStream inputStream = readFile("src/test/resources/ru/yandex/bitbucket/plugin/configprocessor/entity/yamlParserTest.yaml");
        Project project = readYaml(inputStream);
        assertEquals("testProject", project.getName());
        List<ConfigTCBuild> buildList = project.getTcBuilds();

        ConfigTCBuild build1 = buildList.get(0);
        assertEquals("Main TCBuild Configuration", build1.getName());
        assertEquals("tc://main", build1.getId());
        assertFalse(build1.getOptional());

        ConfigTCBuild build2 = buildList.get(1);
        assertEquals("Test TCBuild Configuration", build2.getName());
        assertEquals("tc://test", build2.getId());
        assertTrue(build2.getOptional());
        assertEquals(build2.getIncludedPaths(),
                Arrays.asList(new Path("level1/test1"), new Path("level1/test2/*")));
        assertEquals(build2.getExcludedPaths(),
                Arrays.asList(new Path("*.xml"), new Path("level1/test3/*")));
        assertEquals(build2.getIncludedTargetBranches(),
                Arrays.asList(new Branch("test1"), new Branch("test2/*")));
        assertEquals(build2.getExcludedTargetBranches(),
                Arrays.asList(new Branch("*/test3"), new Branch("test4")));
    }
}
