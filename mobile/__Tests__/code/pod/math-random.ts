import { Int32 } from '../../../../../common/ys'
import { RandomProvider } from '../../../code/utils/random'

export class MathRandom implements RandomProvider {
  public static INSTANCE = new MathRandom()

  private constructor() {}

  public generate(n: Int32): Int32 {
    return Math.floor(Math.random() * n)
  }
}
