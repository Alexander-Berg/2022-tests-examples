import { int64 } from '../../../../../../../common/ys'
import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { DeltaApiRequest } from '../../../../../code/api/entities/delta-api/delta-api-request'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(DeltaApiRequest, () => {
  it('should generate proper request parts', () => {
    const request = new DeltaApiRequest(12345, 123, int64(1130000000639027))
    expect(request.encoding().kind).toBe(RequestEncodingKind.url)
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.version()).toBe(NetworkAPIVersions.v2)
    expect(request.path()).toBe('changes')
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        revision: 12345,
        max_count: 123,
        uid: 1130000000639027,
        new: true,
      }),
    )
  })
})
