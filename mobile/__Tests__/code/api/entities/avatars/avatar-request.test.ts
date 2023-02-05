import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { AvatarRequest } from '../../../../../code/api/entities/avatars/avatar-request'
import { EmailWithName } from '../../../../../code/api/entities/recipient/email'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(AvatarRequest, () => {
  it('should represent ava2 request', () => {
    const request = new AvatarRequest([
      EmailWithName.fromNameAndEmail('Sample Yandex', 'sample@yandex.ru'),
      EmailWithName.fromNameAndEmail(null, ''),
    ])
    expect(request.encoding().kind).toBe(RequestEncodingKind.json)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.path()).toBe('ava2')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        request: ['"Sample Yandex" <sample@yandex.ru>', ''],
      }),
    )
  })
})
