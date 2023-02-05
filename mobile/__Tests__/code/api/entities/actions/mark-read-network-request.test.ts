import { int64 } from '../../../../../../../common/ys'
import { MarkReadNetworkRequest } from '../../../../../code/api/entities/actions/mark-read-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(MarkReadNetworkRequest, () => {
  it('should build Mark as Read request', () => {
    const request = new MarkReadNetworkRequest([int64(1), int64(2)], [int64(3)], true)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
        tids: '3',
      }),
    )
    expect(request.path()).toBe('mark_read')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
  it('should build Mark as Unread request', () => {
    const request = new MarkReadNetworkRequest([int64(1), int64(2)], [], false)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
      }),
    )
    expect(request.path()).toBe('mark_unread')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
