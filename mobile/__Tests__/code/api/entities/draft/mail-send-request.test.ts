import { resolve } from '../../../../../../../common/xpromise-support'
import { int64, Nullable, YSError } from '../../../../../../../common/ys'
import { ArrayJSONItem, MapJSONItem, StringJSONItem } from '../../../../../../common/code/json/json-types'
import { getVoid } from '../../../../../../common/code/result/result'
import { createMockInstance } from '../../../../../../common/__tests__/__helpers__/utils'
import { YSPair } from '../../../../../../common/code/utils/tuples'
import {
  MailSendRequest,
  MailSendRequestBuilder,
} from '../../../../../../mapi/code/api/entities/draft/mail-send-request'
import {
  fillInMailSendRequest,
  fillReplyForwardOptions,
  parseSingleAddressLine,
} from '../../../../../code/api/entities/draft/fill-mail-send-request'
import { Models } from '../../../../../../xmail/code/models'
import { Registry } from '../../../../../../xmail/code/registry'
import { ServiceLocatorItems } from '../../../../../../xmail/code/utils/service-locator'
import { MockHighPrecisionTimer, MockStorage } from '../../../../../../xmail/__tests__/__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../../../../xmail/__tests__/__helpers__/test-id-support'
import { AttachmentSizes } from '../../../../../code/busilogics/draft/attachment-sizes'
import { DiskAttachBundle, DraftAttachEntry } from '../../../../../code/busilogics/draft/draft-attach-entry'
import { DraftDataWrapper, ReplyType } from '../../../../../code/busilogics/draft/draft-data-wrapper'
import { Rfc822Token } from '../../../../../code/service/rfc822-tokenizer'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../../common/__tests__/__helpers__/mock-patches'

describe(MailSendRequest, () => {
  beforeAll(() => {
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      MockStorage(),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
  })
  afterAll(() => Models.drop())
  it('should be creatable with all allowed fields as nulls and default values', () => {
    const sendRequest = new MailSendRequest(
      'composeCheck',
      null,
      null,
      'Body of mail message',
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      3,
    )
    const mapJsonItem = sendRequest.asMapJSONItem()
    expect(mapJsonItem.asMap().size).toBe(7)
    expect(mapJsonItem).toEqual(
      new MapJSONItem()
        .putString('compose_check', 'composeCheck')
        .putString('subj', '')
        .putString('to', '')
        .putString('from_name', '')
        .putString('from_mailbox', '')
        .putString('send', 'Body of mail message')
        .putString('ttype', 'html'),
    )
  })
  it('should be creatable with all non-nulls and default values', () => {
    const sendRequest = new MailSendRequest(
      'composeCheck',
      'my_friend@yandex.ru',
      'my_secret_friend@yandex.ru',
      'Body of mail message',
      'someReferences',
      'in reply to some theme',
      'some draft base',
      'some_narod_attachId',
      ['partId1', 'partId2', 'partId3'],
      'some reply',
      'forward',
      'some template base',
      ['attachId1', 'attachId2', 'attachId3'],
      3,
    )
    const map1 = sendRequest.asMapJSONItem()
    expect(map1.asMap().size).toBe(18)
    expect(map1).toEqual(
      new MapJSONItem()
        .putString('compose_check', 'composeCheck')
        .putString('subj', '')
        .putString('to', '')
        .putString('cc', 'my_friend@yandex.ru')
        .putString('bcc', 'my_secret_friend@yandex.ru')
        .putString('from_name', '')
        .putString('from_mailbox', '')
        .putString('send', 'Body of mail message')
        .putString('references', 'someReferences')
        .putString('inreplyto', 'in reply to some theme')
        .putString('draft_base', 'some draft base')
        .putString('narod_att', 'some_narod_attachId')
        .put('parts', new ArrayJSONItem(['partId1', 'partId2', 'partId3'].map((item) => new StringJSONItem(item))))
        .putString('reply', 'some reply')
        .putString('forward', 'forward')
        .putString('template_base', 'some template base')
        .put(
          'att_ids',
          new ArrayJSONItem(['attachId1', 'attachId2', 'attachId3'].map((item) => new StringJSONItem(item))),
        )
        .putString('ttype', 'html'),
    )
  })
  it('should be creatable with all non-nulls and non-default values', () => {
    const sendRequest = new MailSendRequest(
      'composeCheck',
      'my_friend@yandex.ru',
      'my_secret_friend@yandex.ru',
      'Body of mail message',
      'someReferences',
      'in reply to some theme',
      'some draft base',
      'some_narod_attachId',
      ['partId1', 'partId2', 'partId3'],
      'some reply',
      'forward',
      'some template base',
      ['attachId1', 'attachId2', 'attachId3'],
      3,
      'subject',
      'friend@yandex.ru',
      'Me',
      'me@yandex.ru',
    )
    const mapJson = sendRequest.asMapJSONItem()
    const map1 = mapJson.asMap()

    expect(map1.size).toBe(18)
    expect(mapJson).toEqual(
      new MapJSONItem()
        .putString('compose_check', 'composeCheck')
        .putString('subj', 'subject')
        .putString('to', 'friend@yandex.ru')
        .putString('cc', 'my_friend@yandex.ru')
        .putString('bcc', 'my_secret_friend@yandex.ru')
        .putString('from_name', 'Me')
        .putString('from_mailbox', 'me@yandex.ru')
        .putString('send', 'Body of mail message')
        .putString('references', 'someReferences')
        .putString('inreplyto', 'in reply to some theme')
        .putString('draft_base', 'some draft base')
        .putString('narod_att', 'some_narod_attachId')
        .put('parts', new ArrayJSONItem(['partId1', 'partId2', 'partId3'].map((item) => new StringJSONItem(item))))
        .putString('reply', 'some reply')
        .putString('forward', 'forward')
        .putString('template_base', 'some template base')
        .put(
          'att_ids',
          new ArrayJSONItem(['attachId1', 'attachId2', 'attachId3'].map((item) => new StringJSONItem(item))),
        )
        .putString('ttype', 'html'),
    )
  })
  it('should be convertable to Builder ', () => {
    const sendRequest = new MailSendRequest(
      'composeCheck',
      'my_friend@yandex.ru',
      'my_secret_friend@yandex.ru',
      'Body of mail message',
      'someReferences',
      'in reply to some theme',
      'some draft base',
      'some_narod_attachId',
      ['partId1', 'partId2', 'partId3'],
      'some reply',
      'forward',
      'some template base',
      ['attachId1', 'attachId2', 'attachId3'],
      3,
      'subject',
      'friend@yandex.ru',
      'Me',
      'me@yandex.ru',
    )
      .toBuilder()
      .build()
    expect(sendRequest.composeCheck).toBe('composeCheck')
    expect(sendRequest.body).toBe('Body of mail message')
    expect(sendRequest.subject).toBe('subject')
    expect(sendRequest.to).toBe('friend@yandex.ru')
    expect(sendRequest.fromName).toBe('Me')
    expect(sendRequest.fromMailbox).toBe('me@yandex.ru')
    expect(sendRequest.cc).toBe('my_friend@yandex.ru')
    expect(sendRequest.bcc).toBe('my_secret_friend@yandex.ru')
    expect(sendRequest.references).toBe('someReferences')
    expect(sendRequest.inReplyTo).toBe('in reply to some theme')
    expect(sendRequest.draftBase).toBe('some draft base')
    expect(sendRequest.narodAtt).toBe('some_narod_attachId')
    expect(sendRequest.parts).toStrictEqual(['partId1', 'partId2', 'partId3'])
    expect(sendRequest.reply).toBe('some reply')
    expect(sendRequest.forward).toBe('forward')
    expect(sendRequest.templateBase).toBe('some template base')
    expect(sendRequest.attachIds).toStrictEqual(['attachId1', 'attachId2', 'attachId3'])
    expect(sendRequest.attachesCount).toBe(3)
  })

  describe('fillInMailSendRequest', () => {
    it('should return promise with prepared MailSendRequest with proper baseMessageId', (done) => {
      jest.mock('../../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockImplementation((serviceLocatorItem) => {
          if (serviceLocatorItem === ServiceLocatorItems.handler) {
            return {
              destroy: jest.fn().mockReturnValue(getVoid()),
              hasMessages: jest.fn().mockReturnValue(false),
              post: jest.fn().mockReturnValue(resolve(getVoid())),
            }
          } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
            return {
              get: jest.fn().mockReturnValue(null),
              put: jest.fn().mockReturnValue(null),
              remove: jest.fn().mockReturnValue(true),
            }
          } else if (serviceLocatorItem === ServiceLocatorItems.rfc822Tokenizer) {
            return {
              tokenize: jest
                .fn()
                .mockReturnValue([new Rfc822Token('', 'me@yandex.ru', 'some comment')])
                .mockReturnValueOnce([new Rfc822Token(null, 'me@yandex.ru', 'some comment')]),
            }
          }
          return null
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const newUploadedAttachesPromise = [
        new DraftAttachEntry(
          int64(11),
          int64(22),
          'someTempMulOrDiskUrl',
          'somefile',
          'my attach',
          int64(10000),
          'text/plain',
          false,
          false,
          false,
          'tmp/localPath',
        ),
      ]
      const diskAttachesPromise = [
        new DiskAttachBundle('diskAttach1', 'https://yandex.ru/download/diskAttach1.png', int64(32768)),
      ]
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().drafts().getMidByDid = jest.fn().mockReturnValue(resolve(int64(111)))
      Models.instance().drafts().getMidByDidOrReject = jest.fn().mockReturnValue(resolve(int64(111)))
      Models.instance().composeStore().loadBody = jest.fn().mockReturnValue(resolve('body body body'))
      const draftAttachments = Models.instance().draftAttachments()
      draftAttachments.getPinnedNonDiskAttachesHids = jest.fn().mockReturnValue(resolve(['part1', 'part2']))
      draftAttachments.formNarodAttachParameter = jest.fn().mockReturnValue('prepared narod attach html')
      const draftDataWrapper = new DraftDataWrapper(
        int64(1),
        int64(22),
        'someAction',
        'me@yandex.ru',
        'you@yandex.ru',
        'him@yandex.ru',
        'hidden@yandex.ru',
        'some subject',
        'body body',
        null,
        'some refs',
        ReplyType.REPLY,
        int64(333),
        int64(444),
      )
      fillInMailSendRequest(
        Models.instance(),
        int64(12345),
        draftDataWrapper,
        newUploadedAttachesPromise,
        diskAttachesPromise,
      ).then((mailSendRequest) => {
        expect(mailSendRequest).toStrictEqual(
          new MailSendRequestBuilder()
            .setAttachIds(['someTempMulOrDiskUrl'])
            .setTo('you@yandex.ru')
            .setBcc('hidden@yandex.ru')
            .setCc('him@yandex.ru')
            .setBody('body body body')
            .setComposeCheck('')
            .setDraftBase('12345')
            .setForward(null)
            .setFromMailbox('me@yandex.ru')
            .setFromName('')
            .setInReplyTo(null)
            .setNarodAtt('prepared narod attach html')
            .setParts(['part1', 'part2'])
            .setReferences('some refs')
            .setReply(null)
            .setSubject('some subject')
            .setTemplateBase(null)
            .build(),
        )
        fillInMailSendRequest(
          Models.instance(),
          int64(12345),
          draftDataWrapper,
          newUploadedAttachesPromise,
          diskAttachesPromise,
        ).then((mailSendRequest2) => {
          expect(mailSendRequest2).toStrictEqual(
            new MailSendRequestBuilder()
              .setAttachIds(['someTempMulOrDiskUrl'])
              .setTo('you@yandex.ru')
              .setBcc('hidden@yandex.ru')
              .setCc('him@yandex.ru')
              .setBody('body body body')
              .setComposeCheck('')
              .setDraftBase('12345')
              .setForward(null)
              .setFromMailbox('me@yandex.ru')
              .setFromName('')
              .setInReplyTo(null)
              .setNarodAtt('prepared narod attach html')
              .setParts(['part1', 'part2'])
              .setReferences('some refs')
              .setReply(null)
              .setSubject('some subject')
              .setTemplateBase(null)
              .build(),
          )
          Models.drop()
          done()
        })
      })
    })
    it('should return promise with prepared MailSendRequest without attaches', (done) => {
      jest.mock('../../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue({
          tokenize: jest.fn().mockReturnValue([new Rfc822Token('Me', 'me@yandex.ru', 'some comment')]),
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().drafts().getMidByDid = jest.fn().mockReturnValue(resolve(int64(1)))
      Models.instance().drafts().getMidByDidOrReject = jest.fn().mockReturnValue(resolve(int64(1)))
      const draftDataWrapper = new DraftDataWrapper(
        int64(1),
        int64(22),
        'someAction',
        'me@yandex.ru',
        'you@yandex.ru',
        'him@yandex.ru',
        'hidden@yandex.ru',
        'some subject',
        'body body',
        null,
        'some refs',
        ReplyType.REPLY,
        int64(333),
        int64(1),
      )
      const draftAttachments = Models.instance().draftAttachments()
      draftAttachments.getPinnedNonDiskAttachesHids = jest.fn().mockReturnValue(resolve([]))
      draftAttachments.formNarodAttachParameter = jest.fn().mockReturnValue('prepared narod attach html')
      const composeStore = Models.instance().composeStore()
      composeStore.loadBody = jest.fn().mockReturnValue(resolve('body body body'))
      fillInMailSendRequest(Models.instance(), int64(-1111), draftDataWrapper, [], []).then((mailSendRequest) => {
        expect(mailSendRequest).toStrictEqual(
          new MailSendRequestBuilder()
            .setAttachIds(null)
            .setTo('you@yandex.ru')
            .setBcc('hidden@yandex.ru')
            .setCc('him@yandex.ru')
            .setBody('body body body')
            .setComposeCheck('')
            .setDraftBase(null)
            .setForward(null)
            .setFromMailbox('me@yandex.ru')
            .setFromName('Me')
            .setInReplyTo(null)
            .setNarodAtt(null)
            .setParts([])
            .setReferences('some refs')
            .setReply('333')
            .setSubject('some subject')
            .setTemplateBase(null)
            .build(),
        )
        Models.drop()
        done()
      })
    })
    it('should return promise with prepared MailSendRequest with invalid baseMessageId', (done) => {
      jest.mock('../../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockImplementation((serviceLocatorItem) => {
          if (serviceLocatorItem === ServiceLocatorItems.handler) {
            return {
              destroy: jest.fn().mockReturnValue(getVoid()),
              hasMessages: jest.fn().mockReturnValue(false),
              post: jest.fn().mockReturnValue(resolve(getVoid())),
            }
          } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
            return {
              get: jest.fn().mockReturnValue(null),
              put: jest.fn().mockReturnValue(null),
              remove: jest.fn().mockReturnValue(true),
            }
          } else if (serviceLocatorItem === ServiceLocatorItems.rfc822Tokenizer) {
            return {
              tokenize: jest.fn().mockReturnValue([new Rfc822Token('Me', 'me@yandex.ru', 'some comment')]),
            }
          }
          return null
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      const newUploadedAttachesPromise = [
        new DraftAttachEntry(
          int64(11),
          int64(22),
          'someTempMulOrDiskUrl',
          'somefile',
          'my attach',
          int64(10000),
          'text/plain',
          false,
          false,
          false,
          'tmp/localPath',
        ),
      ]
      const diskAttachesPromise = [
        new DiskAttachBundle('diskAttach1', 'https://yandex.ru/download/diskAttach1.png', int64(32768)),
      ]
      const draftDataWrapper = new DraftDataWrapper(
        int64(1),
        int64(22),
        'someAction',
        'me@yandex.ru',
        'you@yandex.ru',
        'him@yandex.ru',
        'hidden@yandex.ru',
        'some subject',
        'body body',
        null,
        'some refs',
        ReplyType.REPLY,
        int64(333),
        int64(-1),
      )
      const draftAttachments = Models.instance().draftAttachments()
      draftAttachments.getPinnedNonDiskAttachesHids = jest.fn().mockReturnValue(resolve(['part1', 'part2']))
      draftAttachments.formNarodAttachParameter = jest.fn().mockReturnValue('prepared narod attach html')
      const composeStore = Models.instance().composeStore()
      composeStore.loadBody = jest.fn().mockReturnValue(resolve('body body body'))
      fillInMailSendRequest(
        Models.instance(),
        int64(-1111),
        draftDataWrapper,
        newUploadedAttachesPromise,
        diskAttachesPromise,
      ).then((mailSendRequest) => {
        expect(mailSendRequest).toStrictEqual(
          new MailSendRequestBuilder()
            .setAttachIds(['someTempMulOrDiskUrl'])
            .setTo('you@yandex.ru')
            .setBcc('hidden@yandex.ru')
            .setCc('him@yandex.ru')
            .setBody('body body body')
            .setComposeCheck('')
            .setDraftBase(null)
            .setForward(null)
            .setFromMailbox('me@yandex.ru')
            .setFromName('Me')
            .setInReplyTo(null)
            .setNarodAtt('prepared narod attach html')
            .setParts(null)
            .setReferences('some refs')
            .setReply('333')
            .setSubject('some subject')
            .setTemplateBase(null)
            .build(),
        )
        Models.drop()
        done()
      })
    })

    it('should reject promise if message body could not be loaded', (done) => {
      jest.mock('../../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockImplementation((serviceLocatorItem) => {
          if (serviceLocatorItem === ServiceLocatorItems.handler) {
            return {
              destroy: jest.fn().mockReturnValue(getVoid()),
              hasMessages: jest.fn().mockReturnValue(false),
              post: jest.fn().mockReturnValue(resolve(getVoid())),
            }
          } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
            return {
              get: jest.fn().mockReturnValue(null),
              put: jest.fn().mockReturnValue(null),
              remove: jest.fn().mockReturnValue(true),
            }
          } else if (serviceLocatorItem === ServiceLocatorItems.rfc822Tokenizer) {
            return {
              tokenize: jest.fn().mockReturnValue([new Rfc822Token('Me', 'me@yandex.ru', 'some comment')]),
            }
          }
          return null
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      const newUploadedAttachesPromise = [
        new DraftAttachEntry(
          int64(11),
          int64(22),
          'someTempMulOrDiskUrl',
          'somefile',
          'my attach',
          int64(10000),
          'text/plain',
          false,
          false,
          false,
          'tmp/localPath',
        ),
      ]
      const diskAttachesPromise = [
        new DiskAttachBundle('diskAttach1', 'https://yandex.ru/download/diskAttach1.png', int64(32768)),
      ]
      const draftDataWrapper = new DraftDataWrapper(
        int64(1),
        int64(22),
        'someAction',
        '',
        'you@yandex.ru',
        'him@yandex.ru',
        'hidden@yandex.ru',
        'some subject',
        'body body',
        null,
        'some refs',
        ReplyType.REPLY,
        int64(333),
        int64(-1),
      )
      const draftAttachments = Models.instance().draftAttachments()
      draftAttachments.getPinnedNonDiskAttachesHids = jest.fn().mockReturnValue(resolve(['part1', 'part2']))
      draftAttachments.formNarodAttachParameter = jest.fn().mockReturnValue('prepared narod attach html')
      const composeStore = Models.instance().composeStore()
      composeStore.loadBody = jest.fn().mockReturnValue(resolve(null))
      fillInMailSendRequest(
        Models.instance(),
        int64(-1111),
        draftDataWrapper,
        newUploadedAttachesPromise,
        diskAttachesPromise,
      ).failed((errorReason) => {
        expect(errorReason).toStrictEqual(new YSError('Can not load body in send task: body is null'))
        Models.drop()
        done()
      })
    })
  })
})

describe(MailSendRequestBuilder, () => {
  it('should be creatable with all empty or default fields', () => {
    const request: MailSendRequest = new MailSendRequestBuilder().build()
    expect(request.composeCheck).toBe('')
    expect(request.body).toBe('')
    expect(request.subject).toBe('')
    expect(request.to).toBe('')
    expect(request.fromName).toBe('')
    expect(request.fromMailbox).toBe('')
    expect(request.cc).toBeNull()
    expect(request.bcc).toBeNull()
    expect(request.references).toBeNull()
    expect(request.inReplyTo).toBeNull()
    expect(request.draftBase).toBeNull()
    expect(request.narodAtt).toBeNull()
    expect(request.parts).toBeNull()
    expect(request.reply).toBeNull()
    expect(request.forward).toBeNull()
    expect(request.templateBase).toBeNull()
    expect(request.attachIds).toBeNull()
    expect(request.attachesCount).toBe(0)
  })
  it('should be creatable with all non-empty/non-default fields', () => {
    const request: MailSendRequest = new MailSendRequestBuilder()
      .setComposeCheck('composeCheck')
      .setCc('cc')
      .setBcc('bcc')
      .setBody('body')
      .setReferences('references')
      .setInReplyTo('inReplyTo')
      .setDraftBase('draftBase')
      .setNarodAtt('narodAtt')
      .setParts(['pars1', 'part2'])
      .setReply('reply')
      .setForward('forward')
      .setTemplateBase('templateBase')
      .setAttachIds(['attachId1', 'attachId2'])
      .setSubject('subject')
      .setTo('to')
      .setFromName('fromName')
      .setFromMailbox('fromMailbox')
      .build()
    expect(request.composeCheck).toBe('composeCheck')
    expect(request.body).toBe('body')
    expect(request.subject).toBe('subject')
    expect(request.to).toBe('to')
    expect(request.fromName).toBe('fromName')
    expect(request.fromMailbox).toBe('fromMailbox')
    expect(request.cc).toBe('cc')
    expect(request.bcc).toBe('bcc')
    expect(request.references).toBe('references')
    expect(request.inReplyTo).toBe('inReplyTo')
    expect(request.draftBase).toBe('draftBase')
    expect(request.narodAtt).toBe('narodAtt')
    expect(request.parts).toStrictEqual(['pars1', 'part2'])
    expect(request.reply).toBe('reply')
    expect(request.forward).toBe('forward')
    expect(request.templateBase).toBe('templateBase')
    expect(request.attachIds).toStrictEqual(['attachId1', 'attachId2'])
    expect(request.attachesCount).toBe(2)
  })
})
describe(parseSingleAddressLine, () => {
  it('should successfully parse rfc compliant line', () => {
    jest.mock('../../../../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        tokenize: jest.fn().mockReturnValue([new Rfc822Token('Mr Smith', 'mr_smith@yandex.ru', 'some comment')]),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    expect(parseSingleAddressLine('some address')).toStrictEqual(
      new YSPair<Nullable<string>, Nullable<string>>('mr_smith@yandex.ru', 'Mr Smith'),
    )
  })
  it('should return default value in pair if could not parse line', () => {
    jest.mock('../../../../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        tokenize: jest.fn().mockReturnValue([]),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    expect(parseSingleAddressLine('some address')).toStrictEqual(
      new YSPair<Nullable<string>, Nullable<string>>(null, ''),
    )
  })
  it('should return default value in pair if input is null', () => {
    jest.mock('../../../../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        tokenize: jest.fn().mockReturnValue([]),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      MockStorage(),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    expect(parseSingleAddressLine(null)).toStrictEqual(new YSPair<Nullable<string>, Nullable<string>>(null, ''))
    Models.drop()
  })
  it('should return default value in pair if input is empty', () => {
    jest.mock('../../../../../code/registry')
    const mockLocateFunction = jest.fn().mockReturnValue({
      locate: jest.fn().mockReturnValue({
        tokenize: jest.fn().mockReturnValue([]),
      }),
    })
    Registry.getServiceLocator = mockLocateFunction.bind(Registry)
    expect(parseSingleAddressLine('')).toStrictEqual(new YSPair<Nullable<string>, Nullable<string>>(null, ''))
  })
})
describe(fillReplyForwardOptions, () => {
  it('should set reply field fo ReplyType.REPLY', () => {
    const builder = new MailSendRequestBuilder()
    fillReplyForwardOptions(ReplyType.REPLY, int64(123), builder, new TestIDSupport())
    const request = builder.build()
    expect(request.reply).toBe('123')
  })
  it('should set forward field fo ReplyType.FORWARD', () => {
    const builder = new MailSendRequestBuilder()
    fillReplyForwardOptions(ReplyType.FORWARD, int64(123), builder, new TestIDSupport())
    const request = builder.build()
    expect(request.forward).toBe('123')
  })
  it('should set forward field fo ReplyType.TEMPLATE', () => {
    const builder = new MailSendRequestBuilder()
    fillReplyForwardOptions(ReplyType.TEMPLATE, int64(123), builder, new TestIDSupport())
    const request = builder.build()
    expect(request.templateBase).toBe('123')
  })
  it('should not set any field fo ReplyType.NONE', () => {
    const builder = new MailSendRequestBuilder()
    fillReplyForwardOptions(ReplyType.NONE, int64(123), builder, new TestIDSupport())
    const request = builder.build()
    expect(request.templateBase).toBeNull()
    expect(request.reply).toBe(null)
    expect(request.forward).toBe(null)
  })
})
