import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { SearchRequest } from '../../../../../code/api/entities/search/search-request'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(SearchRequest, () => {
  it('should create "only_new" request', () => {
    const request = SearchRequest.loadOnlyNew(1, 2)
    expect(request.encoding().kind).toBe(RequestEncodingKind.json)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.path()).toBe('only_new')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem().putInt32('request_disk_attaches', 1))
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        first: 1,
        last: 2,
      }),
    )
  })

  it('should create "with_attachments" request', () => {
    const request = SearchRequest.loadWithAttachments(1, 2)
    expect(request.encoding().kind).toBe(RequestEncodingKind.json)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.path()).toBe('with_attachments')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem().putInt32('request_disk_attaches', 1))
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        first: 1,
        last: 2,
      }),
    )
  })
})
