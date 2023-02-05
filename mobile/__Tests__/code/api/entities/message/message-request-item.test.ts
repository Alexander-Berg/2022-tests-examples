import { int64 } from '../../../../../../../common/ys'
import { idToString } from '../../../../../code/api/common/id'
import { MessageRequestItem } from '../../../../../code/api/entities/message/message-request-item'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'

describe(MessageRequestItem, () => {
  it('should create request for threads', () => {
    const result = MessageRequestItem.threads(int64(1), 5, 15)
    expect(result.params()).toStrictEqual(
      JSONItemFromJSON({
        fid: idToString(int64(1)),
        first: 5,
        last: 15,
        threaded: true,
        md5: '',
        returnIfModified: true,
      }),
    )
  })
  it('should create request for messages in threads', () => {
    const result = MessageRequestItem.messagesInThread(int64(4), 5, 15)
    expect(result.params()).toStrictEqual(
      JSONItemFromJSON({
        tid: idToString(int64(4)),
        first: 5,
        last: 15,
        threaded: false,
        md5: '',
        returnIfModified: true,
      }),
    )
  })
  it('should create request for messages in folder', () => {
    const result = MessageRequestItem.messagesInFolder(int64(1), 5, 15)
    expect(result.params()).toStrictEqual(
      JSONItemFromJSON({
        fid: idToString(int64(1)),
        first: 5,
        last: 15,
        threaded: false,
        md5: '',
        returnIfModified: true,
      }),
    )
  })
  it('should create request for messages with label', () => {
    const result = MessageRequestItem.messagesWithLabel('LID', 5, 15)
    expect(result.params()).toStrictEqual(
      JSONItemFromJSON({
        lid: 'LID',
        first: 5,
        last: 15,
        threaded: false,
        md5: '',
        returnIfModified: true,
      }),
    )
  })
})
