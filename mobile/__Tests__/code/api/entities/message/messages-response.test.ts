import { IntegerJSONItem } from '../../../../../../common/code/json/json-types'
import { messageMetaFromJSONItem } from '../../../../../code/api/entities/message/message-meta'
import {
  messageResponseHeaderFromJSONItem,
  MessagesResponseHeader,
} from '../../../../../code/api/entities/message/message-response-header'
import {
  MessageResponse,
  messageResponseFromJSONItem,
  MessageResponsePayload,
} from '../../../../../code/api/entities/message/messages-response'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../../../common/__tests__/__helpers__/utils'
import sample from './sample.json'

describe(MessageResponse, () => {
  describe(messageResponseFromJSONItem, () => {
    it('should return null if JSON Item is malformed', () => {
      expect(messageResponseFromJSONItem(IntegerJSONItem.fromInt32(10))).toBeNull()
      expect(messageResponseFromJSONItem(JSONItemFromJSON({}))).toBeNull()
    })
    it('should return error if JSON Item is one of an error', () => {
      const messageResponse = messageResponseFromJSONItem(
        JSONItemFromJSON({
          status: {
            status: 2,
            phrase: 'PHRASE',
            trace: 'TRACE',
          },
        }),
      )
      expect(messageResponse).not.toBeNull()
      expect(messageResponse!.networkStatus()).toStrictEqual(
        new NetworkStatus(NetworkStatusCode.temporaryError, 'TRACE', 'PHRASE'),
      )
      expect(messageResponse!.payload).toBeNull()
    })
    it('should parse JSON Item is not error', () => {
      const result = messageResponseFromJSONItem(JSONItemFromJSON(sample))
      expect(result!.networkStatus()).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      expect(result!.payload).toStrictEqual(
        sample.map(
          (item) =>
            new MessageResponsePayload(
              messageResponseHeaderFromJSONItem(JSONItemFromJSON(item.header))!,
              (item.messageBatch.messages as readonly any[]).map(
                (msg) => messageMetaFromJSONItem(JSONItemFromJSON(msg))!,
              ),
            ),
        ),
      )
    })
    it('should return empty messages payload if batch is empty', () => {
      const emptyBatch = clone(sample[0])
      emptyBatch.messageBatch = {}
      const result = messageResponseFromJSONItem(JSONItemFromJSON([emptyBatch]))
      expect(result).toStrictEqual(
        new MessageResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new MessageResponsePayload(messageResponseHeaderFromJSONItem(JSONItemFromJSON(emptyBatch.header))!, []),
        ]),
      )
    })
    it('should return error if header is malformed', () => {
      const badHeader = clone(sample[0])
      badHeader.header = []
      const result = messageResponseFromJSONItem(JSONItemFromJSON([badHeader]))
      expect(result).toStrictEqual(
        new MessageResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new MessageResponsePayload(
            MessagesResponseHeader.withError(-1),
            (badHeader.messageBatch.messages as readonly any[]).map(
              (msg) => messageMetaFromJSONItem(JSONItemFromJSON(msg))!,
            ),
          ),
        ]),
      )
    })
    it('should skip message item if message item is malformed', () => {
      const badMessageMeta = clone(sample[0])
      badMessageMeta.messageBatch.messages = [10]
      const result = messageResponseFromJSONItem(JSONItemFromJSON([badMessageMeta]))
      expect(result).toStrictEqual(
        new MessageResponse(new NetworkStatus(NetworkStatusCode.ok), [
          new MessageResponsePayload(messageResponseHeaderFromJSONItem(JSONItemFromJSON(badMessageMeta.header))!, []),
        ]),
      )
    })
  })
})
