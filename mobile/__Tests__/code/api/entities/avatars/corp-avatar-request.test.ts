import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { CorpAvatarRequest } from '../../../../../code/api/entities/avatars/corp-avatar-request'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(CorpAvatarRequest, () => {
  it('should represent corp_ava request', () => {
    const request = new CorpAvatarRequest('sample@yandex-team.ru')
    expect(request.encoding().kind).toBe(RequestEncodingKind.url)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.path()).toBe('corp_ava')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        login: 'sample@yandex-team.ru',
      }),
    )
  })
})
