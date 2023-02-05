import { Throwing } from '../../../../../common/ys'
import { App, MBTAction, MBTComponent } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../../testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { GoToBackgroundState, GoToForegroundState } from '../actions/general/application-state-actions'

export class BackgroundRunningStateComponent implements MBTComponent {
  public static readonly type: string = 'BackgroundRunningStateComponent'

  public getComponentType(): string {
    return BackgroundRunningStateComponent.type
  }

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {}

  public tostring(): string {
    return this.getComponentType()
  }
}

export class ApplicationRunningStateActions implements MBTComponentActions {
  public getActions(_model: App): MBTAction[] {
    const actions: MBTAction[] = []
    actions.push(new GoToBackgroundState())
    actions.push(new GoToForegroundState())
    return actions
  }
}
