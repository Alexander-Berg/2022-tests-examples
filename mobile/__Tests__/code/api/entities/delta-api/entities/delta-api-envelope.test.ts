import { int64 } from '../../../../../../../../common/ys'
import { idFromString } from '../../../../../../code/api/common/id'
import {
  DeltaApiAttachment,
  deltaApiAttachmentToJSONItem,
  DeltaApiEnvelope,
  DeltaApiRecipient,
  DeltaApiSubject,
  isDeltaApiSubjectEmpty,
} from '../../../../../../code/api/entities/delta-api/entities/delta-api-envelope'
import { JSONItemFromJSON, mapJSONItemFromObject } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { clone } from '../../../../../../../common/__tests__/__helpers__/utils'
import json from './envelope.json'

describe(DeltaApiEnvelope, () => {
  it('should parse Message descriptor (Envelope) from JSON', () => {
    const envelope = DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(json))
    expect(envelope).not.toBeNull()
    expect(envelope!.attachments).toStrictEqual([new DeltaApiAttachment('1.1', 'image/png', 'image.png', 12345)])
    expect(envelope!.attachmentsCount).toBe(1)
    expect(envelope!.attachmentsFullSize).toBe(12345)
    expect(envelope!.bcc).toStrictEqual([new DeltaApiRecipient('bcc', 'yandex.ru', 'BCC Address')])
    expect(envelope!.cc).toStrictEqual([new DeltaApiRecipient('cc', 'yandex.com', 'CC Address')])
    expect(envelope!.date).toBe(int64(1533560112 * 1000))
    expect(envelope!.fid).toBe(int64(1))
    expect(envelope!.firstline).toBe('The first line')
    expect(envelope!.from).toStrictEqual([new DeltaApiRecipient('from', 'yandex.uk', 'From Address')])
    expect(envelope!.imapId).toBe('164')
    expect(envelope!.inReplyTo).toBe('inReply@gmail.com')
    expect(envelope!.lids).toStrictEqual(['103', '24', 'FAKE_MULCA_SHARED_LBL', 'FAKE_RECENT_LBL'])
    expect(envelope!.mid).toBe(idFromString('166351711235998026'))
    expect(envelope!.newCount).toBe(1)
    expect(envelope!.receiveDate).toBe(int64(1533560112 * 1000))
    expect(envelope!.references).toBe('references')
    expect(envelope!.replyTo).toStrictEqual([new DeltaApiRecipient('inReply', 'gmail.com', 'inReply@gmail.com')])
    expect(envelope!.revision).toBe(861)
    expect(envelope!.rfcId).toBe('<1281533560112@myt4-da42643ad020.qloud-c.yandex.net>')
    expect(envelope!.size).toBe(1826)
    expect(envelope!.stid).toBe('320.mail:0.E3960:3118342638140744549041442953089')
    expect(envelope!.subject).toBe('The Subject')
    expect(envelope!.subjectInfo).toStrictEqual(
      new DeltaApiSubject('Some Type', 'Prefix', 'The Subject', 'Postfix', true),
    )
    expect(envelope!.threadCount).toBe(2)
    expect(envelope!.tid).toBe(idFromString('164944336352444719'))
    expect(envelope!.to).toStrictEqual([new DeltaApiRecipient('to', 'yandex.kz', 'to@yandex.kz')])
    expect(envelope!.types).toStrictEqual([4, 55])
    expect(envelope!.uidl).toBe('uidl')
  })
  it('should return null from malformed JSON', () => {
    const envelope = DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON([json]))
    expect(envelope).toBeNull()
  })
  it("should skip recipient if it's malformed", () => {
    const badJSON = clone(json)
    badJSON.cc = [
      [{ local: 'l', domain: 'd.com', displayName: 'l d' }],
      { local: 'l1', domain: 'd1.com', displayName: 'l1 d1' },
    ]
    const envelope = DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(badJSON))
    expect(envelope!.cc).toStrictEqual([new DeltaApiRecipient('l1', 'd1.com', 'l1 d1')])
  })
  it("should skip subject info if it's malformed", () => {
    const badJSON = clone(json)
    badJSON.subjectInfo = []
    const envelope = DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(badJSON))
    expect(envelope!.subjectInfo).toBeNull()
  })
  it("should skip attachment info if it's malformed", () => {
    const badJSON = clone(json)
    badJSON.attachments = [
      [
        {
          m_hid: '1.1',
          m_contentType: 'image',
          m_fileName: 'image.png',
          m_size: 1000,
        },
      ],
      {
        m_hid: '1.2',
        m_contentType: 'pdf',
        m_fileName: 'file.pdf',
        m_size: 2000,
      },
    ]
    const envelope = DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(badJSON))
    expect(envelope!.attachments).toStrictEqual([new DeltaApiAttachment('1.2', 'pdf', 'file.pdf', 2000)])
  })
  it("should set tid null if it's null or empty in json", () => {
    const badJSON = clone(json)
    badJSON.threadId = ''
    expect(DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(badJSON))!.tid).toBeNull()
    delete badJSON.threadId
    expect(DeltaApiEnvelope.fromJSONItem(JSONItemFromJSON(badJSON))!.tid).toBeNull()
  })
})

describe(DeltaApiRecipient, () => {
  it('should build full displayable name', () => {
    expect(new DeltaApiRecipient('local', 'domain.com', 'First Last').asString()).toBe(
      '"First Last" <local@domain.com>',
    )
    expect(new DeltaApiRecipient('local', 'domain.com', '').asString()).toBe('<local@domain.com>')
    expect(new DeltaApiRecipient('', 'domain.com', 'First Last').asString()).toBe('"First Last"')
    expect(new DeltaApiRecipient('local', '', 'First Last').asString()).toBe('"First Last"')
    expect(new DeltaApiRecipient('', '', 'First Last').asString()).toBe('"First Last"')
    expect(new DeltaApiRecipient('', 'domain.com', '').asString()).toBe('')
    expect(new DeltaApiRecipient('local', '', '').asString()).toBe('')
    expect(new DeltaApiRecipient('', '', '').asString()).toBe('')
  })
})

describe(isDeltaApiSubjectEmpty, () => {
  it('should return true if null is passed', () => {
    expect(isDeltaApiSubjectEmpty(null)).toBe(true)
  })
  it('should return true if subject is "No subject"', () => {
    expect(isDeltaApiSubjectEmpty(new DeltaApiSubject('type', 'prefix', 'No subject', 'postfix', true))).toBe(true)
  })
  it('should return true if subject, prefix and postfix are empty', () => {
    expect(isDeltaApiSubjectEmpty(new DeltaApiSubject('type', '', '', '', true))).toBe(true)
  })
  it('should return false if either subject, prefix or postfix are not empty', () => {
    expect(isDeltaApiSubjectEmpty(new DeltaApiSubject('type', 'p', '', '', true))).toBe(false)
    expect(isDeltaApiSubjectEmpty(new DeltaApiSubject('type', '', 'p', '', true))).toBe(false)
    expect(isDeltaApiSubjectEmpty(new DeltaApiSubject('type', '', '', 'p', true))).toBe(false)
  })
})

describe(deltaApiAttachmentToJSONItem, () => {
  it('should build a Map from DeltaApiAttachment', () => {
    expect(deltaApiAttachmentToJSONItem(new DeltaApiAttachment('HID', 'CONTENT', 'FILENAME', 12345))).toEqual(
      mapJSONItemFromObject({
        m_hid: 'HID',
        m_contentType: 'CONTENT',
        m_fileName: 'FILENAME',
        m_size: 12345,
      }),
    )
  })
})
