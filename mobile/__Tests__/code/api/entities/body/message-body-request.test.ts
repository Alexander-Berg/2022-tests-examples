import { int64 } from '../../../../../../../common/ys'
import { MapJSONItem } from '../../../../../../common/code/json/json-types'
import { MessageBodyRequest } from '../../../../../code/api/entities/body/message-body-request'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

describe(MessageBodyRequest, () => {
  it('should build message body request', () => {
    const request = new MessageBodyRequest([int64(1), int64(2), int64(3)])
    expect(request.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(request.headersExtra()).toStrictEqual(new MapJSONItem())
    expect(request.version()).toBe(NetworkAPIVersions.v1)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.path()).toBe('message_body')
    expect(request.encoding().kind).toBe(RequestEncodingKind.json)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        novdirect: true,
        mids: '1,2,3',
      }),
    )
  })
})
