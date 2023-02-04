package ru.yandex.realty.util.clusterization;

import org.junit.Test;
import ru.yandex.common.util.graph.GraphUtils;
import ru.yandex.common.util.graph.IncidenceFunction;
import ru.yandex.realty.util.clusterization.Clusterizer;
import ru.yandex.realty.util.clusterization.CriterionBuilder;
import ru.yandex.realty.util.clusterization.Storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static ru.yandex.common.util.collections.CollectionFactory.newHashSet;
import static ru.yandex.common.util.collections.CollectionFactory.newLinkedList;

/**
 * author: rmuzhikov
 */
public class ClusterizerTest {
    @Test
    public void testFindConnectedComponents() {
        final boolean[][] graphEdges = getGraphEdges();

        AbstractStorage<Integer, Integer> graphStorage = new AbstractStorage<Integer, Integer>() {
            //find all vertex that are adjacent to given
            @Override
            public Iterable<Integer> find(Integer vertex) {
                List<Integer> linkedVertexes = newLinkedList();
                int i = 0;
                for(boolean hasEdge : graphEdges[vertex]) {
                    if (hasEdge) {
                        linkedVertexes.add(i);
                    }
                    ++i;
                }
                return linkedVertexes;
            }

            @Override
            public Iterator<Integer> iterator() {
                List<Integer> list = new ArrayList<Integer>();
                for(int i = 0; i < graphEdges.length; ++i) {
                    list.add(i);
                }
                return list.iterator();
            }
        };

        CriterionBuilder<Integer, Integer> adjacentCriterionBuilder = vertex -> vertex;

        final int[] connectedComponents = GraphUtils.findConnectedComponents(graphEdges.length, (i, j) -> graphEdges[i][j]);

        int handledVertexes = 0;
        GraphClusterizer clusterizer = new GraphClusterizer(graphStorage, adjacentCriterionBuilder, graphEdges.length);
        for (List<Integer> cc : clusterizer) {
            Set<Integer> components = newHashSet();
            for (Integer vertex : cc) {
                components.add(connectedComponents[vertex]);
                ++handledVertexes;
            }
            assertEquals(1, components.size());
        }
        assertEquals(graphEdges.length, handledVertexes);
    }

    private boolean[][] getGraphEdges() {
        boolean[][] graphEdges = new boolean[7][7];
        graphEdges[0] = new boolean[]{true, true, true, false, false, false, false};
        graphEdges[1] = new boolean[]{true, true, true, false, false, false, false};
        graphEdges[2] = new boolean[]{true, true, true, false, false, false, false};
        graphEdges[3] = new boolean[]{false, false, false, true, true, true , false};
        graphEdges[4] = new boolean[]{false, false, false, true, true, false, false};
        graphEdges[5] = new boolean[]{false, false, false, true, false, true, false};
        graphEdges[6] = new boolean[]{false, false, false, false, false, false, true};
        return graphEdges;
    }

    private class GraphClusterizer extends Clusterizer<Integer, Integer, Integer> {
        private final int numVertices;
        public GraphClusterizer(Storage<Integer, Integer ,Integer> storage, CriterionBuilder<Integer, Integer> criterionBuilder, final int numVertices)
        {
            super(storage, criterionBuilder);
            this.numVertices = numVertices;
        }

        @Override
        protected Clusterizer.State<Integer, Integer> init() {
            return  new State<Integer, Integer>() {
                private boolean[] visited = new boolean[numVertices];
                private boolean[] usedCriterions  = new boolean[numVertices];

                @Override
                public void setVisited(Integer vertex) {
                    visited[vertex] = true;
                }

                @Override
                public boolean isNotVisited(Integer vertex) {
                    return !visited[vertex];
                }

                @Override
                public void setUsed(Integer criterion) {
                    usedCriterions[criterion] = true;
                }

                @Override
                public boolean isUsed(Integer criterion) {
                    return usedCriterions[criterion];
                }

                @Override
                public void clearUsedCriterions() {}
            };
        }
    }
}

