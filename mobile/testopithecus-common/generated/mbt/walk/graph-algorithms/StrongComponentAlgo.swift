// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/graph-algorithms/strong-component-algo.ts >>>

import Foundation

open class StrongComponentAlgo {
  @discardableResult
  open class func getStrongConnectedComponents<T>(_ graph: CompressedGraph<T>) -> Stack<YSSet<Int32>> {
    let reversedGraph: CompressedGraph<T> = CompressedGraph()
    for edge in graph.edges {
      reversedGraph.addEdge(edge.getTo(), edge.getFrom(), edge.getAction())
    }
    let topSort = TopSortAlgo.getTopSort(graph)
    let used: YSSet<Int32> = YSSet<Int32>()
    let components: Stack<YSSet<Int32>> = Stack<YSSet<Int32>>()
    while topSort.size() > 0 {
      let vertex = topSort.top()
      topSort.pop()
      if !used.has(vertex) {
        let component = StrongComponentAlgo.backwardDFS(reversedGraph, vertex, used)
        components.push(component)
      }
    }
    return components
  }

  @discardableResult
  open class func getCondensedGraph<T>(_ graph: CompressedGraph<T>) -> CompressedGraph<Edge<Int32, T>> {
    let condensed: CompressedGraph<Edge<Int32, T>> = CompressedGraph()
    let components = StrongComponentAlgo.getStrongConnectedComponents(graph)
    let vertexToComponent: YSMap<Int32, Int32> = YSMap<Int32, Int32>()
    for i in stride(from: 0, to: components.size(), by: 1) {
      for vertex in components.`get`(i).values() {
        vertexToComponent.set(vertex, i)
      }
    }
    for edge in graph.edges {
      let from = edge.getFrom()
      let to = edge.getTo()
      let componentFrom = vertexToComponent.`get`(from)!
      let componentTo = vertexToComponent.`get`(to)!
      if componentFrom != componentTo {
        condensed.addEdge(componentFrom, componentTo, edge)
      }
    }
    return condensed
  }

  @discardableResult
  private class func backwardDFS<T>(_ graph: CompressedGraph<T>, _ vertex: Int32, _ used: YSSet<Int32>) -> YSSet<Int32> {
    let component: YSSet<Int32> = YSSet<Int32>()
    used.add(vertex)
    component.add(vertex)
    let stack: Stack<Int32> = Stack<Int32>()
    stack.push(vertex)
    while stack.size() > 0 {
      let current = stack.top()
      stack.pop()
      for edgeId in graph.getEdgesId(current) {
        let to = graph.edges[edgeId].getTo()
        if !used.has(to) {
          stack.push(to)
          used.add(to)
          component.add(to)
        }
      }
    }
    return component
  }

}

