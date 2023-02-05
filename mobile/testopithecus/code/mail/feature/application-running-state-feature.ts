import { Throwing } from '../../../../../common/ys'
import { Feature } from '../../../../testopithecus-common/code/mbt/mbt-abstractions'

export interface ApplicationRunningState {
  getApplicationRunningState(): Throwing<AppRunningState>

  changeApplicationRunningState(state: AppRunningState): Throwing<void>
}

export class ApplicationRunningStateFeature extends Feature<ApplicationRunningState> {
  public static get: ApplicationRunningStateFeature = new ApplicationRunningStateFeature()

  private constructor() {
    super('ApplicationRunningState', 'Изменение состояния приложения - background, foreground и тд.')
  }
}

export enum AppRunningState {
  unknown,
  notRunning,
  runningBackgroundSuspended,
  runningBackground,
  runningForeground,
}
