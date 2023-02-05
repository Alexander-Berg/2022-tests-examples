import { Int64, Nullable } from '../../../../common/ys'
import { AccountType2 } from '../mbt/test/mbt-test'

export type Uid = string

export class UserAccount {
  public constructor(public login: string, public password: string, public uid: Uid = '') {}
}

export class OAuthUserAccount {
  public constructor(
    public account: UserAccount,
    public oauthToken: Nullable<string>,
    public readonly type: AccountType2,
  ) {}
}

export interface UserLock {
  lockedAccount(): UserAccount

  ping(newLockTtlMs: Int64): void

  release(): void
}

export interface UserPool {
  tryAcquire(tryAcquireTimeoutMs: Int64, lockTtlMs: Int64): Nullable<UserLock>

  // только для тестовых целей, чтобы сбросить все локи
  reset(): void
}
