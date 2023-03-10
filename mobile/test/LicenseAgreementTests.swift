// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM test/license-agreement-tests.ts >>>

import Foundation

open class LicenseAgreementPayWithNewCardTest: MBTTest<PaymentDataPreparer> {
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is shown on the Pay with new card screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(91)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).setPaymentMethodsFilter(PaymentMethodsFilter(false, false, false, false))
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(StartRegularPaymentProcessAction())
  }

}

open class LicenseAgreementExistingPaymentMethodsTest: MBTTest<PaymentDataPreparer> {
  private var card: BoundCard = BoundCard.generated()
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is shown on the Payment methods screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(92)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).addBoundCard(self.card)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(StartRegularPaymentProcessAction())
  }

}

open class LicenseAgreementPayWithNewCardNoAcquirerTest: MBTTest<PaymentDataPreparer> {
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is not shown on the Pay with new card screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(94)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].setPaymentMethodsFilter(PaymentMethodsFilter(false, false, false, false))
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(StartRegularPaymentProcessAction())
  }

}

open class LicenseAgreementBindCardTest: MBTTest<PaymentDataPreparer> {
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is not shown on the Bind screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(95)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(StartCardBindingProcessAction())
  }

}

open class LicenseAgreementPayWithExistingMethodsNoAcquirerTest: MBTTest<PaymentDataPreparer> {
  private var card: BoundCard = BoundCard.generated()
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is not shown on the Payment methods screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(97)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].addBoundCard(self.card)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(StartRegularPaymentProcessAction())
  }

}

open class LicenseAgreementPreselectPayWithExistingCardCVVTest: MBTTest<PaymentDataPreparer> {
  private var forceCvv: Bool = true
  private var card: BoundCard = BoundCard.generated()
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is shown on the Preselect screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(98)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).addBoundCard(self.card).setForceCvv(self.forceCvv)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(AssertAction()).then(StartPreselectPaymentProcessAction()).then(SetPaymentButtonStatusAction(true)).then(AssertAction()).then(SelectPaymentMethodAction(0)).then(PressSelectButtonAction()).then(SetPaymentButtonStatusAction(false))
  }

}

open class LicenseAgreementPreselectPayWithExistingCardCVVNoAcquirerTest: MBTTest<PaymentDataPreparer> {
  private var forceCvv: Bool = true
  private var card: BoundCard = BoundCard.generated()
  public init() {
    super.init("\(TestScenario.licenseAgreement) License agreement is not shown on the Preselect screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(100)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].addBoundCard(self.card).setForceCvv(self.forceCvv)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(AssertAction()).then(StartPreselectPaymentProcessAction()).then(SetPaymentButtonStatusAction(true)).then(AssertAction()).then(SelectPaymentMethodAction(0)).then(PressSelectButtonAction()).then(SetPaymentButtonStatusAction(false))
  }

}

open class LicenseAgreementOpenTest: MBTTest<PaymentDataPreparer> {
  private var card: BoundCard = BoundCard.generated()
  public init() {
    super.init("\(TestScenario.licenseAgreement) Open license agreement screen")
  }

  open override func setupSettings(_ settings: TestSettings) -> Void {
    settings.commonCase(101)
  }

  open override func prepareAccounts(_ preparers: YSArray<PaymentDataPreparer>) -> Void {
    preparers[0].setUseYaOplata(true).setAcquirer(Acquirer.kassa).addBoundCard(self.card)
  }

  @discardableResult
  open override func requiredAccounts() -> YSArray<AccountType2> {
    return YSArray(AccountType2.YandexTest)
  }

  @discardableResult
  open override func scenario(_ accounts: YSArray<UserAccount>, _ model: AppModel!, _ supportedFeatures: YSArray<FeatureID>) -> TestPlan {
    return TestPlan.empty().then(OpenSampleAppAction()).then(AssertAction()).then(StartRegularPaymentProcessAction()).then(AssertAction()).then(OpenFullLicenseAgreementAction()).then(AssertAction()).then(CloseFullLicenseAgreementAction())
  }

}

