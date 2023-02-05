import { Int32, range } from '../../../../../../common/ys'
import { Edge } from '../data-structures/edge'
import { CompressedGraph } from '../data-structures/graph'
import { Stack } from '../data-structures/stack'
import { TopSortAlgo } from './topsort-algo'

export class StrongComponentAlgo {
  public static getStrongConnectedComponents<T>(graph: CompressedGraph<T>): Stack<Set<Int32>> {
    const reversedGraph: CompressedGraph<T> = new CompressedGraph()
    for (const edge of graph.edges) {
      reversedGraph.addEdge(edge.getTo(), edge.getFrom(), edge.getAction())
    }

    const topSort = TopSortAlgo.getTopSort(graph)

    const used: Set<Int32> = new Set<Int32>()
    const components: Stack<Set<Int32>> = new Stack<Set<Int32>>()
    while (topSort.size() > 0) {
      const vertex = topSort.top()
      topSort.pop()
      if (!used.has(vertex)) {
        const component = StrongComponentAlgo.backwardDFS(reversedGraph, vertex, used)
        components.push(component)
      }
    }
    return components
  }

  public static getCondensedGraph<T>(graph: CompressedGraph<T>): CompressedGraph<Edge<Int32, T>> {
    const condensed: CompressedGraph<Edge<Int32, T>> = new CompressedGraph()
    const components = StrongComponentAlgo.getStrongConnectedComponents(graph)
    const vertexToComponent: Map<Int32, Int32> = new Map<Int32, Int32>()
    for (const i of range(0, components.size())) {
      for (const vertex of components.get(i).values()) {
        vertexToComponent.set(vertex, i)
      }
    }
    for (const edge of graph.edges) {
      const from = edge.getFrom()
      const to = edge.getTo()
      const componentFrom = vertexToComponent.get(from)!
      const componentTo = vertexToComponent.get(to)!
      if (componentFrom !== componentTo) {
        condensed.addEdge(componentFrom, componentTo, edge)
      }
    }
    return condensed
  }

  private static backwardDFS<T>(graph: CompressedGraph<T>, vertex: Int32, used: Set<Int32>): Set<Int32> {
    const component: Set<Int32> = new Set<Int32>()
    used.add(vertex)
    component.add(vertex)
    const stack: Stack<Int32> = new Stack<Int32>()
    stack.push(vertex)
    while (stack.size() > 0) {
      const current = stack.top()
      stack.pop()
      for (const edgeId of graph.getEdgesId(current)) {
        const to = graph.edges[edgeId].getTo()
        if (!used.has(to)) {
          stack.push(to)
          used.add(to)
          component.add(to)
        }
      }
    }
    return component
  }
}
