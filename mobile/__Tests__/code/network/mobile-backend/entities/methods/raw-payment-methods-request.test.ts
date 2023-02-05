import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { RawPaymentMethodsRequest } from '../../../../../../code/network/mobile-backend/entities/methods/raw-payment-methods-request'

describe(RawPaymentMethodsRequest, () => {
  it('should build RawPaymentMethodsRequest request', () => {
    const request = new RawPaymentMethodsRequest()
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params().asMap().size).toBe(0)
    expect(request.targetPath()).toBe('payment_methods')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
