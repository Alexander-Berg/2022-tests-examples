import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { ContainersRequest } from '../../../../../code/api/entities/container/containers-request'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(ContainersRequest, () => {
  it('should provide request attributes', () => {
    const request = new ContainersRequest()
    expect(request.method()).toBe(NetworkMethod.get)
    expect(request.params().asMap().size).toBe(0)
    expect(request.path()).toBe('xlist')
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.encoding().kind).toBe(RequestEncodingKind.url)
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
  })
})
