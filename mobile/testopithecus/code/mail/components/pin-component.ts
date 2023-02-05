import { Throwing } from '../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import {
  ChangePasswordAction,
  EnterPasswordAction,
  ResetPasswordAction,
  TurnOffLoginUsingPasswordAction,
  TurnOnLoginUsingPasswordAction,
} from '../actions/pin-actions'

export class PinComponent implements MBTComponent {
  public static readonly type: string = 'PinComponent'

  public getComponentType(): string {
    return PinComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {}

  public tostring(): string {
    return this.getComponentType()
  }
}

export class PinActions implements MBTComponentActions {
  private password: string = '1234'
  private newPassword: string = '5678'
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new TurnOnLoginUsingPasswordAction(this.password))
    actions.push(new TurnOffLoginUsingPasswordAction())
    actions.push(new EnterPasswordAction(this.password))
    actions.push(new ChangePasswordAction(this.newPassword))
    actions.push(new ResetPasswordAction())
    return actions
  }
}
