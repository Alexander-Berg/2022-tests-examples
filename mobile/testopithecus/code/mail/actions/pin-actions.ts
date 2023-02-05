import { requireNonNull } from '../../../../testopithecus-common/code/utils/utils'
import { Nullable, Throwing } from '../../../../../common/ys'
import { EventusEvent } from '../../../../eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../../testopithecus-common/code/mbt/mbt-abstractions'
import { BackgroundRunningStateComponent } from '../components/background-running-state-component'
import { LoginComponent } from '../components/login-component'
import { PinComponent } from '../components/pin-component'
import { ApplicationRunningStateFeature, AppRunningState } from '../feature/application-running-state-feature'
import { PinFeature } from '../feature/pin-feature'

export class TurnOnLoginUsingPasswordAction implements MBTAction {
  public static readonly type: MBTActionType = 'TurnOnLoginUsingPasswordAction'

  public constructor(protected password: string) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PinFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isLoginUsingPasswordEnabled = PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled()
    return !isLoginUsingPasswordEnabled
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    PinFeature.get.forceCast(model).turnOnLoginUsingPassword(this.password)
    PinFeature.get.forceCast(application).turnOnLoginUsingPassword(this.password)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TurnOnLoginUsingPassword'
  }

  public getActionType(): string {
    return TurnOnLoginUsingPasswordAction.type
  }
}

export class TurnOffLoginUsingPasswordAction implements MBTAction {
  public static readonly type: MBTActionType = 'TurnOffLoginUsingPasswordAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PinFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isLoginUsingPasswordEnabled = PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled()
    return isLoginUsingPasswordEnabled
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    PinFeature.get.forceCast(model).turnOffLoginUsingPassword()
    PinFeature.get.forceCast(application).turnOffLoginUsingPassword()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'TurnOffLoginUsingPassword'
  }

  public getActionType(): string {
    return TurnOffLoginUsingPasswordAction.type
  }
}

export class ChangePasswordAction implements MBTAction {
  public static readonly type: MBTActionType = 'ChangePasswordAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PinFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isLoginUsingPasswordEnabled = PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled()
    return isLoginUsingPasswordEnabled
  }

  public constructor(protected newPassword: string) {}

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    PinFeature.get.forceCast(model).changePassword(this.newPassword)
    PinFeature.get.forceCast(application).changePassword(this.newPassword)
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'ChangePassword'
  }

  public getActionType(): string {
    return ChangePasswordAction.type
  }
}

export class EnterPasswordAction implements MBTAction {
  public static readonly type: MBTActionType = 'EnterPasswordAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PinFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isLoginUsingPasswordEnabled = PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled()
    return isLoginUsingPasswordEnabled
  }

  public constructor(protected password: string) {}

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    PinFeature.get.forceCast(model).enterPassword(this.password)
    PinFeature.get.forceCast(application).enterPassword(this.password)
    return this.previousComponentsExceptBackgroundAndPin(history)
  }

  private previousComponentsExceptBackgroundAndPin(history: MBTHistory): MBTComponent {
    let previousComponent: Nullable<MBTComponent> = null
    for (const component of history.allPreviousComponents.reverse()) {
      if (![BackgroundRunningStateComponent.type, PinComponent.type].includes(component.tostring())) {
        previousComponent = component
        break
      }
    }
    return requireNonNull(previousComponent, 'No previous component')
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'EnterPassword'
  }

  public getActionType(): string {
    return EnterPasswordAction.type
  }
}

export class ResetPasswordAction implements MBTAction {
  public static readonly type: MBTActionType = 'ResetPasswordAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PinFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    const isLoginUsingPasswordEnabled = PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled()
    return isLoginUsingPasswordEnabled
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    PinFeature.get.forceCast(model).resetPassword()
    PinFeature.get.forceCast(application).resetPassword()
    return new LoginComponent()
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'ResetPassword'
  }

  public getActionType(): string {
    return ResetPasswordAction.type
  }
}

export class WaitForPinToTriggerAction implements MBTAction {
  public static readonly type: MBTActionType = 'WaitForPinToTriggerAction'

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return PinFeature.get.includedAll(modelFeatures, applicationFeatures)
  }
  public canBePerformed(model: App): Throwing<boolean> {
    const state = ApplicationRunningStateFeature.get.forceCast(model).getApplicationRunningState()
    return PinFeature.get.forceCast(model).isLoginUsingPasswordEnabled() || state === AppRunningState.runningBackground
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    PinFeature.get.forceCast(model).waitForPinToTrigger()
    PinFeature.get.forceCast(application).waitForPinToTrigger()
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'WaitForPinToTriggerAction'
  }

  public getActionType(): string {
    return WaitForPinToTriggerAction.type
  }
}
