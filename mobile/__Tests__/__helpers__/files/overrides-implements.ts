import { Int32, Nullable } from '../../../src/ys-tools/ys'
import { B0, B1, B11, B2, Base, I2, IntermediateFlag, R } from './overrides-implements-base'

// tslint:disable: member-ordering
// tslint:disable: max-classes-per-file

export class A extends Base<string> implements I2 {
  public readonly iProp1: string
  public iProp2: string
  public iMethod(): string { return 'hello' }

  public readonly preProp1: string
  public preProp2: string
  public preMethod(): Int32 { return 10 }

  public readonly prop1: string
  public prop2: string
  public method(): Nullable<string> { return null }

  public readonly a: Int32 = 10
  public m(): boolean {
    return true
  }

  public constructor(public readonly cp: Int32, public readonly cp1: boolean) {
    super(cp)
  }
}

export class B3 extends B2 {
  public constructor(
    a: Int32,
    b: Nullable<ReadonlyArray<R<Int32>>>,
    c: readonly string[],
    d: R<Int32>,
    e: (a: R<Int32>) => string,
  ) {
    super(a, b, c, d, e)
  }
}

export class B4 extends B2 {
  public constructor(
    a: Int32,
    c: readonly string[],
    d: R<Int32>,
    e: (a: R<Int32>) => string,
  ) {
    super(a, null, c, d, e)
  }
}

export class B5 extends B0 {
  public constructor(a: Int32) { super() }
}

export class B6 extends B1 {
  public constructor(
    a: Int32,
    b: Nullable<ReadonlyArray<R<string>>>,  // notice mismatch: Int32 -> string
    c: readonly string[],
    d: R<Int32>,
    e: (a: R<Int32>) => string,
  ) {
    super(a, b, c, d, e)
  }
}

export class B7 extends B11 {
  public constructor(a: Int32) {
    super(a)
  }
}

export class StringFlag extends IntermediateFlag<string> {
  constructor(name: string, value: string) {
    super(name, value)
  }
}
