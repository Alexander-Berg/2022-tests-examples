import { int64 } from '../../../../../../../common/ys'
import { ClearFolderNetworkRequest } from '../../../../../code/api/entities/actions/clear-forlder-network-request'
import { NetworkMethod, JsonRequestEncoding } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(ClearFolderNetworkRequest, () => {
  it('should build Clear Folder request', () => {
    const request = new ClearFolderNetworkRequest(int64(987654321))
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        fid: '987654321',
      }),
    )
    expect(request.path()).toBe('clear_folder')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.version()).toStrictEqual(NetworkAPIVersions.v1)
  })
})
