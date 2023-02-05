import { Nullable, range, undefinedToNull } from '../../../../common/ys'
import { AccountType2 } from '../mbt/test/mbt-test'
import { requireNonNull } from '../utils/utils'
import { DebugUserPool } from './debug-user-pool'
import { UserAccount, UserPool } from './user-pool'
import { UserService } from './user-service'
import { UserServicePool } from './user-service-pool'

export enum TusEnv {
  PROD = 'prod',
  TEST = 'test',
  EXTERNAL = 'external',
}

export class UserServiceEnsemble {
  private userEnsembleMap: Map<AccountType2, UserPool>

  public constructor(
    private readonly userService: UserService,
    requiredAccounts: AccountType2[],
    private readonly tusConsumer: string,
    userTags: string[],
  ) {
    this.userEnsembleMap = new Map<AccountType2, UserPool>()
    for (const i of range(0, requiredAccounts.length)) {
      const accountType = requiredAccounts[i]
      this.userEnsembleMap.set(accountType, this.getUserPool(accountType, userTags))
    }
  }

  public getAccountByType(type: AccountType2): UserPool {
    return requireNonNull(undefinedToNull(this.userEnsembleMap.get(type)), 'Пул юзеров не может быть null!')
  }

  private getUserPool(accountType: AccountType2, userTags: string[]): UserPool {
    const accountToDebug = this.getDebugAccount(accountType)
    if (accountToDebug !== null) {
      return new DebugUserPool(accountToDebug!)
    }
    const userTag = userTags.length > 0 ? userTags.join(',') : UserServiceEnsemble.getTagByAccountType(accountType)
    const tusEnv = UserServiceEnsemble.getEnvironmentByAccountType(accountType)
    return new UserServicePool(this.userService, tusEnv, this.tusConsumer, userTag)
  }

  private getDebugAccount(accountType: AccountType2): Nullable<UserAccount> {
    switch (accountType) {
      // case AccountType2.Mail:
      //   return new UserAccount('yandex.test1@mail.ru', 'testqa_123')
      // case AccountType2.YandexTeam:
      //   return new UserAccount('calendartestuser@yandex-team.ru', '', '1120000000004717')
      default:
        return null
    }
  }

  public static getTagByAccountType(type: AccountType2): string {
    switch (type) {
      case AccountType2.Yandex:
        return 'yandex'
      case AccountType2.YandexTeam:
        return 'yandex-team'
      case AccountType2.Yahoo:
        return 'yahoo'
      case AccountType2.Google:
        return 'google'
      case AccountType2.Mail:
        return 'mail.ru'
      case AccountType2.Rambler:
        return 'rambler'
      case AccountType2.Hotmail:
        return 'hotmail'
      case AccountType2.Outlook:
        return 'outlook'
      case AccountType2.YandexTest:
        return 'yandextest'
      case AccountType2.Other:
      default:
        return 'other'
    }
    return 'other'
  }

  private static getEnvironmentByAccountType(type: AccountType2): TusEnv {
    switch (type) {
      case AccountType2.YandexTest:
        return TusEnv.TEST
      case AccountType2.Outlook:
        return TusEnv.EXTERNAL
      case AccountType2.Other:
        return TusEnv.EXTERNAL
      default:
        return TusEnv.PROD
    }
  }
}
