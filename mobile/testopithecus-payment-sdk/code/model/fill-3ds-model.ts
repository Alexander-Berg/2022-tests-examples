import { Int32, Throwing } from '../../../../common/ys'
import { Fill3ds } from '../feature/fill-3ds-feature'

export class Fill3dsModel implements Fill3ds {
  public constructor() {}

  private pageForceClosed: boolean = false

  public waitFor3dsPage(mSec: Int32): Throwing<boolean> {
    return true
  }

  public fill3dsCode(code: string): Throwing<void> {
    this.pageForceClosed = false
  }

  public close3dsPage(): Throwing<void> {
    this.pageForceClosed = true
  }

  public is3dsPageForceClosed(): boolean {
    return this.pageForceClosed
  }
}
