import { Throwing } from '../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { UserAccount } from '../../../../testopithecus-common/code/users/user-pool'
import { YandexLoginAction } from '../actions/login/login-actions'
import { YandexLoginFeature } from '../feature/login-features'

export class LoginComponent implements MBTComponent {
  public static readonly type: string = 'LoginComponent'

  /**
   * Компонент экрана залогина.
   */
  public constructor() {}

  public getComponentType(): string {
    return LoginComponent.type
  }

  public async assertMatches(_model: App, _application: App): Throwing<Promise<void>> {
    // Кажется, мы не можем написать для этого компонента нормальные ассерты
  }

  public tostring(): string {
    return 'LoginComponent'
  }
}

export class ReloginComponent implements MBTComponent {
  public static readonly type: string = 'LoginComponent'

  /**
   * Компонент экрана залогина.
   */
  public constructor() {}

  public getComponentType(): string {
    return ReloginComponent.type
  }

  public async assertMatches(_model: App, _application: App): Throwing<Promise<void>> {
    // Кажется, мы не можем написать для этого компонента нормальные ассерты
  }

  public tostring(): string {
    return 'ReloginComponent'
  }
}

export class AllLoginActions implements MBTComponentActions {
  public constructor(private accounts: UserAccount[]) {}

  public getActions(model: App): MBTAction[] {
    const actions: MBTAction[] = []

    YandexLoginFeature.get.performIfSupported(model, (_mailboxModel) => {
      this.accounts.forEach((acc) => actions.push(new YandexLoginAction(acc)))
    })

    return actions
  }
}
