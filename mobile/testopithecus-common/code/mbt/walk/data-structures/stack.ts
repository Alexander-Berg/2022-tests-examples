import { Int32 } from '../../../../../../common/ys'

export class Stack<T> {
  public stack: T[]

  public constructor() {
    this.stack = []
  }

  public push(item: T): void {
    this.stack.push(item)
  }

  public pop(): void {
    this.stack.pop()
  }

  public top(): T {
    return this.stack[this.stack.length - 1]
  }

  public get(index: Int32): T {
    return this.stack[index]
  }

  public clear(): void {
    this.stack = []
  }

  public size(): number {
    return this.stack.length
  }
}
