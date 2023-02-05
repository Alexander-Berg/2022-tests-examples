import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'
import {
  messageResponseHeaderFromJSONItem,
  MessageResponseHeaderPayload,
  MessagesResponseHeader,
} from '../../../../../code/api/entities/message/message-response-header'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import sample from './sample.json'

describe(MessagesResponseHeader, () => {
  describe(messageResponseHeaderFromJSONItem, () => {
    it('should return null if JSON Item is malformed', () => {
      expect(messageResponseHeaderFromJSONItem(new ArrayJSONItem())).toBeNull()
    })
    it('should return empty values if error flag set in response', () => {
      const example = { ...sample[0].header, error: 2 }
      expect(messageResponseHeaderFromJSONItem(JSONItemFromJSON(example))).toStrictEqual(
        MessagesResponseHeader.withError(2),
      )
    })
    it('should return erroneous values if error flag set in response', () => {
      const example = { ...sample[0].header, error: 2 }
      expect(messageResponseHeaderFromJSONItem(JSONItemFromJSON(example))).toStrictEqual(
        MessagesResponseHeader.withError(2),
      )
    })
    it('should be constructible from JSON Item', () => {
      expect(messageResponseHeaderFromJSONItem(JSONItemFromJSON(sample[0].header))).toStrictEqual(
        MessagesResponseHeader.withPayload('8e5f82976fca0b0aff13d584acbafc39', 10, 5, true, 1),
      )
    })
  })
  it('should construct erroneous value', () => {
    const value = MessagesResponseHeader.withError(2)
    expect(value).toBeInstanceOf(MessagesResponseHeader)
    expect(value.error).toBe(2)
    expect(value.payload).toBeNull()
  })
  it('should construct value with payload', () => {
    const value = MessagesResponseHeader.withPayload('MD5', 100, 10, true, 5)
    expect(value).toBeInstanceOf(MessagesResponseHeader)
    expect(value.error).toBe(1)
    expect(value.payload).not.toBeNull()
    expect(value.payload!).toStrictEqual(new MessageResponseHeaderPayload('MD5', 100, 10, true, 5))
  })
})
