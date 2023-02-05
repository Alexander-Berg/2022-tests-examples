/* eslint-disable @typescript-eslint/unbound-method */
import { reject, resolve } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { Uri } from '../../../../common/code/uri/uri'
import { BankName } from '../../../code/busilogics/bank-name'
import { EmptyPaymentMethodsDecorator } from '../../../code/busilogics/payment-methods-decorator'
import { ApplePayProcessing } from '../../../code/interfaces/apple-pay-processing'
import { BillingProcessing } from '../../../code/interfaces/billing-processing'
import { GooglePayProcessing } from '../../../code/interfaces/google-pay-processing'
import { AvailableMethods } from '../../../code/models/available-methods'
import { PaymentPollingResult } from '../../../code/models/payment-polling-result'
import { Acquirer } from '../../../code/network/mobile-backend/entities/init/acquirer'
import { NewCard } from '../../../code/models/new-card'
import { InitializationParams } from '../../../code/network/mobile-backend/entities/init/initialization-params'
import { PaymethodMarkup } from '../../../code/network/mobile-backend/entities/init/paymethod-markup'
import {
  EnabledPaymentMethod,
  PaymentMethod,
} from '../../../code/network/mobile-backend/entities/methods/payment-method'
import { PaymentSettings } from '../../../code/models/payment-settings'
import { ChallengeCallback, SbpUrlCallback } from '../../../code/busilogics/billing-service'
import { SbpPollingStrategy } from '../../../code/busilogics/handle-check-payment-polling'
import { InitPaymentResponse } from '../../../code/network/mobile-backend/entities/init/init-payment-response'
import {
  PaymentRequestSynchronizer,
  PaymentDetails,
  totalAmount,
} from '../../../code/busilogics/payment-request-synchronizer'

const initResponse = buildInitResponse()
const error = new YSError('error')
const callback: ChallengeCallback = { show3ds(uri: Uri): void {}, hide3ds(): void {} }
const spbCallback: SbpUrlCallback = { process(uri: Uri): void {} }

function buildInitResponse(
  googlePayAvailable: boolean = false,
  applePayAvailable: boolean = false,
  paymentMethods: PaymentMethod[] = [new PaymentMethod('123', 'test', 'visa', false, BankName.UnknownBank, null, null)],
  enabledPaymentMethods: EnabledPaymentMethod[] = [],
  hasGooglePaySettings: boolean = googlePayAvailable,
): InitPaymentResponse {
  return new InitPaymentResponse(
    'success',
    'token',
    'url',
    Acquirer.kassa,
    'sandbox',
    '1000',
    'RUB',
    null,
    null,
    'url',
    googlePayAvailable,
    applePayAvailable,
    paymentMethods,
    enabledPaymentMethods,
  )
}

describe(PaymentRequestSynchronizer, () => {
  describe('Load PaymentMethods test cases', () => {
    it('should return payment methods after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.loadPaymentMethods().then((res) => {
        expect(res).toStrictEqual(new AvailableMethods(initResponse.paymentMethods, false, false, false, false))
        done()
      })
    })

    it('should return payment methods without apple/google pay if no processor provided', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(true, true))),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(2)
      requestSynchronizer.loadPaymentMethods().then((res) => {
        expect(res.isApplePayAvailable).toBeFalsy()
        expect(res.isGooglePayAvailable).toBeFalsy()
        done()
      })
    })

    it('should return payment methods without google pay if not supported in response even if local pay settings exists', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(false, false))),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(
        billingService,
        MockGooglePayProcessing(),
        null,
        new EmptyPaymentMethodsDecorator(),
        InitializationParams.DEFAULT,
      )
      expect.assertions(2)
      requestSynchronizer.loadPaymentMethods().then((res) => {
        expect(res.isApplePayAvailable).toBeFalsy()
        expect(res.isGooglePayAvailable).toBeFalsy()
        done()
      })
    })

    it('should return payment methods with google pay if supported in response and only local pay settings exists', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest
          .fn()
          .mockReturnValue(
            resolve(
              buildInitResponse(
                true,
                false,
                [new PaymentMethod('123', 'test', 'visa', false, BankName.UnknownBank, null, null)],
                [],
                false,
              ),
            ),
          ),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(
        billingService,
        MockGooglePayProcessing(),
        null,
        new EmptyPaymentMethodsDecorator(),
        InitializationParams.DEFAULT,
      )
      expect.assertions(2)
      requestSynchronizer.loadPaymentMethods().then((res) => {
        expect(res.isApplePayAvailable).toBeFalsy()
        expect(res.isGooglePayAvailable).toBeTruthy()
        done()
      })
    })

    it('should return payment methods with apple/google pay if available and processor provided', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(true, true))),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(
        billingService,
        MockGooglePayProcessing(),
        MockApplePayProcessing(),
      )
      expect.assertions(2)
      requestSynchronizer.loadPaymentMethods().then((res) => {
        expect(res.isApplePayAvailable).toBeTruthy()
        expect(res.isGooglePayAvailable).toBeTruthy()
        done()
      })
    })

    it('should return error on loadPaymentMethods after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.loadPaymentMethods().failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return payment methods with sbp', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest
          .fn()
          .mockReturnValue(resolve(buildInitResponse(false, false, [], [new EnabledPaymentMethod('sbp_qr')]))),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.loadPaymentMethods().then((res) => {
        expect(res.isSpbQrAvailable).toBe(true)
        done()
      })
    })
  })

  describe('Load Settings test cases', () => {
    it('should return payment settings after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.loadSettings().then((res) => {
        expect(res).toStrictEqual(
          new PaymentSettings(
            initResponse.total,
            initResponse.currency,
            initResponse.licenseURL,
            initResponse.acquirer,
            initResponse.environment,
            initResponse.merchantInfo,
            initResponse.payMethodMarkup,
            initResponse.creditFormUrl,
          ),
        )
        done()
      })
    })

    it('should return error on loadSettings after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.loadSettings().failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  describe('Load Payment Details test cases', () => {
    it('should return payment settings after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.loadPaymentDetails().then((res) => {
        expect(res).toStrictEqual(
          new PaymentDetails(
            new AvailableMethods(initResponse.paymentMethods, false, false, false, false),
            new PaymentSettings(
              initResponse.total,
              initResponse.currency,
              initResponse.licenseURL,
              initResponse.acquirer,
              initResponse.environment,
              initResponse.merchantInfo,
              initResponse.payMethodMarkup,
              initResponse.creditFormUrl,
            ),
          ),
        )
        done()
      })
    })
  })

  describe('Pay with PaymentMethod test cases', () => {
    it('should pay after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        pay: jest.fn().mockReturnValue(resolve(PaymentPollingResult.SUCCESS)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.pay('id', '123', null, callback).then((res) => {
        expect(res).toBe(PaymentPollingResult.SUCCESS)
        done()
      })
    })

    it('should return error on pay after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.pay('id', '123', null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on pay after payment failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        pay: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.pay('id', '123', null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  describe('Pay with SBP test cases', () => {
    it('should pay after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        sbpPay: jest.fn().mockReturnValue(resolve(PaymentPollingResult.SUCCESS)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.sbpPay(SbpPollingStrategy.resolveOnSuccess, null, spbCallback).then((res) => {
        expect(res).toBe(PaymentPollingResult.SUCCESS)
        done()
      })
    })

    it('should return error on pay after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.sbpPay(SbpPollingStrategy.resolveOnSuccess, null, spbCallback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on pay after payment failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        sbpPay: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.sbpPay(SbpPollingStrategy.resolveOnSuccess, null, spbCallback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  describe('Pay with NewCard test cases', () => {
    it('should pay with new card after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        newCardPay: jest.fn().mockReturnValue(resolve(PaymentPollingResult.SUCCESS)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.newCardPay(new NewCard('', '', '', '', false), null, callback).then((res) => {
        expect(res).toBe(PaymentPollingResult.SUCCESS)
        done()
      })
    })

    it('should return error on pay with new card after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.newCardPay(new NewCard('', '', '', '', false), null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on pay with new card after payment failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        newCardPay: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.newCardPay(new NewCard('', '', '', '', false), null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  describe('ApplePay test cases', () => {
    it('should applepay after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(false, true))),
        applePay: jest.fn().mockReturnValue(resolve(PaymentPollingResult.SUCCESS)),
      })
      const applePayProcessing = MockApplePayProcessing({
        pay: jest.fn().mockReturnValue(resolve('token')),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, applePayProcessing)
      expect.assertions(1)
      requestSynchronizer.applePay(null, callback).then((res) => {
        expect(res).toBe(PaymentPollingResult.SUCCESS)
        done()
      })
    })

    it('should return error on applepay if applepayprocessing not provided', (done) => {
      const requestSynchronizer = new PaymentRequestSynchronizer(
        MockBillingProcessing(),

        null,
        null,
      )
      expect.assertions(1)
      requestSynchronizer.applePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(new YSError('ApplePay is unavailable'))
        done()
      })
    })

    it('should return error on applepay after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, MockApplePayProcessing())
      expect.assertions(1)
      requestSynchronizer.applePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on applepay if applepay unavailable', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(false, false))),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, MockApplePayProcessing())
      expect.assertions(1)
      requestSynchronizer.applePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(new YSError('ApplePay is unavailable'))
        done()
      })
    })

    it('should return error on applepay after applepayprocessing error', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(false, true))),
      })
      const applePayProcessing = MockApplePayProcessing({
        pay: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, applePayProcessing)
      expect.assertions(1)
      requestSynchronizer.applePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on applepay after payment failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(false, true))),
        applePay: jest.fn().mockReturnValue(reject(error)),
      })
      const applePayProcessing = MockApplePayProcessing({
        pay: jest.fn().mockReturnValue(resolve('token')),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, applePayProcessing)
      expect.assertions(1)
      requestSynchronizer.applePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  describe('GooglePay test cases', () => {
    it('should googlepay after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(true, false))),
        googlePay: jest.fn().mockReturnValue(resolve(PaymentPollingResult.SUCCESS)),
      })
      const googlePayProcessing = MockGooglePayProcessing({
        pay: jest.fn().mockReturnValue(resolve('token')),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, googlePayProcessing, null)
      expect.assertions(1)
      requestSynchronizer.googlePay(null, callback).then((res) => {
        expect(res).toBe(PaymentPollingResult.SUCCESS)
        done()
      })
    })

    it('should override googlepay settings', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(false, false))),
        googlePay: jest.fn().mockReturnValue(resolve(PaymentPollingResult.SUCCESS)),
      })
      const googlePayProcessing = MockGooglePayProcessing({
        pay: jest.fn().mockReturnValue(resolve('token')),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(
        billingService,
        googlePayProcessing,
        null,
        new EmptyPaymentMethodsDecorator(),
        InitializationParams.DEFAULT,
      )
      expect.assertions(1)
      requestSynchronizer.googlePay(null, callback).then((res) => {
        expect(res).toBe(PaymentPollingResult.SUCCESS)
        done()
      })
    })

    it('should return error on googlepay if googlepayprocessing not provided', (done) => {
      const requestSynchronizer = new PaymentRequestSynchronizer(
        MockBillingProcessing(),

        null,
        null,
      )
      expect.assertions(1)
      requestSynchronizer.googlePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(new YSError('GooglePay is unavailable'))
        done()
      })
    })

    it('should return error on googlepay after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, MockGooglePayProcessing(), null)
      expect.assertions(1)
      requestSynchronizer.googlePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on googlepay after googlepayprocessing error', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(true, false))),
      })
      const googlePayProcessing = MockGooglePayProcessing({
        pay: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, googlePayProcessing, null)
      expect.assertions(1)
      requestSynchronizer.googlePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on googlepay after payment failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(buildInitResponse(true, false))),
        googlePay: jest.fn().mockReturnValue(reject(error)),
      })
      const googlePayProcessing = MockGooglePayProcessing({
        pay: jest.fn().mockReturnValue(resolve('token')),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, googlePayProcessing, null)
      expect.assertions(1)
      requestSynchronizer.googlePay(null, callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  describe('Tinkoff credit test cases', () => {
    it('should pay after success init', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        waitForTinkoffCreditResult: jest.fn().mockReturnValue(resolve(PaymentPollingResult.WAIT_FOR_PROCESSING)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.waitForTinkoffCreditResult(callback).then((res) => {
        expect(res).toBe(PaymentPollingResult.WAIT_FOR_PROCESSING)
        done()
      })
    })

    it('should return error on pay after init failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.waitForTinkoffCreditResult(callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })

    it('should return error on pay after payment failed', (done) => {
      const billingService = MockBillingProcessing({
        initialization: jest.fn().mockReturnValue(resolve(initResponse)),
        waitForTinkoffCreditResult: jest.fn().mockReturnValue(reject(error)),
      })
      const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
      expect.assertions(1)
      requestSynchronizer.waitForTinkoffCreditResult(callback).failed((e) => {
        expect(e).toStrictEqual(error)
        done()
      })
    })
  })

  it('should call cancelPaying', (done) => {
    const billingService = MockBillingProcessing({
      cancelPaying: jest.fn().mockImplementation(done()),
    })
    const requestSynchronizer = new PaymentRequestSynchronizer(billingService, null, null)
    requestSynchronizer.cancelPaying()
  })

  describe('Total amount for markup', () => {
    it('should use total', () => {
      const initResponse = buildInitResponse()
      expect(totalAmount(initResponse)).toStrictEqual('1000')
    })
    it('should use markup card', () => {
      const initResponse = new InitPaymentResponse(
        'success',
        'token',
        'url',
        Acquirer.kassa,
        'sandbox',
        '1000',
        'RUB',
        null,
        new PaymethodMarkup('100'),
        null,
        false,
        false,
        [],
        [],
      )
      expect(totalAmount(initResponse)).toStrictEqual('100')
    })
  })
})

export function MockApplePayProcessing(patch: Partial<ApplePayProcessing> = {}): ApplePayProcessing {
  return Object.assign(
    {},
    {
      close: jest.fn(),
      pay: jest.fn(),
    },
    patch,
  )
}

export function MockGooglePayProcessing(patch: Partial<GooglePayProcessing> = {}): GooglePayProcessing {
  return Object.assign(
    {},
    {
      pay: jest.fn(),
    },
    patch,
  )
}

export function MockBillingProcessing(patch: Partial<BillingProcessing> = {}): BillingProcessing {
  return Object.assign(
    {},
    {
      pay: jest.fn(),
      initialization: jest.fn(),
      newCardPay: jest.fn(),
      googlePay: jest.fn(),
      applePay: jest.fn(),
      sbpPay: jest.fn(),
      waitForTinkoffCreditResult: jest.fn(),
      cancelPaying: jest.fn(),
    },
    patch,
  )
}
