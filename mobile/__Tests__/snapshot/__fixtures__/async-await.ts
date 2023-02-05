/* eslint-disable */
import { Throwing } from '../../../src/ys-tools/ys'

export interface I {
  f(): Promise<void>
}

export class B implements I {
  async f(): Promise<void> {}
}

function f(): Promise<void> {
  return new B().f()
}

export class A implements I {
  async f(): Promise<void> {
    await new B().f()
    const a: Promise<void> = new B().f()
    try {
      await this.g()
    } catch (e) {}
  }

  async g(): Throwing<Promise<void>> {}
}
