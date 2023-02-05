import { int64, Int64, int64ToInt32, Nullable } from '../../../../common/ys'
import { currentTimeMs } from '../utils/utils'
import { UserAccount, UserLock, UserPool } from './user-pool'
import { UserService, UserServiceAccount } from './user-service'
import { TusEnv } from './user-service-ensemble'

export class UserServicePool implements UserPool {
  public constructor(
    private userService: UserService,
    private tusEnv: TusEnv,
    private tusConsumer: string,
    private tag: Nullable<string>,
  ) {}

  public tryAcquire(tryAcquireTimeoutMs: Int64, lockTtlMs: Int64): Nullable<UserLock> {
    const start = currentTimeMs()
    while (currentTimeMs() < start + tryAcquireTimeoutMs) {
      const user = this.userService.getAccount(
        this.tusEnv,
        this.tusConsumer,
        this.tag,
        int64ToInt32(lockTtlMs / int64(1000)),
        false,
        null,
      )
      if (user !== null) {
        return new UserServiceLock(this.userService, this.tusEnv, this.tusConsumer, user!)
      }
    }
    return null
  }

  public reset(): void {
    const account = this.userService.getAccount(this.tusEnv, this.tusConsumer, this.tag, 0, true, null)
    if (account === null) {
      return
    }
    this.userService.unlockAccount(this.tusEnv, account!.uid)
  }
}

export class UserServiceLock implements UserLock {
  public constructor(
    private userService: UserService,
    private tusEnv: TusEnv,
    private tusConsumer: string,
    private account: UserServiceAccount,
  ) {}

  public lockedAccount(): UserAccount {
    return new UserAccount(this.account.login, this.account.password, this.account.uid)
  }

  public ping(newLockTtlMs: Int64): void {
    this.userService.getAccount(
      this.tusEnv,
      this.tusConsumer,
      null,
      int64ToInt32(newLockTtlMs / int64(1000)),
      true,
      this.account.uid,
    )
  }

  public release(): void {
    this.userService.unlockAccount(this.tusEnv, this.account.uid)
  }
}
