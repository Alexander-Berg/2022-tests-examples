import { Logger } from '../../../../../common/code/logging/logger'
import { Int32, Int64 } from '../../../../../../common/ys'
import { Edge } from './edge'

export class CompressedGraph<EdgeType> {
  public adjList: Int32[][] = []
  public edges: Edge<Int32, EdgeType>[] = []

  public addEdge(from: Int32, to: Int32, action: EdgeType): void {
    while (this.adjList.length <= from || this.adjList.length <= to) {
      this.adjList.push([])
    }
    this.adjList[from].push(this.edges.length)
    this.edges.push(new Edge(from, to, action))
  }

  public getDegree(vertex: Int32): Int32 {
    return this.adjList.length > vertex ? this.adjList[vertex].length : 0
  }

  /*
  public copy(): CompressedGraph<EdgeType> {
      const graph: CompressedGraph<EdgeType> = new CompressedGraph();
      for (const edge of this.edges) {
          graph.add_edge(edge.getFrom(), edge.getTo(), edge.getAction());
      }
      return graph;
  }
   */

  public size(): Int32 {
    return this.adjList.length
  }

  public countOfEdges(): Int32 {
    return this.edges.length
  }

  /*
  public vertexes(): IterableIterator<VertexType> {
      return this.adjList.keys();
  }
   */

  public getEdgesId(vertex: Int32): Int32[] {
    while (this.adjList.length <= vertex) {
      this.adjList.push([])
    }
    return this.adjList[vertex]
  }
}

export class Graph<EdgeType> extends CompressedGraph<EdgeType> {
  private vertexToId: Map<Int64, Int32> = new Map<Int64, Int32>()

  public addVertex(vertex: Int64): void {
    if (!this.vertexToId.has(vertex)) {
      this.vertexToId.set(vertex, this.vertexToId.size)
    }
  }

  public addEdgeVV(from: Int64, to: Int64, action: EdgeType): void {
    this.addVertex(from)
    this.addVertex(to)
    super.addEdge(this.vertexToId.get(from)!, this.vertexToId.get(to)!, action)
  }

  public getDegreeV(vertex: Int64): Int32 {
    return super.getDegree(this.vertexToId.get(vertex)!)
  }

  public print(logger: Logger): void {
    logger.info('digraph g {')
    // for (const vertex of this.adjList) {
    //   logger.log(`    ${vertex};`);
    // }
    // for (const edge of this.edges) {
    //   logger.log(`    ${edge.getFrom()} -> ${edge.getTo()} [label="${edge.getAction().tostring()}"]`);
    // }
    logger.info('}')
  }
}
