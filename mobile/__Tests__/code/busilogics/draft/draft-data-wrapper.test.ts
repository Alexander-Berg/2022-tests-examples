import { int64 } from '../../../../../../common/ys'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { idToString } from '../../../../../mapi/code/api/common/id'
import {
  DraftDataWrapper,
  int32ToReplyType,
  ReplyType,
  replyTypeToInt32,
} from '../../../../code/busilogics/draft/draft-data-wrapper'

describe(replyTypeToInt32, () => {
  it('should convert ReplyType Enum item to Int32', () => {
    expect(replyTypeToInt32(ReplyType.NONE)).toBe(0)
    expect(replyTypeToInt32(ReplyType.REPLY)).toBe(1)
    expect(replyTypeToInt32(ReplyType.FORWARD)).toBe(2)
    expect(replyTypeToInt32(ReplyType.TEMPLATE)).toBe(3)
  })
})
describe(int32ToReplyType, () => {
  it('should convert Int32 to ReplyType Enum item', () => {
    expect(int32ToReplyType(0)).toBe(ReplyType.NONE)
    expect(int32ToReplyType(1)).toBe(ReplyType.REPLY)
    expect(int32ToReplyType(2)).toBe(ReplyType.FORWARD)
    expect(int32ToReplyType(3)).toBe(ReplyType.TEMPLATE)
    expect(int32ToReplyType(4)).toBe(ReplyType.NONE)
  })
})
describe(DraftDataWrapper, () => {
  it('should be creatable with all non-null input params', () => {
    const draftDataWrapper = new DraftDataWrapper(
      int64(111),
      int64(222),
      'some_action',
      'me@yandex.ru',
      'you@yandex.ru',
      'him@yandex.ru',
      'hidden@yandex.ru',
      'subject',
      'body',
      'someRfcId',
      'someRefs',
      ReplyType.FORWARD,
      int64(333),
      int64(444),
    )
    expect(draftDataWrapper.accountId).toBe(int64(111))
    expect(draftDataWrapper.draftId).toBe(int64(222))
    expect(draftDataWrapper.action).toBe('some_action')
    expect(draftDataWrapper.from).toBe('me@yandex.ru')
    expect(draftDataWrapper.to).toBe('you@yandex.ru')
    expect(draftDataWrapper.cc).toBe('him@yandex.ru')
    expect(draftDataWrapper.bcc).toBe('hidden@yandex.ru')
    expect(draftDataWrapper.subject).toBe('subject')
    expect(draftDataWrapper.body).toBe('body')
    expect(draftDataWrapper.rfcId).toBe('someRfcId')
    expect(draftDataWrapper.references).toBe('someRefs')
    expect(draftDataWrapper.replyType).toBe(ReplyType.FORWARD)
    expect(draftDataWrapper.replyMid).toBe(int64(333))
    expect(draftDataWrapper.baseMessageId).toBe(int64(444))
  })
  it('should be creatable with all nullable params set to null', () => {
    const draftDataWrapper = new DraftDataWrapper(
      int64(111),
      int64(222),
      'some_action',
      'me@yandex.ru',
      'you@yandex.ru',
      null,
      null,
      'subject',
      'body',
      null,
      null,
      ReplyType.FORWARD,
      int64(333),
      int64(444),
    )
    expect(draftDataWrapper.accountId).toBe(int64(111))
    expect(draftDataWrapper.draftId).toBe(int64(222))
    expect(draftDataWrapper.action).toBe('some_action')
    expect(draftDataWrapper.from).toBe('me@yandex.ru')
    expect(draftDataWrapper.to).toBe('you@yandex.ru')
    expect(draftDataWrapper.cc).toBeNull()
    expect(draftDataWrapper.bcc).toBeNull()
    expect(draftDataWrapper.subject).toBe('subject')
    expect(draftDataWrapper.body).toBe('body')
    expect(draftDataWrapper.rfcId).toBeNull()
    expect(draftDataWrapper.references).toBeNull()
    expect(draftDataWrapper.replyType).toBe(ReplyType.FORWARD)
    expect(draftDataWrapper.replyMid).toBe(int64(333))
    expect(draftDataWrapper.baseMessageId).toBe(int64(444))
  })
  it('should be creatable from JSONItem with all non null fields', () => {
    const mapJSONItem = new MapJSONItem()
      .putString('accountId', idToString(int64(111))!)
      .putString('draftId', idToString(int64(222))!)
      .putString('action', 'some_action')
      .putString('from', 'me@yandex.ru')
      .putString('to', 'you@yandex.ru')
      .putString('cc', 'him@yandex.ru')
      .putString('bcc', 'hidden@yandex.ru')
      .putString('subject', 'subject')
      .putString('body', 'body')
      .putString('rfcId', 'someRfcId')
      .putString('references', 'someRefs')
      .putInt32('replyType', 2)
      .putString('replyMid', idToString(int64(333))!)
      .putString('baseMessageId', idToString(int64(444))!)
    const draftDataWrapper = DraftDataWrapper.fromJSONItem(mapJSONItem)
    expect(draftDataWrapper).not.toBeNull()
    expect(draftDataWrapper!.accountId).toBe(int64(111))
    expect(draftDataWrapper!.draftId).toBe(int64(222))
    expect(draftDataWrapper!.action).toBe('some_action')
    expect(draftDataWrapper!.from).toBe('me@yandex.ru')
    expect(draftDataWrapper!.to).toBe('you@yandex.ru')
    expect(draftDataWrapper!.cc).toBe('him@yandex.ru')
    expect(draftDataWrapper!.bcc).toBe('hidden@yandex.ru')
    expect(draftDataWrapper!.subject).toBe('subject')
    expect(draftDataWrapper!.body).toBe('body')
    expect(draftDataWrapper!.rfcId).toBe('someRfcId')
    expect(draftDataWrapper!.references).toBe('someRefs')
    expect(draftDataWrapper!.replyType).toBe(ReplyType.FORWARD)
    expect(draftDataWrapper!.replyMid).toBe(int64(333))
    expect(draftDataWrapper!.baseMessageId).toBe(int64(444))
  })
  it('should be creatable from JSONItem with all nullable fields set to null', () => {
    const mapJSONItem = new MapJSONItem()
      .putString('accountId', idToString(int64(111))!)
      .putString('draftId', idToString(int64(222))!)
      .putString('action', 'some_action')
      .putString('from', 'me@yandex.ru')
      .putString('to', 'you@yandex.ru')
      .putString('subject', 'subject')
      .putString('body', 'body')
      .putInt32('replyType', 2)
      .putString('replyMid', idToString(int64(333))!)
      .putString('baseMessageId', idToString(int64(444))!)
    const draftDataWrapper = DraftDataWrapper.fromJSONItem(mapJSONItem)
    expect(draftDataWrapper).not.toBeNull()
    expect(draftDataWrapper!.accountId).toBe(int64(111))
    expect(draftDataWrapper!.draftId).toBe(int64(222))
    expect(draftDataWrapper!.action).toBe('some_action')
    expect(draftDataWrapper!.from).toBe('me@yandex.ru')
    expect(draftDataWrapper!.to).toBe('you@yandex.ru')
    expect(draftDataWrapper!.cc).toBeNull()
    expect(draftDataWrapper!.bcc).toBeNull()
    expect(draftDataWrapper!.subject).toBe('subject')
    expect(draftDataWrapper!.body).toBe('body')
    expect(draftDataWrapper!.rfcId).toBeNull()
    expect(draftDataWrapper!.references).toBeNull()
    expect(draftDataWrapper!.replyType).toBe(ReplyType.FORWARD)
    expect(draftDataWrapper!.replyMid).toBe(int64(333))
    expect(draftDataWrapper!.baseMessageId).toBe(int64(444))
  })
  it('should not be creatable from JSONItem if one of required fields is null', () => {
    const mapJSONItem = new MapJSONItem()
      .putString('accountId', idToString(int64(111))!)
      .putString('draftId', idToString(int64(222))!)
      .putString('from', 'me@yandex.ru')
      .putString('to', 'you@yandex.ru')
      .putString('subject', 'subject')
      .putString('body', 'body')
      .putInt32('replyType', 2)
      .putString('replyMid', idToString(int64(333))!)
      .putString('baseMessageId', idToString(int64(444))!)
    const draftDataWrapper = DraftDataWrapper.fromJSONItem(mapJSONItem)
    expect(draftDataWrapper).toBeNull()
  })
  it('should be representable as JSONItem with all non-null fields', () => {
    const draftDataWrapper = new DraftDataWrapper(
      int64(111),
      int64(222),
      'some_action',
      'me@yandex.ru',
      'you@yandex.ru',
      'him@yandex.ru',
      'hidden@yandex.ru',
      'subject',
      'body',
      'someRfcId',
      'someRefs',
      ReplyType.FORWARD,
      int64(333),
      int64(444),
    )
    const asJSONItem = draftDataWrapper.asJSONItem()
    expect(asJSONItem).toStrictEqual(
      new MapJSONItem()
        .putString('accountId', idToString(int64(111))!)
        .putString('draftId', idToString(int64(222))!)
        .putString('action', 'some_action')
        .putString('from', 'me@yandex.ru')
        .putString('to', 'you@yandex.ru')
        .putString('cc', 'him@yandex.ru')
        .putString('bcc', 'hidden@yandex.ru')
        .putString('subject', 'subject')
        .putString('body', 'body')
        .putString('rfcId', 'someRfcId')
        .putString('references', 'someRefs')
        .putInt32('replyType', 2)
        .putString('replyMid', idToString(int64(333))!)
        .putString('baseMessageId', idToString(int64(444))!),
    )
  })
  it('should be representable as JSONItem with optional fields set to null', () => {
    const draftDataWrapper = new DraftDataWrapper(
      int64(111),
      int64(222),
      'some_action',
      'me@yandex.ru',
      'you@yandex.ru',
      null,
      null,
      'subject',
      'body',
      null,
      null,
      ReplyType.FORWARD,
      int64(333),
      int64(444),
    )
    const asJSONItem = draftDataWrapper.asJSONItem()
    expect(asJSONItem).toStrictEqual(
      new MapJSONItem()
        .putString('accountId', idToString(int64(111))!)
        .putString('draftId', idToString(int64(222))!)
        .putString('action', 'some_action')
        .putString('from', 'me@yandex.ru')
        .putString('to', 'you@yandex.ru')
        .putString('subject', 'subject')
        .putString('body', 'body')
        .putInt32('replyType', 2)
        .putString('replyMid', idToString(int64(333))!)
        .putString('baseMessageId', idToString(int64(444))!),
    )
  })
})
