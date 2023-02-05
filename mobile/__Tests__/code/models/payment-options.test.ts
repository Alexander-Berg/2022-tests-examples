import { BankName } from '../../../code/busilogics/bank-name'
import {
  NewCardPaymentOption,
  StoredCardPaymentOption,
  GooglePaymentOption,
  ApplePaymentOption,
  SbpPaymentOption,
  DefaultPaymentOptionVisitor,
  PaymentOptionIds,
  PaymentOptionsBuilder,
  AllCasesPaymentOptionVisitor,
  CashPaymentOption,
  TinkoffCreditOption,
} from '../../../code/models/payment-options'
import { PaymentMethod } from '../../../code/network/mobile-backend/entities/methods/payment-method'
import { AvailableMethodsBuilder } from '../../../code/models/available-methods'

describe('Payment Option', () => {
  const allCasesVisitor = new AllCasesPaymentOptionVisitor<string>(
    (option: NewCardPaymentOption) => 'NewCardPaymentOption',
    (option: StoredCardPaymentOption) => 'StoredCardPaymentOption',
    (option: GooglePaymentOption) => 'GooglePaymentOption',
    (option: ApplePaymentOption) => 'ApplePaymentOption',
    (option: SbpPaymentOption) => 'SbpPaymentOption',
    (option: CashPaymentOption) => 'CashPaymentOption',
    (option: TinkoffCreditOption) => 'TinkoffCreditOption',
  )

  describe(NewCardPaymentOption, () => {
    it('should accept visitor', () => {
      const option = new NewCardPaymentOption()
      expect(option.getId()).toBe(PaymentOptionIds.NEW_CARD_ID)

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setNewCardPaymentOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('NewCardPaymentOption')
    })
  })
  describe(StoredCardPaymentOption, () => {
    it('should accept visitor', () => {
      const option = new StoredCardPaymentOption(
        new PaymentMethod('id', '', '', false, BankName.UnknownBank, null, null),
      )
      expect(option.getId()).toBe('id')

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setStoredCardPaymentOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('StoredCardPaymentOption')
    })
  })
  describe(GooglePaymentOption, () => {
    it('should accept visitor', () => {
      const option = new GooglePaymentOption()
      expect(option.getId()).toBe(PaymentOptionIds.GOOGLE_PAY_ID)

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setGooglePaymentOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('GooglePaymentOption')
    })
  })
  describe(ApplePaymentOption, () => {
    it('should accept visitor', () => {
      const option = new ApplePaymentOption()
      expect(option.getId()).toBe(PaymentOptionIds.APPLE_PAY_ID)

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setApplePaymentOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('ApplePaymentOption')
    })
  })
  describe(SbpPaymentOption, () => {
    it('should accept visitor', () => {
      const option = new SbpPaymentOption()
      expect(option.getId()).toBe(PaymentOptionIds.SBP_ID)

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setSbpPaymentOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('SbpPaymentOption')
    })
  })
  describe(CashPaymentOption, () => {
    it('should accept visitor', () => {
      const option = new CashPaymentOption()
      expect(option.getId()).toBe(PaymentOptionIds.CASH_ID)

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setCashPaymentOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('CashPaymentOption')
    })
  })
  describe(TinkoffCreditOption, () => {
    it('should accept visitor', () => {
      const option = new TinkoffCreditOption()
      expect(option.getId()).toBe(PaymentOptionIds.TINKOFF_CREDIT_ID)

      let visitor = DefaultPaymentOptionVisitor.withDefault((option) => 'default')
      expect(option.accept(visitor)).toBe('default')

      visitor = visitor.setTinkoffCreditOptionVisitor((option) => 'custom')
      expect(option.accept(visitor)).toBe('custom')

      expect(option.accept(allCasesVisitor)).toBe('TinkoffCreditOption')
    })
  })
})

describe(PaymentOptionsBuilder, () => {
  it('should build payment options', () => {
    const builder = new PaymentOptionsBuilder()
    expect(builder.getAllMethods()).toStrictEqual([])
    builder
      .setAvailableMethods(
        new AvailableMethodsBuilder()
          .setPaymentMethods([new PaymentMethod('', '', '', false, BankName.UnknownBank, null, null)])
          .setIsApplePayAvailable(true)
          .setIsGooglePayAvailable(true)
          .setIsSpbQrAvailable(true)
          .setIsCashAvailable(true)
          .build(),
      )
      .setHasNewCard(true)

    expect(builder.getAllMethods()).toStrictEqual([
      new StoredCardPaymentOption(new PaymentMethod('', '', '', false, BankName.UnknownBank, null, null)),
      new ApplePaymentOption(),
      new GooglePaymentOption(),
      new CashPaymentOption(),
      new SbpPaymentOption(),
      new NewCardPaymentOption(),
    ])
  })

  it('should get preferred payment option', () => {
    const builder = new PaymentOptionsBuilder()
    expect(builder.getPreferredMethod('id')).toBeNull()
    builder.setAvailableMethods(
      new AvailableMethodsBuilder()
        .setPaymentMethods([new PaymentMethod('id', '', '', false, BankName.UnknownBank, null, null)])
        .build(),
    )
    expect(builder.getPreferredMethod('id')).toStrictEqual(
      new StoredCardPaymentOption(new PaymentMethod('id', '', '', false, BankName.UnknownBank, null, null)),
    )
  })
})
