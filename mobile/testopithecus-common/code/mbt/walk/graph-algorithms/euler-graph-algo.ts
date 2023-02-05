import { Int32 } from '../../../../../../common/ys'
import { CompressedGraph } from '../data-structures/graph'
import { Stack } from '../data-structures/stack'
import { DistanceAlgo } from './distance-algo'

export class EulerGraphAlgo {
  public static isThereEulerCircleInComponent<T>(graph: CompressedGraph<T>, component: Set<Int32>): boolean {
    const balance = EulerGraphAlgo.getVertexBalance(graph, component)
    for (const value of balance.values()) {
      if (value !== 0) {
        return false
      }
    }
    return true
  }

  public static getEulerCircleInComponent<T>(graph: CompressedGraph<T>, component: Set<Int32>, start: Int32): Int32[] {
    EulerGraphAlgo.createEulerCircleInComponent(graph, component)
    const path: Int32[] = []
    const stack: Stack<VertexAndLastEdgeId> = new Stack()
    stack.push(new VertexAndLastEdgeId(start, -1))
    const used: Set<Int32> = new Set<Int32>()
    while (stack.size() > 0) {
      const vertex = stack.top().vertex
      const action = stack.top().lastEdgeId
      let flag = true
      for (const edgeId of graph.getEdgesId(vertex)) {
        const to = graph.edges[edgeId].getTo()
        if (!used.has(edgeId) && component.has(to)) {
          stack.push(new VertexAndLastEdgeId(to, edgeId))
          used.add(edgeId)
          flag = false
          break
        }
      }
      if (flag) {
        stack.pop()
        path.push(action)
      }
    }
    path.pop()
    return path.reverse()
  }

  private static getVertexBalance<T>(graph: CompressedGraph<T>, component: Set<Int32>): Map<Int32, Int32> {
    const balance: Map<Int32, Int32> = new Map<Int32, Int32>()
    for (const vertex of component.values()) {
      balance.set(vertex, 0)
    }
    for (const edge of graph.edges) {
      const from = edge.getFrom()
      const to = edge.getTo()
      if (component.has(from) && component.has(to)) {
        balance.set(from, balance.get(from)! - 1)
        balance.set(to, balance.get(to)! + 1)
      }
    }
    return balance
  }

  private static createEulerCircleInComponent<T>(graph: CompressedGraph<T>, component: Set<Int32>): void {
    const balance = EulerGraphAlgo.getVertexBalance(graph, component)
    let disbalance: Int32 = 0
    for (const value of balance.values()) {
      if (value > 0) {
        disbalance += value
      }
    }
    if (disbalance === 0) {
      return
    }

    const ends: Set<Int32> = new Set<Int32>()
    const starts: Set<Int32> = new Set<Int32>()
    for (const vertex of balance.keys()) {
      if (balance.get(vertex)! < 0) {
        ends.add(vertex)
      }
      if (balance.get(vertex)! > 0) {
        starts.add(vertex)
      }
    }

    for (const vertex of starts.values()) {
      const distances = DistanceAlgo.getDistances(graph, vertex, component)
      const paths: Stack<Int32[]> = new Stack()
      const desc: PathDescription[] = []
      for (const endVertex of ends.values()) {
        const pathToEnd = DistanceAlgo.getPathTo(endVertex, distances, graph)
        desc.push(new PathDescription(pathToEnd.length, paths.size(), endVertex))
        paths.push(pathToEnd)
      }
      desc.sort((a, b) => a.length - b.length)
      let currentBalance: Int32 = balance.get(vertex)!
      let index: Int32 = 0
      while (currentBalance > 0) {
        const pathDescription = desc[index]
        const pathId = pathDescription.index
        if (paths.get(pathId).length > 0) {
          const to = pathDescription.endVertex
          if (balance.get(to) !== 0) {
            for (const edgeId of paths.get(pathId)) {
              const edge = graph.edges[edgeId]
              graph.addEdge(edge.getFrom(), edge.getTo(), edge.getAction())
            }
            balance.set(to, balance.get(to)! + 1)
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

class PathDescription {
  public length: Int32
  public index: Int32
  public endVertex: Int32

  public constructor(length: Int32, index: Int32, endVertex: Int32) {
    this.length = length
    this.index = index
    this.endVertex = endVertex
  }
}

class VertexAndLastEdgeId {
  public vertex: Int32
  public lastEdgeId: Int32

  public constructor(vertex: Int32, lastEdgeId: Int32) {
    this.vertex = vertex
    this.lastEdgeId = lastEdgeId
  }
}
