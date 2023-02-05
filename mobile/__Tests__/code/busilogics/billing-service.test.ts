/* eslint-disable @typescript-eslint/unbound-method */
import { reject, resolve, take } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { flushPromises } from '../../../../common/__tests__/__helpers__/flush-promises'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { Uri } from '../../../../common/code/uri/uri'
import { Uris } from '../../../../common/native-modules/native-modules'
import { EventParams, PaymentAnalytics } from '../../../code/analytics/payment-analytics'
import { BillingService, ChallengeCallback, SbpUrlCallback } from '../../../code/busilogics/billing-service'
import { BillingServiceError } from '../../../code/busilogics/billing-service-error'
import { SbpPollingStrategy } from '../../../code/busilogics/handle-check-payment-polling'
import { PayBindingInfo } from '../../../code/busilogics/pay-binding-service'
import { PayBinding } from '../../../code/interfaces/pay-binding'
import { NewCard } from '../../../code/models/new-card'
import { Payer } from '../../../code/models/payer'
import { PaymentToken } from '../../../code/models/payment-token'
import { PaymentPollingResult } from '../../../code/models/payment-polling-result'
import { DiehardBackendApi } from '../../../code/network/diehard-backend/diehard-backend-api'
import { CheckPaymentResponse } from '../../../code/network/diehard-backend/entities/check-payment/check-payment-response'
import { SupplyApplePayRequest } from '../../../code/network/diehard-backend/entities/supply/supply-apple-pay-request'
import { SupplyGooglePayRequest } from '../../../code/network/diehard-backend/entities/supply/supply-google-pay-request'
import { SupplyNewCardRequest } from '../../../code/network/diehard-backend/entities/supply/supply-new-card-request'
import { SupplyPaymentResponse } from '../../../code/network/diehard-backend/entities/supply/supply-payment-response'
import { SupplySbpPaymentRequest } from '../../../code/network/diehard-backend/entities/supply/supply-sbp-payment-request'
import { SupplyStoredCardRequest } from '../../../code/network/diehard-backend/entities/supply/supply-stored-card-request'
import { Acquirer } from '../../../code/network/mobile-backend/entities/init/acquirer'
import { InitPaymentResponse } from '../../../code/network/mobile-backend/entities/init/init-payment-response'
import { InitializationParams } from '../../../code/network/mobile-backend/entities/init/initialization-params'
import { PaymethodMarkup } from '../../../code/network/mobile-backend/entities/init/paymethod-markup'
import { MobileBackendApi } from '../../../code/network/mobile-backend/mobile-backend-api'

const payer = new Payer('token', '123', 'test@ya.ru')
const purchaseToken = 'purchase_token_123'
const tokenProvider = resolve(new PaymentToken(purchaseToken))
const orderTag = 'orderTag_123'
const tokenProviderWithOrderTag = resolve(new PaymentToken(purchaseToken, orderTag))
const error = new YSError('error')
const initResponse = new InitPaymentResponse(
  'success',
  purchaseToken,
  'url',
  Acquirer.kassa,
  'sandbox',
  '1000',
  'RUB',
  null,
  null,
  'url',
  false,
  false,
  [],
  [],
)
const initResponseCardAmount = new InitPaymentResponse(
  'success',
  purchaseToken,
  'url',
  Acquirer.kassa,
  'sandbox',
  '1000',
  'RUB',
  null,
  new PaymethodMarkup('990'),
  'url',
  false,
  false,
  [],
  [],
)
const diehard = createMockInstance(DiehardBackendApi, {})
const mobile = createMockInstance(MobileBackendApi, {
  initializePayment: jest.fn().mockReturnValue(resolve(initResponse)),
})
const CheckPaymentResponses = {
  noChallenge: new CheckPaymentResponse('success', null, null, null, null, null),
  waitForProcessing: new CheckPaymentResponse('wait_for_processing', null, null, null, null, null),
  // eslint-disable-next-line prettier/prettier
  threeDSChallenge: new CheckPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
    null,
  ),
  // eslint-disable-next-line prettier/prettier
  threeDSSuccess: new CheckPaymentResponse(
    'wait_for_notification',
    null,
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
    'success',
  ),
  paymentFormUrl: new CheckPaymentResponse('wait_for_notification', null, null, null, 'https://qr.nspk.ru/', null),
}
const diehardResponse = new SupplyPaymentResponse('success', null, null)
const card = new NewCard('1234', '12', '21', '123', true)
const callback: ChallengeCallback = { show3ds(uri: Uri): void {}, hide3ds(): void {} }
const sbpCallback: SbpUrlCallback = { process(uri: Uri): void {} }
const overrideEmail = 'email2@yandex.ru'

describe(BillingService, () => {
  describe('Init test cases', () => {
    it('should get InitResponse on success init', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(3)
      expect(billingService.getPurchaseToken()).toBeNull()
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        expect(res).toStrictEqual(initResponse)
        expect(billingService.getPurchaseToken()).toStrictEqual(initResponse.token)
        done()
      })
    })

    it('should get error on init failed', (done) => {
      const patchMobile = createMockInstance(MobileBackendApi, {
        initializePayment: jest.fn().mockReturnValue(reject(error)),
      })
      const billingService = new BillingService(payer, tokenProvider, diehard, patchMobile, MockPayBinding())
      expect.assertions(2)
      billingService.initialization(InitializationParams.DEFAULT, false).failed((res) => {
        expect(res).toStrictEqual(error)
        expect(billingService.getPurchaseToken()).toBeNull()
        done()
      })
    })

    it('should log payment amounts', (done) => {
      const mobileApi = createMockInstance(MobileBackendApi, {
        initializePayment: jest.fn().mockReturnValue(resolve(initResponseCardAmount)),
      })
      const billingService = new BillingService(payer, tokenProvider, diehard, mobileApi, MockPayBinding())

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        const additionalParams = PaymentAnalytics.environment.getAdditionalParams()
        expect(additionalParams.get(EventParams.PURCHASE_TOTAL_AMOUNT)).toBe('1000')
        expect(additionalParams.get(EventParams.PURCHASE_CARD_AMOUNT)).toBe('990')
        expect(additionalParams.get(EventParams.PURCHASE_CURRENCY)).toBe('RUB')
        done()
      })
    })
  })

  describe('Pay test cases', () => {
    it('should get no challenge on success pay', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.pay('id', '123', null, callback).then((res) => {
          expect(patchedDiehard.supplyStoredCard).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should override email', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(2)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.pay('id', '123', overrideEmail, callback).then((res) => {
          expect(patchedDiehard.supplyStoredCard).toBeCalledWith(
            new SupplyStoredCardRequest(payer.oauthToken, purchaseToken, overrideEmail, 'id', '123'),
          )
          expect(patchedDiehard.checkPayment).toBeCalled()
          done()
        })
      })
    })

    it('should get error on empty purchase token', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.pay('id', '123', null, callback).failed((res) => {
        expect(res).toStrictEqual(BillingServiceError.notInitialized())
        done()
      })
    })

    it('should get error on empty email if not logged in', (done) => {
      const payerWithoutEmail = new Payer('', '123', null)
      const billingService = new BillingService(payerWithoutEmail, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.pay('id', '123', null, callback).failed((res) => {
          expect(res).toStrictEqual(BillingServiceError.noEmail())
          done()
        })
      })
    })

    it('should get error on empty email for ya oplata', (done) => {
      const payerWithoutEmail = new Payer('token', '123', null)
      const yaOplataPaymentToken = 'payment:gabcd'
      const billingService = new BillingService(
        payerWithoutEmail,
        resolve(new PaymentToken(yaOplataPaymentToken)),
        diehard,
        mobile,
        MockPayBinding(),
      )

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.pay('id', '123', null, callback).failed((res) => {
          expect(res).toStrictEqual(BillingServiceError.noEmail())
          done()
        })
      })
    })

    it('should get no error on empty email if logged in', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const payerLoggedWithoutEmail = new Payer('token', '123', null)
      const billingService = new BillingService(
        payerLoggedWithoutEmail,
        tokenProvider,
        patchedDiehard,
        mobile,
        MockPayBinding(),
      )

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.pay('id', '123', null, callback).then((res) => {
          expect(patchedDiehard.supplyStoredCard).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should get error on pay failed', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(reject(error)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.pay('id', '123', null, callback).failed((res) => {
          expect(res).toStrictEqual(error)
          done()
        })
      })
    })
  })

  describe('SBP pay test cases', () => {
    it('should perform success pay', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplySbpPay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.paymentFormUrl)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then(() => {
        billingService.sbpPay(SbpPollingStrategy.resolveOnSbpUrl, null, sbpCallback).then((res) => {
          expect(patchedDiehard.supplySbpPay).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should override email', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplySbpPay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.paymentFormUrl)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(2)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.sbpPay(SbpPollingStrategy.resolveOnSbpUrl, overrideEmail, sbpCallback).then(() => {
          expect(patchedDiehard.supplySbpPay).toBeCalledWith(
            new SupplySbpPaymentRequest(payer.oauthToken, purchaseToken, overrideEmail),
          )
          expect(patchedDiehard.checkPayment).toBeCalled()
          done()
        })
      })
    })

    it('should get error on empty purchase token', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.sbpPay(SbpPollingStrategy.resolveOnSbpUrl, null, sbpCallback).failed((e) => {
        expect(e).toStrictEqual(BillingServiceError.notInitialized())
        done()
      })
    })
  })

  describe('Pay NewCard test cases', () => {
    it('should get no challenge on success pay new card', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyNewCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.newCardPay(card, null, callback).then((res) => {
          expect(patchedDiehard.supplyNewCard).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should override email on pay new card', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyNewCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(2)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.newCardPay(card, overrideEmail, callback).then((res) => {
          expect(patchedDiehard.supplyNewCard).toBeCalledWith(
            new SupplyNewCardRequest(
              payer.oauthToken,
              purchaseToken,
              overrideEmail,
              card.cardNumber,
              card.expirationMonth,
              card.expirationYear,
              card.cvn,
              card.shouldBeStored,
            ),
          )
          expect(patchedDiehard.checkPayment).toBeCalled()
          done()
        })
      })
    })

    it('should get error on empty purchase token', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.newCardPay(card, null, callback).failed((res) => {
        expect(res).toStrictEqual(BillingServiceError.notInitialized())
        done()
      })
    })

    it('should get error on pay new card failed', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyNewCard: jest.fn().mockReturnValue(reject(error)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.newCardPay(card, null, callback).failed((res) => {
          expect(res).toStrictEqual(error)
          done()
        })
      })
    })
  })

  describe('GooglePay test cases', () => {
    it('should get no challenge on success pay new card', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyGooglePay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.googlePay('token', null, callback).then((res) => {
          expect(patchedDiehard.supplyGooglePay).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should get no challenge on success pay new card with order tag', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyGooglePay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const payBinding = MockPayBinding({
        bindGooglePayToken: jest.fn().mockReturnValue(resolve(new PayBindingInfo('googleToken', 'trustPaymentId'))),
      })
      const billingService = new BillingService(payer, tokenProviderWithOrderTag, patchedDiehard, mobile, payBinding)

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.googlePay('token', null, callback).then((res) => {
          expect(patchedDiehard.supplyGooglePay).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should override email on success GooglePay', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyGooglePay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.googlePay('token', overrideEmail, callback).then((res) => {
          expect(patchedDiehard.supplyGooglePay).toBeCalledWith(
            new SupplyGooglePayRequest(payer.oauthToken, purchaseToken, overrideEmail, 'token', null),
          )
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should get error on empty purchase token', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.googlePay('token', null, callback).failed((res) => {
        expect(res).toStrictEqual(BillingServiceError.notInitialized())
        done()
      })
    })

    it('should get error on pay failed', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyGooglePay: jest.fn().mockReturnValue(reject(error)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.googlePay('token', null, callback).failed((res) => {
          expect(res).toStrictEqual(error)
          done()
        })
      })
    })
  })

  describe('ApplePay test cases', () => {
    it('should get no challenge on success applepay', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyApplePay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const payBinding = MockPayBinding({
        bindAppleToken: jest.fn().mockReturnValue(resolve(new PayBindingInfo('applePayMethodId', 'trustPaymentId'))),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, payBinding)

      expect.assertions(4)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.applePay('token', null, callback).then((res) => {
          expect(payBinding.bindAppleToken).toBeCalledWith('token', purchaseToken, null)
          expect(patchedDiehard.supplyApplePay).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should get no challenge on success applepay with order tag', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyApplePay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const payBinding = MockPayBinding({
        bindAppleToken: jest.fn().mockReturnValue(resolve(new PayBindingInfo('applePayMethodId', 'trustPaymentId'))),
      })
      const billingService = new BillingService(payer, tokenProviderWithOrderTag, patchedDiehard, mobile, payBinding)

      expect.assertions(4)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.applePay('token', null, callback).then((res) => {
          expect(payBinding.bindAppleToken).toBeCalledWith('token', purchaseToken, orderTag)
          expect(patchedDiehard.supplyApplePay).toBeCalled()
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should override email on success applepay', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyApplePay: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const payBinding = MockPayBinding({
        bindAppleToken: jest.fn().mockReturnValue(resolve(new PayBindingInfo('applePayMethodId', 'trustPaymentId'))),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, payBinding)

      expect.assertions(3)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.applePay('token', overrideEmail, callback).then((res) => {
          expect(patchedDiehard.supplyApplePay).toBeCalledWith(
            new SupplyApplePayRequest(payer.oauthToken, purchaseToken, overrideEmail, null, 'applePayMethodId'),
          )
          expect(patchedDiehard.checkPayment).toBeCalled()
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          done()
        })
      })
    })

    it('should get error on empty purchase token', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.applePay('token', null, callback).failed((res) => {
        expect(res).toStrictEqual(BillingServiceError.notInitialized())
        done()
      })
    })

    it('should get error on applepay failed', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyApplePay: jest.fn().mockReturnValue(reject(error)),
      })
      const payBinding = MockPayBinding({
        bindAppleToken: jest.fn().mockReturnValue(resolve(new PayBindingInfo('applePayMethodId', 'trustPaymentId'))),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, payBinding)

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.applePay('token', null, callback).failed((res) => {
          expect(res).toStrictEqual(error)
          done()
        })
      })
    })

    it('should get error on pay binding failed', (done) => {
      const payBinding = MockPayBinding({
        bindAppleToken: jest.fn().mockReturnValue(reject(error)),
      })
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, payBinding)

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.applePay('token', null, callback).failed((res) => {
          expect(res).toStrictEqual(error)
          done()
        })
      })
    })
  })

  it('call cancelPaying does nothing', () => {
    const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())
    billingService.cancelPaying()
  })

  describe('Check status test cases', () => {
    it('should get 3DS challenge on success pay', async () => {
      jest.useFakeTimers()

      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest
          .fn()
          .mockReturnValueOnce(resolve(CheckPaymentResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckPaymentResponses.threeDSSuccess))
          .mockReturnValueOnce(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())
      const challengeCallback: ChallengeCallback = { show3ds: jest.fn(), hide3ds: jest.fn() }

      expect.assertions(5)
      await take(billingService.initialization(InitializationParams.DEFAULT, false))

      const payPromise = billingService.pay('id', '123', null, challengeCallback)

      // kick off startPolling() retry
      await flushPromises()
      jest.runAllTimers()
      await flushPromises()
      jest.runAllTimers()

      await take(payPromise)

      expect(challengeCallback.show3ds).toBeCalledWith(
        Uris.fromString(CheckPaymentResponses.threeDSChallenge.redirectURL!),
      )
      expect(challengeCallback.show3ds).toBeCalledTimes(1)
      expect(challengeCallback.hide3ds).toBeCalledTimes(1)
      expect(patchedDiehard.supplyStoredCard).toBeCalled()
      expect(patchedDiehard.checkPayment).toBeCalledTimes(3)
    })

    it('should cancel paying', async () => {
      jest.useFakeTimers()

      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest
          .fn()
          .mockReturnValueOnce(resolve(CheckPaymentResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckPaymentResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(2)
      await take(billingService.initialization(InitializationParams.DEFAULT, false))

      const payPromise = billingService.pay('id', '123', null, callback)

      // kick off startPolling() retry
      await flushPromises()
      billingService.cancelPaying()
      jest.runAllTimers()

      await expect(take(payPromise)).rejects.toBeInstanceOf(YSError)
      expect(patchedDiehard.checkPayment).toBeCalledTimes(1)
    })
  })

  describe('Tinkoff credit test cases', () => {
    it('should get no challenge on success pay', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.noChallenge)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(2)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.waitForTinkoffCreditResult(callback).then((res) => {
          expect(res).toStrictEqual(PaymentPollingResult.SUCCESS)
          expect(patchedDiehard.checkPayment).toBeCalled()
          done()
        })
      })
    })

    it('should get WAIT_FOR_PROCESSING result', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        supplyStoredCard: jest.fn().mockReturnValue(resolve(diehardResponse)),
        checkPayment: jest.fn().mockReturnValue(resolve(CheckPaymentResponses.waitForProcessing)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(2)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.waitForTinkoffCreditResult(callback).then((res) => {
          expect(res).toStrictEqual(PaymentPollingResult.WAIT_FOR_PROCESSING)
          expect(patchedDiehard.checkPayment).toBeCalled()
          done()
        })
      })
    })

    it('should get error on empty purchase token', (done) => {
      const billingService = new BillingService(payer, tokenProvider, diehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.waitForTinkoffCreditResult(callback).failed((res) => {
        expect(res).toStrictEqual(BillingServiceError.notInitialized())
        done()
      })
    })

    it('should get error on pay failed', (done) => {
      const patchedDiehard = createMockInstance(DiehardBackendApi, {
        checkPayment: jest.fn().mockReturnValue(reject(error)),
      })
      const billingService = new BillingService(payer, tokenProvider, patchedDiehard, mobile, MockPayBinding())

      expect.assertions(1)
      billingService.initialization(InitializationParams.DEFAULT, false).then((res) => {
        billingService.waitForTinkoffCreditResult(callback).failed((res) => {
          expect(res).toStrictEqual(error)
          done()
        })
      })
    })
  })
})

export function MockPayBinding(patch: Partial<PayBinding> = {}): PayBinding {
  return Object.assign(
    {},
    {
      bindAppleToken: jest.fn(),
      bindGooglePayToken: jest.fn(),
    },
    patch,
  )
}
