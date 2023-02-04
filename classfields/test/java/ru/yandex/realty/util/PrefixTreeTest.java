package ru.yandex.realty.util;

import org.apache.commons.lang.Validate;
import org.junit.Test;

import java.util.Arrays;

public class PrefixTreeTest {

    @Test
    public void testEmpty() throws Exception {
        PrefixTree<String> empty = new PrefixTree<>();
        Validate.isTrue(empty.getWithPrefix("test").isEmpty());

    }

    @Test
    public void testGetWithPrefix() throws Exception {
        String[] data = new String[] {
                "Top",
                "Hop",
                "Tor",
                "HipHop",
                "Rock",
                "Rage",
                "Rate",
                "R"
        };
        PrefixTree<String> trie = new PrefixTree<>();
        for (String cur : data) {
            trie.set(cur, cur);
        }

        Validate.isTrue(trie.getWithPrefix("ABC").size() == 0);

        Validate.isTrue(trie.getWithPrefix("Top").size() == 1);
        Validate.isTrue(trie.getWithPrefix("Top").contains("Top"));

        Validate.isTrue(trie.getWithPrefix("To").size() == 2);
        Validate.isTrue(trie.getWithPrefix("To").contains("Top"));
        Validate.isTrue(trie.getWithPrefix("To").contains("Tor"));

        Validate.isTrue(trie.getWithPrefix("R").size() == 4);
        Validate.isTrue(trie.getWithPrefix("R").contains("R"));
        Validate.isTrue(trie.getWithPrefix("R").contains("Rate"));
        Validate.isTrue(trie.getWithPrefix("R").contains("Rage"));
        Validate.isTrue(trie.getWithPrefix("R").contains("Rock"));

        Validate.isTrue(trie.getWithPrefix("").size() == data.length);
        Validate.isTrue(trie.getWithPrefix("").containsAll(Arrays.asList(data)));

    }

//    private static Set<GeoObjectType> SUPPORTED_GEO_TYPES = EnumSet.of(
//            GeoObjectType.CITY,
//            GeoObjectType.SUBJECT_FEDERATION,
//            GeoObjectType.CITY_DISTRICT,
//            GeoObjectType.SUBJECT_FEDERATION_DISTRICT,
//            GeoObjectType.METRO_STATION,
//            GeoObjectType.NOT_ADMINISTRATIVE_DISTRICT
//    );
//
//    private static void bfs(RegionGraph regionGraph, Node node, Callback<Node> callback) {
//        Queue<Node> queue = newLinkedList();
//        Set<Node> visited = newHashSet();
//        queue.add(node);
//        visited.add(node);
//        while (!queue.isEmpty()) {
//            Node cur = queue.poll();
//            callback.doWith(cur);
//            for (Node child : regionGraph.getChildrenNodes(cur)) {
//                if (!visited.contains(child)) {
//                    queue.add(child);
//                    visited.add(child);
//                }
//            }
//        }
//    }
//
//
//    @Test
//    public void testGetWithPrefix1() throws Exception {
//
//        RegionGraph regionGraph = RegionGraphIO.read(new File("/home/rmuzhikov/region_graph-6-445"), RegionGraphFeature.ALL_FEATURES);
//        final PrefixTree<Set<Long>> prefixTree = new PrefixTree<>();
//        bfs(regionGraph, regionGraph.getNodeByGeoId(Regions.RUSSIA), new Callback<Node>() {
//            @Override
//            public void doWith(Node node) {
//                if (!SUPPORTED_GEO_TYPES.contains(node.getType())) {
//                    return;
//                }
//                String lcName = node.getName().getFullName().toLowerCase();
//                addNode(prefixTree, node.getId(), lcName.trim());
//
//                StringTokenizer st = new StringTokenizer(lcName, " \t-", false);
//                while (st.hasMoreTokens()) {
//                    addNode(prefixTree, node.getId(), st.nextToken());
//                }
//            }
//        });
//        Thread.sleep(10000000);
//        printWithTime("мос", prefixTree);
////        printWithTime("ленин", prefixTree);
////        printWithTime("мос", prefixTree);
////        printWithTime("rfht", prefixTree);
//
//    }
//
//    @Test
//    public void testGetWithPrefix2() throws Exception {
//        SitesGroupingService sitesGroupingService = new SitesGroupingService();
//        sitesGroupingService.setLoader(new DataLoader() {
//            @Override
//            public InputStream load() throws DataLoadException {
//                try {
//                    return new FileInputStream(new File("/home/rmuzhikov/sites-20-839"));
//                } catch (IOException e) {
//                    throw new DataLoadException(e);
//                }
//            }
//        });
//        sitesGroupingService.afterPropertiesSet();
//        PrefixTree<Set<Site>> prefixTree = new PrefixTree<>();
//        for (Site site : sitesGroupingService.getAllSites()) {
//            Set<String> names = newHashSet();
//            names.add(site.getName());
//            names.addAll(site.getAliases());
//            for (String name : names) {
//                String lcName = name.toLowerCase();
//                addNode(prefixTree, site, lcName.trim());
//
//                StringTokenizer st = new StringTokenizer(lcName, " \t-", false);
//                while (st.hasMoreTokens()) {
//                    addNode(prefixTree, site, st.nextToken());
//                }
//            }
//        }
//        Thread.sleep(10000000);
//        printWithTime("грин", prefixTree);
//        printWithTime("ленин", prefixTree);
//        printWithTime("мос", prefixTree);
//
//    }
//
//    private static <T> void addNode(PrefixTree<Set<T>> prefixTree, T node, String text) {
//        Set<T> nodes = prefixTree.get(text);
//        if (nodes == null) {
//            nodes = newHashSet();
//            prefixTree.set(text, nodes);
//        }
//        nodes.add(node);
//    }
//
//    private static <T> void printWithTime(String text, PrefixTree<Set<T>> prefixTree) {
//        long startTime = System.currentTimeMillis();
//        System.out.println(prefixTree.getWithPrefix(text));
//        System.out.println(System.currentTimeMillis() - startTime);
//        System.out.println();
//    }
}