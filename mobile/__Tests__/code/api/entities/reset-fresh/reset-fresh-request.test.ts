import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { ResetFreshRequest } from '../../../../../code/api/entities/reset-fresh/reset-fresh-request'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(ResetFreshRequest, () => {
  it('should create "reset_fresh" request', () => {
    const request = new ResetFreshRequest()
    expect(request.encoding().kind).toBe(RequestEncodingKind.json)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.path()).toBe('reset_fresh')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(new MapJSONItem())
  })
})
