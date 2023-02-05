import { Int32 } from '../../../../../common/ys'
import { SyncSleep } from '../../../code/utils/sync-sleep'

export class SyncSleepImpl implements SyncSleep {
  public static readonly instance = new SyncSleepImpl()

  public sleepMs(milliseconds: Int32): void {
    const start = Date.now()
    while (Date.now() < start + milliseconds) {}
  }
}
