// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/components/login-component.ts >>>

import Foundation

open class LoginComponent: MBTComponent {
  public static let type: String = "LoginComponent"
  public init() {
  }

  @discardableResult
  open func getComponentType() -> String {
    return LoginComponent.type
  }

  @discardableResult
  open func assertMatches(_ _model: App, _ _application: App) throws -> Void {
  }

  @discardableResult
  open func tostring() -> String {
    return "LoginComponent"
  }

}

open class ReloginComponent: MBTComponent {
  public static let type: String = "LoginComponent"
  public init() {
  }

  @discardableResult
  open func getComponentType() -> String {
    return ReloginComponent.type
  }

  @discardableResult
  open func assertMatches(_ _model: App, _ _application: App) throws -> Void {
  }

  @discardableResult
  open func tostring() -> String {
    return "ReloginComponent"
  }

}

open class AllLoginActions: MBTComponentActions {
  private var accounts: YSArray<UserAccount>
  public init(_ accounts: YSArray<UserAccount>) {
    self.accounts = accounts
  }

  @discardableResult
  open func getActions(_ model: App) -> YSArray<MBTAction> {
    let actions: YSArray<MBTAction> = YSArray()
    YandexLoginFeature.`get`.performIfSupported(model, {
      (_mailboxModel) in
      self.accounts.forEach({
        (acc) in
        actions.push(YandexLoginAction(acc))
      })
    })
    return actions
  }

}

