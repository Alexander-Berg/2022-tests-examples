import { InitializationParams } from '../../../../../../code/network/mobile-backend/entities/init/initialization-params'
import { InitPaymentRequest } from '../../../../../../code/network/mobile-backend/entities/init/init-payment-request'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'

describe(InitPaymentRequest, () => {
  it('should build InitPaymentRequest request with initialization params', () => {
    const params = new InitializationParams('psuid', 'tsid', 'appid')
    const request = new InitPaymentRequest('token', 'email', true, params)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        token: 'token',
        email: 'email',
        psuid: 'psuid',
        tsid: 'tsid',
        turboapp_id: 'appid',
        credit: true,
      }),
    )
    expect(request.targetPath()).toBe('init_payment')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
  it('should build InitPaymentRequest request without initialization params', () => {
    const request = new InitPaymentRequest('token', 'email', false, InitializationParams.DEFAULT)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        token: 'token',
        email: 'email',
        credit: false,
      }),
    )
    expect(request.targetPath()).toBe('init_payment')
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
