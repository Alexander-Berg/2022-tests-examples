import { Int32, Nullable, range } from '../../../../common/ys'
import { copyArray } from '../utils/utils'
import { UserAccount } from './user-pool'

export class AccountsManager {
  public constructor(
    public readonly accounts: UserAccount[],
    public indexesOfLoggedInAccounts: Int32[] = [],
    public currentAccount: Nullable<Int32> = null,
    public indexesOfRevokedTokenAccounts: Int32[] = [],
  ) {}

  public logInToAccount(account: UserAccount): void {
    if (this.isAccountLoggedIn(account.login)) {
      this.switchToAccount(account.login)
    }

    for (const i of range(0, this.accounts.length)) {
      if (this.accounts[i].login === account.login && this.accounts[i].password === account.password) {
        this.indexesOfLoggedInAccounts.push(i)
        this.currentAccount = i
        return
      }
    }

    throw new Error(`Account (login=${account.login};password=${account.password}) hasn't been downloaded yet`)
  }

  public switchToAccount(login: string): void {
    if (!this.isAccountLoggedIn(login)) {
      throw new Error(`Account for (login=${login}) hasn't been logged in yet`)
    }

    for (const i of range(0, this.accounts.length)) {
      if (this.accounts[i].login === login) {
        this.currentAccount = i
        return
      }
    }
    throw new Error(`Account for (login=${login}) hasn't been logged in yet`)
  }

  public switchToAccountByOrder(loginOrder: Int32): void {
    this.switchToAccount(this.accounts[loginOrder].login)
  }

  public isLoggedIn(): boolean {
    return this.currentAccount !== null
  }

  public isAccountLoggedIn(login: string): boolean {
    return this.indexesOfLoggedInAccounts.filter((i) => this.accounts[i].login === login).length > 0
  }

  public isAccountWithExpiredToken(login: string): boolean {
    return this.indexesOfRevokedTokenAccounts.filter((i) => this.accounts[i].login === login).length > 0
  }

  public getLoggedInAccounts(): UserAccount[] {
    const accountsWhichAreLoggedIn: UserAccount[] = []
    this.indexesOfLoggedInAccounts.forEach((i) => accountsWhichAreLoggedIn.push(this.accounts[i]))
    return accountsWhichAreLoggedIn
  }

  public logoutAccount(login: string): void {
    if (!this.isAccountLoggedIn(login)) {
      throw new Error(`Account for (login=${login}) hasn't been logged in yet`)
    }

    for (const i of range(0, this.indexesOfLoggedInAccounts.length)) {
      if (this.accounts[this.indexesOfLoggedInAccounts[i]].login === login) {
        this.indexesOfLoggedInAccounts = this.indexesOfLoggedInAccounts.filter(
          (index) => index !== this.indexesOfLoggedInAccounts[i],
        )
        if (this.indexesOfLoggedInAccounts.length === 0) {
          this.currentAccount = null
        } else if (i === 0) {
          this.currentAccount = 0
        } else {
          this.currentAccount = i - 1
        }
        return
      }
    }
    throw new Error(`Account for (login=${login}) hasn't been logged in yet`)
  }

  public revokeToken(account: UserAccount): void {
    if (!this.isAccountLoggedIn(account.login)) {
      throw new Error(`Account for (login=${account.login}) hasn't been logged in yet`)
    }
    for (const i of range(0, this.indexesOfLoggedInAccounts.length)) {
      if (this.accounts[this.indexesOfLoggedInAccounts[i]].login === account.login) {
        this.indexesOfRevokedTokenAccounts.push(this.indexesOfLoggedInAccounts[i])
        return
      }
    }
    throw new Error(`Account for (login=${account.login}) hasn't been logged in yet`)
  }

  public copy(): AccountsManager {
    return new AccountsManager(
      copyArray(this.accounts),
      copyArray(this.indexesOfLoggedInAccounts),
      this.currentAccount,
      copyArray(this.indexesOfRevokedTokenAccounts),
    )
  }

  public exitFromReloginWindow(): void {
    this.currentAccount = null
    let i = 0
    for (const account of this.accounts) {
      if (!this.isAccountWithExpiredToken(account.login)) {
        this.currentAccount = this.indexesOfLoggedInAccounts[i]
        return
      }
      i = i + 1
    }
  }
}
