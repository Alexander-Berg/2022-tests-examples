import { PseudoRandomProvider } from '../../../xpackages/testopithecus-common/code/utils/pseudo-random'
import { randomInterval } from '../../../xpackages/testopithecus-common/code/utils/random'
import { Int32 } from '../../../common/ys'

export class Timestamps {
  private static readonly start: Date = new Date()
  public static readonly size = 100

  public static getDate(order: Int32): Date {
    const t = new Date()
    t.setTime(Timestamps.start.getTime() + order * 60 * 60 * 1000)
    return t
  }

  public static getFirstDate(): Date {
    return this.getDate(0)
  }

  public static getLastDate(): Date {
    return this.getDate(Timestamps.size - 1)
  }

  public static randomInterval(allowEmpty: boolean): Date[] {
    const [start, end] = randomInterval(PseudoRandomProvider.INSTANCE, 0, Timestamps.size)
    if (start !== end || allowEmpty) {
      return [Timestamps.getDate(start), Timestamps.getDate(end)]
    }
    return Timestamps.randomInterval(allowEmpty)
  }
}
