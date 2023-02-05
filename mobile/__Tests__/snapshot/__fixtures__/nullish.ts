/* eslint-disable @typescript-eslint/no-unused-vars,@typescript-eslint/no-inferrable-types */

import { Nullable } from '../../../src/ys-tools/ys'

export class Foo {
  public nullProp: Nullable<Foo> = null
  public valProp: string = ''

  public bar(): Nullable<Foo> {
    return null
  }
}

export function test(): void {
  const foo = new Foo()
  let value = null
  value = foo.valProp
  value = foo.nullProp ?? new Foo()
  value = foo.bar() ?? new Foo()
  value = foo.bar()?.bar()
  value = foo.bar()?.nullProp
  value = foo.bar()?.valProp
  value = foo.bar()?.bar() ?? null
  value = foo.bar()?.nullProp ?? null
  value = foo.bar()?.valProp ?? null
}
