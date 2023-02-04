package ru.yandex.webmaster3.storage.sitestructure;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.sitestructure.NewSiteStructure;

/**
 * @author aherman
 */
public class StructureNameUtilTest {
    public static final long RUSSIA_NODE_ID = -1848945125L;
    public static final long NEWS_2015_NODE_ID = 1051172030L;
    public static final long NEWS_2015_04_NODE_ID = 859437301L;

    @Test
    public void testPathToAutomaticNodeId() throws Exception {
        Assert.assertEquals(NewSiteStructure.ROOT_NODE_ID, StructureNameUtil.getSiteNodeId("/"));
        Assert.assertEquals(RUSSIA_NODE_ID, StructureNameUtil.getSiteNodeId("/russia"));
        Assert.assertEquals(NEWS_2015_NODE_ID, StructureNameUtil.getSiteNodeId("/news/2015"));
        Assert.assertEquals(NEWS_2015_04_NODE_ID, StructureNameUtil.getSiteNodeId("/news/2015/04"));
    }

    @Test
    public void testGetFullPath() throws Exception {
        NodeName n1, n2, n3;
        List<NodeName> nodes = Lists.newArrayList(
                n1 = new NodeName(1, null, "/"),
                n2 = new NodeName(2, 1L, "/russia"),
                new NodeName(3, 1L, "/news"),
                new NodeName(4, 3L, "/2015"),
                n3 = new NodeName(5, 4L, "/04")
        );

        Map<Long, NodeName> nodesMap =
                nodes.stream().collect(Collectors.toMap(NodeName::getId, r -> r));

        Assert.assertEquals(
                "/",
                StructureNameUtil.getFullPath(n1, nodesMap, NodeName::getParrentId, NodeName::getName, n -> n.getParrentId() == null)
        );
        Assert.assertEquals(
                "/russia",
                StructureNameUtil.getFullPath(n2, nodesMap, NodeName::getParrentId, NodeName::getName, n -> n.getParrentId() == null)
        );
        Assert.assertEquals(
                "/news/2015/04",
                StructureNameUtil.getFullPath(n3, nodesMap, NodeName::getParrentId, NodeName::getName, n -> n.getParrentId() == null)
        );
    }

    @Test
    public void testGetFullPath1() throws Exception {
        NodeName n1 = new NodeName(1, null, "/");
        NodeName n2 = new NodeName(2, 1L, "/russia");

        Assert.assertEquals(
                "/",
                StructureNameUtil.getFullPath(n1, Collections.<Long, NodeName>emptyMap(),
                        NodeName::getParrentId, NodeName::getName, n -> n.getParrentId() == null)
        );
        Assert.assertEquals(
                "/russia",
                StructureNameUtil.getFullPath(n2, Collections.<Long, NodeName>emptyMap(),
                        NodeName::getParrentId, NodeName::getName, n -> n.getParrentId() == null)
        );
    }

    private static class NodeName {
        private final long id;
        private final Long parrentId;
        private final String name;

        public NodeName(long id, Long parrentId, String name) {
            this.id = id;
            this.parrentId = parrentId;
            this.name = name;
        }

        public long getId() {
            return id;
        }

        public Long getParrentId() {
            return parrentId;
        }

        public String getName() {
            return name;
        }
    }

}
