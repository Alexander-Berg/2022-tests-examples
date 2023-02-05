import { NetworkMethod, UrlRequestEncoding } from '../../../../../common/code/network/network-request'
import { IsAuthorizedRequest } from '../../../../code/network/yandex-pay-backend/is-authorized-request'

describe(IsAuthorizedRequest, () => {
  it('should represent "app/beta/allowed" request', () => {
    const request = new IsAuthorizedRequest()
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.targetPath()).toBe('api/mobile/v1/wallet/app/beta/allowed')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params().asMap().size).toBe(0)
  })
})
