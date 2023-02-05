import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { SupplySbpPaymentRequest } from '../../../../../../code/network/diehard-backend/entities/supply/supply-sbp-payment-request'

describe(SupplySbpPaymentRequest, () => {
  it('should build SupplySbpPaymentRequest request', () => {
    const request = new SupplySbpPaymentRequest('token', 'purchaseToken', 'email@ya.ru')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          purchase_token: 'purchaseToken',
          email: 'email@ya.ru',
          payment_method: 'sbp_qr',
          cvn: '',
        },
      }),
    )
    expect(request.targetPath()).toBe('supply_payment_data')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
