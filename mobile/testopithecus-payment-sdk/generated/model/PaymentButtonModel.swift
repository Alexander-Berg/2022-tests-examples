// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM model/payment-button-model.ts >>>

import Foundation

open class PaymentButtonModel: PaymentButton {
  private var buttonText: String = ""
  private var labelText: String = ""
  private var enabled: Bool = false
  private var buttonAction: ((Bool) throws -> Void)! = nil
  private let amount: String
  private let currency: String
  public init(_ amount: String, _ currency: String) {
    self.amount = amount
    self.currency = currency
    self.buttonText = PaymentButtonLabel.pay
    self.labelText = PaymentButtonLabel.label(self.currency, self.amount)
  }

  @discardableResult
  open func getButtonText() throws -> String {
    return self.buttonText
  }

  open func setButtonText(_ value: String) -> Void {
    self.buttonText = value
  }

  @discardableResult
  open func getLabelText() throws -> String {
    return self.labelText
  }

  open func setLabelText(_ value: String) -> Void {
    self.labelText = value
  }

  open func setButtonAction(_ buttonAction: @escaping (Bool) throws -> Void) -> Void {
    self.buttonAction = buttonAction
  }

  @discardableResult
  open func isEnabled() throws -> Bool {
    return self.enabled
  }

  @discardableResult
  open func setEnabledInModel(_ value: Bool) throws -> Void {
    self.enabled = value
  }

  @discardableResult
  open func pressButton() throws -> Void {
    if self.buttonAction != nil {
      let action = self.buttonAction!
      (try action(self.enabled))
    }
  }

}

open class PaymentButtonLabel {
  public static let addSberbankCard: String = "Add SberBank card"
  public static let addCard: String = "Add"
  public static let select: String = "Select"
  public static let close: String = "Close"
  public static let pay: String = "Pay"
  public static let enterCvv: String = "Enter CVV"
  @discardableResult
  open class func label(_ currency: String, _ amount: String) -> String {
    let amountNumber: Double! = stringToDouble(amount)
    let parts = amount.split(".")
    if amountNumber == nil || parts.length != 2 {
      return "\(currency) \(amount)"
    }
    if stringToInt32(parts[1]) == 0 {
      return "\(currency) \(parts[0])"
    } else {
      return "\(currency) \(amount)"
    }
  }

}

