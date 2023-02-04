package ru.yandex.solomon.flags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.flags.FeatureFlag.EXPRESSION_LAST_VERSION;
import static ru.yandex.solomon.flags.FeatureFlag.GRID_DOWNSAMPLING;
import static ru.yandex.solomon.flags.FeatureFlag.METADATA_FROM_DATAPROXY;
import static ru.yandex.solomon.flags.FeatureFlag.SERIES_FROM_DATAPROXY;
import static ru.yandex.solomon.flags.FeatureFlag.TEST;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertEmpty;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertHasFlag;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertHasNotFlag;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertNotEmpty;

/**
 * @author Vladimir Gordiychuk
 */
public class FeatureFlagsConfigTest {
    private FeatureFlagsConfig config;

    @Before
    public void setUp() throws Exception {
        config = new FeatureFlagsConfig();
    }

    @Test
    public void empty() {
        FeatureFlags flags = new FeatureFlags();
        assertEmpty(flags);
        config.combineFlags(flags, "projectId", "shardId", "clusterId", "serviceId", "");
        assertEmpty(flags);
        assertNull(config.define(TEST, "projectId", "shardId", "clusterId", "serviceId", ""));
    }

    @Test
    public void fromShard() {
        FeatureFlags flags = new FeatureFlags();
        config.getProjectFlags("projectId").addShardFlag("shardId", TEST, true);
        config.combineFlags(flags, "projectId", "shardId", "clusterId", "serviceId", "");
        assertHasFlag(flags, TEST);
        assertEquals("shard", config.define(TEST, "projectId", "shardId", "clusterId", "serviceId", ""));
    }

    @Test
    public void fromProject() {
        config.addProjectFlag("projectId", TEST, true);
        {
            FeatureFlags flags = new FeatureFlags();
            config.combineFlags(flags, "projectId", "shardId", "clusterId", "serviceId", "");
            assertHasFlag(flags, TEST);
            assertEquals("project", config.define(TEST, "projectId", "shardId", "clusterId", "serviceId", ""));
        }
        {
            FeatureFlags flags = config.getFlags("projectId", "sp");
            assertHasFlag(flags, TEST);
        }
    }

    @Test
    public void fromDefault() {
        config.addDefaultFlag(TEST, true);
        {
            FeatureFlags flags = new FeatureFlags();
            config.combineFlags(flags, "projectId", "shardId", "clusterId", "serviceId", "");
            assertHasFlag(flags, TEST);
            assertEquals("default", config.define(TEST, "projectId", "shardId", "clusterId", "serviceId", ""));
        }
        {
            FeatureFlags flags = config.getFlags("projectId", "sp");
            assertHasFlag(flags, TEST);
        }
    }

    @Test
    public void fromYasmDefault() {
        config.addDefaultFlag(SERIES_FROM_DATAPROXY, false);
        {
            FeatureFlags flags = new FeatureFlags();
            config.combineFlags(flags, "yasm_projectId", "shardId", "clusterId", "serviceId", "");
            assertHasFlag(flags, SERIES_FROM_DATAPROXY);
            assertHasFlag(flags, EXPRESSION_LAST_VERSION);
            assertHasFlag(flags, METADATA_FROM_DATAPROXY);
            assertHasFlag(flags, GRID_DOWNSAMPLING);
            assertEquals("yasm_default", config.define(SERIES_FROM_DATAPROXY, "yasm_projectId", "shardId", "clusterId", "serviceId", ""));
        }
        {
            FeatureFlags flags = config.getFlags("yasm_projectId", "sp");
            assertHasFlag(flags, SERIES_FROM_DATAPROXY);
        }
    }

    @Test
    public void fromServiceProvider() {
        config.addServiceProviderFlag("compute", TEST, true);
        {
            FeatureFlags flags = new FeatureFlags();
            config.combineFlags(flags, "projectId", "shardId", "clusterId", "serviceId", "compute");
            assertHasFlag(flags, TEST);
        }
        {
            String define = config.define(TEST, "projectId", "shardId", "clusterId", "serviceId", "compute");
            assertEquals("serviceProvider", define);
        }
        {
            FeatureFlags flags = config.getFlags("projectId", "compute");
            assertHasFlag(flags, TEST);
        }
    }

    @Test
    public void shardFlagHasMorePriority() {
        FeatureFlags flags = new FeatureFlags();
        config.addServiceProviderFlag("compute", TEST, true);
        config.getProjectFlags("projectId").addShardFlag("shardId", TEST, false);
        config.combineFlags(flags, "projectId", "shardId", "clusterId", "serviceId", "compute");
        assertHasNotFlag(flags, TEST);
        assertEquals("shard", config.define(TEST, "projectId", "shardId", "clusterId", "serviceId", "compute"));
    }

    @Test
    public void serializeDeserializeAsJson() {
        {
            var source = new FeatureFlagsConfig();
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "");
            assertHasNotFlag(flags, TEST);
            assertTrue(flags.isEmpty());
        }
        {
            var source = new FeatureFlagsConfig();
            source = new FeatureFlagsConfig();
            source.addDefaultFlag(TEST, false);
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsConfig();
            source.addDefaultFlag(TEST, true);
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "");
            assertHasFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsConfig();
            source = new FeatureFlagsConfig();
            source.addServiceProviderFlag("sp", TEST, false);
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "sp");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsConfig();
            source.addServiceProviderFlag("sp", TEST, true);
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "sp");
            assertHasFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsConfig();
            source.addProjectFlag("projectId", TEST, false);
            source.addProjectFlag("test", TEST, true);
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsConfig();
            source.addProjectFlag("projectId", TEST, true);
            source.addProjectFlag("test", TEST, false);
            config = serializeDeserialize(source);
            var flags = config.getFlags("projectId", "shardId", "clusterId", "serviceId", "");
            assertHasFlag(flags, TEST);
            assertNotEmpty(flags);
        }
    }

    private FeatureFlagsConfig serializeDeserialize(FeatureFlagsConfig project) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var result = mapper.writeValueAsString(project);
            System.out.println(project + " serialized as " + result);
            var r = mapper.readValue(result, FeatureFlagsConfig.class);
            assertEquals(project.toString(), r.toString());
            return r;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
