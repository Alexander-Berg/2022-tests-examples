import { resolve } from '../../../../../../common/xpromise-support'
import { int64, YSError } from '../../../../../../common/ys'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../common/__tests__/__helpers__/mock-patches'
import { MapJSONItem } from '../../../../../common/code/json/json-types'
import { getVoid, Result } from '../../../../../common/code/result/result'
import { MessageMeta } from '../../../../../mapi/../mapi/code/api/entities/message/message-meta'
import { MessageTypeFlags } from '../../../../../mapi/../mapi/code/api/entities/message/message-type'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { idToString } from '../../../../../mapi/code/api/common/id'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { MailSendRequest } from '../../../../../mapi/code/api/entities/draft/mail-send-request'
import { Models } from '../../../../../xmail/code/models'
import { Registry } from '../../../../../xmail/code/registry'
import { TaskType } from '../../../../../xmail/code/service/task'
import { AbstractDraftTask } from '../../../../../xmail/code/service/tasks/abstract-draft-task'
import { ServiceLocatorItems } from '../../../../../xmail/code/utils/service-locator'
import {
  MockHighPrecisionTimer,
  MockStorage,
  MockWithinTransaction,
} from '../../../../../xmail/__tests__/__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../../../xmail/__tests__/__helpers__/test-id-support'
import { AttachmentSizes } from '../../../../code/busilogics/draft/attachment-sizes'
import { DraftDataWrapper, ReplyType } from '../../../../code/busilogics/draft/draft-data-wrapper'
import { Rfc822Token } from '../../../../code/service/rfc822-tokenizer'

describe(AbstractDraftTask, () => {
  describe('fromJSONItem', () => {
    it('should return InnerBaseDraftTask crated from json', async (done) => {
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
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      const draftDataJson = new MapJSONItem()
        .putInt64('accountId', int64(1234))
        .putInt64('draftId', int64(333))
        .putString('action', 'someAction')
        .putString('from', 'me@yandex.ru')
        .putString('to', 'you@yandex.ru')
        .putString('subject', 'really important subject')
        .putString('body', 'body body body')
        .putInt32('replyType', 1)
        .putInt64('replyMid', int64(98765))
        .putInt64('baseMessageId', int64(5555))
      const baseInnerTaskJson = new MapJSONItem()
        .putInt32('version', 1)
        .putInt64('uid', int64(12345))
        .putInt64('revision', int64(2))
        .put('draftData', draftDataJson)
      const baseInnerTask = (await AbstractDraftTask.fromJSONItem(
        baseInnerTaskJson,
        Models.instance(),
      )) as AbstractDraftTask
      expect(baseInnerTask).not.toBeNull()
      expect(baseInnerTask.version).toBe(1)
      expect(baseInnerTask.uid).toBe(int64(12345))
      expect(baseInnerTask.getType()).toBe(TaskType.saveDraft)
      expect(baseInnerTask.getDraftId()).toBe(int64(333))
      expect(baseInnerTask.draftData).toStrictEqual(
        new DraftDataWrapper(
          int64(1234),
          int64(333),
          'someAction',
          'me@yandex.ru',
          'you@yandex.ru',
          null,
          null,
          'really important subject',
          'body body body',
          null,
          null,
          ReplyType.REPLY,
          int64(98765),
          int64(5555),
        ),
      )
      baseInnerTask
        .sendToServerWithMailSendRequest(
          new MailSendRequest(
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
          ),
        )
        .then((networkStatus) => {
          expect(networkStatus).toBeNull()
          Models.drop()
          done()
        })
    })
    it('should return null if base task could not be serialized', async () => {
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
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      const draftDataJson = new MapJSONItem()
        .putInt64('accountId', int64(1234))
        .putInt64('draftId', int64(333))
        .putString('action', 'someAction')
        .putString('from', 'me@yandex.ru')
        .putString('to', 'you@yandex.ru')
        .putString('subject', 'really important subject')
        .putString('body', 'body body body')
        .putInt32('replyType', 1)
        .putInt64('replyMid', int64(98765))
        .putInt64('baseMessageId', int64(5555))
      const baseInnerTaskJson = new MapJSONItem()
        .putInt64('uid', int64(12345))
        .putInt64('revision', int64(2))
        .put('draftData', draftDataJson)
      const baseInnerTask = await AbstractDraftTask.fromJSONItem(baseInnerTaskJson, Models.instance())
      expect(baseInnerTask).toBeNull()
      Models.drop()
    })
    it('should return null if revision field is missing in json', async () => {
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
      const draftDataJson = new MapJSONItem()
        .putInt64('accountId', int64(1234))
        .putInt64('draftId', int64(333))
        .putString('action', 'someAction')
        .putString('from', 'me@yandex.ru')
        .putString('to', 'you@yandex.ru')
        .putString('subject', 'really important subject')
        .putString('body', 'body body body')
        .putInt32('replyType', 1)
        .putInt64('replyMid', int64(98765))
        .putInt64('baseMessageId', int64(5555))
      const baseInnerTaskJson = new MapJSONItem()
        .putInt32('version', 1)
        .putInt64('uid', int64(12345))
        .put('draftData', draftDataJson)
      const baseInnerTask = await AbstractDraftTask.fromJSONItem(baseInnerTaskJson, Models.instance())
      expect(baseInnerTask).toBeNull()
      Models.drop()
    })
    it('should return null if draftData field is missing in json', async () => {
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
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      const baseInnerTaskJson = new MapJSONItem()
        .putInt32('version', 1)
        .putInt64('uid', int64(12345))
        .putInt64('revision', int64(2))
      const baseInnerTask = await AbstractDraftTask.fromJSONItem(baseInnerTaskJson, Models.instance())
      expect(baseInnerTask).toBeNull()
      Models.drop()
    })
  })
  describe('serialize', () => {
    it('should be serializable to json', async () => {
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        }),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      const draftDataJson = new MapJSONItem()
        .putString('accountId', idToString(int64(1234))!)
        .putString('draftId', idToString(int64(333))!)
        .putString('action', 'someAction')
        .putString('from', 'me@yandex.ru')
        .putString('to', 'you@yandex.ru')
        .putString('subject', 'really important subject')
        .putString('body', 'body body body')
        .putInt32('replyType', 1)
        .putString('replyMid', idToString(int64(98765))!)
        .putString('baseMessageId', idToString(int64(5555))!)
      const baseInnerTaskJson = new MapJSONItem()
        .putInt32('version', 1)
        .putInt64('uid', int64(12345))
        .putInt64('revision', int64(2))
        .put('draftData', draftDataJson)
      const baseInnerTask = (await AbstractDraftTask.fromJSONItem(
        baseInnerTaskJson,
        Models.instance(),
      )) as AbstractDraftTask
      const serializedJson = baseInnerTask.serialize()
      baseInnerTaskJson.putInt32('taskType', TaskType.saveDraft)
      expect(serializedJson).toStrictEqual(baseInnerTaskJson)
      Models.drop()
    })
  })
  it('should remove pending operation on successfull sending data to server', async (done) => {
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      }),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    Models.instance().drafts().removePendingOp = jest.fn().mockReturnValue(resolve(getVoid()))
    Models.instance().drafts().getMidByDidOrReject = jest.fn().mockReturnValue(resolve(int64(1)))
    Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
    Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
    Models.instance().draftAttachments().pinDiskAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
    Models.instance().draftAttachments().cleanDraftAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
    Models.instance().draftAttachments().deleteTempFiles = jest.fn().mockReturnValue(resolve(getVoid()))
    const task = await createSampleAbstractDraftTask()
    task.onSuccess().then((_) => {
      expect(Models.instance().drafts().removePendingOp).toBeCalledWith(int64(333), int64(2))
      Models.drop()
      done()
    })
  })
  describe('sendDataToServer', () => {
    it('should create network request, send data to server and not notify and not call onSuccess if network status is not ok', async (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue(createMockLocate())
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        }),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().messages().messageMetaByMid = jest.fn().mockReturnValue(resolve({}))
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      Models.instance().drafts().getMidByDidOrReject = jest.fn().mockReturnValue(resolve(int64(1)))
      Models.instance().draftAttachments().pinDiskAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().cleanDraftAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().deleteTempFiles = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().getPinnedNonDiskAttachesHids = jest.fn().mockReturnValue(resolve([]))
      Models.instance().composeStore().loadBody = jest.fn().mockReturnValue(resolve('some body'))
      const mockNotifyScheduleCheckAttachesInLastSend = jest.spyOn(
        Models.instance().taskActionsNotifier,
        'notifyScheduleCheckAttachesInLastSend',
      )
      const sampleTask = await createSampleAbstractDraftTask()
      const mockOnSuccess = jest.spyOn(sampleTask, 'onSuccess')
      sampleTask.sendToServerWithMailSendRequest = jest
        .fn()
        .mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.permanentError, null, null)))
      sampleTask.sendDataToServer().then((status) => {
        expect(mockNotifyScheduleCheckAttachesInLastSend).not.toBeCalled()
        expect(mockOnSuccess).not.toBeCalled()
        expect(Models.instance().drafts().getMidByDidOrReject).toBeCalledWith(int64(333))
        Models.drop()
        done()
      })
    })
    // tslint:disable-next-line:max-line-length
    it('should create network request, send data to server and not notify and not call onSuccess if network status is ok', async (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue(createMockLocate())
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        }),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().messages().messageMetaByMid = jest.fn().mockReturnValue(resolve({}))
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      Models.instance().drafts().getMidByDidOrReject = jest.fn().mockReturnValue(resolve(int64(1)))
      Models.instance().drafts().removePendingOp = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().pinDiskAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().cleanDraftAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().deleteTempFiles = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().draftAttachments().getPinnedNonDiskAttachesHids = jest.fn().mockReturnValue(resolve([]))
      Models.instance().composeStore().loadBody = jest.fn().mockReturnValue(resolve('some body'))
      const mockNotifyScheduleCheckAttachesInLastSend = jest.spyOn(
        Models.instance().taskActionsNotifier,
        'notifyScheduleCheckAttachesInLastSend',
      )
      const sampleTask = await createSampleAbstractDraftTask()
      const mockOnSuccess = jest.spyOn(sampleTask, 'onSuccess')
      sampleTask.sendToServerWithMailSendRequest = jest
        .fn()
        .mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok, null, null)))
      sampleTask.sendDataToServer().then((status) => {
        expect(mockNotifyScheduleCheckAttachesInLastSend).not.toBeCalled()
        expect(mockOnSuccess).toBeCalled()
        expect(Models.instance().drafts().getMidByDidOrReject).toBeCalledWith(int64(333))
        expect(Models.instance().storage.withinTransaction).toBeCalled()
        expect(Models.instance().drafts().removePendingOp).toBeCalledWith(int64(333), int64(2))
        Models.drop()
        done()
      })
    })
  })
  describe('updateDatabase', () => {
    it('should throw error if mid from Draft.draftId was not found', async (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue(createMockLocate())
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        }),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().drafts().getMidByDid = jest.fn().mockReturnValue(resolve(null))
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      const sampleTask = await createSampleAbstractDraftTask()
      sampleTask.updateDatabase().failed((err) => {
        expect(err).toStrictEqual(new YSError('Not found mid for did=333'))
        Models.drop()
        done()
      })
    })
    it('should throw error of MessageMeta for mid could not be found', async (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue(createMockLocate())
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        }),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().drafts().getMidByDid = jest.fn().mockReturnValue(resolve(int64(11)))
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      Models.instance().messages().messageMetaByMid = jest.fn().mockReturnValue(resolve(null))
      const sampleTask = await createSampleAbstractDraftTask()
      sampleTask.updateDatabase().failed((err) => {
        expect(err).toStrictEqual(new TypeError("Cannot read property 'subjectPrefix' of null"))
        Models.drop()
        done()
      })
    })
    it('should get mid from draftId, then find MessageMeta by mid, modify it and insert in DB', async (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue(createMockLocate())
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        }),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().recipients().insertRecipientsForSearch = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().drafts().getMidByDid = jest.fn().mockReturnValue(resolve(int64(11)))
      Models.instance().draftAttachments().getUploadedDraftAttaches = jest.fn().mockReturnValue(resolve([]))
      Models.instance().draftAttachments().getDiskAttachesOfDraft = jest.fn().mockReturnValue(resolve([]))
      Models.instance().serializer.serialize = jest
        .fn()
        .mockReturnValue(new Result<string>('some valid serialized json', null))
      Models.instance().messages().insertMessages = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().insertBodyMetaFlat = jest.fn().mockReturnValue(resolve(getVoid()))
      const messageMeta = new MessageMeta(
        int64(11),
        int64(22),
        int64(33),
        [],
        false,
        'Re: ',
        'some subject',
        'some first line',
        'me@yandex.ru',
        false,
        false,
        null,
        int64(123456789),
        false,
        null,
        MessageTypeFlags.delivery,
      )
      Models.instance().messages().messageMetaByMid = jest.fn().mockReturnValue(resolve(messageMeta))
      const sampleTask = await createSampleAbstractDraftTask()
      sampleTask.updateDatabase().then((_) => {
        expect(Models.instance().drafts().getMidByDid).toBeCalledWith(int64(333))
        expect(Models.instance().messages().messageMetaByMid).toHaveBeenNthCalledWith(1, int64(11))
        expect(Models.instance().messages().messageMetaByMid).toHaveBeenNthCalledWith(2, int64(98765))
        expect(Models.instance().recipients().insertRecipientsForSearch).toBeCalledTimes(2)
        expect(Models.instance().messages().insertMessages).toBeCalledWith([
          messageMeta
            .toBuilder()
            .setFirstLine('body body body')
            .setSubjText('really important subject')
            .setSubjPrefix('')
            .build(),
        ])
        expect(Models.instance().messages().insertBodyMetaFlat).toHaveBeenCalledWith(
          int64(11),
          'some valid serialized json',
          null,
          null,
          'text/plain',
          'mis',
        )
        Models.drop()
        done()
      })
    })
  })
})

function createMockLocate(): any {
  return {
    locate: jest.fn().mockImplementation((serviceLocatorItem) => {
      if (serviceLocatorItem === ServiceLocatorItems.handler) {
        return {
          destroy: jest.fn().mockReturnValue(getVoid()),
          hasMessages: jest.fn().mockReturnValue(false),
          post: jest.fn().mockImplementation((toPost) => {
            toPost()
            return resolve(getVoid())
          }),
        }
      } else if (serviceLocatorItem === ServiceLocatorItems.concurrentHashMap) {
        return {
          get: jest.fn().mockReturnValue(null),
          put: jest.fn().mockReturnValue(null),
          remove: jest.fn().mockReturnValue(null),
        }
      } else if (serviceLocatorItem === ServiceLocatorItems.reentrantLock) {
        return {
          executeInLock: jest.fn().mockImplementation((toWrap) => {
            toWrap()
            return resolve(getVoid())
          }),
        }
      } else if (serviceLocatorItem === ServiceLocatorItems.rfc822Tokenizer) {
        return {
          tokenize: jest
            .fn()
            .mockReturnValue([])
            .mockReturnValueOnce([new Rfc822Token('Me', 'me@yandex.ru', null)])
            .mockReturnValueOnce([new Rfc822Token('You', 'you@yandex.ru', null)]),
        }
      }
      return null
    }),
  }
}

async function createSampleAbstractDraftTask(): Promise<AbstractDraftTask> {
  const draftDataJson = new MapJSONItem()
    .putString('accountId', idToString(int64(1234))!)
    .putString('draftId', idToString(int64(333))!)
    .putString('action', 'someAction')
    .putString('from', 'me@yandex.ru')
    .putString('to', 'you@yandex.ru')
    .putString('subject', 'really important subject')
    .putString('body', 'body body body')
    .putInt32('replyType', 1)
    .putString('replyMid', idToString(int64(98765))!)
    .putString('baseMessageId', idToString(int64(5555))!)
  const baseInnerTaskJson = new MapJSONItem()
    .putInt32('version', 1)
    .putInt64('uid', int64(12345))
    .putInt64('revision', int64(2))
    .put('draftData', draftDataJson)
  return (await AbstractDraftTask.fromJSONItem(baseInnerTaskJson, Models.instance())) as AbstractDraftTask
}
