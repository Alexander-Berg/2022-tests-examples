import { Int64, Nullable } from '../../../../common/ys'
import { UserAccount, UserLock, UserPool } from './user-pool'

export class DebugUserPool implements UserPool {
  public constructor(private account: UserAccount) {}

  public tryAcquire(_tryAcquireTimeoutMs: Int64, _lockTtlMs: Int64): Nullable<UserLock> {
    return new DebugLock(this.account)
  }

  public reset(): void {}
}

export class DebugLock implements UserLock {
  public constructor(private account: UserAccount) {}

  public lockedAccount(): UserAccount {
    return this.account
  }

  public ping(_newLockTtlMs: bigint): void {}

  public release(): void {}
}
