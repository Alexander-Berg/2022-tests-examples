import { UrlRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { BankLogosRequest } from '../../../../code/network/yandex-pay-backend/bank-logos-request'

describe(BankLogosRequest, () => {
  it('should represent "bank_logos" request', () => {
    const request = new BankLogosRequest()
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.targetPath()).toBe('web-api/mobile/v2/bank_logos')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params().asMap().size).toBe(0)
  })
})
