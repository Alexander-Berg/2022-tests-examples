import { Int32, Nullable } from '../../../src/ys-tools/ys'

// tslint:disable: max-classes-per-file

export class PreBase {
  public readonly preProp1: string
  public preProp2: string
  public preMethod(): Int32 { return 10 }
}

export class Base<T> extends PreBase {
  public readonly prop1: string
  public prop2: string

  public constructor(public readonly cp: Int32) {
    super()
  }

  public method(): Nullable<T> { return null }
}

export interface I1 {
  readonly iProp1: string
  iProp2: string
  readonly cp1: boolean
  iMethod(): string
}

// tslint:disable: no-empty-interface
export interface I2 extends I1 { }

export interface I3 { }

export class R<T> { }

export class B0 { }
export class B1 extends B0 {
  public constructor(
    a: Int32,
    b: Nullable<ReadonlyArray<R<Int32>>>,
    c: readonly string[],
    d: R<Int32>,
    e: (a: R<Int32>) => string,
  ) { super() }
}
export class B2 extends B1 implements I3 { }

export class B11 implements I3 {
  public constructor(a: Int32) { }
}

export abstract class Flag<T> {
  public readonly name: string
  public readonly value: T

  public constructor(name: string, value: T) {
    this.name = name
    this.value = value
  }
}

export class IntermediateFlag<U> extends Flag<U> {
}
