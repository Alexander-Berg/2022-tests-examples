import { Logger } from '../../../../../common/code/logging/logger'
import { Int32, range } from '../../../../../../common/ys'
import { CompressedGraph } from '../data-structures/graph'
import { Stack } from '../data-structures/stack'
import { DistanceAlgo } from './distance-algo'
import { EulerGraphAlgo } from './euler-graph-algo'
import { StrongComponentAlgo } from './strong-component-algo'
import { TopSortAlgo } from './topsort-algo'

export class LongestPathAlgo {
  public static getLongestPath<T>(graph: CompressedGraph<T>, logger: Logger): T[] {
    if (graph.size() === 0 || graph.countOfEdges() === 0) {
      return []
    }

    const components = StrongComponentAlgo.getStrongConnectedComponents(graph)
    const condensedGraph = StrongComponentAlgo.getCondensedGraph(graph)
    const longestPath = LongestPathAlgo.getLongestPathInCondensedGraph(condensedGraph, components)

    let currentComponent: Int32 = 0
    let currentVertex: Int32 = 0
    for (const i of range(0, components.size())) {
      if (components.get(i).has(currentVertex)) {
        currentComponent = i
      }
    }
    logger.info(`Current component id = ${currentComponent}, size = ${components.get(currentComponent).size}`)
    let path: T[] = EulerGraphAlgo.getEulerCircleInComponent(
      graph,
      components.get(currentComponent),
      currentVertex,
    ).map((eid) => graph.edges[eid].getAction())
    let edgeId: Int32

    while (longestPath.getEdgesId(currentComponent).length > 0) {
      edgeId = longestPath.getEdgesId(currentComponent)[0]
      const condensedEdge = longestPath.edges[edgeId]
      const edge = condensedEdge.getAction()
      const distances = DistanceAlgo.getDistances(graph, currentVertex, components.get(currentComponent))
      const connectedPath = DistanceAlgo.getPathTo(edge.getFrom(), distances, graph).map((eid) =>
        graph.edges[eid].getAction(),
      )
      path = path.concat(connectedPath)
      path.push(edge.getAction())

      currentComponent = condensedEdge.getTo()
      currentVertex = edge.getTo()

      logger.info(`Current component id = ${currentComponent}, size = ${components.get(currentComponent).size}`)
      const componentPath = EulerGraphAlgo.getEulerCircleInComponent(
        graph,
        components.get(currentComponent),
        currentVertex,
      ).map((eid) => graph.edges[eid].getAction())
      path = path.concat(componentPath)
    }
    return path
  }

  private static getLongestPathInCondensedGraph<T, VertexType>(
    condensed: CompressedGraph<T>,
    components: Stack<Set<VertexType>>,
  ): CompressedGraph<T> {
    const path: CompressedGraph<T> = new CompressedGraph()
    const size: Int32[] = []
    for (const _ of range(0, condensed.size())) {
      size.push(0)
    }

    const topSort = TopSortAlgo.getTopSort(condensed)
    for (const i of range(0, topSort.size())) {
      const vertex = topSort.get(i)
      size[vertex] += components.get(vertex).size
      let mx: Int32 = -1
      let edgeIndex: Int32 = -1
      for (const edgeId of condensed.getEdgesId(vertex)) {
        const to = condensed.edges[edgeId].getTo()
        if (size[to] > mx) {
          mx = size[to]
          edgeIndex = edgeId
        }
      }
      if (edgeIndex >= 0) {
        size[vertex] += mx
        path.addEdge(vertex, condensed.edges[edgeIndex].getTo(), condensed.edges[edgeIndex].getAction())
      }
    }
    return path
  }
}
