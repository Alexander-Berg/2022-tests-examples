import { reject, resolve } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { PayBindingInfo, PayBindingService, PayBindingServiceError } from '../../../code/busilogics/pay-binding-service'
import { Merchant } from '../../../code/models/merchant'
import { Payer } from '../../../code/models/payer'
import { DiehardBackendApi } from '../../../code/network/diehard-backend/diehard-backend-api'
import { BindPayTokenResponse } from '../../../code/network/diehard-backend/entities/bind/bind-pay-token-response'
import { RegionIds } from '../../../code/network/diehard-backend/entities/bind/region-ids'

const payer = new Payer('token', '123', 'test@ya.ru')
const merchant = new Merchant('serviceToken', 'name')

describe(PayBindingService, () => {
  describe('bind apple test cases', () => {
    it('should get empty challenge on success binding', (done) => {
      const response = new BindPayTokenResponse('success', null, null, 'apple_pay_method', 'trust_payment_id')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        bindAppleToken: jest.fn().mockReturnValue(resolve(response)),
      })
      const bindingService = new PayBindingService(payer, merchant, diehardApi, RegionIds.russia)

      expect.assertions(1)
      bindingService.bindAppleToken('token', 'purchaseToken', 'orderTag').then((res) => {
        expect(res).toStrictEqual(new PayBindingInfo('apple_pay_method', 'trust_payment_id'))
        done()
      })
    })

    it('should get error on binding failed', (done) => {
      const error = new YSError('error')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        bindAppleToken: jest.fn().mockReturnValue(reject(error)),
      })
      const bindingService = new PayBindingService(payer, merchant, diehardApi, RegionIds.russia)

      expect.assertions(1)
      bindingService.bindAppleToken('token', 'purchaseToken', 'orderTag').failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })

    it('should get error on empty tokens', (done) => {
      const diehardApi = createMockInstance(DiehardBackendApi)
      const bindingService = new PayBindingService(payer, merchant, diehardApi, RegionIds.russia)

      expect.assertions(1)
      bindingService.bindAppleToken('token', null, null).failed((res) => {
        expect(res).toStrictEqual(PayBindingServiceError.emptyPurchaseTokenAndOrderTag())
        done()
      })
    })
  })

  describe('bind google pay test cases', () => {
    it('should get empty challenge on success binding', (done) => {
      const response = new BindPayTokenResponse('success', null, null, 'google_pay_method', 'trust_payment_id')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        bindGooglePayToken: jest.fn().mockReturnValue(resolve(response)),
      })
      const bindingService = new PayBindingService(payer, merchant, diehardApi, RegionIds.russia)

      expect.assertions(1)
      bindingService.bindGooglePayToken('token', 'orderTag').then((res) => {
        expect(res).toStrictEqual(new PayBindingInfo('google_pay_method', 'trust_payment_id'))
        done()
      })
    })

    it('should get error on binding failed', (done) => {
      const error = new YSError('error')
      const diehardApi = createMockInstance(DiehardBackendApi, {
        bindGooglePayToken: jest.fn().mockReturnValue(reject(error)),
      })
      const bindingService = new PayBindingService(payer, merchant, diehardApi, RegionIds.russia)

      expect.assertions(1)
      bindingService.bindGooglePayToken('token', 'orderTag').failed((res) => {
        expect(res).toBe(error)
        done()
      })
    })
  })
})
