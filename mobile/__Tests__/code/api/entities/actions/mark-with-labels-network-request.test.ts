import { int64 } from '../../../../../../../common/ys'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/../../common/__tests__/__helpers__/json-helpers'
import { MarkWithLabelsNetworkRequest } from '../../../../../code/api/entities/actions/mark-with-labels-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(MarkWithLabelsNetworkRequest, () => {
  it('should build Mark With Labels request', () => {
    const request = new MarkWithLabelsNetworkRequest([int64(1), int64(2)], [int64(3)], ['lid1', 'lid2'], true)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
        tids: '3',
        lid: 'lid1,lid2',
        mark: '1',
      }),
    )
    expect(request.path()).toBe('mark_with_label')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
  it('should build Unmark With Labels request', () => {
    const request = new MarkWithLabelsNetworkRequest([int64(1), int64(2)], [], ['lid1', 'lid2'], false)
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
        lid: 'lid1,lid2',
        mark: '0',
      }),
    )
    expect(request.path()).toBe('mark_with_label')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
