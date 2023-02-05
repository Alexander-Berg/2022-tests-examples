/* eslint-disable @typescript-eslint/unbound-method */
import { reject, resolve, take } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { flushPromises } from '../../../../common/__tests__/__helpers__/flush-promises'
import { JSONSerializer } from '../../../../common/code/json/json-serializer'
import { MapJSONItem } from '../../../../common/code/json/json-types'
import { resultError, resultValue } from '../../../../common/code/result/result'
import { Uri } from '../../../../common/code/uri/uri'
import { Uris } from '../../../../common/native-modules/native-modules'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { Merchant } from '../../../code/models/merchant'
import { NewCard } from '../../../code/models/new-card'
import { Payer } from '../../../code/models/payer'
import { ChallengeCallback } from '../../../code/busilogics/billing-service'
import {
  CardBindingInfo,
  CardBindingService,
  CardDataCipher,
  CardDataCipherResult,
} from '../../../code/busilogics/card-binding-service'
import { CardBindingServiceError } from '../../../code/busilogics/card-binding-service-error'
import { BindNewCardResponse } from '../../../code/network/diehard-backend/entities/bind/bind-new-card-response'
import { CheckBindingPaymentResponse } from '../../../code/network/diehard-backend/entities/bind/check-binding-payment-response'
import { DiehardBackendApi } from '../../../code/network/diehard-backend/diehard-backend-api'
import { NewCardBindingRequest } from '../../../code/network/diehard-backend/entities/bind/new-card-binding-request'
import { NewCardBindingResponse } from '../../../code/network/diehard-backend/entities/bind/new-card-binding-response'
import { RegionIds } from '../../../code/network/diehard-backend/entities/bind/region-ids'
import { UnbindCardResponse } from '../../../code/network/diehard-backend/entities/bind/unbind-card-response'
import { MobileBackendApi } from '../../../code/network/mobile-backend/mobile-backend-api'
import { VerifyBindingRequest } from '../../../code/network/mobile-backend/entities/bind/verify-binding-request'
import { VerifyBindingResponse } from '../../../code/network/mobile-backend/entities/bind/verify-binding-response'

const payer = new Payer('token', '123', 'test@ya.ru')
const merchant = new Merchant('serviceToken', 'name')
const card = new NewCard('1234', '12', '21', '123', true)

const error = new YSError('error')
const callback: ChallengeCallback = { show3ds(uri: Uri): void {}, hide3ds(): void {} }
const info = new CardBindingInfo('card-123', '12345')

const CheckBindingResponses = {
  noChallenge: new CheckBindingPaymentResponse('success', null, null, info.cardId, info.rrn, null, null),
  // eslint-disable-next-line prettier/prettier
  threeDSChallenge: new CheckBindingPaymentResponse(
    'wait_for_notification',
    null,
    null,
    info.cardId,
    null,
    'https://trust-test.yandex.ru/web/redirect_3ds',
    null,
  ),
}

describe(CardBindingService, () => {
  describe('bind test cases', () => {
    const mobileApi = createMockInstance(MobileBackendApi)
    const serializer: JSONSerializer = {
      serialize: jest.fn(),
      deserialize: jest.fn(),
    }
    const cardDataCipher: CardDataCipher = {
      encrypt: jest.fn(),
    }

    it('should get empty challenge on success binding', (done) => {
      const response = new BindNewCardResponse('success', null, null, info.cardId)
      const diehardApi = createMockInstance(DiehardBackendApi, {
        bindNewCard: jest.fn().mockReturnValue(resolve(response)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.bind(card).then((res) => {
        expect(res).toStrictEqual(new CardBindingInfo(info.cardId, null))
        done()
      })
    })

    it('should get error on binding failed', (done) => {
      const error = new YSError('error')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        bindNewCard: jest.fn().mockReturnValue(reject(error)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.bind(card).failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })
  })

  describe('bindV2 test cases', () => {
    const cardData = new MapJSONItem()
      .putString('cvn', card.cvn)
      .putString('card_number', card.cardNumber)
      .putString('expiration_year', card.expirationYear)
      .putString('expiration_month', card.expirationMonth)
    const cardDataJson = JSON.stringify(cardData)
    const serializer: JSONSerializer = {
      serialize: jest.fn().mockReturnValue(resultValue(cardDataJson)),
      deserialize: jest.fn(),
    }
    const cipherResult = new CardDataCipherResult('SGkh', 'SHA512')
    const cardDataCipher: CardDataCipher = {
      encrypt: jest.fn().mockReturnValue(resolve(cipherResult)),
    }
    const bindingResponse = new NewCardBindingResponse('card-123')
    const verifyResponse = new VerifyBindingResponse('purchase_token')

    it('should get no challenge on success binding', (done) => {
      const bindingRequest = new NewCardBindingRequest(
        payer.oauthToken!,
        merchant.serviceToken,
        cipherResult.hashAlgorithm,
        cipherResult.dataEncryptedBase64,
        RegionIds.russia,
      )
      const diehardApi = createMockInstance(DiehardBackendApi, {
        newCardBinding: jest.fn().mockReturnValue(resolve(bindingResponse)),
        checkBindingPayment: jest.fn().mockReturnValue(resolve(CheckBindingResponses.noChallenge)),
      })
      const verifyRequest = new VerifyBindingRequest(bindingResponse.bindingId)
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(resolve(verifyResponse)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(6)
      bindingService.bindV2(card, callback).then((res) => {
        expect(serializer.serialize).toBeCalledWith(cardData)
        expect(cardDataCipher.encrypt).toBeCalledWith(cardDataJson)
        expect(diehardApi.newCardBinding).toBeCalledWith(bindingRequest)
        expect(mobileApi.verifyBinding).toBeCalledWith(verifyRequest)
        expect(diehardApi.checkBindingPayment).toBeCalledTimes(1)
        expect(res).toStrictEqual(info)
        done()
      })
    })

    it('should get error if oauth token is empty', (done) => {
      const payer = new Payer(null, null, '')
      const diehardApi = createMockInstance(DiehardBackendApi)
      const mobileApi = createMockInstance(MobileBackendApi)
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.bindV2(card, callback).failed((res) => {
        expect(res).toStrictEqual(CardBindingServiceError.emptyOAuthToken())
        done()
      })
    })

    it('should get error on data serialize failed', (done) => {
      const diehardApi = createMockInstance(DiehardBackendApi)
      const mobileApi = createMockInstance(MobileBackendApi)
      const serializer: JSONSerializer = {
        serialize: jest.fn().mockReturnValue(resultError(error)),
        deserialize: jest.fn(),
      }
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.bindV2(card, callback).failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })

    it('should get error on verify failed', (done) => {
      const diehardApi = createMockInstance(DiehardBackendApi, {
        newCardBinding: jest.fn().mockReturnValue(resolve(bindingResponse)),
      })
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(reject(error)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.bindV2(card, callback).failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })

    it('should get error on check payment failed', (done) => {
      const diehardApi = createMockInstance(DiehardBackendApi, {
        newCardBinding: jest.fn().mockReturnValue(resolve(bindingResponse)),
        checkBindingPayment: jest.fn().mockReturnValue(reject(error)),
      })
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(resolve(verifyResponse)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.bindV2(card, callback).failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })

    it('should get 3DS challenge on success binding', async () => {
      jest.useFakeTimers()

      const diehardApi = createMockInstance(DiehardBackendApi, {
        newCardBinding: jest.fn().mockReturnValue(resolve(bindingResponse)),
        checkBindingPayment: jest
          .fn()
          .mockReturnValueOnce(resolve(CheckBindingResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckBindingResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckBindingResponses.noChallenge)),
      })
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(resolve(verifyResponse)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )
      const callback: ChallengeCallback = { show3ds: jest.fn(), hide3ds: jest.fn() }

      expect.assertions(3)

      const bindPromise = bindingService.bindV2(card, callback)

      // kick off startPolling() retry
      await flushPromises()
      jest.runAllTimers()
      await flushPromises()
      jest.runAllTimers()

      await take(bindPromise)

      expect(callback.show3ds).toBeCalledWith(Uris.fromString(CheckBindingResponses.threeDSChallenge.redirectUrl!))
      expect(callback.show3ds).toBeCalledTimes(1)
      expect(diehardApi.checkBindingPayment).toBeCalledTimes(3)
    })

    it('cancel verify without active verification should do nothing', () => {
      const diehardApi = createMockInstance(DiehardBackendApi)
      const mobileApi = createMockInstance(MobileBackendApi)
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      bindingService.cancelVerify()
    })

    it('should cancel binding verification', async () => {
      jest.useFakeTimers()

      const diehardApi = createMockInstance(DiehardBackendApi, {
        newCardBinding: jest.fn().mockReturnValue(resolve(bindingResponse)),
        checkBindingPayment: jest
          .fn()
          .mockReturnValueOnce(resolve(CheckBindingResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckBindingResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckBindingResponses.noChallenge)),
      })
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(resolve(verifyResponse)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(2)

      const bindPromise = bindingService.bindV2(card, callback)

      // kick off startPolling() retry
      await flushPromises()
      bindingService.cancelVerify()
      jest.runAllTimers()

      await expect(take(bindPromise)).rejects.toBeInstanceOf(YSError)
      expect(diehardApi.checkBindingPayment).toBeCalledTimes(1)
    })
  })

  describe('verify test cases', () => {
    const serializer: JSONSerializer = {
      serialize: jest.fn(),
      deserialize: jest.fn(),
    }
    const cardDataCipher: CardDataCipher = {
      encrypt: jest.fn(),
    }

    it('should get error if oauth token is empty', (done) => {
      const payer = new Payer(null, null, '')
      const diehardApi = createMockInstance(DiehardBackendApi)
      const mobileApi = createMockInstance(MobileBackendApi)
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.verify('card-123', callback).failed((res) => {
        expect(res).toStrictEqual(CardBindingServiceError.emptyOAuthToken())
        done()
      })
    })

    it('should get error on verify failed', (done) => {
      const diehardApi = createMockInstance(DiehardBackendApi)
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(reject(error)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.verify('card-123', callback).failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })

    it('should get 3DS challenge on success binding', async () => {
      jest.useFakeTimers()

      const verifyResponse = new VerifyBindingResponse('purchase_token')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        checkBindingPayment: jest
          .fn()
          .mockReturnValueOnce(resolve(CheckBindingResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckBindingResponses.threeDSChallenge))
          .mockReturnValueOnce(resolve(CheckBindingResponses.noChallenge)),
      })
      const mobileApi = createMockInstance(MobileBackendApi, {
        verifyBinding: jest.fn().mockReturnValue(resolve(verifyResponse)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )
      const callback: ChallengeCallback = { show3ds: jest.fn(), hide3ds: jest.fn() }

      expect.assertions(3)

      const bindPromise = bindingService.verify('card-123', callback)

      // kick off startPolling() retry
      await flushPromises()
      jest.runAllTimers()
      await flushPromises()
      jest.runAllTimers()

      await take(bindPromise)

      expect(callback.show3ds).toBeCalledWith(Uris.fromString(CheckBindingResponses.threeDSChallenge.redirectUrl!))
      expect(callback.show3ds).toBeCalledTimes(1)
      expect(diehardApi.checkBindingPayment).toBeCalledTimes(3)
    })
  })

  describe('unbind test cases', () => {
    const mobileApi = createMockInstance(MobileBackendApi)
    const serializer: JSONSerializer = {
      serialize: jest.fn(),
      deserialize: jest.fn(),
    }
    const cardDataCipher: CardDataCipher = {
      encrypt: jest.fn(),
    }

    it('should get result on success unbinding', (done) => {
      const response = new UnbindCardResponse('success', null, null)
      const diehardApi = createMockInstance(DiehardBackendApi, {
        unbindCard: jest.fn().mockReturnValue(resolve(response)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.unbind('card_id_123').then((res) => {
        expect(diehardApi.unbindCard).toBeCalled()
        done()
      })
    })

    it('should get error on unbinding failed', (done) => {
      const error = new YSError('error')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        unbindCard: jest.fn().mockReturnValue(reject(error)),
      })
      const bindingService = new CardBindingService(
        payer,
        merchant,
        serializer,
        cardDataCipher,
        mobileApi,
        diehardApi,
        RegionIds.russia,
      )

      expect.assertions(1)
      bindingService.unbind('').failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })
  })
})
