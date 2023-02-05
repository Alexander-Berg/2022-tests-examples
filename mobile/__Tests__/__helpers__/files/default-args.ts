import { Int32 } from '../../../src/ys-tools/ys'

// tslint:disable: max-classes-per-file
// tslint:disable: no-empty

export class BaseWithDef {
  public method(a: Int32 = 10): void { }
}

export class Derived extends BaseWithDef {
  public method(a: Int32 = 20): void { }
}
