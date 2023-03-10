// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM action/payment-methods-list-actions.ts >>>

import Foundation

open class SelectPaymentMethodAction: BaseSimpleAction<PaymentMethodsList, PaymentMethodSelectionComponent> {
  private var methodIndex: Int32
  public init(_ methodIndex: Int32) {
    self.methodIndex = methodIndex
    super.init("SelectPaymentMethod")
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: PaymentMethodsList, _ currentComponent: PaymentMethodSelectionComponent) throws -> MBTComponent {
    (try modelOrApplication.selectMethod(self.methodIndex))
    return PaymentMethodSelectedComponent()
  }

  @discardableResult
  open override func canBePerformed(_ model: App) throws -> Bool {
    let methodCanBeSelected = PaymentMethodsListFeature.`get`.forceCast(model).getMethods().length > self.methodIndex
    return (try super.canBePerformed(model)) && methodCanBeSelected
  }

  @discardableResult
  open override func requiredFeature() -> Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class ClickNewCardPaymentMethodAction: BaseSimpleAction<PaymentMethodsList, MBTComponent> {
  public static let type: MBTActionType = "ClickNewCardPaymentMethodAction"
  public init() {
    super.init(ClickNewCardPaymentMethodAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.`get`
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: PaymentMethodsList, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.clickNewCard())
    return PaymentMethodNewCardComponent()
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class SelectSbpMethodAction: BaseSimpleAction<PaymentMethodsList, PaymentMethodSelectionComponent> {
  public init() {
    super.init("SelectSbpMethod")
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: PaymentMethodsList, _ currentComponent: PaymentMethodSelectionComponent) throws -> MBTComponent {
    (try modelOrApplication.selectSbpMethod())
    return PaymentMethodSelectedComponent()
  }

  @discardableResult
  open override func canBePerformed(_ model: App) throws -> Bool {
    let methodCanBeSelected = PaymentMethodsListFeature.`get`.forceCast(model).getMethods().includes(PaymentMethodName.sbp)
    return (try super.canBePerformed(model)) && methodCanBeSelected
  }

  @discardableResult
  open override func requiredFeature() -> Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class TapOnCashPaymentMethodAction: BaseSimpleAction<Preselect, MBTComponent> {
  public static let type: MBTActionType = "TapOnCashPaymentMethodAction"
  public init() {
    super.init(TapOnCashPaymentMethodAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<Preselect> {
    return PreselectFeature.`get`
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: Preselect, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.selectCash())
    return PaymentMethodSelectedComponent()
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class PreselectTapOnOtherCardAction: BaseSimpleAction<Preselect, MBTComponent> {
  public static let type: MBTActionType = "PreselectTapOnOtherCardAction"
  public init() {
    super.init(PreselectTapOnOtherCardAction.type)
  }

  @discardableResult
  open override func requiredFeature() -> Feature<Preselect> {
    return PreselectFeature.`get`
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: Preselect, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnOtherCard())
    return PaymentMethodNewCardComponent()
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class PreselectTapOnAddCardAction: BaseSimpleAction<Preselect, MBTComponent> {
  public static let type: MBTActionType = "PreselectTapOnAddCardAction"
  public init() {
    super.init(PreselectTapOnAddCardAction.type)
  }

  @discardableResult
  open override func canBePerformed(_ model: App) throws -> Bool {
    return (try PaymentButtonFeature.`get`.forceCast(model).isEnabled())
  }

  @discardableResult
  open override func requiredFeature() -> Feature<Preselect> {
    return PreselectFeature.`get`
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: Preselect, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnAddCard())
    return Page3dsComponent()
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class PressSelectButtonAction: MBTAction {
  public static let type: MBTActionType = "PressSelectButtonAction"
  @discardableResult
  open func perform(_ model: App, _ application: App, _ history: MBTHistory) throws -> MBTComponent {
    let modelPreselect = PreselectFeature.`get`.forceCast(model)
    let appPreselect = PreselectFeature.`get`.forceCast(application)
    let readPaymentDetails = ReadPaymentDetailsFeature.`get`.forceCast(model)
    (try modelPreselect.tapOnSelectButton())
    (try appPreselect.tapOnSelectButton())
    if (try modelPreselect.isCashSelected()) {
      return SampleAppComponent()
    }
    return readPaymentDetails.getForceCvv() ? PreselectCvvComponent() : PaymentResultComponent()
  }

  @discardableResult
  open func supported(_ modelFeatures: YSArray<FeatureID>, _ applicationFeatures: YSArray<FeatureID>) -> Bool {
    return PreselectFeature.`get`.includedAll(modelFeatures, applicationFeatures)
  }

  @discardableResult
  open func canBePerformed(_ model: App) throws -> Bool {
    return true
  }

  @discardableResult
  open func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

  @discardableResult
  open func tostring() -> String {
    return self.getActionType()
  }

  @discardableResult
  open func getActionType() -> MBTActionType {
    return PressSelectButtonAction.type
  }

}

open class TapOnCvvFieldOfSelectPaymentMethodAction: BaseSimpleAction<PaymentMethodsList, MBTComponent> {
  public static let type: MBTActionType = "TapOnCvvFieldOfSelectPaymentMethodAction"
  public init() {
    super.init(TapOnCvvFieldOfSelectPaymentMethodAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: PaymentMethodsList, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnCvvField())
    return currentComponent
  }

  @discardableResult
  open override func canBePerformed(_ model: App) throws -> Bool {
    let isSomeMethodSelected = (try PaymentMethodsListFeature.`get`.forceCast(model).getSelected()) >= 0
    return (try super.canBePerformed(model)) && isSomeMethodSelected
  }

  @discardableResult
  open override func requiredFeature() -> Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

open class EnterCvvForSelectPaymentMethodAction: BaseSimpleAction<PaymentMethodsList, MBTComponent> {
  public static let type: MBTActionType = "EnterCvvForSelectPaymentMethodAction"
  private var cvv: MBTActionType
  public override init(_ cvv: MBTActionType) {
    self.cvv = cvv
    super.init(EnterCvvForSelectPaymentMethodAction.type)
  }

  @discardableResult
  open override func performImpl(_ modelOrApplication: PaymentMethodsList, _ currentComponent: MBTComponent) throws -> MBTComponent {
    (try modelOrApplication.tapOnCvvField())
    (try modelOrApplication.setCvvFieldValue(self.cvv))
    return currentComponent
  }

  @discardableResult
  open override func canBePerformed(_ model: App) throws -> Bool {
    let isSomeMethodSelected = (try PaymentMethodsListFeature.`get`.forceCast(model).getSelected()) >= 0
    return (try super.canBePerformed(model)) && isSomeMethodSelected
  }

  @discardableResult
  open override func requiredFeature() -> Feature<PaymentMethodsList> {
    return PaymentMethodsListFeature.`get`
  }

  @discardableResult
  open override func events() -> YSArray<EventusEvent> {
    return YSArray()
  }

}

