/* eslint-disable @typescript-eslint/no-unused-vars */

import { Serializable } from 'typescript-json-serializer'
import { YSError, Throwing, Nullable } from '../../../src/ys-tools/ys'

function f(): Throwing<void> {}

function f2(): void | never {}

function f3(): Throwing<Throwing<void>> {}

export function parseJson<T>(item: string, materializer: (item: string) => Throwing<T>): Nullable<T> {
  try {
    return materializer(item)
  } catch (e) {
    return null
  }
}

export class MyException extends YSError {
  constructor(message: string) {
    super(message)
  }
}

class B {
  public m2(): Throwing<boolean> {
    const i: I = new Impl()
    new Map<string, string>().forEach(
      (value, key): Throwing<void> => {
        i.it()
      },
    )
    throw new MyException('pizza')
  }

  private m3(callback: (s: string) => Throwing<Nullable<boolean>>): boolean {
    try {
      f()
      return this.m2()
    } catch (e) {
      if (e instanceof MyException) {
        return false
      } else {
        return true
      }
    }
  }

  public fatal(): void {
    throw new Error('fatal')
  }

  public fatal2(): void {
    throw new Error()
  }
}

abstract class C<T> extends B {
  m2(): Throwing<boolean> {
    return false
  }
}

@Serializable()
abstract class D extends C<Throwing<void>> {
  public abstract a(): Throwing<boolean>
}

interface I {
  it(): Throwing<void>
}

class Impl implements I {
  it(): Throwing<void> {}
}

export class Foo {
  public callThrowing(): Throwing<Foo> {
    return this
  }

  public testChainedThrowingCalls(): void {
    this.callThrowing().callThrowing().callThrowing()
  }
}
