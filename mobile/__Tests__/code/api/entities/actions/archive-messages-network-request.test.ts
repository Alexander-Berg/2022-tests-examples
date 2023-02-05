import { JsonRequestEncoding, NetworkMethod } from '../../../../../../common/code/network/network-request'
import { int64 } from '../../../../../../../common/ys'
import { ArchiveMessagesNetworkRequest } from '../../../../../code/api/entities/actions/archive-messages-network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(ArchiveMessagesNetworkRequest, () => {
  it('should build Archive Messages request', () => {
    const request = new ArchiveMessagesNetworkRequest([int64(1), int64(2)], [int64(3)], 'Archive')
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
        tids: '3',
        local: 'Archive',
      }),
    )
    expect(request.path()).toBe('archive')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
