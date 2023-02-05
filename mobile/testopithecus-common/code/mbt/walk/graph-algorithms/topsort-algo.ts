import { Int32, range } from '../../../../../../common/ys'
import { CompressedGraph } from '../data-structures/graph'
import { Stack } from '../data-structures/stack'

export class TopSortAlgo {
  public static getTopSort<T>(graph: CompressedGraph<T>): Stack<Int32> {
    const dfs: Stack<State> = new Stack()
    const used: Set<Int32> = new Set<Int32>()
    const result: Stack<Int32> = new Stack()

    for (const vertex of range(0, graph.size())) {
      if (!used.has(vertex)) {
        dfs.push(new State(vertex, 0))
      }
      while (dfs.size() > 0) {
        const state = dfs.top()
        if (state.iterator === 0) {
          used.add(state.vertex)
        }
        let flag = true
        for (const i of range(state.iterator, graph.getEdgesId(state.vertex).length)) {
          const edgeId = graph.getEdgesId(state.vertex)[i]
          const edge = graph.edges[edgeId]
          if (!used.has(edge.getTo())) {
            state.iterator = i + 1
            dfs.push(new State(edge.getTo(), 0))
            flag = false
            break
          }
        }
        if (flag) {
          dfs.pop()
          result.push(state.vertex)
        }
      }
    }
    return result
  }
}

class State {
  public vertex: Int32
  public iterator: Int32

  public constructor(vertex: Int32, iterator: Int32) {
    this.vertex = vertex
    this.iterator = iterator
  }
}
