import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { Folder, FolderType } from '../../../../../mapi/code/api/entities/folder/folder'
import { MessageMeta } from '../../../../../mapi/code/api/entities/message/message-meta'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { Registry } from '../../../../code/registry'
import { InvalidCommandError, TaskType } from '../../../../code/service/task'
import { ArchiveTask } from '../../../../code/service/tasks/archive-task'
import { MockStorage, MockWithinTransaction } from '../../../__helpers__/mock-patches'
import { idstr, MockModels } from '../../../__helpers__/models'

describe(ArchiveTask, () => {
  beforeAll(() => Registry.registerI18n({ localize: jest.fn().mockReturnValue('Archive') }))
  afterAll(Registry.drop)

  it('should be serializable', () => {
    const res = new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), MockModels())
    expect(res.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.archive,
        version: 1,
        uid: int64(2),
        mids: [idstr(3), idstr(4)],
        currentFid: idstr(5),
        targetFid: idstr(-1),
      }),
    )
  })

  it('should be deserializable', async () => {
    const models = MockModels()

    expect(
      await ArchiveTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.archive,
          version: 1,
          uid: '2',
          mids: [idstr(3), idstr(4)],
          currentFid: idstr(5),
          targetFid: idstr(-1),
        }),
        models,
      ),
    ).toMatchObject(new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), models))

    expect(
      await ArchiveTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.archive,
            version: 1,
            uid: '2',
            mids: [idstr(3), idstr(4)],
            currentFid: idstr(5),
            targetFid: idstr(-1),
          },
        ]),
        models,
      ),
    ).toBeNull()
  })

  it('should return type', () => {
    const task = new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), MockModels())
    expect(task.getType()).toBe(TaskType.archive)
  })

  it('should send request to server', (done) => {
    const messages = createMockInstance(Messages, {
      sendArchiveMessagesAction: jest.fn().mockReturnValue(resolve(new NetworkStatus(NetworkStatusCode.ok))),
    })
    const models = MockModels(undefined, { messages })
    const task = new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), models)
    expect.assertions(2)
    task.sendDataToServer().then((res) => {
      expect(messages.sendArchiveMessagesAction).toBeCalledWith([int64(3), int64(4)], 'Archive')
      expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      done()
    })
  })

  describe(ArchiveTask.create, () => {
    it('should reject on empty message IDs', (done) => {
      const models = MockModels()
      ArchiveTask.create(1, int64(2), [], models).failed((error) => {
        expect(error).toStrictEqual(
          new InvalidCommandError(
            'Failed to create ArchiveTask. MessageIDs should have at least one message to get folderID.',
          ),
        )
        done()
      })
    })

    it('should reject on empty "messageMetaByMid"', (done) => {
      const messages: Messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve(null)),
      })
      const models = MockModels(undefined, { messages })
      ArchiveTask.create(1, int64(2), [int64(3), int64(4)], models).failed((error) => {
        expect(messages.messageMetaByMid).toBeCalledWith(int64(3))
        expect(error).toStrictEqual(
          new InvalidCommandError(
            'Failed to create ArchiveTask. Can\'t find fid by local mid. Message with mid = "3" does not exist.',
          ),
        )
        done()
      })
    })

    it('should be creatable', (done) => {
      const messages: Messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve(createMockInstance(MessageMeta, { fid: int64(1) }))),
      })
      const models = MockModels(undefined, { messages })
      ArchiveTask.create(1, int64(2), [int64(3), int64(4)], models).then((task) => {
        expect(task).toBeInstanceOf(ArchiveTask)
        expect(task as ArchiveTask).toStrictEqual(new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(1), models))
        expect(messages.messageMetaByMid).toBeCalledWith(int64(3))
        done()
      })
    })
  })

  it('should perform database update if "archive" fid exists', (done) => {
    const messages = createMockInstance(Messages, {
      moveMessages: jest.fn().mockReturnValue(resolve(getVoid())),
      resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const folders = createMockInstance(Folders, {
      fetchFirstFidByType: jest.fn().mockReturnValue(resolve(int64(6))),
    })

    const models = MockModels(undefined, { folders, messages })
    const task = new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), models)

    expect.assertions(3)
    task.updateDatabase().then((_) => {
      expect(folders.fetchFirstFidByType).toBeCalledWith(FolderType.archive)
      expect(messages.moveMessages).toBeCalledWith([int64(3), int64(4)], int64(6), int64(5))
      expect(messages.resetMessagesTimestamp).toBeCalledWith([int64(3), int64(4)])
      done()
    })
  })

  it('should perform database update and create fake "archive" folder if "archive" fid does not exist', (done) => {
    const messages = createMockInstance(Messages, {
      moveMessages: jest.fn().mockReturnValue(resolve(getVoid())),
      resetMessagesTimestamp: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const folders = createMockInstance(Folders, {
      fetchFirstFidByType: jest.fn().mockReturnValue(resolve(null)),
      insertFoldersHard: jest.fn().mockReturnValue(resolve(getVoid())),
      insertFolderDefaults: jest.fn().mockReturnValue(resolve(getVoid())),
      cleanupOrphanFoldersEntities: jest.fn().mockReturnValue(resolve(getVoid())),
    })

    const storage = MockStorage({
      withinTransaction: MockWithinTransaction<any>(),
    })
    const models = MockModels({ storage }, { folders, messages })
    const task = new ArchiveTask(1, int64(2), [int64(3), int64(4)], int64(5), models)
    const fakeFolder = new Folder(int64(-8), FolderType.archive, 'Archive', 0, null, 0, 0)

    expect.assertions(6)
    task.updateDatabase().then((_) => {
      expect(folders.fetchFirstFidByType).toBeCalledWith(FolderType.archive)
      expect(folders.insertFoldersHard).toBeCalledWith([fakeFolder])
      expect(folders.insertFolderDefaults).toBeCalledWith([fakeFolder], false, false)
      expect(folders.cleanupOrphanFoldersEntities).toBeCalledWith([fakeFolder])
      expect(messages.moveMessages).toBeCalledWith([int64(3), int64(4)], int64(-8), int64(5))
      expect(messages.resetMessagesTimestamp).toBeCalledWith([int64(3), int64(4)])
      done()
    })
  })
})
