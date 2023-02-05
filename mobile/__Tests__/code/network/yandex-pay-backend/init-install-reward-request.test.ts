import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { InitInstallRewardRequest } from '../../../../code/network/yandex-pay-backend/init-install-reward-request'

describe(InitInstallRewardRequest, () => {
  it('should represent "rewards/init-install-reward" request', () => {
    const request = new InitInstallRewardRequest('device-id-1')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/nfc/v1/rewards/init-install-reward')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        device_id: 'device-id-1',
      }),
    )
  })
})
