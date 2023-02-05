import { Throwing } from '../../../../../../common/ys'
import { requireNonNull } from '../../../../../testopithecus-common/code/utils/utils'
import { EventusEvent } from '../../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { BackgroundRunningStateComponent } from '../../components/background-running-state-component'
import { PinComponent } from '../../components/pin-component'
import { ApplicationRunningStateFeature, AppRunningState } from '../../feature/application-running-state-feature'
import { PinFeature } from '../../feature/pin-feature'

export class GoToBackgroundState implements MBTAction {
  public static readonly type: MBTActionType = 'GoToBackgroundState'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return ApplicationRunningStateFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return (
      ApplicationRunningStateFeature.get.forceCast(model).getApplicationRunningState() ===
      AppRunningState.runningForeground
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ApplicationRunningStateFeature.get.forceCast(model).changeApplicationRunningState(AppRunningState.runningBackground)
    ApplicationRunningStateFeature.get
      .forceCast(application)
      .changeApplicationRunningState(AppRunningState.runningBackground)
    return new BackgroundRunningStateComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'GoToBackgroundState'
  }

  public getActionType(): string {
    return GoToBackgroundState.type
  }
}

export class GoToForegroundState implements MBTAction {
  public static readonly type: MBTActionType = 'GoToForegroundState'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ApplicationRunningStateFeature.get.includedAll(modelFeatures, applicationFeatures) &&
      PinFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return (
      ApplicationRunningStateFeature.get.forceCast(model).getApplicationRunningState() ===
      AppRunningState.runningBackground
    )
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    ApplicationRunningStateFeature.get.forceCast(model).changeApplicationRunningState(AppRunningState.runningForeground)
    ApplicationRunningStateFeature.get
      .forceCast(application)
      .changeApplicationRunningState(AppRunningState.runningForeground)
    if (PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled()) {
      return new PinComponent()
    }
    return requireNonNull(history.previousDifferentComponent, 'No previous screen!')
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'GoToForegroundState'
  }

  public getActionType(): string {
    return GoToForegroundState.type
  }
}
