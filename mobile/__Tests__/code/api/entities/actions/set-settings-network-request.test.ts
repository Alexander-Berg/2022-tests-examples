import { SetSettingsNetworkRequest } from '../../../../../code/api/entities/actions/set-settings-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(SetSettingsNetworkRequest, () => {
  it('should build Mark as Spam request', () => {
    const request = new SetSettingsNetworkRequest('Regards \n Sent from XMail')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mobile_sign: 'Regards \n Sent from XMail',
      }),
    )
    expect(request.path()).toBe('set_settings')
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
    expect(request.urlExtra().asMap().size).toBe(0)
  })
})
