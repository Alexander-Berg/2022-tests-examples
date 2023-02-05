import { resolve } from '../../../../../common/xpromise-support'
import { int64 } from '../../../../../common/ys'
import { MockJSONSerializer, mockLogger } from '../../../../common/__tests__/__helpers__/mock-patches'
import { StringJSONItem } from '../../../../common/code/json/json-types'
import { Result } from '../../../../common/code/result/result'
import { JSONItemFromJSONString, JSONItemToJSONString } from '../../../../common/__tests__/__helpers__/json-helpers'
import { FolderType } from '../../../../mapi/../mapi/code/api/entities/folder/folder'
import { MessageMeta } from '../../../../mapi/../mapi/code/api/entities/message/message-meta'
import { ConstantIDs, ID } from '../../../../mapi/code/api/common/id'
import {
  SetParametersItem,
  SetParametersItems,
} from '../../../../mapi/code/api/entities/actions/set-parameters-request'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { DraftAttachments } from '../../../code/busilogics/draft/draft-attachments'
import { DraftDataWrapper, ReplyType } from '../../../code/busilogics/draft/draft-data-wrapper'
import { Registry } from '../../../code/registry'
import { CommandsServiceActions } from '../../../code/service/commands-service-actions'
import { CommandsServiceTaskDescriptor } from '../../../code/service/commands-service-delegate'
import { ArchiveTask } from '../../../code/service/tasks/archive-task'
import { ClearFolderTask } from '../../../code/service/tasks/clear-folder-task'
import { ClearMessagesTask } from '../../../code/service/tasks/clear-messages-task'
import { DeleteDraftEntryTask } from '../../../code/service/tasks/delete-draft-entry-task'
import { DeleteTask } from '../../../code/service/tasks/delete-task'
import { MarkReadTask } from '../../../code/service/tasks/mark-read-task'
import { MarkSpamTask } from '../../../code/service/tasks/mark-spam-task'
import { MarkWithLabelTask } from '../../../code/service/tasks/mark-with-label-task'
import { MoveToFolderTask } from '../../../code/service/tasks/move-to-folder-task'
import { MultiMarkWithLabelTaskApi } from '../../../code/service/tasks/multi-mark-with-label-task-api'
import { MultiMarkWithLabelTaskOffline } from '../../../code/service/tasks/multi-mark-with-label-task-offline'
import { PurgeTask } from '../../../code/service/tasks/purge-task'
import { SaveDraftTask } from '../../../code/service/tasks/save-draft-task'
import { SaveSignatureTask } from '../../../code/service/tasks/save-signature-task'
import { SendMailTask } from '../../../code/service/tasks/send-mail-task'
import { SetParametersTask } from '../../../code/service/tasks/set-parameters-task'
import { UploadAttachTask } from '../../../code/service/tasks/upload-attach-task'
import { makeFolders, makeMessages, MockModels } from '../../__helpers__/models'

describe(CommandsServiceActions, () => {
  beforeAll(() => {
    mockLogger()
  })
  afterAll(() => Registry.drop())
  describe(CommandsServiceActions.createAPITask, () => {
    it('should return MarkAsRead task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.markAsRead,
        new Map([['messageId', [int64(123), int64(456)]]]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MarkReadTask)
        const typedRes = res as MarkReadTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.markRead).toBe(true)
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return MarkAsUnread task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.markAsUnread,
        new Map([['messageId', [int64(123), int64(456)]]]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MarkReadTask)
        const typedRes = res as MarkReadTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.markRead).toBe(false)
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return Archive task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.archive,
        new Map([['messageId', [int64(123), int64(456)]]]),
      )
      const messages = makeMessages()
      const messageMetaByMidSpy = jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(123),
              int64(1),
              null,
              ['1', '2'],
              true,
              null,
              '',
              '',
              '',
              true,
              true,
              null,
              int64(12345),
              false,
              null,
              321,
            ),
          ),
        )
      CommandsServiceActions.createAPITask(
        descriptor,
        MockModels(undefined, {
          messages,
        }),
      ).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(ArchiveTask)
        const typedRes = res as ArchiveTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.currentFid).toBe(int64(1))
        expect(typedRes.models).not.toBeNull()
        messageMetaByMidSpy.mockRestore()
        done()
      })
    })
    it('should return Delete(from push) task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.delete,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['FROM_PUSH', true],
        ]),
      )
      const messages = makeMessages()
      const messageMetaByMidSpy = jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(123),
              int64(1),
              null,
              ['1', '2'],
              true,
              null,
              '',
              '',
              '',
              true,
              true,
              null,
              int64(12345),
              false,
              null,
              321,
            ),
          ),
        )
      const folders = makeFolders()
      const fetchFirstFidByTypeSpy = jest.spyOn(folders, 'fetchFirstFidByType').mockReturnValue(resolve(int64(2)))
      CommandsServiceActions.createAPITask(
        descriptor,
        MockModels(undefined, {
          messages,
          folders,
        }),
      ).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(DeleteTask)
        const typedRes = res as DeleteTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.currentFid).toBe(int64(1))
        expect(typedRes.targetFid).toBe(int64(2))
        expect(typedRes.fromPush).toBe(true)
        expect(typedRes.models).not.toBeNull()
        messageMetaByMidSpy.mockRestore()
        fetchFirstFidByTypeSpy.mockRestore()
        done()
      })
    })
    it('should return Delete(unspecified) task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.delete,
        new Map<string, any>([['messageId', [int64(123), int64(456)]]]),
      )
      const messages = makeMessages()
      const messageMetaByMidSpy = jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(123),
              int64(1),
              null,
              ['1', '2'],
              true,
              null,
              '',
              '',
              '',
              true,
              true,
              null,
              int64(12345),
              false,
              null,
              321,
            ),
          ),
        )
      const folders = makeFolders()
      const fetchFirstFidByTypeSpy = jest.spyOn(folders, 'fetchFirstFidByType').mockReturnValue(resolve(int64(2)))
      CommandsServiceActions.createAPITask(
        descriptor,
        MockModels(undefined, {
          messages,
          folders,
        }),
      ).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(DeleteTask)
        const typedRes = res as DeleteTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.currentFid).toBe(int64(1))
        expect(typedRes.targetFid).toBe(int64(2))
        expect(typedRes.fromPush).toBe(false)
        expect(typedRes.models).not.toBeNull()
        messageMetaByMidSpy.mockRestore()
        fetchFirstFidByTypeSpy.mockRestore()
        done()
      })
    })
    it('should return MarkMessageWithLabel task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.markMessageWithLabel,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['labelId', '5'],
          ['mark', true],
        ]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MarkWithLabelTask)
        const typedRes = res as MarkWithLabelTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.labelID).toBe('5')
        expect(typedRes.mark).toBe(true)
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return MultiMarkWithLabelTask task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.multiMarkMessageWithLabelsApi,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['labelId', ['5', '6']],
          ['mark', true],
        ]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MultiMarkWithLabelTaskApi)
        const typedRes = res as MultiMarkWithLabelTaskApi
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.labelIDs).toStrictEqual(['5', '6'])
        expect(typedRes.mark).toBe(true)
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return MoveToFolder task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.moveToFolder,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['folderId', '5'],
          ['currentFolderId', '1'],
        ]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MoveToFolderTask)
        const typedRes = res as MoveToFolderTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.targetFid).toBe(int64(5))
        expect(typedRes.currentFid).toBe(int64(1))
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return MarkAsSpam task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.markAsSpam,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['currentFolderId', '1'],
        ]),
      )
      const folders = makeFolders()
      const fetchFirstFidByTypeSpy = jest.spyOn(folders, 'fetchFirstFidByType').mockReturnValue(resolve(int64(2)))
      CommandsServiceActions.createAPITask(descriptor, MockModels(undefined, { folders })).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MarkSpamTask)
        const typedRes = res as MarkSpamTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.isSpam).toBe(true)
        expect(typedRes.currentFid).toBe(int64(1))
        expect(typedRes.targetFid).toBe(int64(2))
        expect(typedRes.models).not.toBeNull()
        fetchFirstFidByTypeSpy.mockRestore()
        done()
      })
    })
    it('should return MarkAsNotSpam task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.markAsNotSpam,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['currentFolderId', '2'],
        ]),
      )
      const folders = makeFolders()
      const fetchFirstFidByTypeSpy = jest.spyOn(folders, 'fetchFirstFidByType').mockImplementation((type) =>
        resolve(
          ({
            [FolderType.inbox]: int64(1),
            [FolderType.spam]: int64(2),
          } as any)[type],
        ),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels(undefined, { folders })).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MarkSpamTask)
        const typedRes = res as MarkSpamTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.isSpam).toBe(false)
        expect(typedRes.currentFid).toBe(int64(2))
        expect(typedRes.targetFid).toBe(int64(1))
        expect(typedRes.models).not.toBeNull()
        fetchFirstFidByTypeSpy.mockRestore()
        done()
      })
    })
    it('should return SaveSignature task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.saveSignature,
        new Map<string, any>([['signature', 'SIGNATURE']]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(SaveSignatureTask)
        const typedRes = res as SaveSignatureTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.signature).toBe('SIGNATURE')
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return ClearFolder task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.clearFolder,
        new Map<string, any>([['folderId', int64(1)]]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(ClearFolderTask)
        const typedRes = res as ClearFolderTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.targetFid).toBe(int64(1))
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return MultiMarkWithLabelTaskOffline task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.multiMarkWithLabelOffline,
        new Map<string, any>([
          ['messageId', [int64(123), int64(456)]],
          ['labelId', ['2', '3', '4']],
          [
            'marksMap',
            new Map([
              ['2', true],
              ['3', false],
              ['4', true],
            ]),
          ],
        ]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(MultiMarkWithLabelTaskOffline)
        const typedRes = res as MultiMarkWithLabelTaskOffline
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.labelIDs).toStrictEqual(['2', '3', '4'])
        expect(typedRes.markMap).toStrictEqual(
          new Map([
            ['2', true],
            ['3', false],
            ['4', true],
          ]),
        )
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return Purge task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.purge,
        new Map<string, any>([['messageId', [int64(123), int64(456)]]]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(PurgeTask)
        const typedRes = res as PurgeTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return ClearMessages task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.clearMessages,
        new Map<string, any>([['messageId', [int64(123), int64(456)]]]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(ClearMessagesTask)
        const typedRes = res as ClearMessagesTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.messageIDs).toStrictEqual([int64(123), int64(456)])
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return SendMessage task', (done) => {
      const draftWrapper = new DraftDataWrapper(
        int64(1),
        int64(2),
        'ACTION',
        'FROM',
        'TO',
        'CC',
        'BCC',
        'SUBJ',
        'BODY',
        'RFCID',
        'REFS',
        ReplyType.REPLY,
        int64(3),
        int64(4),
      )
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.sendMail,
        new Map<string, any>([
          ['draftRevision', int64(5)],
          ['draftIDATA', JSONItemToJSONString(draftWrapper.asJSONItem())],
        ]),
      )
      const serializer = MockJSONSerializer()
      serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
      const models = MockModels(
        { serializer },
        {
          draftAttachments: createMockInstance(DraftAttachments, {
            getDiskAttachesOfDraft: jest.fn().mockReturnValue(resolve([])),
            getUploadedDraftAttaches: jest.fn().mockReturnValue(resolve([])),
          }),
        },
      )
      CommandsServiceActions.createAPITask(descriptor, models).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(SendMailTask)
        const typedRes = res as SendMailTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.revision).toBe(int64(5))
        expect(typedRes.draftData).toStrictEqual(draftWrapper)
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return SaveDraft task', (done) => {
      const draftWrapper = new DraftDataWrapper(
        int64(1),
        int64(2),
        'ACTION',
        'FROM',
        'TO',
        'CC',
        'BCC',
        'SUBJ',
        'BODY',
        'RFCID',
        'REFS',
        ReplyType.REPLY,
        int64(3),
        int64(4),
      )
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.saveDraft,
        new Map<string, any>([
          ['draftRevision', int64(5)],
          ['draftIDATA', JSONItemToJSONString(draftWrapper.asJSONItem())],
        ]),
      )
      const serializer = MockJSONSerializer()
      serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
      const models = MockModels(
        { serializer },
        {
          draftAttachments: createMockInstance(DraftAttachments, {
            getDiskAttachesOfDraft: jest.fn().mockReturnValue(resolve([])),
            getUploadedDraftAttaches: jest.fn().mockReturnValue(resolve([])),
          }),
        },
      )
      CommandsServiceActions.createAPITask(descriptor, models).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(SaveDraftTask)
        const typedRes = res as SaveDraftTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.revision).toBe(int64(5))
        expect(typedRes.draftData).toStrictEqual(draftWrapper)
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return DeleteDraftEntry task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.deleteDraftEntry,
        new Map<string, any>([
          ['draftId', int64(5)],
          ['draftRevision', int64(6)],
        ]),
      )
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(DeleteDraftEntryTask)
        const typedRes = res as DeleteDraftEntryTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.draftID).toBe(int64(5))
        expect(typedRes.revision).toBe(int64(6))
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    it('should return UploadAttachTask task', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(
        int64(123),
        true,
        CommandsServiceActions.uploadAttachment,
        new Map<string, any>([
          ['draftId', int64(2)],
          ['file', 'file'],
          ['draftAttachName', 'display name'],
        ]),
      )
      const serializer = MockJSONSerializer()
      serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
      const models = MockModels({ serializer })
      CommandsServiceActions.createAPITask(descriptor, models).then((res) => {
        expect(res).not.toBeNull()
        expect(res!).toBeInstanceOf(UploadAttachTask)
        const typedRes = res as UploadAttachTask
        expect(typedRes.uid).toBe(int64(123))
        expect(typedRes.version).toBe(1)
        expect(typedRes.draftId).toBe(int64(2))
        expect(typedRes.draftAttachId).toBe(DraftAttachments.NO_ATTACHMENT_ID)
        expect(typedRes.localFileToUpload).toBe('file')
        expect(typedRes.displayName).toBe('display name')
        expect(typedRes.models).not.toBeNull()
        done()
      })
    })
    describe(SetParametersTask, () => {
      it('should return SetParametersTask task', (done) => {
        const items = new SetParametersItems([
          SetParametersItem.createOpenFromWeb(false),
          SetParametersItem.createShowFolderTabs(true),
        ])
        const descriptor = new CommandsServiceTaskDescriptor(
          int64(2),
          true,
          CommandsServiceActions.setParameters,
          new Map<string, any>([['parameters', items.toJSONItem()]]),
        )
        const serializer = MockJSONSerializer()
        serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
        const models = MockModels({ serializer })
        CommandsServiceActions.createAPITask(descriptor, models).then((res) => {
          expect(res).not.toBeNull()
          expect(res!).toBeInstanceOf(SetParametersTask)
          const typedRes = res as SetParametersTask
          expect(typedRes.uid).toBe(int64(2))
          expect(typedRes.version).toBe(1)
          expect(typedRes.parameters).toStrictEqual(items)
          expect(typedRes.models).not.toBeNull()
          done()
        })
      })
      it('should return null intead of SetParametersTask task if parameters are not specified', (done) => {
        const descriptor = new CommandsServiceTaskDescriptor(
          int64(2),
          true,
          CommandsServiceActions.setParameters,
          new Map<string, any>([['empty', '']]),
        )
        const serializer = MockJSONSerializer()
        serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
        const models = MockModels({ serializer })
        CommandsServiceActions.createAPITask(descriptor, models).then((res) => {
          expect(res).toBeNull()
          done()
        })
      })
      it('should return null intead of SetParametersTask task if parameters are malformed', (done) => {
        const descriptor = new CommandsServiceTaskDescriptor(
          int64(2),
          true,
          CommandsServiceActions.setParameters,
          new Map<string, any>([['parameters', new StringJSONItem('hello world')]]),
        )
        const serializer = MockJSONSerializer()
        serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
        const models = MockModels({ serializer })
        CommandsServiceActions.createAPITask(descriptor, models).then((res) => {
          expect(res).toBeNull()
          done()
        })
      })
    })
    it('should return null for unknown tasks', (done) => {
      const descriptor = new CommandsServiceTaskDescriptor(int64(123), true, 'random', new Map<string, any>())
      CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
        expect(res).toBeNull()
        done()
      })
    })
    describe('should return null for unsupported tasks', () => {
      it.each([CommandsServiceActions.moveToTab])('return null for %s', async (task) => {
        const descriptor = new CommandsServiceTaskDescriptor(int64(123), true, task, new Map<string, any>())
        await CommandsServiceActions.createAPITask(descriptor, MockModels()).then((res) => {
          expect(res).toBeNull()
        })
      })
    })
    describe(CommandsServiceActions.extractMessageIDs, () => {
      it('should extract messageIDs from the map', () => {
        expect(
          CommandsServiceActions.extractMessageIDs(
            new Map<string, any>([['messageId', [int64(1), int64(2)] as readonly ID[]]]),
          ),
        ).toStrictEqual([int64(1), int64(2)])
        expect(
          CommandsServiceActions.extractMessageIDs(
            new Map<string, any>([['unknown', [int64(1), int64(2)] as readonly ID[]]]),
          ),
        ).toHaveLength(0)
      })
    })
    describe(CommandsServiceActions.extractLabelID, () => {
      it('should extract labelID from the map', () => {
        expect(
          CommandsServiceActions.extractLabelID(
            new Map<string, any>([['labelId', '1']]),
          ),
        ).toBe('1')
        expect(
          CommandsServiceActions.extractLabelID(
            new Map<string, any>([['unknown', '1']]),
          ),
        ).toBeNull()
      })
    })
    describe(CommandsServiceActions.extractLabelIDs, () => {
      it('should extract labelIDs from the map', () => {
        expect(
          CommandsServiceActions.extractLabelIDs(
            new Map<string, any>([['labelId', ['1', '2', '3']]]),
          ),
        ).toStrictEqual(['1', '2', '3'])
        expect(
          CommandsServiceActions.extractLabelIDs(
            new Map<string, any>([['unknown', ['1', '2']]]),
          ),
        ).toHaveLength(0)
      })
    })
    describe(CommandsServiceActions.extractMarkMap, () => {
      it('should extract Mark map from the map', () => {
        expect(
          CommandsServiceActions.extractMarkMap(
            new Map<string, any>([
              [
                'marksMap',
                new Map([
                  ['1', true],
                  ['2', false],
                ]),
              ],
            ]),
          ),
        ).toStrictEqual(
          new Map([
            ['1', true],
            ['2', false],
          ]),
        )
        expect(
          CommandsServiceActions.extractMarkMap(
            new Map<string, any>([
              [
                'unknown',
                new Map([
                  ['1', true],
                  ['2', false],
                ]),
              ],
            ]),
          ),
        ).toBeNull()
      })
    })
    describe(CommandsServiceActions.extractCurrentFolderID, () => {
      it('should extract current folder ID from the map', () => {
        expect(
          CommandsServiceActions.extractCurrentFolderID(
            new Map<string, any>([['currentFolderId', int64(1)]]),
          ),
        ).toBe(int64(1))
        expect(
          CommandsServiceActions.extractCurrentFolderID(
            new Map<string, any>([['unknown', int64(1)]]),
          ),
        ).toBe(ConstantIDs.noFolderID)
      })
    })
    describe(CommandsServiceActions.extractID, () => {
      it('should extract ID from the map', () => {
        expect(
          CommandsServiceActions.extractID(
            new Map<string, any>([['key', int64(1)]]),
            'key',
          ),
        ).toBe(int64(1))
        expect(
          CommandsServiceActions.extractID(
            new Map<string, any>([['key', '1']]),
            'key',
          ),
        ).toBe(int64(1))
        expect(
          CommandsServiceActions.extractID(
            new Map<string, any>([['unknown', int64(1)]]),
            'key',
          ),
        ).toBeNull()
        expect(
          CommandsServiceActions.extractID(
            new Map<string, any>([['key', true]]),
            'key',
          ),
        ).toBeNull()
      })
    })
    describe(CommandsServiceActions.extractIDOrDefault, () => {
      it('should extract ID from the map', () => {
        expect(
          CommandsServiceActions.extractIDOrDefault(
            new Map<string, any>([['key', int64(1)]]),
            'key',
            int64(2),
          ),
        ).toBe(int64(1))
        expect(
          CommandsServiceActions.extractIDOrDefault(
            new Map<string, any>([['key', '1']]),
            'key',
            int64(2),
          ),
        ).toBe(int64(1))
        expect(
          CommandsServiceActions.extractIDOrDefault(
            new Map<string, any>([['unknown', int64(1)]]),
            'key',
            int64(2),
          ),
        ).toBe(int64(2))
        expect(
          CommandsServiceActions.extractIDOrDefault(
            new Map<string, any>([['key', true]]),
            'key',
            int64(2),
          ),
        ).toBe(int64(2))
        expect(
          CommandsServiceActions.extractIDOrDefault(
            new Map<string, any>([['key', '']]),
            'key',
            int64(2),
          ),
        ).toBe(int64(2))
      })
    })
    describe(CommandsServiceActions.extractDraftDataWrapper, () => {
      it('should extract ID from the map', () => {
        const draft = new DraftDataWrapper(
          int64(1),
          int64(2),
          'ACTION',
          'FROM',
          'TO',
          'CC',
          'BCC',
          'SUBJ',
          'BODY',
          'RFCID',
          'REFS',
          ReplyType.REPLY,
          int64(3),
          int64(4),
        )
        const json = JSONItemToJSONString(draft.asJSONItem())

        const serializer = MockJSONSerializer()
        serializer.deserialize = jest.fn((str: string) => new Result(JSONItemFromJSONString(str), null)) as any
        const models = MockModels({ serializer })

        expect(
          CommandsServiceActions.extractDraftDataWrapper(
            new Map<string, any>([['draftIDATA', json]]),
            models,
          ),
        ).toStrictEqual(draft)
        expect(
          CommandsServiceActions.extractDraftDataWrapper(
            new Map<string, any>([['key', json]]),
            models,
          ),
        ).toBeNull()
        expect(
          CommandsServiceActions.extractDraftDataWrapper(
            new Map<string, any>([['draftIDATA', '{"key": "value"}']]),
            models,
          ),
        ).toBeNull()
      })
    })
    describe(CommandsServiceActions.extractRevision, () => {
      it('should extract revision from the map', () => {
        expect(
          CommandsServiceActions.extractRevision(
            new Map<string, any>([['draftRevision', int64(1)]]),
          ),
        ).toBe(int64(1))
        expect(
          CommandsServiceActions.extractRevision(
            new Map<string, any>([['key', 'value']]),
          ),
        ).toBeNull()
      })
    })
  })
})
