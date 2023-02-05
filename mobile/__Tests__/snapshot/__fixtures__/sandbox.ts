/* eslint-disable @typescript-eslint/no-unused-vars */

import { Nullable } from '../../../src/ys-tools/ys'

function f(): void {}

class A {
  private m1(): Nullable<boolean> {
    f()
    return 1 > 0 ? true : false
  }
}

interface I {
  m1(): void
  m2(): boolean
}
