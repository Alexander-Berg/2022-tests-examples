import { resolve, reject } from '../../../../../../common/xpromise-support'
import { YSError } from '../../../../../../common/ys'
import { JSONSerializer } from '../../../../../common/code/json/json-serializer'
import { Network } from '../../../../../common/code/network/network'
import { PlatformType } from '../../../../../common/code/network/platform'
import { resultValue } from '../../../../../common/code/result/result'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { YandexPayAnalytics } from '../../../../code/analytics/yandex-pay-analytics'
import { AuthMethods } from '../../../../code/models/auth-methods'
import { CardArt } from '../../../../code/models/card-art'
import { CardNetworks } from '../../../../code/models/card-networks'
import { CountryCodes } from '../../../../code/models/country-codes'
import { CurrencyCodes } from '../../../../code/models/currency-codes'
import { Merchant } from '../../../../code/models/merchant'
import { Order } from '../../../../code/models/order'
import { OrderItem } from '../../../../code/models/order-item'
import { OrderTotal } from '../../../../code/models/order-total'
import { PaymentMethod } from '../../../../code/models/payment-method'
import { PaymentMethodTypes } from '../../../../code/models/payment-method-types'
import { PaymentSheet } from '../../../../code/models/payment-sheet'
import { PushTokenType } from '../../../../code/models/push-token-type'
import { UserCard } from '../../../../code/models/user-card'
import { BankLogosRequest } from '../../../../code/network/yandex-pay-backend/bank-logos-request'
import { BankLogosResponse } from '../../../../code/network/yandex-pay-backend/bank-logos-response'
import { EncryptedAppIdRequest } from '../../../../code/network/yandex-pay-backend/encrypted-app-id-request'
import { EncryptedAppIdResponse } from '../../../../code/network/yandex-pay-backend/encrypted-app-id-response'
import { GetAllowedBinsRequest } from '../../../../code/network/yandex-pay-backend/get-allowed-bins-request'
import { GetAllowedBinsResponse } from '../../../../code/network/yandex-pay-backend/get-allowed-bins-response'
import { GetInstallRewardRequest } from '../../../../code/network/yandex-pay-backend/get-install-reward-request'
import { GetInstallRewardResponse } from '../../../../code/network/yandex-pay-backend/get-install-reward-response'
import { InitInstallRewardRequest } from '../../../../code/network/yandex-pay-backend/init-install-reward-request'
import { InitInstallRewardResponse } from '../../../../code/network/yandex-pay-backend/init-install-reward-response'
import { IsAuthorizedRequest } from '../../../../code/network/yandex-pay-backend/is-authorized-request'
import { IsAuthorizedResponse } from '../../../../code/network/yandex-pay-backend/is-authorized-response'
import { IsReadyToPayRequest } from '../../../../code/network/yandex-pay-backend/is-ready-to-pay-request'
import { IsReadyToPayResponse } from '../../../../code/network/yandex-pay-backend/is-ready-to-pay-response'
import { PayCheckoutRequest } from '../../../../code/network/yandex-pay-backend/pay-checkout-request'
import { PayCheckoutResponse } from '../../../../code/network/yandex-pay-backend/pay-checkout-response'
import { RegisterPushTokenRequest } from '../../../../code/network/yandex-pay-backend/register-push-token-request'
import { RegisterPushTokenResponse } from '../../../../code/network/yandex-pay-backend/register-push-token-response'
import { ResponseProcessor } from '../../../../code/network/yandex-pay-backend/response-processor'
import { SetDefaultCardRequest } from '../../../../code/network/yandex-pay-backend/set-default-card-request'
import { SetDefaultCardResponse } from '../../../../code/network/yandex-pay-backend/set-default-card-response'
import { SyncUserCardRequest } from '../../../../code/network/yandex-pay-backend/sync-user-card-request'
import { SyncUserCardResponse } from '../../../../code/network/yandex-pay-backend/sync-user-card-response'
import { UserCardsRequest } from '../../../../code/network/yandex-pay-backend/user-cards-request'
import { UserCardsResponse } from '../../../../code/network/yandex-pay-backend/user-cards-response'
import { UserProfileRequest } from '../../../../code/network/yandex-pay-backend/user-profile-request'
import { UserProfileResponse } from '../../../../code/network/yandex-pay-backend/user-profile-response'
import { ValidateRequest } from '../../../../code/network/yandex-pay-backend/validate-request'
import { ValidateResponse } from '../../../../code/network/yandex-pay-backend/validate-response'
import { YandexPayApi } from '../../../../code/network/yandex-pay-backend/yandex-pay-api'
import { bankLogo } from '../../../__helpers__/bank-logo-helper'
import { TestNetwork } from '../../../__helpers__/test-network'
import { TestResponse } from '../../../__helpers__/test-response'
import { TestSerializer } from '../../../__helpers__/test-serializer'

function createApi(executeRaw: Network['executeRaw'], deserialize: JSONSerializer['deserialize']): YandexPayApi {
  return new YandexPayApi(
    createMockInstance(TestNetwork, {
      executeRaw,
    }),
    new ResponseProcessor(
      createMockInstance(TestSerializer, {
        deserialize,
      }),
    ),
  )
}

describe(YandexPayApi, () => {
  afterEach(() => jest.restoreAllMocks())
  describe('checkout', () => {
    it('run checkout request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        payment_token: 'token',
        payment_method_info: {
          type: 'CARD',
          card_last4: '1234',
          card_network: 'AMEX',
        },
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'payCheckout')
      const request = new PayCheckoutRequest(
        'card1',
        'merchantorigin',
        new PaymentSheet(
          new Merchant('m1', 'merchant1'),
          new Order('order1', new OrderTotal('1000.00', 'total1'), [
            new OrderItem('item1', '250.00', null, null),
            new OrderItem('item2', '750.00', null, null),
          ]),
          CurrencyCodes.rub,
          CountryCodes.ru,
          [
            new PaymentMethod(
              [AuthMethods.cloudToken, AuthMethods.panOnly],
              PaymentMethodTypes.card,
              'G1',
              [CardNetworks.amex, CardNetworks.discover],
              'GM1',
            ),
            new PaymentMethod([AuthMethods.panOnly], PaymentMethodTypes.card, 'G2', [CardNetworks.jcb], 'GM2'),
          ],
        ),
      )
      const result = await api.checkout(request)
      expect(result).toBeInstanceOf(PayCheckoutResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.paymentCardNetwork).toBe(CardNetworks.amex)
      expect(result.paymentMethodCardLastDigits).toBe('1234')
      expect(result.paymentMethodType).toBe(PaymentMethodTypes.card)
      expect(result.paymentToken).toBe('token')
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run checkout request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'payCheckout')
      const request = new PayCheckoutRequest(
        'card1',
        'merchantorigin',
        new PaymentSheet(
          new Merchant('m1', 'merchant1'),
          new Order('order1', new OrderTotal('1000.00', 'total1'), [
            new OrderItem('item1', '450.00', null, null),
            new OrderItem('item2', '550.00', null, null),
          ]),
          CurrencyCodes.rub,
          CountryCodes.ru,
          [
            new PaymentMethod(
              [AuthMethods.cloudToken, AuthMethods.panOnly],
              PaymentMethodTypes.card,
              'G1',
              [CardNetworks.amex, CardNetworks.discover],
              'GM1',
            ),
            new PaymentMethod([AuthMethods.panOnly], PaymentMethodTypes.card, 'G2', [CardNetworks.jcb], 'GM2'),
          ],
        ),
      )
      api.checkout(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })

  describe('setDefaultCard', () => {
    it('run setDefaultCard request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({})
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'setDefaultCard')
      const request = new SetDefaultCardRequest('card1')
      const result = await api.setDefaultCard(request)
      expect(result).toBeInstanceOf(SetDefaultCardResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run setDefaultCard request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'setDefaultCard')
      const request = new SetDefaultCardRequest('card1')
      api.setDefaultCard(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })

  describe('validate', () => {
    it('run validate request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({})
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'validate')
      const request = new ValidateRequest(
        'merOrigin1',
        new PaymentSheet(
          new Merchant('merchantId', 'merchantName'),
          new Order('orderRequestGroupId', new OrderTotal('1000.00', 'orderRequestGroupLabel'), [
            new OrderItem('item1', '150.00', null, null),
            new OrderItem('item2', '850.00', null, null),
          ]),
          CurrencyCodes.rub,
          CountryCodes.ru,
          [
            new PaymentMethod(
              [AuthMethods.cloudToken, AuthMethods.panOnly],
              PaymentMethodTypes.card,
              'Gateway1',
              [CardNetworks.amex, CardNetworks.discover],
              'GatewayMerchantId1',
            ),
            new PaymentMethod(
              [AuthMethods.cloudToken],
              PaymentMethodTypes.card,
              'Gateway2',
              [CardNetworks.jcb, CardNetworks.maestro],
              'GatewayMerchantId2',
            ),
          ],
        ),
      )
      const result = await api.validate(request)
      expect(result).toBeInstanceOf(ValidateResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run validate request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'validate')
      const request = new ValidateRequest(
        'merOrigin1',
        new PaymentSheet(
          new Merchant('merchantId', 'merchantName'),
          new Order('orderRequestGroupId', new OrderTotal('1000.00', 'orderRequestGroupLabel'), [
            new OrderItem('item1', '100.00', null, null),
            new OrderItem('item2', '900.00', null, null),
          ]),
          CurrencyCodes.rub,
          CountryCodes.ru,
          [
            new PaymentMethod(
              [AuthMethods.cloudToken, AuthMethods.panOnly],
              PaymentMethodTypes.card,
              'Gateway1',
              [CardNetworks.amex, CardNetworks.discover],
              'GatewayMerchantId1',
            ),
            new PaymentMethod(
              [AuthMethods.cloudToken],
              PaymentMethodTypes.card,
              'Gateway2',
              [CardNetworks.jcb, CardNetworks.maestro],
              'GatewayMerchantId2',
            ),
          ],
        ),
      )
      api.validate(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })

  describe('userCards', () => {
    it('run userCards request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        cards: [
          {
            id: 'id-1',
            trust_card_id: 'trust_card_1',
            uid: 1,
            last4: '1111',
            card_network: 'AMEX',
            card_art: {
              pictures: {
                original: {
                  uri: 'https://card.1.com',
                },
              },
            },
            issuer_bank: 'issuer1',
            allowed_auth_methods: ['CLOUD_TOKEN', 'PAN_ONLY'],
            bin: '123456',
          },
        ],
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'userCards')
      const request = new UserCardsRequest()
      const result = await api.userCards(request)
      expect(result).toBeInstanceOf(UserCardsResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.cards).toStrictEqual([
        new UserCard(
          'id-1',
          'trust_card_1',
          [AuthMethods.cloudToken, AuthMethods.panOnly],
          'issuer1',
          1,
          CardNetworks.amex,
          '1111',
          new CardArt(new Map([['original', 'https://card.1.com']])),
          '123456',
        ),
      ])
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run userCards request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'userCards')
      const request = new UserCardsRequest()
      api.userCards(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })

  describe('isReadyToPay', () => {
    it('run isReadyToPay request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        is_ready_to_pay: true,
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'isReadyToPay')
      const request = new IsReadyToPayRequest('merOrigin1', 'mer1', true, [
        new PaymentMethod([AuthMethods.cloudToken], PaymentMethodTypes.card, 'g1', [CardNetworks.amex], 'gm1'),
      ])
      const result = await api.isReadyToPay(request)
      expect(result).toBeInstanceOf(IsReadyToPayResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.isReadyToPay).toBe(true)
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run isReadyToPay request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'isReadyToPay')
      const request = new IsReadyToPayRequest('merOrigin1', 'mer1', true, [
        new PaymentMethod([AuthMethods.panOnly], PaymentMethodTypes.card, 'g1', [CardNetworks.discover], 'gm1'),
      ])
      api.isReadyToPay(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('loadUserProfile', () => {
    it('run loadUserProfile request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        name: 'NAME',
        uid: 'UID',
        avatar: {
          lodpiUrl: 'lodpiUrl',
          hidpiUrl: 'hidpiUrl',
        },
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'loadUserProfile')
      const request = new UserProfileRequest()
      const result = await api.loadUserProfile(request)
      expect(result).toBeInstanceOf(UserProfileResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.name).toBe('NAME')
      expect(result.uid).toBe('UID')
      expect(result.loDpiUrl).toBe('lodpiUrl')
      expect(result.hiDpiUrl).toBe('hidpiUrl')
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run loadUserProfile request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'loadUserProfile')
      const request = new UserProfileRequest()
      api.loadUserProfile(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('syncUserCard', () => {
    it('run syncUserCard request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        id: 'id-1',
        uid: 1,
        last4: '1111',
        card_network: 'AMEX',
        card_art: {
          pictures: {
            original: {
              uri: 'https://card.1.com',
            },
          },
        },
        issuer_bank: 'issuer1',
        allowed_auth_methods: ['CLOUD_TOKEN', 'PAN_ONLY'],
        bin: '123456',
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'syncUserCard')
      const request = new SyncUserCardRequest('card_id')
      const result = await api.syncUserCard(request)
      expect(result).toBeInstanceOf(SyncUserCardResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.card).toStrictEqual(
        new UserCard(
          'id-1',
          null,
          [AuthMethods.cloudToken, AuthMethods.panOnly],
          'issuer1',
          1,
          CardNetworks.amex,
          '1111',
          new CardArt(new Map([['original', 'https://card.1.com']])),
          '123456',
        ),
      )
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run syncUserCard request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'syncUserCard')
      const request = new SyncUserCardRequest('card_id')
      api.syncUserCard(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('encryptedAppId', () => {
    it('run encryptedAppId request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        encrypted_app_id: 'id-1',
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'encryptedAppId')
      const request = new EncryptedAppIdRequest('app_id')
      const result = await api.encryptedAppId(request)
      expect(result).toBeInstanceOf(EncryptedAppIdResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.encryptedAppId).toBe('id-1')
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run encryptedAppId request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'encryptedAppId')
      const request = new EncryptedAppIdRequest('app_id')
      api.encryptedAppId(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('bankLogos', () => {
    it('run bankLogos request, return response if success, and trace analytics', async () => {
      const response = new TestResponse(
        200,
        JSON.stringify({
          AK_BARS: {
            FULL: {
              LIGHT: 'ak-bars-full-light',
              DARK: 'ak-bars-full-dark',
              MONO: 'ak-bars-full-mono',
            },
            SHORT: {
              LIGHT: 'ak-bars-short-light',
            },
          },
          ALFABANK: {
            FULL: {
              DARK: 'alfa-full-dark',
            },
            SHORT: {
              LIGHT: 'alfa-short-light',
              DARK: 'alfa-short-dark',
              MONO: 'alfa-short-mono',
            },
          },
        }),
      )
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'bankLogos')
      const request = new BankLogosRequest()
      const result = await api.bankLogos(request)
      expect(result).toBeInstanceOf(BankLogosResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.logos).toStrictEqual(
        new Map([
          [
            'AK_BARS',
            bankLogo({
              full: { light: 'ak-bars-full-light', dark: 'ak-bars-full-dark', mono: 'ak-bars-full-mono' },
              short: { light: 'ak-bars-short-light' },
            }),
          ],
          [
            'ALFABANK',
            bankLogo({
              full: { dark: 'alfa-full-dark' },
              short: { light: 'alfa-short-light', dark: 'alfa-short-dark', mono: 'alfa-short-mono' },
            }),
          ],
        ]),
      )
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run bankLogos request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'bankLogos')
      const request = new BankLogosRequest()
      api.bankLogos(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('getAllowedBins', () => {
    it('run bins/allowed request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        bins: [false, '123', '456', 10],
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'getAllowedBins')
      const request = new GetAllowedBinsRequest()
      const result = await api.getAllowedBins(request)
      expect(result).toBeInstanceOf(GetAllowedBinsResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.bins).toStrictEqual(['123', '456'])
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run bins/allowed request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'getAllowedBins')
      const request = new GetAllowedBinsRequest()
      api.getAllowedBins(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('registerPushToken', () => {
    it('run registerPushToken request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({})
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'registerPushToken')
      const request = new RegisterPushTokenRequest(
        'APPID',
        'APPVERSION',
        'HARDWAREID',
        'PUSHTOKEN',
        PushTokenType.firebase,
        PlatformType.android,
        'DEVICENAME',
        'ZONEID',
        'INSTALLID',
        'DEVICEID',
      )
      const result = await api.registerPushToken(request)
      expect(result).toBeInstanceOf(RegisterPushTokenResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run registerPushToken request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'registerPushToken')
      const request = new RegisterPushTokenRequest(
        'APPID',
        'APPVERSION',
        'HARDWAREID',
        'PUSHTOKEN',
        PushTokenType.huawei,
        PlatformType.android,
        'DEVICENAME',
        'ZONEID',
        'INSTALLID',
        'DEVICEID',
      )
      api.registerPushToken(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('isAuthorized', () => {
    it('run beta/allowed request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        allowed: true,
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'isAuthorized')
      const request = new IsAuthorizedRequest()
      const result = await api.isAuthorized(request)
      expect(result).toBeInstanceOf(IsAuthorizedResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.isAuthorized).toBe(true)
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('run beta/allowed request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'isAuthorized')
      const request = new IsAuthorizedRequest()
      api.isAuthorized(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('initInstallReward', () => {
    it('should run rewards/init-install-reward request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        reward: {
          amount: '200.0',
        },
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'initInstallReward')
      const request = new InitInstallRewardRequest('device-id-1')
      const result = await api.initInstallReward(request)
      expect(result).toBeInstanceOf(InitInstallRewardResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.rewardAmount).toBe('200.0')
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('should run rewards/init-install-reward request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'initInstallReward')
      const request = new InitInstallRewardRequest('device-id-1')
      api.initInstallReward(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
  describe('getInstallReward', () => {
    it('should run rewards/get-install-reward request, return response if success, and trace analytics', async () => {
      const response = TestResponse.success({
        reward: {
          amount: '200.0',
        },
      })
      const executeRaw = jest.fn().mockReturnValue(resolve(response))
      const deserialize = jest.fn().mockReturnValue(resultValue(response.fullJSONItem()))
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'getInstallReward')
      const request = new GetInstallRewardRequest('device-id-1')
      const result = await api.getInstallReward(request)
      expect(result).toBeInstanceOf(GetInstallRewardResponse)
      expect(result.status).toBe('success')
      expect(result.code).toBe(200)
      expect(result.rewardAmount).toBe('200.0')
      expect(executeRaw).toBeCalledWith(request)
      expect(eventsSpy).toBeCalled()
      eventsSpy.mockRestore()
    })
    it('should run rewards/get-install-reward request, return error if any, and trace analytics', (done) => {
      const executeRaw = jest.fn().mockReturnValue(reject(new YSError('ERROR')))
      const deserialize = jest.fn()
      const api = createApi(executeRaw, deserialize)
      const eventsSpy = jest.spyOn(YandexPayAnalytics.events, 'getInstallReward')
      const request = new GetInstallRewardRequest('device-id-1')
      api.getInstallReward(request).failed((reason) => {
        expect(reason.message).toBe('ERROR')
        expect(executeRaw).toBeCalledWith(request)
        expect(eventsSpy).toBeCalled()
        eventsSpy.mockRestore()
        done()
      })
    })
  })
})
