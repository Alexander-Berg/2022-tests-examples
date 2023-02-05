import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { CheckPaymentRequest } from '../../../../../../code/network/diehard-backend/entities/check-payment/check-payment-request'

describe(CheckPaymentRequest, () => {
  it('should build CheckPaymentRequest request', () => {
    const request = new CheckPaymentRequest('token')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          purchase_token: 'token',
        },
      }),
    )
    expect(request.targetPath()).toBe('check_payment')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
