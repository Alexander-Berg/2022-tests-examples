import { Throwing } from '../../../../common/ys'
import { EventusEvent } from '../../../eventus-common/code/eventus-event'
import { BaseSimpleAction } from '../../../testopithecus-common/code/mbt/base-simple-action'
import {
  App,
  Feature,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../testopithecus-common/code/mbt/mbt-abstractions'
import { PaymentMethodNewCardComponent } from '../component/payment-method-new-card-component'
import { PaymentMethodSelectionComponent } from '../component/payment-method-selection-component'
import { SampleAppComponent } from '../component/sample-app-component'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import { PaymentMethodsListFeature } from '../feature/payment-methods-list-feature'
import { PaymentAdditionalSettings, SampleApp, SampleAppFeature } from '../feature/sample-app-feature'

export class OpenSampleAppAction implements MBTAction {
  public static readonly type: MBTActionType = 'OpenSampleAppAction'

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const readPaymentDetails = ReadPaymentDetailsFeature.get.forceCast(model)
    const account = readPaymentDetails.getAccount()
    const merchantId = readPaymentDetails.getMerchantId()
    const paymentId = readPaymentDetails.getPaymentId()
    const forceCvv = readPaymentDetails.getForceCvv()
    const filter = readPaymentDetails.getPaymentMethodsFilter()
    const isDarkModeEnabled = readPaymentDetails.isDarkModeEnabled()
    const personalInfoShowingMode = readPaymentDetails.getPersonalInfoShowingMode()
    const authorizationMode = readPaymentDetails.getAuthorizationMode()
    const bindingV2Enabled = readPaymentDetails.isBindingV2Enabled()
    const cashEnabled = readPaymentDetails.isCashEnabled()
    const additionalSettings = new PaymentAdditionalSettings(
      forceCvv,
      filter,
      isDarkModeEnabled,
      personalInfoShowingMode,
      authorizationMode,
      bindingV2Enabled,
      cashEnabled,
    )
    const modelStartPaymentProcess = SampleAppFeature.get.forceCast(model)
    const appStartPaymentProcess = SampleAppFeature.get.forceCast(application)

    modelStartPaymentProcess.startSampleApp(account, merchantId, paymentId, additionalSettings)
    appStartPaymentProcess.startSampleApp(account, merchantId, paymentId, additionalSettings)
    return new SampleAppComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      ReadPaymentDetailsFeature.get.included(modelFeatures) &&
      SampleAppFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return OpenSampleAppAction.type
  }
}

export class StartCardBindingProcessAction extends BaseSimpleAction<SampleApp, SampleAppComponent> {
  public static readonly type: MBTActionType = 'StartCardBindingProcessAction'

  public constructor() {
    super(StartCardBindingProcessAction.type)
  }

  public performImpl(modelOrApplication: SampleApp, currentComponent: SampleAppComponent): Throwing<MBTComponent> {
    modelOrApplication.bindCard()
    return new PaymentMethodNewCardComponent()
  }

  public requiredFeature(): Feature<SampleApp> {
    return SampleAppFeature.get
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class StartRegularPaymentProcessAction implements MBTAction {
  public static readonly type: MBTActionType = 'StartRegularPaymentProcessAction'

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelStartPaymentProcess = SampleAppFeature.get.forceCast(model)
    const appStartPaymentProcess = SampleAppFeature.get.forceCast(application)

    modelStartPaymentProcess.startRegularPayment()
    appStartPaymentProcess.startRegularPayment()

    const isSomePaymentMethodsAvailable = PaymentMethodsListFeature.get.forceCast(model).getMethods().length > 0
    return isSomePaymentMethodsAvailable ? new PaymentMethodSelectionComponent() : new PaymentMethodNewCardComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SampleAppFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return StartRegularPaymentProcessAction.type
  }
}

export class StartPreselectPaymentProcessAction implements MBTAction {
  public static readonly type: MBTActionType = 'StartPreselectPaymentProcessAction'

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelStartPaymentProcess = SampleAppFeature.get.forceCast(model)
    const appStartPaymentProcess = SampleAppFeature.get.forceCast(application)

    modelStartPaymentProcess.startPreselectPayment()
    appStartPaymentProcess.startPreselectPayment()

    const isSomePaymentMethodsAvailable = PaymentMethodsListFeature.get.forceCast(model).getMethods().length > 0
    return isSomePaymentMethodsAvailable ? new PaymentMethodSelectionComponent() : new PaymentMethodNewCardComponent()
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return SampleAppFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return this.getActionType()
  }

  public getActionType(): MBTActionType {
    return StartPreselectPaymentProcessAction.type
  }
}
