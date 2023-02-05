import { int64 } from '../../../../../../../common/ys'
import { MarkSpamNetworkRequest } from '../../../../../code/api/entities/actions/mark-spam-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(MarkSpamNetworkRequest, () => {
  it('should build Mark as Spam request', () => {
    const request = new MarkSpamNetworkRequest([int64(9999), int64(8888)], [int64(7777)], int64(18), true)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '9999,8888',
        tids: '7777',
        current_folder: '18',
      }),
    )
    expect(request.path()).toBe('foo')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
  it('should build Unmark as Spam request', () => {
    const request = new MarkSpamNetworkRequest([int64(7777), int64(6666)], [], int64(13), false)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '7777,6666',
        current_folder: '13',
      }),
    )
    expect(request.path()).toBe('antifoo')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
