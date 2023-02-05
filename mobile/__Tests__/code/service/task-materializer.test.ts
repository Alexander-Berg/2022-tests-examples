import { resolve } from '../../../../../common/xpromise-support'
import { int64 } from '../../../../../common/ys'
import { MockJSONSerializer } from '../../../../common/__tests__/__helpers__/mock-patches'
import { JSONItem } from '../../../../common/code/json/json-types'
import { Result } from '../../../../common/code/result/result'
import { JSONItemFromJSONString, JSONItemToJSONString } from '../../../../common/__tests__/__helpers__/json-helpers'
import { FolderType } from '../../../../mapi/../mapi/code/api/entities/folder/folder'
import {
  SetParametersItem,
  SetParametersItems,
} from '../../../../mapi/code/api/entities/actions/set-parameters-request'
import { createMockInstance } from '../../../../common/__tests__/__helpers__/utils'
import { DraftAttachments } from '../../../code/busilogics/draft/draft-attachments'
import { DraftDataWrapper, ReplyType } from '../../../code/busilogics/draft/draft-data-wrapper'
import { Folders } from '../../../code/busilogics/folders/folders'
import { TaskType } from '../../../code/service/task'
import { getTaskMaterializerByType } from '../../../code/service/task-materializer'
import { ArchiveTask } from '../../../code/service/tasks/archive-task'
import { ClearFolderTask } from '../../../code/service/tasks/clear-folder-task'
import { DeleteDraftEntryTask } from '../../../code/service/tasks/delete-draft-entry-task'
import { DeleteTask } from '../../../code/service/tasks/delete-task'
import { MarkReadTask } from '../../../code/service/tasks/mark-read-task'
import { MarkSpamTask } from '../../../code/service/tasks/mark-spam-task'
import { MarkWithLabelTask } from '../../../code/service/tasks/mark-with-label-task'
import { MoveToFolderTask } from '../../../code/service/tasks/move-to-folder-task'
import { MultiMarkWithLabelTaskApi } from '../../../code/service/tasks/multi-mark-with-label-task-api'
import { PurgeTask } from '../../../code/service/tasks/purge-task'
import { SaveDraftTask } from '../../../code/service/tasks/save-draft-task'
import { SaveSignatureTask } from '../../../code/service/tasks/save-signature-task'
import { SendMailTask } from '../../../code/service/tasks/send-mail-task'
import { SetParametersTask } from '../../../code/service/tasks/set-parameters-task'
import { UploadAttachTask } from '../../../code/service/tasks/upload-attach-task'
import { MockModels } from '../../__helpers__/models'

const Serializer = MockJSONSerializer({
  serialize: jest.fn((jsonItem: JSONItem): Result<string> => new Result(JSONItemToJSONString(jsonItem), null)),
  deserialize: jest.fn((str, materializer) => {
    const obj = JSONItemFromJSONString(str)
    return materializer(obj)
  }) as any,
})

describe(getTaskMaterializerByType, () => {
  it('should return null for unknown types', () => {
    expect(getTaskMaterializerByType(100500)).toBeNull()
  })
  it.each([
    TaskType.markRead,
    TaskType.markUnread,
    TaskType.delete,
    TaskType.markWithLabel,
    TaskType.moveToFolder,
    TaskType.spam,
    TaskType.unspam,
    TaskType.archive,
    TaskType.saveSignature,
    TaskType.clearFolder,
    TaskType.multiMarkWithLabel,
    TaskType.deleteDraftEntry,
    TaskType.sendMessage,
    TaskType.saveDraft,
    TaskType.purge,
    TaskType.uploadAttach,
    TaskType.setParameters,
  ])('should return function for known and supported type: %i', (taskType) => {
    expect(getTaskMaterializerByType(taskType)).toBeInstanceOf(Function)
  })

  it.each([TaskType.clearMessage, TaskType.multiMarkWithLabelOffline])(
    'should return null for non-implemented types: %i',
    (taskType) => {
      expect(getTaskMaterializerByType(taskType)).toBeNull()
    },
  )

  it('should return materializer for markRead task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new MarkReadTask(1, int64(2), [int64(3), int64(4)], true, models)
    const materializer = getTaskMaterializerByType(TaskType.markRead)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for markUnread task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new MarkReadTask(1, int64(2), [int64(3), int64(4)], false, models)
    const materializer = getTaskMaterializerByType(TaskType.markUnread)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for delete task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new DeleteTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), true, models)
    const materializer = getTaskMaterializerByType(TaskType.delete)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for markWithLabel task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new MarkWithLabelTask(1, int64(2), [int64(3), int64(4)], true, 'lid', models)
    const materializer = getTaskMaterializerByType(TaskType.markWithLabel)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for moveToFolder task', async () => {
    const models = MockModels(
      { serializer: Serializer },
      {
        folders: createMockInstance(Folders, {
          fetchFirstFidByType: jest.fn((type) =>
            resolve(
              ({
                [FolderType.inbox]: int64(1),
                [FolderType.spam]: int64(2),
              } as any)[type],
            ),
          ),
        }),
      },
    )
    const task = new MoveToFolderTask(1, int64(2), [int64(3), int64(4)], int64(5), int64(6), models)
    const materializer = getTaskMaterializerByType(TaskType.moveToFolder)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for spam task', (done) => {
    const models = MockModels(
      { serializer: Serializer },
      {
        folders: createMockInstance(Folders, {
          fetchFirstFidByType: jest.fn((type) =>
            resolve(
              ({
                [FolderType.inbox]: int64(1),
                [FolderType.spam]: int64(2),
              } as any)[type],
            ),
          ),
        }),
      },
    )
    MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), true, models).then(async (t) => {
      const materializer = getTaskMaterializerByType(TaskType.spam)
      expect(materializer).not.toBeNull()
      expect(await materializer!(t.serialize(), models)).toStrictEqual(t)
      done()
    })
  })
  it('should return materializer for unspam task', (done) => {
    const models = MockModels(
      { serializer: Serializer },
      {
        folders: createMockInstance(Folders, {
          fetchFirstFidByType: jest.fn((type) =>
            resolve(
              ({
                [FolderType.inbox]: int64(1),
                [FolderType.spam]: int64(2),
              } as any)[type],
            ),
          ),
        }),
      },
    )
    MarkSpamTask.create(1, int64(2), [int64(3), int64(4)], int64(5), false, models).then(async (t) => {
      const materializer = getTaskMaterializerByType(TaskType.unspam)
      expect(materializer).not.toBeNull()
      expect(await materializer!(t.serialize(), models)).toStrictEqual(t)
      done()
    })
  })
  it('should return materializer for archive task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), models)
    const materializer = getTaskMaterializerByType(TaskType.archive)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for saveSignature task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new SaveSignatureTask(1, int64(2), 'signature', models)
    const materializer = getTaskMaterializerByType(TaskType.saveSignature)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for clearFolder task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new ClearFolderTask(1, int64(2), int64(3), models)
    const materializer = getTaskMaterializerByType(TaskType.clearFolder)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for multiMarkWithLabel task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new MultiMarkWithLabelTaskApi(1, int64(2), [int64(3), int64(4)], ['5', '6'], true, models)
    const materializer = getTaskMaterializerByType(TaskType.multiMarkWithLabel)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for deleteDraftEntry task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new DeleteDraftEntryTask(1, int64(2), int64(3), int64(4), models)
    const materializer = getTaskMaterializerByType(TaskType.deleteDraftEntry)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for sendMessage task', async () => {
    const models = MockModels(
      { serializer: Serializer },
      {
        draftAttachments: createMockInstance(DraftAttachments, {
          getUploadedDraftAttaches: jest.fn().mockReturnValue(resolve([])),
          getDiskAttachesOfDraft: jest.fn().mockReturnValue(resolve([])),
        }),
      },
    )
    const task = await SendMailTask.create(
      1,
      int64(2),
      int64(3),
      new DraftDataWrapper(
        int64(4),
        int64(5),
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
        int64(6),
        int64(7),
      ),
      models,
    )
    const materializer = getTaskMaterializerByType(TaskType.sendMessage)
    expect(materializer).not.toBeNull()
    expect(task).not.toBeNull()
    expect(await materializer!(task!.serialize(), models)).toStrictEqual(task!)
  })
  it('should return materializer for saveDraft task', async () => {
    const models = MockModels(
      { serializer: Serializer },
      {
        draftAttachments: createMockInstance(DraftAttachments, {
          getUploadedDraftAttaches: jest.fn().mockReturnValue(resolve([])),
          getDiskAttachesOfDraft: jest.fn().mockReturnValue(resolve([])),
        }),
      },
    )
    const task = await SaveDraftTask.create(
      1,
      int64(2),
      int64(3),
      new DraftDataWrapper(
        int64(4),
        int64(5),
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
        int64(6),
        int64(7),
      ),
      models,
    )
    const materializer = getTaskMaterializerByType(TaskType.saveDraft)
    expect(materializer).not.toBeNull()
    expect(task).not.toBeNull()
    expect(await materializer!(task!.serialize(), models)).toStrictEqual(task!)
  })
  it('should return materializer for purge task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new PurgeTask(1, int64(2), [int64(3), int64(4)], models)
    const materializer = getTaskMaterializerByType(TaskType.purge)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for upload attach task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), null, null, models)
    const materializer = getTaskMaterializerByType(TaskType.uploadAttach)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
  it('should return materializer for set parameters task', async () => {
    const models = MockModels({ serializer: Serializer })
    const task = new SetParametersTask(
      1,
      int64(2),
      new SetParametersItems([SetParametersItem.createOpenFromWeb(true), SetParametersItem.createShowFolderTabs(true)]),
      models,
    )
    const materializer = getTaskMaterializerByType(TaskType.setParameters)
    expect(materializer).not.toBeNull()
    expect(await materializer!(task.serialize(), models)).toStrictEqual(task)
  })
})
