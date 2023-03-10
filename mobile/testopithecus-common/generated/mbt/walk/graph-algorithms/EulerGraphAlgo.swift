// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/walk/graph-algorithms/euler-graph-algo.ts >>>

import Foundation

open class EulerGraphAlgo {
  @discardableResult
  open class func isThereEulerCircleInComponent<T>(_ graph: CompressedGraph<T>, _ component: YSSet<Int32>) -> Bool {
    let balance = EulerGraphAlgo.getVertexBalance(graph, component)
    for value in balance.values() {
      if value != 0 {
        return false
      }
    }
    return true
  }

  @discardableResult
  open class func getEulerCircleInComponent<T>(_ graph: CompressedGraph<T>, _ component: YSSet<Int32>, _ start: Int32) -> YSArray<Int32> {
    EulerGraphAlgo.createEulerCircleInComponent(graph, component)
    let path: YSArray<Int32> = YSArray()
    let stack: Stack<VertexAndLastEdgeId> = Stack()
    stack.push(VertexAndLastEdgeId(start, -1))
    let used: YSSet<Int32> = YSSet<Int32>()
    while stack.size() > 0 {
      let vertex = stack.top().vertex
      let action = stack.top().lastEdgeId
      var flag = true
      for edgeId in graph.getEdgesId(vertex) {
        let to = graph.edges[edgeId].getTo()
        if !used.has(edgeId) && component.has(to) {
          stack.push(VertexAndLastEdgeId(to, edgeId))
          used.add(edgeId)
          flag = false
          break
        }
      }
      if flag {
        stack.pop()
        path.push(action)
      }
    }
    path.pop()
    return path.reverse()
  }

  @discardableResult
  private class func getVertexBalance<T>(_ graph: CompressedGraph<T>, _ component: YSSet<Int32>) -> YSMap<Int32, Int32> {
    let balance: YSMap<Int32, Int32> = YSMap<Int32, Int32>()
    for vertex in component.values() {
      balance.set(vertex, 0)
    }
    for edge in graph.edges {
      let from = edge.getFrom()
      let to = edge.getTo()
      if component.has(from) && component.has(to) {
        balance.set(from, balance.`get`(from)! - 1)
        balance.set(to, balance.`get`(to)! + 1)
      }
    }
    return balance
  }

  private class func createEulerCircleInComponent<T>(_ graph: CompressedGraph<T>, _ component: YSSet<Int32>) -> Void {
    let balance = EulerGraphAlgo.getVertexBalance(graph, component)
    var disbalance: Int32 = 0
    for value in balance.values() {
      if value > 0 {
        disbalance += value
      }
    }
    if disbalance == 0 {
      return
    }
    let ends: YSSet<Int32> = YSSet<Int32>()
    let starts: YSSet<Int32> = YSSet<Int32>()
    for vertex in balance.keys() {
      if balance.`get`(vertex)! < 0 {
        ends.add(vertex)
      }
      if balance.`get`(vertex)! > 0 {
        starts.add(vertex)
      }
    }
    for vertex in starts.values() {
      let distances = DistanceAlgo.getDistances(graph, vertex, component)
      let paths: Stack<YSArray<Int32>> = Stack()
      let desc: YSArray<PathDescription> = YSArray()
      for endVertex in ends.values() {
        let pathToEnd = DistanceAlgo.getPathTo(endVertex, distances, graph)
        desc.push(PathDescription(pathToEnd.length, paths.size(), endVertex))
        paths.push(pathToEnd)
      }
      desc.sort({
        (a, b) in
        a.length - b.length
      })
      var currentBalance: Int32 = balance.`get`(vertex)!
      var index: Int32 = 0
      while currentBalance > 0 {
        let pathDescription = desc[index]
        let pathId = pathDescription.index
        if paths.`get`(pathId).length > 0 {
          let to = pathDescription.endVertex
          if balance.get(to) != 0 {
            for edgeId in paths.`get`(pathId) {
              let edge = graph.edges[edgeId]
              graph.addEdge(edge.getFrom(), edge.getTo(), edge.getAction())
            }
            balance.set(to, balance.`get`(to)! + 1)
            index -= 1
            currentBalance -= 1
          } else {
            ends.delete(to)
          }
        }
        index += 1
      }
      balance.set(vertex, 0)
    }
  }

}

fileprivate class PathDescription {
  public var length: Int32
  public var index: Int32
  public var endVertex: Int32
  public init(_ length: Int32, _ index: Int32, _ endVertex: Int32) {
    self.length = length
    self.index = index
    self.endVertex = endVertex
  }

}

fileprivate class VertexAndLastEdgeId {
  public var vertex: Int32
  public var lastEdgeId: Int32
  public init(_ vertex: Int32, _ lastEdgeId: Int32) {
    self.vertex = vertex
    self.lastEdgeId = lastEdgeId
  }

}

