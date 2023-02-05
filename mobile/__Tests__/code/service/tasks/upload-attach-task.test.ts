import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/../mapi/code/api/entities/status/network-status'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { AttachmentsUploader, AttachmentUploadData } from '../../../../code/busilogics/draft/attachments-uploader'
import { DraftAttachEntry } from '../../../../code/busilogics/draft/draft-attach-entry'
import { DraftAttachments } from '../../../../code/busilogics/draft/draft-attachments'
import { Drafts } from '../../../../code/busilogics/draft/drafts'
import { Messages } from '../../../../code/busilogics/messages/messages'
import { InvalidCommandError, TaskType } from '../../../../code/service/task'
import { UploadAttachTask } from '../../../../code/service/tasks/upload-attach-task'
import { idstr, MockModels } from '../../../__helpers__/models'

describe(UploadAttachTask, () => {
  it('should be deserializable', async () => {
    const models = MockModels()
    expect(
      await UploadAttachTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.uploadAttach,
          version: 1,
          uid: '2',
          draftId: '3',
          draftAttachId: '4',
        }),
        models,
      ),
    ).toMatchObject(new UploadAttachTask(1, int64(2), int64(3), int64(4), null, null, models))

    expect(
      await UploadAttachTask.fromJSONItem(
        JSONItemFromJSON([
          {
            taskType: TaskType.uploadAttach,
            version: 1,
            uid: '2',
            draftId: '3',
            draftAttachId: '4',
          },
        ]),
        models,
      ),
    ).toBeNull()

    expect(
      await UploadAttachTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.uploadAttach,
          version: 1,
          uid: '2',
        }),
        models,
      ),
    ).toBeNull()

    expect(
      await UploadAttachTask.fromJSONItem(
        JSONItemFromJSON({
          taskType: TaskType.uploadAttach,
          version: 1,
          uid: '2',
          draftId: '3',
        }),
        models,
      ),
    ).toBeNull()
  })

  it('should be serializable', () => {
    const models = MockModels()
    const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)
    expect(task.serialize()).toStrictEqual(
      JSONItemFromJSON({
        taskType: TaskType.uploadAttach,
        version: 1,
        uid: int64(2),
        draftId: idstr(3),
        draftAttachId: idstr(4),
      }),
    )
  })

  describe(UploadAttachTask.prototype.sendDataToServer, () => {
    it('should fail if no draft attach id', (done) => {
      const models = MockModels()
      const task = new UploadAttachTask(
        1,
        int64(2),
        int64(3),
        DraftAttachments.NO_ATTACHMENT_ID,
        'file',
        'name',
        models,
      )

      expect.assertions(1)
      task.sendDataToServer().failed((e) => {
        expect(e).toStrictEqual(
          new InvalidCommandError('Failed to upload attach. Expected "draftAttachId" to be present.'),
        )
        done()
      })
    })

    it('should fail if no mid', (done) => {
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(null)),
      })
      const models = MockModels({}, { drafts })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      expect.assertions(2)
      task.sendDataToServer().failed((e) => {
        expect(drafts.getMidByDid).toBeCalledWith(int64(3))
        expect(e).toStrictEqual(new InvalidCommandError('Failed to upload attach. Draft entry not found for did = 3'))
        done()
      })
    })

    it('should fail if no message meta', (done) => {
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve(null)),
      })
      const models = MockModels({}, { drafts, messages })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      expect.assertions(2)
      task.sendDataToServer().failed((e) => {
        expect(messages.messageMetaByMid).toBeCalledWith(int64(10))
        expect(e).toStrictEqual(new InvalidCommandError('Failed to upload attach. Message meta not found for did = 3'))
        done()
      })
    })

    it('should fail if no draft attach', (done) => {
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(null)),
      })
      const models = MockModels({}, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      expect.assertions(2)
      task.sendDataToServer().failed((e) => {
        expect(draftAttachments.getDraftAttachement).toBeCalledWith(int64(4))
        expect(e).toStrictEqual(
          new InvalidCommandError('Failed to upload attach. Draft attach entry not found for attach id = 4'),
        )
        done()
      })
    })

    it('should upload disk attach', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        true,
        false,
        null,
      )
      const status = new NetworkStatus(NetworkStatusCode.ok)
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn().mockReturnValue(resolve(new AttachmentUploadData(status, 'url'))),
        uploadOrdinaryAttach: jest.fn(),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadToDisk).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).toBeCalledWith(int64(4), 'url')
        expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        done()
      })
    })
    it('should not update draft entry if disk attachment upload failed with null response', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        true,
        false,
        null,
      )
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn().mockReturnValue(resolve(null)),
        uploadOrdinaryAttach: jest.fn(),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadToDisk).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).not.toBeCalled()
        expect(res).toBeNull()
        done()
      })
    })
    it('should not update draft entry if disk attachment upload failed with non-ok response', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        true,
        false,
        null,
      )
      const result = new AttachmentUploadData(new NetworkStatus(NetworkStatusCode.permanentError), 'url')
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn().mockReturnValue(resolve(result)),
        uploadOrdinaryAttach: jest.fn(),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadToDisk).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).not.toBeCalled()
        expect(res).toStrictEqual(result.status)
        done()
      })
    })
    it('should not update draft entry if disk attachment upload failed with null url', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        true,
        false,
        null,
      )
      const result = new AttachmentUploadData(new NetworkStatus(NetworkStatusCode.ok), null)
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn().mockReturnValue(resolve(result)),
        uploadOrdinaryAttach: jest.fn(),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadToDisk).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).not.toBeCalled()
        expect(res).toStrictEqual(result.status)
        done()
      })
    })

    it('should upload ordinary attach', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        false,
        false,
        null,
      )
      const status = new NetworkStatus(NetworkStatusCode.ok)
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn(),
        uploadOrdinaryAttach: jest.fn().mockReturnValue(resolve(new AttachmentUploadData(status, 'url'))),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadOrdinaryAttach).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).toBeCalledWith(int64(4), 'url')
        expect(res).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        done()
      })
    })
    it('should not update draft entry if ordinary attach uploading failed with null result', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        false,
        false,
        null,
      )
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn(),
        uploadOrdinaryAttach: jest.fn().mockReturnValue(resolve(null)),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadOrdinaryAttach).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).not.toBeCalled()
        expect(res).toBeNull()
        done()
      })
    })
    it('should not update draft entry if ordinary attach uploading failed with non-ok result', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        false,
        false,
        null,
      )
      const result = new AttachmentUploadData(new NetworkStatus(NetworkStatusCode.permanentError), 'url')
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn(),
        uploadOrdinaryAttach: jest.fn().mockReturnValue(resolve(result)),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadOrdinaryAttach).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).not.toBeCalled()
        expect(res).toStrictEqual(result.status)
        done()
      })
    })
    it('should not update draft entry if ordinary attach uploading failed with null url', (done) => {
      const draftAttach = new DraftAttachEntry(
        int64(111),
        int64(123),
        'https://someUrl.org/id=123',
        'someFileUri',
        'some display name',
        int64(16000),
        null,
        false,
        false,
        false,
        null,
      )
      const result = new AttachmentUploadData(new NetworkStatus(NetworkStatusCode.ok), null)
      const drafts = createMockInstance(Drafts, {
        getMidByDid: jest.fn().mockReturnValue(resolve(int64(10))),
        updateURL: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = createMockInstance(Messages, {
        messageMetaByMid: jest.fn().mockReturnValue(resolve({})),
      })
      const draftAttachments = createMockInstance(DraftAttachments, {
        getDraftAttachement: jest.fn().mockReturnValue(resolve(draftAttach)),
      })
      const attachmentsUploader: AttachmentsUploader = {
        uploadToDisk: jest.fn(),
        uploadOrdinaryAttach: jest.fn().mockReturnValue(resolve(result)),
      }
      const models = MockModels({ attachmentsUploader }, { drafts, messages, draftAttachments })
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', 'name', models)

      task.sendDataToServer().then((res) => {
        expect(attachmentsUploader.uploadOrdinaryAttach).toBeCalledWith(draftAttach)
        expect(drafts.updateURL).not.toBeCalled()
        expect(res).toStrictEqual(result.status)
        done()
      })
    })
  })

  describe(UploadAttachTask.prototype.sendDataToServer, () => {
    it('should fail for null "localFileToUpload"', (done) => {
      const models = MockModels()
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), null, 'name', models)

      expect.assertions(1)
      task.updateDatabase().failed((e) => {
        expect(e).toStrictEqual(new InvalidCommandError('localFileToUpload is null'))
        done()
      })
    })

    it('should fail for null "displayName"', (done) => {
      const models = MockModels()
      const task = new UploadAttachTask(1, int64(2), int64(3), int64(4), 'file', null, models)

      expect.assertions(1)
      task.updateDatabase().failed((e) => {
        expect(e).toStrictEqual(new InvalidCommandError('displayName is null'))
        done()
      })
    })

    it('should update database', (done) => {
      const draftAttachments = createMockInstance(DraftAttachments, {
        insertNewAttach: jest.fn().mockReturnValue(resolve(int64(100))),
      })
      const models = MockModels({}, { draftAttachments })
      const task = new UploadAttachTask(
        1,
        int64(2),
        int64(3),
        DraftAttachments.NO_ATTACHMENT_ID,
        'file',
        'file name',
        models,
      )

      expect.assertions(2)
      task.updateDatabase().then((res) => {
        expect(draftAttachments.insertNewAttach).toBeCalledWith('file', int64(3), 'file name')
        expect(task.draftAttachId).toStrictEqual(int64(100))
        done()
      })
    })
  })
})
