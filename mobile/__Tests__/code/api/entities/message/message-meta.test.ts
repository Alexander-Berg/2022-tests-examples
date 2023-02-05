import { int64, stringToInt64 } from '../../../../../../../common/ys'
import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'
import { idFromString } from '../../../../../code/api/common/id'
import {
  DeltaApiAttachment,
  DeltaApiEnvelope,
  DeltaApiRecipient,
  DeltaApiSubject,
} from '../../../../../code/api/entities/delta-api/entities/delta-api-envelope'
import {
  Attachment,
  Attachments,
  attachmentsFromDeltaApiAttachments,
  attachmentsFromJSONItem,
  deltaApiEnvelopeToMessageMeta,
  getMidToTimestampMap,
  MessageMeta,
  MessageMetaBuilder,
  messageMetaFromJSONItem,
} from '../../../../../code/api/entities/message/message-meta'
import {
  MessageTypeFlags,
  messageTypeMaskFromServerMessageTypes,
} from '../../../../../code/api/entities/message/message-type'
import { EmailWithName } from '../../../../../code/api/entities/recipient/email'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../../../common/__tests__/__helpers__/utils'
import sample from './sample.json'

describe(MessageMeta, () => {
  describe(messageMetaFromJSONItem, () => {
    it('should return null if malformed JSON Item is passed', () => {
      expect(messageMetaFromJSONItem(new ArrayJSONItem())).toBeNull()
    })
    it('should return Message Meta from JSON Item (no attachments)', () => {
      const item = sample[0].messageBatch.messages[0]
      const result = messageMetaFromJSONItem(JSONItemFromJSON(item))
      expect(result).toStrictEqual(
        new MessageMeta(
          idFromString(item.mid)!,
          idFromString(item.fid)!,
          idFromString(item.tid),
          item.lid,
          item.subjEmpty,
          item.subjPrefix,
          item.subjText,
          item.firstLine,
          EmailWithName.fromNameAndEmail(item.from.name, item.from.email).asString(),
          item.status.includes(1),
          false,
          null,
          stringToInt64(item.utc_timestamp)! * int64(1000),
          item.hasAttach,
          null,
          messageTypeMaskFromServerMessageTypes(item.types.map((v) => Number.parseInt(v, 10)!)),
        ),
      )
    })
    it('should return Message Meta from JSON Item (with attachments)', () => {
      const item = sample[2].messageBatch.messages[0]
      const result = messageMetaFromJSONItem(JSONItemFromJSON(item))
      expect(result).toStrictEqual(
        new MessageMeta(
          idFromString(item.mid)!,
          idFromString(item.fid)!,
          idFromString(item.tid),
          item.lid,
          item.subjEmpty,
          item.subjPrefix,
          item.subjText,
          item.firstLine,
          EmailWithName.fromNameAndEmail(item.from.name, item.from.email).asString(),
          item.status.includes(1),
          false,
          null,
          stringToInt64(item.utc_timestamp)! * int64(1000),
          item.hasAttach,
          attachmentsFromJSONItem(JSONItemFromJSON((item as any).attachments))!,
          messageTypeMaskFromServerMessageTypes(item.types.map((v) => Number.parseInt(v, 10)!)),
        ),
      )
    })
    it('should return Message Meta from JSON Item (malformed attachments)', () => {
      const item = { ...sample[2].messageBatch.messages[0], attachments: [] }
      const result = messageMetaFromJSONItem(JSONItemFromJSON(item))
      expect(result).toStrictEqual(
        new MessageMeta(
          idFromString(item.mid)!,
          idFromString(item.fid)!,
          idFromString(item.tid),
          item.lid,
          item.subjEmpty,
          item.subjPrefix,
          item.subjText,
          item.firstLine,
          EmailWithName.fromNameAndEmail(item.from.name, item.from.email).asString(),
          item.status.includes(1),
          false,
          null,
          stringToInt64(item.utc_timestamp)! * int64(1000),
          item.hasAttach,
          null,
          messageTypeMaskFromServerMessageTypes(item.types.map((v) => Number.parseInt(v, 10)!)),
        ),
      )
    })
    it('should skip malformed attachments when parsing Message Meta', () => {
      const item = clone(sample[2].messageBatch.messages[0])
      item.attachments.attachments[3] = [] // One with hid 1.5
      const result = messageMetaFromJSONItem(JSONItemFromJSON(item))
      expect(result).toBeInstanceOf(MessageMeta)
      expect(result!.attachments!.attachments.map((a) => a.hid)).toStrictEqual(['1.2', '1.3', '1.4', '1.6'])
    })
    it('should assume the message is read if an unread mark is incorrect', () => {
      const item = clone(sample[0].messageBatch.messages[0])
      item.status = [1]
      expect(messageMetaFromJSONItem(JSONItemFromJSON(item))!.unread).toBe(true)
      item.status = [true]
      expect(messageMetaFromJSONItem(JSONItemFromJSON(item))!.unread).toBe(false)
    })
  })
})

describe(deltaApiEnvelopeToMessageMeta, () => {
  it('should build message meta from envelope', () => {
    const timestamp = int64(12345)
    const messageMeta = deltaApiEnvelopeToMessageMeta(
      new DeltaApiEnvelope(
        int64(301),
        int64(101),
        int64(401),
        10001,
        timestamp,
        timestamp,
        [new DeltaApiRecipient('from', 'domain.com', 'From Name')],
        [new DeltaApiRecipient('replyTo', 'domain.com', 'ReplyTo Name')],
        'RE: Subject',
        new DeltaApiSubject('type', 'RE:', 'Subject', 'POST', true),
        [new DeltaApiRecipient('cc', 'domain.com', 'Cc Name')],
        [new DeltaApiRecipient('bcc', 'domain.com', 'Bcc Name')],
        [new DeltaApiRecipient('to', 'domain.com', 'To Name')],
        'UIDL',
        'IMAPID',
        'STID',
        'Firstline',
        'inreply@domain.com',
        'references',
        'RFCID',
        5431,
        0,
        0,
        1,
        1000,
        [new DeltaApiAttachment('hid1', 'image/png', 'image.png', 1000)],
        ['lid1', 'lid2', 'FAKE_SEEN_LBL'],
        [4, 103],
      ),
    )
    expect(messageMeta).toStrictEqual(
      new MessageMeta(
        int64(301),
        int64(101),
        int64(401),
        ['lid1', 'lid2', 'FAKE_SEEN_LBL'],
        false,
        'RE:',
        'Subject',
        'Firstline',
        '"From Name" <from@domain.com>',
        false,
        false,
        null,
        timestamp,
        true,
        new Attachments([
          new Attachment('hid1', 'image.png', 'image', false, int64(1000), 'image/png', true, '', '', false, null),
        ]),
        MessageTypeFlags.people | MessageTypeFlags.tPeople,
      ),
    )
  })
  it('should build message meta from envelope with empty subject', () => {
    const timestamp = int64(12345)
    const messageMeta = deltaApiEnvelopeToMessageMeta(
      new DeltaApiEnvelope(
        int64(301),
        int64(101),
        int64(401),
        10001,
        timestamp,
        timestamp,
        [],
        [new DeltaApiRecipient('replyTo', 'domain.com', 'ReplyTo Name')],
        '',
        null,
        [new DeltaApiRecipient('cc', 'domain.com', 'Cc Name')],
        [new DeltaApiRecipient('bcc', 'domain.com', 'Bcc Name')],
        [new DeltaApiRecipient('to', 'domain.com', 'To Name')],
        'UIDL',
        'IMAPID',
        'STID',
        'Firstline',
        'inreply@domain.com',
        'references',
        'RFCID',
        5431,
        0,
        0,
        1,
        1000,
        [],
        ['lid1', 'lid2'],
        [4, 103],
      ),
    )
    expect(messageMeta).toStrictEqual(
      new MessageMeta(
        int64(301),
        int64(101),
        int64(401),
        ['lid1', 'lid2'],
        true,
        null,
        '',
        'Firstline',
        '',
        true,
        false,
        null,
        timestamp,
        true,
        new Attachments([]),
        MessageTypeFlags.people | MessageTypeFlags.tPeople,
      ),
    )
  })
})

describe(attachmentsFromDeltaApiAttachments, () => {
  it('should filter out Disk attachments', () => {
    const allAttachments = [
      new DeltaApiAttachment('hid1', 'image/png', 'image.png', 1000),
      new DeltaApiAttachment('hid3', 'image/png', 'narod_attachment_links.html', 2000),
      new DeltaApiAttachment('hid3', 'document/pdf', 'file.pdf', 3000),
      new DeltaApiAttachment('hid4', '', 'file.pdf', 4000),
    ]
    expect(attachmentsFromDeltaApiAttachments(allAttachments).attachments).toStrictEqual([
      new Attachment('hid1', 'image.png', 'image', false, int64(1000), 'image/png', true, '', '', false, null),
      new Attachment('hid3', 'file.pdf', 'document', false, int64(3000), 'document/pdf', false, null, '', false, null),
      new Attachment('hid4', 'file.pdf', '', false, int64(4000), '', false, null, '', false, null),
    ])
  })
})

describe(getMidToTimestampMap, () => {
  it('should return a map of mids to timestamps from metas', () => {
    expect(getMidToTimestampMap([]).size).toBe(0)
    expect(
      getMidToTimestampMap([
        new MessageMeta(
          int64(301),
          int64(101),
          null,
          [],
          true,
          null,
          '',
          '',
          '',
          false,
          false,
          null,
          int64(12345),
          false,
          null,
          MessageTypeFlags.people | MessageTypeFlags.tPeople,
        ),
        new MessageMeta(
          int64(302),
          int64(101),
          null,
          [],
          true,
          null,
          '',
          '',
          '',
          false,
          false,
          null,
          int64(23456),
          false,
          null,
          MessageTypeFlags.people | MessageTypeFlags.tPeople,
        ),
        new MessageMeta(
          int64(301),
          int64(101),
          null,
          [],
          true,
          null,
          '',
          '',
          '',
          false,
          false,
          null,
          int64(34567),
          false,
          null,
          MessageTypeFlags.people | MessageTypeFlags.tPeople,
        ),
      ]),
    ).toStrictEqual(
      new Map([
        [int64(301), int64(34567)],
        [int64(302), int64(23456)],
      ]),
    )
  })
})
describe(MessageMetaBuilder, () => {
  it(' should be convertable to MessageMeta back and forth', () => {
    const meta = new MessageMeta(
      int64(301),
      int64(101),
      null,
      ['label1', 'label2'],
      true,
      null,
      '',
      '',
      '',
      false,
      false,
      null,
      int64(34567),
      false,
      null,
      MessageTypeFlags.people | MessageTypeFlags.tPeople,
    )
    const meta2 = meta.toBuilder().build()
    expect(meta).toStrictEqual(meta2)
  })
  it(' should be constructable from MessageMeta using copying constructor', () => {
    const meta = new MessageMeta(
      int64(301),
      int64(101),
      null,
      ['label1', 'label2'],
      true,
      null,
      '',
      '',
      '',
      false,
      false,
      null,
      int64(34567),
      false,
      null,
      MessageTypeFlags.people | MessageTypeFlags.tPeople,
    )
    const meta2 = new MessageMetaBuilder(meta).build()
    expect(meta).toStrictEqual(meta2)
  })
  it(' should allow modification and then building MessageMeta', () => {
    const meta = new MessageMeta(
      int64(301),
      int64(101),
      null,
      ['label1', 'label2'],
      true,
      null,
      '',
      '',
      '',
      false,
      false,
      null,
      int64(34567),
      false,
      null,
      MessageTypeFlags.people | MessageTypeFlags.tPeople,
    )
    const builder = meta.toBuilder()
    builder.setAttachments(
      new Attachments([
        new Attachment(
          '1.1.2',
          'myAttach',
          'someFileClass',
          false,
          int64(10000),
          'text/html',
          false,
          null,
          'https://ya.ru',
          false,
          null,
        ),
      ]),
    )
    builder.setFid(int64(102))
    builder.setFirstLine('Hello..')
    builder.setHasAttach(true)
    builder.setLids(['label3', 'label4'])
    builder.setMid(int64(302))
    builder.setSearchOnly(false)
    builder.setSender('someone@yandex.ru')
    builder.setShowFor('someShowFor')
    builder.setSubjEmpty(false)
    builder.setSubjPrefix('Re')
    builder.setSubjText('Important subject')
    builder.setTid(int64(1))
    builder.setTimestamp(int64(12345678))
    builder.setTypeMask(MessageTypeFlags.eshop)
    builder.setUnread(true)
    expect(builder.getMid()).toBe(int64(302))
    const meta2 = builder.build()
    expect(meta2).toStrictEqual(
      new MessageMeta(
        int64(302),
        int64(102),
        int64(1),
        ['label3', 'label4'],
        false,
        'Re',
        'Important subject',
        'Hello..',
        'someone@yandex.ru',
        true,
        false,
        'someShowFor',
        int64(12345678),
        true,
        new Attachments([
          new Attachment(
            '1.1.2',
            'myAttach',
            'someFileClass',
            false,
            int64(10000),
            'text/html',
            false,
            null,
            'https://ya.ru',
            false,
            null,
          ),
        ]),
        MessageTypeFlags.eshop,
      ),
    )
  })
})
