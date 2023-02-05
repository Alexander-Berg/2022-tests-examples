import { reject, resolve } from '../../../../../common/xpromise-support'
import { YSError } from '../../../../../common/ys'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { BankName } from '../../../code/busilogics/bank-name'
import { AvailableMethods } from '../../../code/models/available-methods'
import { PaymentMethod } from '../../../code/network/mobile-backend/entities/methods/payment-method'
import { RawPaymentMethodsResponse } from '../../../code/network/mobile-backend/entities/methods/raw-payment-methods-response'
import { MobileBackendApi } from '../../../code/network/mobile-backend/mobile-backend-api'
import { RawPaymentMethodsProvider } from '../../../code/busilogics/raw-payment-methods-provider'

describe(RawPaymentMethodsProvider, () => {
  it('should get payment methods on success', (done) => {
    const methods = [
      new PaymentMethod('123', 'test', 'visa', false, BankName.UnknownBank, null, null),
      new PaymentMethod('321', 'test', 'MasterCard', true, BankName.UnknownBank, null, null),
    ]
    const response = new RawPaymentMethodsResponse('success', true, true, methods, [])
    const mobileApi = createMockInstance(MobileBackendApi, {
      rawPaymentMethods: jest.fn().mockReturnValue(resolve(response)),
    })
    const provider = new RawPaymentMethodsProvider(mobileApi)

    expect.assertions(1)
    provider.paymentMethods().then((res) => {
      expect(res).toStrictEqual(new AvailableMethods(methods, true, true, false, false))
      done()
    })
  })

  it('should get error on request failed', (done) => {
    const error = new YSError('error')
    const mobileApi = createMockInstance(MobileBackendApi, {
      rawPaymentMethods: jest.fn().mockReturnValue(reject(error)),
    })
    const provider = new RawPaymentMethodsProvider(mobileApi)

    expect.assertions(1)
    provider.paymentMethods().failed((res) => {
      expect(res).toStrictEqual(error)
      done()
    })
  })
})
