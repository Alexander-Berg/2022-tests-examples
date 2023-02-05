import { int64 } from '../../../../../../../common/ys'
import { MoveToFolderNetworkRequest } from '../../../../../code/api/entities/actions/move-to-folder-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(MoveToFolderNetworkRequest, () => {
  it('should build Move to Folder request', () => {
    const request = new MoveToFolderNetworkRequest([int64(1), int64(2)], [int64(5)], int64(3), int64(4))
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        mids: '1,2',
        tids: '5',
        fid: '3',
        current_folder: '4',
      }),
    )
    expect(request.path()).toBe('move_to_folder')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
