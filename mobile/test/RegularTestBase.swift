// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mbt/test/regular-test-base.ts >>>

import Foundation

open class RegularTestBase<T>: MBTTest<T> {
  public let accountType: AccountType2
  public init(_ description: String, _ accountType: AccountType2, _ suite: YSArray<TestSuite> = YSArray(TestSuite.Fixed)) {
    self.accountType = accountType
    super.init(description, suite)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(self.accountType)
  }

  open override func prepareAccounts(_ preparers: YSArray<T>) -> Void {
    if preparers.length != 1 {
      fatalError("Тесты на базе RegularTestBase должны наливать ровно один аккаунт!")
    }
    self.prepareAccount(preparers[0])
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ _model: AppModel!, _ _supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    if accounts.length != 1 {
      fatalError("Тесты на базе RegularTestBase должны использовать ровно один аккаунт!")
    }
    return self.regularScenario(accounts[0])
  }

  @discardableResult
  open func regularScenario(_ account: UserAccount) -> TestPlan {
    fatalError("Must be overridden in subclasses")
  }

  open func prepareAccount(_ preparer: T) -> Void {
    fatalError("Must be overridden in subclasses")
  }

}

