import { Int32, int64, Int64, range, stringToInt64 } from '../../../../../../common/ys'

export class HashBuilder {
  private static mod: Int64 = stringToInt64('1125899839733759')!
  private static multiplier: Int64 = int64(63)
  private hash: Int64 = int64(0)

  private static getHashOfString(str: string): Int64 {
    let hash: Int64 = int64(0)
    const multiplier: Int64 = int64(257)
    for (const i of range(0 as Int32, str.length)) {
      const ch = str.charCodeAt(i)
      hash = (hash * multiplier + int64(ch)) % this.mod
    }
    return hash
  }

  public addInt64(n: Int64): HashBuilder {
    this.hash = (this.hash * HashBuilder.multiplier + n) % HashBuilder.mod
    return this
  }

  public addInt(n: number): HashBuilder {
    return this.addInt64(int64(n))
  }

  public addBoolean(condition: boolean): HashBuilder {
    return this.addInt64(int64(condition ? 1 : 0))
  }

  public addString(str: string): HashBuilder {
    return this.addInt64(HashBuilder.getHashOfString(str))
  }

  public build(): Int64 {
    return this.hash
  }
}
