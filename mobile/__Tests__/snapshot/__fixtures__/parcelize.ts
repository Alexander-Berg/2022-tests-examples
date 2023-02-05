/* eslint-disable @typescript-eslint/no-unused-vars */

import { Parcelable, Parcelize } from '../../../src/ys-tools/ys'

abstract class A implements Parcelable {
  public abstract f(): void
}

@Parcelize
class B extends A {
  public f(): void {}
}

@Parcelize
class Foo implements Parcelable {
  public f(): void {}
}
