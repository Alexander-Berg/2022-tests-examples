import { int64 } from '../../../../../../../common/ys'
import { ArrayJSONItem, MapJSONItem } from '../../../../../../common/code/json/json-types'
import { MessageRequestItem } from '../../../../../code/api/entities/message/message-request-item'
import { MessagesRequestPack } from '../../../../../code/api/entities/message/messages-request-pack'
import { NetworkMethod, RequestEncodingKind } from '../../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { SearchRequest } from '../../../../../code/api/entities/search/search-request'
import { NetworkAPIVersions } from '../../../../../code/api/mail-network-request'

function checkCommonAttributes(pack: MessagesRequestPack): void {
  expect(pack.method()).toBe(NetworkMethod.post)
  expect(pack.version()).toBe(NetworkAPIVersions.v1)
  expect(pack.path()).toBe('messages')
  expect(pack.encoding().kind).toBe(RequestEncodingKind.json)
  expect(pack.headersExtra()).toStrictEqual(new MapJSONItem())
}

describe(MessagesRequestPack, () => {
  it('should be creatible from an array of message requests (empty)', () => {
    const pack = new MessagesRequestPack([], false)
    checkCommonAttributes(pack)
    expect(pack.urlExtra()).toStrictEqual(new MapJSONItem())
    expect(pack.params()).toStrictEqual(
      JSONItemFromJSON({
        requests: [],
      }),
    )
  })
  it('should be creatible from an array of message requests (nonempty)', () => {
    const requests = [
      MessageRequestItem.messagesInFolder(int64(101), 0, 10),
      MessageRequestItem.messagesInThread(int64(401), 10, 20),
      MessageRequestItem.messagesWithLabel('LID1', 20, 30),
      MessageRequestItem.threads(int64(102), 30, 40),
    ]
    const pack = new MessagesRequestPack(requests, true)
    checkCommonAttributes(pack)
    expect(pack.params()).toStrictEqual(
      new MapJSONItem().put(
        'requests',
        new ArrayJSONItem([
          new MapJSONItem(requests[0].params().asMap()),
          new MapJSONItem(requests[1].params().asMap()),
          new MapJSONItem(requests[2].params().asMap()),
          new MapJSONItem(requests[3].params().asMap()),
        ]),
      ),
    )
    expect(pack.urlExtra()).toStrictEqual(
      JSONItemFromJSON({
        [SearchRequest.REQUEST_DISK_ATTACHES_QUERY_PARAM]: 1,
      }),
    )
  })
})
