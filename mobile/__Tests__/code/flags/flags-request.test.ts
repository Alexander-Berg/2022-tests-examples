import { PlatformType } from '../../../../common/code/network/platform'
import { MapJSONItem } from '../../../../common/code/json/json-types'
import { NetworkMethod, RequestEncodingKind } from '../../../../common/code/network/network-request'
import { FlagsRequest } from '../../../code/api/flags-request'

describe(FlagsRequest, () => {
  it('should represent flags request', () => {
    const request = new FlagsRequest('MOBMAIL', '1', {
      type: PlatformType.android,
      isTablet: false,
    })
    expect(request.encoding().kind).toBe(RequestEncodingKind.json)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('v2/flags')
    expect(request.urlExtra()).toStrictEqual(
      new MapJSONItem().putString('handler', 'MOBMAIL').putString('uuid', '1').putString('client', 'aphone'),
    )
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.params()).toStrictEqual(new MapJSONItem().putInt32('version', 1))
  })
})
