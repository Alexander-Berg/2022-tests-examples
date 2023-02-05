import { range, Throwing } from '../../../../../common/ys'
import {
  App,
  MBTAction,
  MBTComponent,
  MBTComponentType,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'
import { ChoseAccountFromAccountsListAction } from '../actions/login/login-actions'
import { AccountsListFeature } from '../feature/login-features'

export class AccountsListComponent implements MBTComponent {
  public static readonly type: string = 'AccountSwitcher'

  public getComponentType(): MBTComponentType {
    return AccountsListComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {}

  public tostring(): string {
    return this.getComponentType()
  }
}

export class AllAccountsManagerActions implements MBTComponentActions {
  public constructor(private accounts: UserAccount[]) {}

  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []
    AccountsListFeature.get.performIfSupported(model, (mailboxModel) => {
      for (const i of range(0, this.accounts.length)) {
        actions.push(new ChoseAccountFromAccountsListAction(this.accounts[i]))
      }
    })
    return actions
  }
}
