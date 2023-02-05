import { Int32 } from '../../../../../../common/ys'
import { CompressedGraph } from '../data-structures/graph'
import { Queue } from '../data-structures/queue'

export class DistanceAlgo {
  public static getDistances<T>(
    graph: CompressedGraph<T>,
    vertex: Int32,
    component: Set<Int32>,
  ): Map<Int32, DistanceAndLastEdgeId> {
    const distances: Map<Int32, DistanceAndLastEdgeId> = new Map<Int32, DistanceAndLastEdgeId>()
    distances.set(vertex, new DistanceAndLastEdgeId(0, -1))
    const queue: Queue<Int32> = new Queue<Int32>()
    queue.push(vertex)
    while (queue.size() > 0) {
      const current = queue.front()
      const currentDistance = distances.get(current)!.distance
      queue.pop()
      for (const edgeId of graph.getEdgesId(current)) {
        const edge = graph.edges[edgeId]
        const to = edge.getTo()
        if (component.has(to) && !distances.has(to)) {
          queue.push(to)
          distances.set(to, new DistanceAndLastEdgeId(currentDistance + 1, edgeId))
        }
      }
    }
    return distances
  }

  public static getPathTo<T>(
    vertex: Int32,
    distances: Map<Int32, DistanceAndLastEdgeId>,
    graph: CompressedGraph<T>,
  ): Int32[] {
    const path: Int32[] = []
    let current = vertex
    while (distances.get(current)!.distance > 0) {
      const edgeId = distances.get(current)!.lastEdgeId
      path.push(edgeId)
      current = graph.edges[edgeId].getFrom()
    }
    return path.reverse()
  }
}

export class DistanceAndLastEdgeId {
  public distance: Int32
  public lastEdgeId: Int32

  public constructor(distance: Int32, lastEdgeId: Int32) {
    this.distance = distance
    this.lastEdgeId = lastEdgeId
  }
}
