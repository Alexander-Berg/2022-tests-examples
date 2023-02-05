import { NetworkMethod, UrlRequestEncoding } from '../../../../../common/code/network/network-request'
import { GetAllowedBinsRequest } from '../../../../code/network/yandex-pay-backend/get-allowed-bins-request'

describe(GetAllowedBinsRequest, () => {
  it('should represent "bins/allowed" request', () => {
    const request = new GetAllowedBinsRequest()
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.targetPath()).toBe('api/mobile/v1/bins/allowed')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params().asMap().size).toBe(0)
  })
})
