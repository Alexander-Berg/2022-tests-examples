import { Throwing } from '../../../../../../common/ys'
import { ApplicationRunningState, AppRunningState } from '../../feature/application-running-state-feature'

export class ApplicationRunningStateModel implements ApplicationRunningState {
  public constructor() {}

  private applicationRunningState: AppRunningState = AppRunningState.runningForeground

  public changeApplicationRunningState(state: AppRunningState): Throwing<void> {
    this.applicationRunningState = state
  }

  public getApplicationRunningState(): Throwing<AppRunningState> {
    return this.applicationRunningState
  }
}
