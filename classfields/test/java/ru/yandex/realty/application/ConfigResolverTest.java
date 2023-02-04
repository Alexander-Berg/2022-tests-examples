package ru.yandex.realty.application;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;

import ru.yandex.common.util.collections.Cf;

import static org.junit.Assert.assertNotNull;

/**
 * @author aherman
 */
public class ConfigResolverTest {
    private ConfigRoot configRoot;
    private File configRootFile;

    @Before
    public void setup() {
        configRootFile = new File("src/test/resources/config");
        configRoot = new ConfigRoot(configRootFile);
    }

    @Test
    public void testResolvePropertiesDevelopment() {
        List<String> components = Cf.list(
                "realty-test-config1",
                "realty-test-config2"
        );
        List<Resource> properties = ConfigResolver.resolveProperties(configRoot, EnvironmentType.DEVELOPMENT, components);
        assertNotNull(properties);
        Assert.assertEquals(2, properties.size());
    }

    @Test
    public void testResolvePropertiesProduction() {
        List<String> components = Cf.list(
                "realty-test-config1",
                "realty-test-config2"
        );
        List<Resource> properties = ConfigResolver.resolveProperties(configRoot, EnvironmentType.PRODUCTION, components);
        assertNotNull(properties);
        Assert.assertEquals(1, properties.size());
    }

    @Test
    public void testResolveContextsDevelopment() {
        List<String> components = Cf.list(
                "classpath:realty-test-config1",
                "realty-test-config2"
        );
        List<Resource> contexts = ConfigResolver.resolveSpringConfigs(configRoot, EnvironmentType.DEVELOPMENT, components);
        assertNotNull(contexts);
        Assert.assertEquals(4, contexts.size());
    }

    @Test
    public void testResolveContextsProduction() {
        List<String> components = Cf.list(
                "realty-test-config1",
                "classpath:realty-test-config2"
        );
        List<Resource> contexts = ConfigResolver.resolveSpringConfigs(configRoot, EnvironmentType.PRODUCTION, components);
        assertNotNull(contexts);
        Assert.assertEquals(3, contexts.size());
    }

    @Test
    public void testAbsolutePath() {
        List<String> components = Cf.list(
                "file:" + new File(configRootFile, "realty-test-config1").getAbsolutePath(),
                "classpath:realty-test-config2"
        );
        List<Resource> contexts = ConfigResolver.resolveSpringConfigs(configRoot, EnvironmentType.PRODUCTION, components);
        assertNotNull(contexts);
        Assert.assertEquals(3, contexts.size());
    }
}
