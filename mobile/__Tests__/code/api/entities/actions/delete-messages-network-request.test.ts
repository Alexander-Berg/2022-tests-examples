import { int64 } from '../../../../../../../common/ys'
import { DeleteMessagesNetworkRequest } from '../../../../../code/api/entities/actions/delete-messages-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(DeleteMessagesNetworkRequest, () => {
  it('should build Delete Messages request', () => {
    const request = new DeleteMessagesNetworkRequest([int64(1), int64(2)], [int64(4)], int64(3))
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
        tids: '4',
        current_folder: '3',
      }),
    )
    expect(request.path()).toBe('delete_items')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
