import { IntegerJSONItem } from '../../../../../../common/code/json/json-types'
import {
  messageResponseHeaderFromJSONItem,
  MessagesResponseHeader,
} from '../../../../../code/api/entities/message/message-response-header'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { threadMetaFromJSONItem } from '../../../../../code/api/entities/thread/thread-meta'
import {
  ThreadResponse,
  threadResponseFromJSONItem,
  ThreadResponsePayload,
} from '../../../../../code/api/entities/thread/threads-response'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../../../common/__tests__/__helpers__/utils'
import sample from './sample.json'

describe(ThreadResponse, () => {
  describe(threadResponseFromJSONItem, () => {
    it('should return null if JSON Item is malformed', () => {
      expect(threadResponseFromJSONItem(IntegerJSONItem.fromInt32(10))).toBeNull()
      expect(threadResponseFromJSONItem(JSONItemFromJSON({}))).toBeNull()
    })
    it('should return error if JSON Item is one of an error', () => {
      const response = threadResponseFromJSONItem(
        JSONItemFromJSON({
          status: {
            status: 2,
            phrase: 'PHRASE',
            trace: 'TRACE',
          },
        }),
      )
      expect(response).not.toBeNull()
      expect(response!.networkStatus()).toStrictEqual(
        new NetworkStatus(NetworkStatusCode.temporaryError, 'TRACE', 'PHRASE'),
      )
      expect(response!.payload).toBeNull()
    })
    it('should parse JSON Item is not error', () => {
      const response = threadResponseFromJSONItem(JSONItemFromJSON(sample))
      expect(response).not.toBeNull()
      expect(response!.status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      expect(response!.payload).toStrictEqual(
        sample.map(
          (item) =>
            new ThreadResponsePayload(
              messageResponseHeaderFromJSONItem(JSONItemFromJSON(item.header))!,
              (item.messageBatch.messages as readonly any[]).map(
                (msg) => threadMetaFromJSONItem(JSONItemFromJSON(msg))!,
              ),
            ),
        ),
      )
    })
    it('should return empty threads payload if batch is empty', () => {
      const emptyBatch = clone(sample[0])
      emptyBatch.messageBatch = {}
      const result = threadResponseFromJSONItem(JSONItemFromJSON([emptyBatch]))
      expect(result).toStrictEqual(
        new ThreadResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new ThreadResponsePayload(messageResponseHeaderFromJSONItem(JSONItemFromJSON(emptyBatch.header))!, []),
        ]),
      )
    })
    it('should return empty threads payload if batch is missing', () => {
      const emptyBatch = clone(sample[0])
      delete emptyBatch.messageBatch
      const result = threadResponseFromJSONItem(JSONItemFromJSON([emptyBatch]))
      expect(result).toStrictEqual(
        new ThreadResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new ThreadResponsePayload(messageResponseHeaderFromJSONItem(JSONItemFromJSON(emptyBatch.header))!, []),
        ]),
      )
    })
    it('should return error if header is malformed', () => {
      const badHeader = clone(sample[0])
      badHeader.header = []
      const result = threadResponseFromJSONItem(JSONItemFromJSON([badHeader]))
      expect(result).toStrictEqual(
        new ThreadResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new ThreadResponsePayload(
            MessagesResponseHeader.withError(-1),
            (badHeader.messageBatch.messages as readonly any[]).map(
              (msg) => threadMetaFromJSONItem(JSONItemFromJSON(msg))!,
            ),
          ),
        ]),
      )
    })
    it('should skip message item if thread item is malformed', () => {
      const badMessageMeta = clone(sample[0])
      badMessageMeta.messageBatch.messages = [10]
      const result = threadResponseFromJSONItem(JSONItemFromJSON([badMessageMeta]))
      expect(result).toStrictEqual(
        new ThreadResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new ThreadResponsePayload(messageResponseHeaderFromJSONItem(JSONItemFromJSON(badMessageMeta.header))!, []),
        ]),
      )
    })
  })
})
