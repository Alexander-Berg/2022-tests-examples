import { UrlRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { UserProfileRequest } from '../../../../code/network/yandex-pay-backend/user-profile-request'

describe(UserProfileRequest, () => {
  it('should represent "user_info" request', () => {
    const request = new UserProfileRequest()
    expect(request.encoding()).toBeInstanceOf(UrlRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.targetPath()).toBe('web-api/mobile/v1/user_info')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params().asMap().size).toBe(0)
  })
})
