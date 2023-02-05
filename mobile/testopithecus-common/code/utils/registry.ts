import { Nullable } from '../../../../common/ys'
import { ErrorThrower } from './../../../testopithecus-common/code/utils/error-thrower'

export class Registry {
  private static instance: Registry = new Registry()

  public errorThrower: Nullable<ErrorThrower> = null

  private constructor() {}

  public static get(): Registry {
    return this.instance
  }
}
