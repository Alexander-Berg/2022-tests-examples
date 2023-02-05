export class Queue<T> {
  public q1: T[] = []
  public q2: T[] = []

  public push(item: T): void {
    this.q1.push(item)
  }

  public pop(): void {
    this.move()
    this.q2.pop()
  }

  public clear(): void {
    this.q1 = []
    this.q2 = []
  }

  public size(): number {
    return this.q1.length + this.q2.length
  }

  public front(): T {
    this.move()
    return this.q2[this.q2.length - 1]
  }

  private move(): void {
    if (this.q2.length > 0) {
      return
    }
    while (this.q1.length > 0) {
      const element = this.q1.pop()
      this.q2.push(element!)
    }
  }
}
