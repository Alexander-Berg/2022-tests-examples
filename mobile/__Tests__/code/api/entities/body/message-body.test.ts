import { int64 } from '../../../../../../../common/ys'
import { ArrayJSONItem } from '../../../../../../common/code/json/json-types'
import { idFromString } from '../../../../../code/api/common/id'
import {
  MessageBody,
  MessageBodyAttach,
  messageBodyAttachFromJSONItem,
  MessageBodyDescriptor,
  messageBodyFromJSONItem,
  MessageBodyInfo,
  messageBodyInfoFromJSONItem,
  MessageBodyPart,
  messageBodyPartFromJSONItem,
  MessageBodyPayload,
  messageBodyResponseFromJSONItem,
} from '../../../../../code/api/entities/body/message-body'
import { Recipient } from '../../../../../code/api/entities/recipient/recipient'
import { NetworkStatus, NetworkStatusCode } from '../../../../../code/api/entities/status/network-status'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'
import sample from './sample-message-body.json'

function parseMessageBodyArray(object: any): MessageBody[] {
  return (JSONItemFromJSON(object) as ArrayJSONItem).asArray().map((i) => messageBodyFromJSONItem(i)!)
}

describe('MessageBodyResponse', () => {
  it('should fail deserialization if JSON is malformed', () => {
    expect(messageBodyResponseFromJSONItem(JSONItemFromJSON({}))).toBeNull()
  })
  it('should return array of bodies', () => {
    expect(messageBodyResponseFromJSONItem(JSONItemFromJSON(sample))!).toStrictEqual([
      expect.any(MessageBody),
      expect.any(MessageBody),
      expect.any(MessageBody),
    ])
  })
})

describe(MessageBody, () => {
  it('should fail deserialization if JSON is malformed', () => {
    expect(messageBodyFromJSONItem(JSONItemFromJSON([]))).toBeNull()
  })
  it('should return empty payload on error network status', () => {
    const badStatusSample = Object.assign([], sample, { 0: { status: { status: 2 } } })
    const result = parseMessageBodyArray(badStatusSample)

    expect(result[0]).toStrictEqual(new MessageBody(new NetworkStatus(NetworkStatusCode.temporaryError), null))
  })
  it('should return last body content type and language', () => {
    const result = parseMessageBodyArray(sample)
    expect(result[0]).toStrictEqual(
      new MessageBody(
        expect.any(NetworkStatus),
        new MessageBodyPayload(
          expect.any(MessageBodyInfo),
          [expect.any(MessageBodyPart), expect.any(MessageBodyPart)],
          'message/delivery-status',
          'en',
          false,
          [],
        ),
      ),
    )
  })
  it('should return default body content type and language', () => {
    const result = parseMessageBodyArray(sample)
    expect(result[1]).toStrictEqual(
      new MessageBody(
        expect.any(NetworkStatus),
        new MessageBodyPayload(expect.any(MessageBodyInfo), [], 'text/html', '', false, []),
      ),
    )
  })
})

describe(MessageBodyInfo, () => {
  it('should fail deserialization if JSON is malformed', () => {
    expect(messageBodyInfoFromJSONItem(JSONItemFromJSON([]))).toBeNull()
  })
  it('should return info with all parsed values', () => {
    const info = parseMessageBodyArray(sample)[0].payload!.info
    expect(info).toStrictEqual(
      new MessageBodyInfo(
        idFromString('168322036072972509')!,
        [expect.any(MessageBodyAttach), expect.any(MessageBodyAttach), expect.any(MessageBodyAttach)],
        [expect.any(Recipient), expect.any(Recipient)],
        '\u003c20190305192408.DFCF6C811D9@mxback15j.mail.yandex.net\u003e',
        'refs',
      ),
    )
  })
  it('should return default values for missing fields', () => {
    const info = parseMessageBodyArray(sample)[1].payload!.info
    expect(info).toStrictEqual(new MessageBodyInfo(idFromString('169166461003104670')!, [], [], '', ''))
  })
})

describe(MessageBodyPart, () => {
  it('should fail deserialization if JSON is malformed', () => {
    expect(messageBodyPartFromJSONItem(JSONItemFromJSON([]))).toBeNull()
  })
  it('should parse body', () => {
    const info = parseMessageBodyArray(sample)[0].payload!.body[0]
    expect(info).toStrictEqual(new MessageBodyPart('1.1', 'Ваше письмо не было доставлено.', 'text/plain', 'ru'))
  })
})

describe(MessageBodyAttach, () => {
  it('should fail deserialization if JSON is malformed', () => {
    expect(messageBodyAttachFromJSONItem(JSONItemFromJSON([]))).toBeNull()
  })
  it('should return attach with all parsed values', () => {
    const info = parseMessageBodyArray(sample)[0].payload!.info.attachments[0]
    expect(info).toStrictEqual(
      new MessageBodyAttach(
        '1.2',
        '127976297.pdf',
        int64(109297),
        'url_1',
        true,
        true,
        true,
        'pdf',
        'application/pdf',
        'sample_id',
      ),
    )
  })
  it('should return default values for missing fields', () => {
    const info = parseMessageBodyArray(sample)[0].payload!.info.attachments[1]
    expect(info).toStrictEqual(
      new MessageBodyAttach(
        '1.3',
        'Empty email.eml',
        int64(2308),
        'url_2',
        false,
        false,
        false,
        null,
        'message/rfc822',
        null,
      ),
    )
  })
  it('should return disk folder attach with all parsed values', () => {
    const info = parseMessageBodyArray(sample)[0].payload!.info.attachments[2]
    expect(info).toStrictEqual(
      new MessageBodyAttach(
        '1.4',
        'Disk Folder Attach',
        int64(0),
        'url_3',
        true,
        false,
        false,
        'folder',
        'application/octet-stream',
        null,
      ),
    )
  })
  describe(MessageBodyDescriptor, () => {
    it('should be creatable from string key with translation info', () => {
      const key = '1234^^ru^^en'
      expect(MessageBodyDescriptor.fromKey(key)).toEqual(new MessageBodyDescriptor(int64(1234), 'ru', 'en'))
    })
    it('should be creatable from string key without translation info', () => {
      const key = '1234'
      expect(MessageBodyDescriptor.fromKey(key)).toEqual(new MessageBodyDescriptor(int64(1234), null, null))
    })
    it('should not be creatable from incorrect string key', () => {
      const key = '1234^^ru'
      expect(MessageBodyDescriptor.fromKey(key)).toBeNull()
    })
    it('should return true for isTranslated for descriptor with translation info', () => {
      const descriptor = new MessageBodyDescriptor(int64(4321), 'ru', 'en')
      expect(descriptor.isTranslated()).toBe(true)
    })
    it('should return false for isTranslated for descriptor without translation info', () => {
      const descriptor = new MessageBodyDescriptor(int64(4321), null, null)
      expect(descriptor.isTranslated()).toBe(false)
    })
  })
})
