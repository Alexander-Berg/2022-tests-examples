import { BankName } from '../../../code/busilogics/bank-name'
import { AvailableMethodsBuilder } from '../../../code/models/available-methods'
import { PartnerInfo, PaymentMethod } from '../../../code/network/mobile-backend/entities/methods/payment-method'
import {
  ConcatPaymentMethodsDecorator,
  EmptyPaymentMethodsDecorator,
  EnableCashDecorator,
  FilterPaymentMethodsDecorator,
  PaymentMethodsCompositeDecorator,
  PaymentMethodsFilter,
} from '../../../code/busilogics/payment-methods-decorator'

describe(EmptyPaymentMethodsDecorator, () => {
  const paymentMethods: PaymentMethod[] = [
    new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null),
  ]
  const availableMethods = new AvailableMethodsBuilder().setPaymentMethods(paymentMethods).build()
  const decorator = new EmptyPaymentMethodsDecorator()

  it('should return same availableMethods', (done) => {
    decorator.decorate(availableMethods).then((methods) => {
      expect(availableMethods).toBe(methods)
      done()
    })
  })
})

describe(FilterPaymentMethodsDecorator, () => {
  const paymentMethods: PaymentMethod[] = [
    new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null),
  ]
  const availableMethods = new AvailableMethodsBuilder()
    .setPaymentMethods(paymentMethods)
    .setIsApplePayAvailable(true)
    .setIsGooglePayAvailable(true)
    .setIsSpbQrAvailable(true)
    .setIsCashAvailable(true)
    .build()

  it('should filter availableMethods', (done) => {
    const filter = new PaymentMethodsFilter(false, false, false, false)
    const decorator = new FilterPaymentMethodsDecorator(filter)
    decorator.decorate(availableMethods).then((methods) => {
      expect(methods).toStrictEqual(
        new AvailableMethodsBuilder()
          .setPaymentMethods([])
          .setIsApplePayAvailable(false)
          .setIsGooglePayAvailable(false)
          .setIsSpbQrAvailable(false)
          .setIsCashAvailable(true)
          .build(),
      )
      done()
    })
  })

  it('should return the same availableMethods', (done) => {
    const filter = new PaymentMethodsFilter()
    const decorator = new FilterPaymentMethodsDecorator(filter)
    decorator.decorate(availableMethods).then((methods) => {
      expect(methods).toStrictEqual(availableMethods)
      done()
    })
  })

  it('should filter yabank', (done) => {
    const regularCard = new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null)
    const listWithYabank: PaymentMethod[] = [
      regularCard,
      new PaymentMethod('idqfa', '', 'sys1', false, BankName.UnknownBank, null, new PartnerInfo(true, true)),
    ]
    const methodsWithYabank = new AvailableMethodsBuilder()
      .setPaymentMethods(listWithYabank)
      .setIsApplePayAvailable(true)
      .setIsGooglePayAvailable(true)
      .setIsSpbQrAvailable(true)
      .setIsCashAvailable(true)
      .build()
    const filter = new PaymentMethodsFilter(true, false, false, false, false)
    const decorator = new FilterPaymentMethodsDecorator(filter)
    decorator.decorate(methodsWithYabank).then((methods) => {
      expect(methods.paymentMethods).toStrictEqual(
        new AvailableMethodsBuilder()
          .setPaymentMethods([regularCard])
          .setIsApplePayAvailable(false)
          .setIsGooglePayAvailable(false)
          .setIsSpbQrAvailable(false)
          .setIsCashAvailable(true)
          .build().paymentMethods,
      )
      done()
    })
  })

  it('should pass yabank', (done) => {
    const listWithYabank: PaymentMethod[] = [
      new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null),
      new PaymentMethod('idqfa', '', 'sys1', false, BankName.UnknownBank, null, new PartnerInfo(true, true)),
    ]
    const methodsWithYabank = new AvailableMethodsBuilder()
      .setPaymentMethods(listWithYabank)
      .setIsApplePayAvailable(true)
      .setIsGooglePayAvailable(true)
      .setIsSpbQrAvailable(true)
      .setIsCashAvailable(true)
      .build()
    const filter = new PaymentMethodsFilter(true, false, false, false, true)
    const decorator = new FilterPaymentMethodsDecorator(filter)
    decorator.decorate(methodsWithYabank).then((methods) => {
      expect(methods.paymentMethods).toStrictEqual(
        new AvailableMethodsBuilder()
          .setPaymentMethods(listWithYabank)
          .setIsApplePayAvailable(false)
          .setIsGooglePayAvailable(false)
          .setIsSpbQrAvailable(false)
          .setIsCashAvailable(true)
          .build().paymentMethods,
      )
      done()
    })
  })
})

describe(ConcatPaymentMethodsDecorator, () => {
  const paymentMethods: PaymentMethod[] = [
    new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null),
  ]
  const availableMethods = new AvailableMethodsBuilder().setPaymentMethods(paymentMethods).build()
  const paymentMethodsToConcat: PaymentMethod[] = [
    new PaymentMethod('id2', 'ac2', 'sys2', true, BankName.UnknownBank, null, null),
  ]
  const decorator = new ConcatPaymentMethodsDecorator(paymentMethodsToConcat)

  it('decorate should return summary paymentMethods', (done) => {
    decorator.decorate(availableMethods).then((methods) => {
      expect(methods.paymentMethods).toStrictEqual([
        new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null),
        new PaymentMethod('id2', 'ac2', 'sys2', true, BankName.UnknownBank, null, null),
      ])
      done()
    })
  })
})

describe(EnableCashDecorator, () => {
  const availableMethods = new AvailableMethodsBuilder().build()

  it('decorate should return availableMethods with enabled cash', (done) => {
    const decorator = new EnableCashDecorator(true)
    const availableMethodsWithCash = new AvailableMethodsBuilder().setIsCashAvailable(true).build()
    decorator.decorate(availableMethods).then((methods) => {
      expect(methods).toStrictEqual(availableMethodsWithCash)
      done()
    })
  })

  it('decorate should return same availableMethods', (done) => {
    const decorator = new EnableCashDecorator(false)
    decorator.decorate(availableMethods).then((methods) => {
      expect(methods).toStrictEqual(availableMethods)
      done()
    })
  })
})

describe(PaymentMethodsCompositeDecorator, () => {
  const paymentMethods: PaymentMethod[] = [
    new PaymentMethod('id1', 'ac1', 'sys1', false, BankName.UnknownBank, null, null),
  ]
  const availableMethods = new AvailableMethodsBuilder().setPaymentMethods(paymentMethods).build()
  const paymentMethodsToConcat: PaymentMethod[] = [
    new PaymentMethod('id2', 'ac2', 'sys2', true, BankName.UnknownBank, null, null),
  ]
  const decorator = new PaymentMethodsCompositeDecorator()
  decorator.addDecorator(new ConcatPaymentMethodsDecorator(paymentMethodsToConcat))

  it('decorate should return composite availableMethods', (done) => {
    decorator.decorate(availableMethods).then((methods) => {
      expect(methods.paymentMethods.length).toBe(2)
      done()
    })
  })
})
