import { resolve } from '../../../../../../common/xpromise-support'
import { int64, YSError } from '../../../../../../common/ys'
import { ArrayJSONItem, MapJSONItem } from '../../../../../common/code/json/json-types'
import { getVoid } from '../../../../../common/code/result/result'
import { SaveMailNetworkRequest } from '../../../../../mapi/code/api/entities/actions/save-mail-network-request'
import { SendMailNetworkRequest } from '../../../../../mapi/code/api/entities/actions/send-mail-network-request'
import { MailSendRequest, MailSendRequestBuilder } from '../../../../../mapi/code/api/entities/draft/mail-send-request'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { saveDraftResponseFromJSONItem } from '../../../../../xmail/../mapi/code/api/entities/draft/save-draft-response'
import { Attachments, MessageMeta } from '../../../../../xmail/../mapi/code/api/entities/message/message-meta'
import { MessageTypeFlags } from '../../../../../xmail/../mapi/code/api/entities/message/message-type'
import { NetworkStatusCode } from '../../../../../xmail/../mapi/code/api/entities/status/network-status'
import { Models } from '../../../../../xmail/code/models'
import { Registry } from '../../../../../xmail/code/registry'
import {
  MockCursorWithArray,
  MockHighPrecisionTimer,
  MockStorage,
  MockStorageStatement,
  MockWithinTransaction,
} from '../../../../../xmail/__tests__/__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../../../xmail/__tests__/__helpers__/test-id-support'
import { MessageBodyMeta } from '../../../../code/api/entities/body/message-body-meta'
import { AttachmentSizes } from '../../../../code/busilogics/draft/attachment-sizes'
import { DraftAttachments } from '../../../../code/busilogics/draft/draft-attachments'
import { Drafts } from '../../../../code/busilogics/draft/drafts'
import { MockModels } from '../../../__helpers__/models'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../common/__tests__/__helpers__/mock-patches'

describe(Drafts, () => {
  it('should send correct network request and parse SaveDraftResponse from response in sendSaveDraftAction', (done) => {
    const network = MockNetwork()
    const responseJson = new MapJSONItem()
      .put('status', new MapJSONItem().putInt32('status', NetworkStatusCode.ok))
      .putInt64('mid', int64(111))
      .putInt64('fid', int64(222))
      .put('attachments', new MapJSONItem().put('attachment', new ArrayJSONItem([])))
    network.execute = jest.fn().mockReturnValue(resolve(responseJson))
    Models.setupInstance(
      'body-dir',
      network,
      MockStorage(),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    const drafts = new Drafts(Models.instance())
    const mailSendRequest: MailSendRequest = new MailSendRequestBuilder()
      .setComposeCheck('composeCheck')
      .setCc('second_friend@yandex.ru')
      .setBcc('hidden_frient@yandex.ru')
      .setBody('Hello! This is Body')
      .setInReplyTo('in reply to mail')
      .setParts(['part1', 'part2'])
      .setReply('reply')
      .setForward('forward')
      .setAttachIds(['attachId1', 'attachId2'])
      .setSubject('Very important subject')
      .setTo('friend@yandex.ru')
      .setFromName('Me')
      .setFromMailbox('me@yandex.ru')
      .build()
    drafts.sendSaveDraftAction(mailSendRequest).then((response) => {
      expect(network.execute).toBeCalledWith(new SaveMailNetworkRequest(mailSendRequest))
      expect(response).toStrictEqual(saveDraftResponseFromJSONItem(responseJson))
      Models.drop()
      done()
    })
  })
  it('should send correct network request and parse SaveDraftResponse from response in sendSendMailAction', (done) => {
    const network = MockNetwork()
    const responseJson = new MapJSONItem()
      .put('status', new MapJSONItem().putInt32('status', NetworkStatusCode.ok))
      .putInt64('mid', int64(111))
      .putInt64('fid', int64(222))
      .put('attachments', new MapJSONItem().put('attachment', new ArrayJSONItem([])))
    network.execute = jest.fn().mockReturnValue(resolve(responseJson))
    Models.setupInstance(
      'body-dir',
      network,
      MockStorage(),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    const drafts = new Drafts(Models.instance())
    const mailSendRequest: MailSendRequest = new MailSendRequestBuilder()
      .setComposeCheck('composeCheck')
      .setCc('second_friend@yandex.ru')
      .setBcc('hidden_frient@yandex.ru')
      .setBody('Hello! This is Body')
      .setInReplyTo('in reply to mail')
      .setParts(['part1', 'part2'])
      .setReply('reply')
      .setForward('forward')
      .setAttachIds(['attachId1', 'attachId2'])
      .setSubject('Very important subject')
      .setTo('friend@yandex.ru')
      .setFromName('Me')
      .setFromMailbox('me@yandex.ru')
      .build()
    drafts.sendSendMailAction(mailSendRequest).then((response) => {
      expect(network.execute).toBeCalledWith(new SendMailNetworkRequest(mailSendRequest))
      expect(response).toStrictEqual(saveDraftResponseFromJSONItem(responseJson))
      Models.drop()
      done()
    })
  })
  describe('getMidByDidOrReject', () => {
    it('should throw error if no mid found for specified draftId', (done) => {
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
      const drafts = new Drafts(Models.instance())
      drafts.getMidByDid = jest.fn().mockReturnValue(resolve(null))
      drafts.getMidByDidOrReject(int64(123)).failed((err) => {
        expect(err).toStrictEqual(new YSError('Not found mid for did=123'))
        Models.drop()
        done()
      })
    })
    it('should return mid found by getMidByDid', (done) => {
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
      const drafts = new Drafts(Models.instance())
      const draftId = int64(111)
      drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
      drafts.getMidByDidOrReject(int64(123)).then((id) => {
        expect(id).toBe(draftId)
        Models.drop()
        done()
      })
    })
  })
  it('should execute correct request to DB and notify pending_compose_ops table in removePendingOp', (done) => {
    const mockStorage = MockStorage({
      notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      mockStorage,
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    const drafts = new Drafts(Models.instance())
    const draftId = int64(111)
    drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
    drafts.removePendingOp(int64(123), int64(1)).then((_) => {
      expect(mockStorage.runStatement).toBeCalledWith(
        'DELETE FROM pending_compose_ops WHERE did = 123 AND revision = 1;',
      )
      expect(mockStorage.notifyAboutChanges).toBeCalledWith(['pending_compose_ops'])
      Models.drop()
      done()
    })
  })
  // tslint:disable-next-line:max-line-length
  it('should execute correct request to DB and notify pending_compose_ops table in removeAllPendingOpsByDid', (done) => {
    const mockStorage = MockStorage({
      notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      mockStorage,
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    const drafts = new Drafts(Models.instance())
    const draftId = int64(111)
    drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
    drafts.removeAllPendingOpsByDid(int64(123)).then((_) => {
      expect(mockStorage.runStatement).toBeCalledWith('DELETE FROM pending_compose_ops WHERE did = 123;')
      expect(mockStorage.notifyAboutChanges).toBeCalledWith(['pending_compose_ops'])
      Models.drop()
      done()
    })
  })
  it('should execute correct request to DB and notify pending_compose_ops table in hardDeleteDraftEntry', (done) => {
    const mockStorage = MockStorage({
      notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      mockStorage,
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    const drafts = new Drafts(Models.instance())
    const draftId = int64(111)
    drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
    drafts.hardDeleteDraftEntry(int64(123)).then((_) => {
      expect(mockStorage.runStatement).toBeCalledWith('DELETE FROM draft_entry WHERE did = 123;')
      expect(mockStorage.notifyAboutChanges).toBeCalledWith(['draft_entry'])
      Models.drop()
      done()
    })
  })
  describe('updateMid', () => {
    it('should return immediately if new mid equals old mid', (done) => {
      const mockStorage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        runQuery: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        mockStorage,
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      const drafts = new Drafts(Models.instance())
      const draftId = int64(111)
      drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
      drafts.updateMid(int64(123), int64(123)).then((_) => {
        expect(mockStorage.runStatement).not.toBeCalled()
        expect(mockStorage.runQuery).not.toBeCalled()
        expect(mockStorage.notifyAboutChanges).not.toBeCalled()
        Models.drop()
        done()
      })
    })
    // tslint:disable-next-line:max-line-length
    it('should update messagesMetas and bodies and update messages-to-folders connections from old mid to new mid', (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue({
          get: jest.fn().mockReturnValue(''),
          put: jest.fn().mockReturnValue(''),
          remove: jest.fn().mockReturnValue(true),
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const mockStorage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        runQuery: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const fileSystem = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
        exists: jest.fn().mockReturnValue(resolve(true)),
        move: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        mockStorage,
        MockJSONSerializer(),
        fileSystem,
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().messages().fetchMessageMetasByMids = jest
        .fn()
        .mockReturnValue(
          resolve([
            new MessageMeta(
              int64(123),
              int64(1),
              int64(2),
              ['lab1l'],
              false,
              null,
              'subjText',
              'some first line',
              'me@yandex.ru',
              false,
              false,
              null,
              int64(123456),
              false,
              new Attachments([]),
              MessageTypeFlags.people,
            ),
          ]),
        )
      Models.instance().messages().deleteEntriesByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().deleteMessageBodyMetasByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().deleteMessageBodyMetasByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().updateMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().updateMessageBodyMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().recipients().deleteRecipientsByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().folders().updateFolderMessagesConnection = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().folders().fetchFirstFidByType = jest.fn().mockReturnValue(resolve(int64(22)))
      Models.instance().bodyStore().getMessageDirectoryPath = jest.fn().mockImplementation((mid) => `path_${mid}`)
      Models.instance().composeStore().removeFromPendingSaveQueue = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().composeStore().updateMid = jest.fn().mockReturnValue(resolve(getVoid()))

      Models.instance().messages().fetchMessagesBodyMetasByMids = jest
        .fn()
        .mockReturnValue(
          resolve([new MessageBodyMeta(int64(123), 'someRecipients', 'someRfcId', 'someRef', 'text/plain', null)]),
        )
      const drafts = new Drafts(Models.instance())
      const draftId = int64(111)
      drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
      drafts.updateMid(int64(123), int64(456)).then((_) => {
        const modelsInstance = Models.instance()
        expect(modelsInstance.messages().fetchMessageMetasByMids).toBeCalledWith([int64(123)])
        expect(modelsInstance.messages().deleteEntriesByMids).toBeCalledWith([int64(456)])
        expect(modelsInstance.messages().fetchMessagesBodyMetasByMids).toBeCalledWith([int64(123)])
        expect(modelsInstance.messages().deleteMessageBodyMetasByMids).toBeCalledWith([int64(456)])
        expect(modelsInstance.recipients().deleteRecipientsByMids).toBeCalledWith([int64(456)])
        expect(modelsInstance.messages().updateMid).toBeCalledWith(int64(123), int64(456))
        expect(modelsInstance.messages().updateMessageBodyMid).toBeCalledWith(int64(123), int64(456))
        expect(modelsInstance.folders().updateFolderMessagesConnection).toBeCalledWith(
          int64(123),
          int64(456),
          int64(22),
        )
        expect(modelsInstance.bodyStore().getMessageDirectoryPath).toHaveBeenNthCalledWith(1, int64(456))
        expect(modelsInstance.bodyStore().getMessageDirectoryPath).toHaveBeenNthCalledWith(2, int64(123))
        expect(modelsInstance.fileSystem.exists).toBeCalledWith('path_123')
        expect(modelsInstance.fileSystem.delete).toBeCalledWith('path_456')
        expect(modelsInstance.composeStore().removeFromPendingSaveQueue).toBeCalledWith(int64(456))
        Models.drop()
        done()
      })
    })
    it('should skip updating message meta and bodies if meta and body for new mid was not found', (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue({
          get: jest.fn().mockReturnValue(''),
          put: jest.fn().mockReturnValue(''),
          remove: jest.fn().mockReturnValue(true),
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const mockStorage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        runQuery: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const fileSystem = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
        exists: jest.fn().mockReturnValue(resolve(true)),
        move: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        mockStorage,
        MockJSONSerializer(),
        fileSystem,
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().messages().fetchMessageMetasByMids = jest.fn().mockReturnValue(resolve([]))
      Models.instance().messages().deleteEntriesByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().deleteMessageBodyMetasByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().deleteMessageBodyMetasByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().updateMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().updateMessageBodyMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().recipients().deleteRecipientsByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().folders().updateFolderMessagesConnection = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().folders().fetchFirstFidByType = jest.fn().mockReturnValue(resolve(int64(22)))
      Models.instance().bodyStore().getMessageDirectoryPath = jest.fn().mockImplementation((mid) => `path_${mid}`)
      Models.instance().composeStore().removeFromPendingSaveQueue = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().composeStore().updateMid = jest.fn().mockReturnValue(resolve(getVoid()))

      Models.instance().messages().fetchMessagesBodyMetasByMids = jest.fn().mockReturnValue(resolve([]))
      const drafts = new Drafts(Models.instance())
      const draftId = int64(111)
      drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
      drafts.updateMid(int64(123), int64(456)).then((_) => {
        const modelsInstance = Models.instance()
        expect(modelsInstance.messages().fetchMessageMetasByMids).toBeCalledWith([int64(123)])
        expect(modelsInstance.messages().deleteEntriesByMids).not.toBeCalledWith([int64(456)])
        expect(modelsInstance.messages().fetchMessagesBodyMetasByMids).toBeCalledWith([int64(123)])
        expect(modelsInstance.messages().deleteMessageBodyMetasByMids).not.toBeCalledWith([int64(456)])
        expect(modelsInstance.recipients().deleteRecipientsByMids).not.toBeCalledWith([int64(456)])
        expect(modelsInstance.messages().updateMid).toBeCalledWith(int64(123), int64(456))
        expect(modelsInstance.messages().updateMessageBodyMid).toBeCalledWith(int64(123), int64(456))
        expect(modelsInstance.folders().updateFolderMessagesConnection).toBeCalledWith(
          int64(123),
          int64(456),
          int64(22),
        )
        expect(modelsInstance.bodyStore().getMessageDirectoryPath).toHaveBeenNthCalledWith(1, int64(456))
        expect(modelsInstance.bodyStore().getMessageDirectoryPath).toHaveBeenNthCalledWith(2, int64(123))
        expect(modelsInstance.fileSystem.exists).toBeCalledWith('path_123')
        expect(modelsInstance.fileSystem.delete).toBeCalledWith('path_456')
        expect(modelsInstance.composeStore().removeFromPendingSaveQueue).toBeCalledWith(int64(456))
        Models.drop()
        done()
      })
    })
    it('should skip updating message meta and bodies if meta and body for new mid was not found', (done) => {
      jest.mock('../../../../code/registry')
      const mockLocateFunction = jest.fn().mockReturnValue({
        locate: jest.fn().mockReturnValue({
          get: jest.fn().mockReturnValue(''),
          put: jest.fn().mockReturnValue(''),
          remove: jest.fn().mockReturnValue(true),
        }),
      })
      Registry.getServiceLocator = mockLocateFunction.bind(Registry)
      const mockStorage = MockStorage({
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        runQuery: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const fileSystem = MockFileSystem({
        delete: jest.fn().mockReturnValue(resolve(getVoid())),
        exists: jest.fn().mockReturnValue(resolve(false)),
        move: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        mockStorage,
        MockJSONSerializer(),
        fileSystem,
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      Models.instance().messages().fetchMessageMetasByMids = jest.fn().mockReturnValue(resolve([]))
      Models.instance().messages().deleteEntriesByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().deleteMessageBodyMetasByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().deleteMessageBodyMetasByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().updateMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().updateMessageBodyMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().recipients().deleteRecipientsByMids = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().folders().updateFolderMessagesConnection = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().folders().fetchFirstFidByType = jest.fn().mockReturnValue(resolve(int64(22)))
      Models.instance().bodyStore().getMessageDirectoryPath = jest.fn().mockImplementation((mid) => `path_${mid}`)
      Models.instance().composeStore().removeFromPendingSaveQueue = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().composeStore().updateMid = jest.fn().mockReturnValue(resolve(getVoid()))
      Models.instance().messages().fetchMessagesBodyMetasByMids = jest.fn().mockReturnValue(resolve([]))
      const drafts = new Drafts(Models.instance())
      const draftId = int64(111)
      drafts.getMidByDid = jest.fn().mockReturnValue(resolve(draftId))
      drafts.updateMid(int64(123), int64(456)).then((_) => {
        const modelsInstance = Models.instance()
        expect(modelsInstance.messages().fetchMessageMetasByMids).toBeCalledWith([int64(123)])
        expect(modelsInstance.messages().deleteEntriesByMids).not.toBeCalledWith([int64(456)])
        expect(modelsInstance.messages().fetchMessagesBodyMetasByMids).toBeCalledWith([int64(123)])
        expect(modelsInstance.messages().deleteMessageBodyMetasByMids).not.toBeCalledWith([int64(456)])
        expect(modelsInstance.recipients().deleteRecipientsByMids).not.toBeCalledWith([int64(456)])
        expect(modelsInstance.messages().updateMid).toBeCalledWith(int64(123), int64(456))
        expect(modelsInstance.messages().updateMessageBodyMid).toBeCalledWith(int64(123), int64(456))
        expect(modelsInstance.folders().updateFolderMessagesConnection).toBeCalledWith(
          int64(123),
          int64(456),
          int64(22),
        )
        expect(modelsInstance.bodyStore().getMessageDirectoryPath).toHaveBeenNthCalledWith(1, int64(456))
        expect(modelsInstance.bodyStore().getMessageDirectoryPath).toHaveBeenNthCalledWith(2, int64(123))
        expect(modelsInstance.fileSystem.exists).toBeCalledWith('path_123')
        expect(modelsInstance.fileSystem.move).toBeCalledWith('path_456', 'path_123')
        expect(modelsInstance.composeStore().updateMid).toBeCalledWith(int64(123), int64(456))
        Models.drop()
        done()
      })
    })
  })
  describe('getMidByDid', () => {
    it('should execute correct query and return null if no drafts for specififed draftId were found', (done) => {
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([])))
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      const drafts = new Drafts(Models.instance())
      drafts.getMidByDid(int64(123)).then((id) => {
        expect(storage.runQuery).toBeCalledWith('SELECT mid FROM draft_entry WHERE did = ?;', ['123'])
        expect(id).toBeNull()
        Models.drop()
        done()
      })
    })
    it('should execute correct query and return ID from first row from cursor', (done) => {
      const mockRunQuery = jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(123)], [int64(456)]])))
      const storage = MockStorage({
        runQuery: mockRunQuery,
      })
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      const drafts = new Drafts(Models.instance())
      drafts.getMidByDid(int64(123)).then((id) => {
        expect(storage.runQuery).toBeCalledWith('SELECT mid FROM draft_entry WHERE did = ?;', ['123'])
        expect(id).toBe(int64(123))
        Models.drop()
        done()
      })
    })
  })
  describe(Drafts.prototype.deleteDraftEntryIfActual, () => {
    it('should remove non-pending drafts by did and revision', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const idSupport = new TestIDSupport()
      const drafts = new Drafts(MockModels({ storage, idSupport }))
      drafts.deleteDraftEntryIfActual(int64(1), int64(2)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.draft_entry} WHERE did = 1 AND revision = 2 AND NOT EXISTS (SELECT * FROM ${EntityKind.pending_compose_ops} WHERE did = 1 AND revision = 2);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.draft_entry])
        done()
      })
    })
  })
  describe(Drafts.prototype.deleteDraftsByMids, () => {
    it('it should return immediatelly if mids is empty', (done) => {
      const models = MockModels()
      const drafts = new Drafts(models)
      drafts.deleteDraftsByMids([]).then((_) => {
        expect(models.storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('it should fetch drafts by mids, delete them and attaches if exist', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([['4'], ['5'], ['6']]))),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        deleteByDids: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const models = MockModels({ storage }, { draftAttachments })
      const drafts = new Drafts(models)
      const deleteDraftEntriesByDidsSpy = jest
        .spyOn(drafts, 'deleteDraftEntriesByDids')
        .mockReturnValue(resolve(getVoid()))
      drafts.deleteDraftsByMids([int64(1), int64(2), int64(3)]).then((_) => {
        expect(models.storage.runQuery).toBeCalledWith(
          `SELECT did FROM ${EntityKind.draft_entry} WHERE mid IN (1, 2, 3);`,
          [],
        )
        expect(deleteDraftEntriesByDidsSpy).toBeCalledWith([int64(4), int64(5), int64(6)])
        expect(draftAttachments.deleteByDids).toBeCalled()
        deleteDraftEntriesByDidsSpy.mockRestore()
        done()
      })
    })
    it('it should fetch drafts by mids, skip deleting them and attaches if not exist', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const models = MockModels({ storage })
      const drafts = new Drafts(models)
      const deleteDraftEntriesByDidsSpy = jest
        .spyOn(drafts, 'deleteDraftEntriesByDids')
        .mockReturnValue(resolve(getVoid()))
      drafts.deleteDraftsByMids([int64(1), int64(2), int64(3)]).then((_) => {
        expect(models.storage.runQuery).toBeCalledWith(
          `SELECT did FROM ${EntityKind.draft_entry} WHERE mid IN (1, 2, 3);`,
          [],
        )
        expect(deleteDraftEntriesByDidsSpy).not.toBeCalled()
        deleteDraftEntriesByDidsSpy.mockRestore()
        done()
      })
    })
  })
  describe(Drafts.prototype.deleteDraftEntriesByDids, () => {
    it('should return immediatelly if dids is empty', (done) => {
      const storage = MockStorage()
      const models = MockModels({ storage })
      const drafts = new Drafts(models)
      drafts.deleteDraftEntriesByDids([]).then((_) => {
        expect(models.storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should delete drafts by dids', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const models = MockModels({ storage })
      const drafts = new Drafts(models)
      drafts.deleteDraftEntriesByDids([int64(1), int64(2)]).then((_) => {
        expect(models.storage.runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.draft_entry} WHERE did IN (1, 2);`)
        expect(models.storage.notifyAboutChanges).toBeCalledWith([EntityKind.draft_entry])
        done()
      })
    })
  })
  describe(Drafts.prototype.updateURL, () => {
    it('should update draft_attach table with url by draft attach ID', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const drafts = new Drafts(MockModels({ storage }))
      drafts.updateURL(int64(1), 'url').then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `UPDATE ${EntityKind.draft_attach} SET temp_mul_or_disk_url = ?, uploaded = 1 WHERE attach_id = ?;`,
        )
        expect(statement.execute).toBeCalledWith(['url', '1'])
        expect(statement.close).toBeCalled()
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.draft_attach])
        done()
      })
    })
  })
})
