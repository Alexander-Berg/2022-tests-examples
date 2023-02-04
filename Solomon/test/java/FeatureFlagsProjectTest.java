package ru.yandex.solomon.flags;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.flags.FeatureFlag.TEST;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertEmpty;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertHasFlag;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertHasNotFlag;
import static ru.yandex.solomon.flags.FeatureFlagsMatchers.assertNotEmpty;

/**
 * @author Vladimir Gordiychuk
 */
public class FeatureFlagsProjectTest {

    private FeatureFlagsProject project;

    @Before
    public void setUp() throws Exception {
        project = new FeatureFlagsProject(new FeatureFlags());
    }

    @Test
    public void empty() {
        FeatureFlags flags = new FeatureFlags();
        assertEmpty(flags);
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        assertEmpty(flags);
    }

    @Test
    public void fromShard() {
        FeatureFlags flags = new FeatureFlags();
        project.addShardFlag("shardId", TEST, true);
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        assertHasFlag(flags, TEST);
    }

    @Test
    public void fromCluster() {
        FeatureFlags flags = new FeatureFlags();
        project.addClusterFlag("clusterId", TEST, true);
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        assertHasFlag(flags, TEST);
    }

    @Test
    public void fromService() {
        FeatureFlags flags = new FeatureFlags();
        project.addServiceFlag("serviceId", TEST, true);
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        assertHasFlag(flags, TEST);
    }

    @Test
    public void fromProject() {
        FeatureFlags flags = new FeatureFlags();
        project.addProjectFlag(TEST, true);
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        project.combineFlags(flags, "shardId", "clusterId", "serviceId");
        assertHasFlag(flags, TEST);
    }

    @Test
    public void shardWin() {
        {
            FeatureFlags flags = new FeatureFlags();
            project.addShardFlag("shardId", TEST, true);
            project.addClusterFlag("clusterId", TEST, false);
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasFlag(flags, TEST);
        }
        {
            FeatureFlags flags = new FeatureFlags();
            project.addShardFlag("shardId", TEST, false);
            project.addClusterFlag("clusterId", TEST, true);
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
        }
    }

    @Test
    public void clusterWin() {
        {
            FeatureFlags flags = new FeatureFlags();
            project.addClusterFlag("clusterId", TEST, true);
            project.addServiceFlag("serviceId", TEST, false);
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasFlag(flags, TEST);
        }
        {
            FeatureFlags flags = new FeatureFlags();
            project.addClusterFlag("clusterId", TEST, false);
            project.addServiceFlag("serviceId", TEST, true);
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
        }
    }

    @Test
    public void serviceWin() {
        {
            FeatureFlags flags = new FeatureFlags();
            project.addServiceFlag("serviceId", TEST, true);
            project.addProjectFlag(TEST, false);
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasFlag(flags, TEST);
        }
        {
            FeatureFlags flags = new FeatureFlags();
            project.addServiceFlag("serviceId", TEST, false);
            project.addProjectFlag(TEST, true);
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
        }
    }

    @Test
    public void serializeDeserializeAsJson() {
        {
            project = serializeDeserialize(new FeatureFlagsProject(FeatureFlags.EMPTY));
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
            assertTrue(flags.isEmpty());
        }
        {
            project = serializeDeserialize(new FeatureFlagsProject(new FeatureFlags()));
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
            assertTrue(flags.isEmpty());
        }
        {
            var source = new FeatureFlagsProject(new FeatureFlags());
            source.addProjectFlag(TEST, true);
            project = serializeDeserialize(source);
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasFlag(flags, TEST);
        }
        {
            var source = new FeatureFlagsProject(new FeatureFlags());
            source.addProjectFlag(TEST, false);
            project = serializeDeserialize(source);
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsProject(new FeatureFlags());
            source.addShardFlag("shardId", TEST, false);
            source.addShardFlag("test", TEST, true);
            project = serializeDeserialize(source);
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsProject(new FeatureFlags());
            source.addClusterFlag("clusterId", TEST, false);
            source.addClusterFlag("test", TEST, true);
            project = serializeDeserialize(source);
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
        {
            var source = new FeatureFlagsProject(new FeatureFlags());
            source.addServiceFlag("serviceId", TEST, false);
            source.addServiceFlag("test", TEST, true);
            project = serializeDeserialize(source);
            FeatureFlags flags = new FeatureFlags();
            project.combineFlags(flags, "shardId", "clusterId", "serviceId");
            assertHasNotFlag(flags, TEST);
            assertNotEmpty(flags);
        }
    }

    private FeatureFlagsProject serializeDeserialize(FeatureFlagsProject project) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            var result = mapper.writeValueAsString(project);
            System.out.println(project + " serialized as " + result);
            var r = mapper.readValue(result, FeatureFlagsProject.class);
            assertEquals(project.toString(), r.toString());
            return r;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
