import { XPromise } from '../../../../common/code/promise/xpromise'
import { OAuthUserAccount, UserAccount } from '../../users/user-pool'
import { AppModelProvider } from '../walk/fixed-scenario-strategy'
import { AccountType2 } from './mbt-test'

export interface AccountDataPreparer {
  /**
   * Это, по-сути, билдер начального состояния данных для аккаунта с возможностью в конце залить подготовленное состояние на бэк
   *
   * @param account - залоченный аккаунт с полученным токеном
   */
  prepare(account: OAuthUserAccount): XPromise<void>
}

export abstract class AccountDataPreparerProvider<T extends AccountDataPreparer> {
  /**
   * Надо вернуть билдер начального состояния данных для залоченного аккаунта
   *
   * @param lockedAccount - залоченный аккаунт
   * @param type - его тип, который просил тест
   */
  abstract provide(lockedAccount: UserAccount, type: AccountType2): T

  /**
   * Надо вернуть скачивалку начального состояния данных для аккаунта
   *
   * @param fulfilledPreparers - заполненные данными билдеры, которые уже залили на бэк свое состояние.
   * При желании можно вытянуть из них тестовые данные и заполнить по ним модель.
   * @param accountsWithTokens - залоченные аккаунты с полученными OAuth-токенами
   */
  abstract provideModelDownloader(fulfilledPreparers: T[], accountsWithTokens: OAuthUserAccount[]): AppModelProvider
}
