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
import { requireNonNull, TestopithecusConstants } from '../../../testopithecus-common/code/utils/utils'
import { BoundCard } from '../card-generator'
import { FillNewCard, FillNewCardFeature } from '../feature/fill-new-card-feature'
import { ReadPaymentDetailsFeature } from '../feature/payment-details-feature'
import { NewCardField, NewCardMode } from '../model/fill-new-card-model'
import { AuthorizationMode } from '../sample/sample-configuration'

export class FillNewCardDataAction implements MBTAction {
  public static readonly type: MBTActionType = 'FillNewCardDataAction'

  public constructor(private readonly card: BoundCard, private readonly save: boolean) {}

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FillNewCardFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(model: App): Throwing<boolean> {
    return true
  }

  private tapAndSetValue(
    model: FillNewCard,
    application: FillNewCard,
    field: NewCardField,
    value: string,
  ): Throwing<void> {
    model.tapOnField(field)
    application.tapOnField(field)
    model.setFieldValue(field, value)
    application.setFieldValue(field, value)
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelFillNewCard = FillNewCardFeature.get.forceCast(model)
    const appFillNewCard = FillNewCardFeature.get.forceCast(application)
    const expirationDate = `${this.card.expirationMonth}${this.card.expirationYear}`

    this.tapAndSetValue(modelFillNewCard, appFillNewCard, NewCardField.cardNumber, this.card.cardNumber)
    this.tapAndSetValue(modelFillNewCard, appFillNewCard, NewCardField.expirationDate, expirationDate)
    this.tapAndSetValue(modelFillNewCard, appFillNewCard, NewCardField.cvv, this.card.cvv)

    const modelReadPaymentDetails = ReadPaymentDetailsFeature.get.forceCast(model)
    if (
      modelReadPaymentDetails.getAuthorizationMode() === AuthorizationMode.authorized &&
      modelFillNewCard.getNewCardMode() === NewCardMode.pay
    ) {
      modelFillNewCard.setSaveCardCheckboxEnabled(this.save)
      appFillNewCard.setSaveCardCheckboxEnabled(this.save)
    }
    return history.currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }

  public tostring(): string {
    return 'FillNewCardDataAction'
  }

  public getActionType(): string {
    return FillNewCardDataAction.type
  }
}

export class TapOnNewCardFieldAction extends BaseSimpleAction<FillNewCard, MBTComponent> {
  public static readonly type: MBTActionType = 'TapOnNewCardFieldAction'

  public constructor(
    private readonly field: NewCardField,
    unusedValue: string = TestopithecusConstants.SWIFT_CONSTRUCTOR_VARIABLE_WORKAROUND_TITLE,
  ) {
    super(TapOnNewCardFieldAction.type)
  }

  public requiredFeature(): Feature<FillNewCard> {
    return FillNewCardFeature.get
  }

  public performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnField(this.field)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class FillNewCardFieldAction extends BaseSimpleAction<FillNewCard, MBTComponent> {
  public static readonly type: MBTActionType = 'FillNewCardFieldAction'

  public constructor(private readonly field: NewCardField, private readonly value: string) {
    super(FillNewCardFieldAction.type)
  }

  public requiredFeature(): Feature<FillNewCard> {
    return FillNewCardFeature.get
  }

  public performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.setFieldValue(this.field, this.value)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class TapAndFillNewCardFieldAction extends BaseSimpleAction<FillNewCard, MBTComponent> {
  public static readonly type: MBTActionType = 'TapAndFillNewCardFieldAction'

  public constructor(private readonly field: NewCardField, private readonly value: string) {
    super(TapAndFillNewCardFieldAction.type)
  }

  public requiredFeature(): Feature<FillNewCard> {
    return FillNewCardFeature.get
  }

  public performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnField(this.field)
    modelOrApplication.setFieldValue(this.field, this.value)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class TapAndPasteNewCardFieldAction extends BaseSimpleAction<FillNewCard, MBTComponent> {
  public static readonly type: MBTActionType = 'TapAndPasteNewCardFieldAction'

  public constructor(private readonly field: NewCardField, private readonly value: string) {
    super(TapAndPasteNewCardFieldAction.type)
  }

  public requiredFeature(): Feature<FillNewCard> {
    return FillNewCardFeature.get
  }

  public performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.tapOnField(this.field)
    modelOrApplication.pasteFieldValue(this.field, this.value)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class PasteNewCardFieldAction extends BaseSimpleAction<FillNewCard, MBTComponent> {
  public static readonly type: MBTActionType = 'PasteNewCardFieldAction'

  public constructor(private readonly field: NewCardField, private readonly value: string) {
    super(PasteNewCardFieldAction.type)
  }

  public requiredFeature(): Feature<FillNewCard> {
    return FillNewCardFeature.get
  }

  public performImpl(modelOrApplication: FillNewCard, currentComponent: MBTComponent): Throwing<MBTComponent> {
    modelOrApplication.pasteFieldValue(this.field, this.value)
    return currentComponent
  }

  public events(): EventusEvent[] {
    return []
  }
}

export class TapOnNewCardBackButtonAction implements MBTAction {
  public static readonly type: MBTActionType = 'TapOnNewCardBackButtonAction'

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    FillNewCardFeature.get.forceCast(model).tapOnBackButton()
    FillNewCardFeature.get.forceCast(application).tapOnBackButton()
    return requireNonNull(history.previousDifferentComponent, 'There is no previous screen')
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return FillNewCardFeature.get.includedAll(modelFeatures, applicationFeatures)
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
    return TapOnNewCardBackButtonAction.type
  }
}
