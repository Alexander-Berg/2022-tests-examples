export class Edge<VertexType, EdgeType> {
  public constructor(private from: VertexType, private to: VertexType, private action: EdgeType) {}

  public getFrom(): VertexType {
    return this.from
  }

  public getTo(): VertexType {
    return this.to
  }

  public getAction(): EdgeType {
    return this.action
  }
}
