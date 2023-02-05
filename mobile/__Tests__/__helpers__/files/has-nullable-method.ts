import { Nullable } from '../../../src/ys-tools/ys'

export class Result<T> {
  private readonly error: Nullable<E>
  public constructor(error: Nullable<E>) {
    this.error = error
  }
  public getError(): E {
    return this.error!
  }
}

// tslint:disable-next-line:no-empty-interface
// tslint:disable-next-line:max-classes-per-file
export class MyError { }
// tslint:disable-next-line:max-classes-per-file
export class E {
  public getInner(): Nullable<MyError> { return null }
}

export function f(completion: (result: Result<string>) => void): void {
  completion(new Result<string>(new E()))
}
