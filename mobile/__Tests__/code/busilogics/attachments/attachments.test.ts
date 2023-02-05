import { resolve } from '../../../../../../common/xpromise-support'
import { Int32, int32ToInt64, int64, int64ToString } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { MessageBodyAttach } from '../../../../../mapi/code/api/entities/body/message-body'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { StorageStatement } from '../../../../code/api/storage/storage-statement'
import { AttachmentsManager } from '../../../../code/busilogics/attachments/attachments'
import { MockCursorWithArray, MockStorage, MockWithinTransaction } from '../../../__helpers__/mock-patches'
import { TestIDSupport } from '../../../__helpers__/test-id-support'

const testIDSupport = new TestIDSupport()

function idstr(value: Int32): string {
  return testIDSupport.toDBValue(int32ToInt64(value))
}

describe(AttachmentsManager, () => {
  describe('clearAttaches', () => {
    it('should not run statements on empty input', (done) => {
      const storage = MockStorage()
      const attachmentsManager = new AttachmentsManager(storage, new TestIDSupport())

      attachmentsManager.clearAttaches([]).then(() => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should delete common and inline attaches', (done) => {
      const withinTransaction = MockWithinTransaction<any>()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })

      const attachmentsManager = new AttachmentsManager(storage, new TestIDSupport())
      attachmentsManager.clearAttaches([int64(1), int64(2), int64(3)]).then(() => {
        expect(runStatement).toBeCalledTimes(2)
        // tslint:disable-next-line:max-line-length
        expect(runStatement.mock.calls[0][0]).toStrictEqual(
          `DELETE FROM ${EntityKind.attachment} WHERE mid IN (1, 2, 3);`,
        )
        expect(notifyAboutChanges.mock.calls[0][0]).toStrictEqual([EntityKind.attachment])
        // tslint:disable-next-line:max-line-length
        expect(runStatement.mock.calls[1][0]).toStrictEqual(
          `DELETE FROM ${EntityKind.inline_attach} WHERE mid IN (1, 2, 3);`,
        )
        expect(notifyAboutChanges.mock.calls[1][0]).toStrictEqual([EntityKind.inline_attach])
        done()
      })
    })
  })
  describe('insertAttaches', () => {
    it('should not execute statements on empty input', (done) => {
      const prepareStatement: Storage['prepareStatement'] = jest.fn()
      const withinTransaction = MockWithinTransaction<any>()
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        prepareStatement,
        withinTransaction,
        notifyAboutChanges,
      })

      const attachmentsManager = new AttachmentsManager(storage, new TestIDSupport())
      attachmentsManager.insertAttaches(int64(301), []).then(() => {
        expect(prepareStatement).not.toBeCalled()
        expect(notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should insert common and inline attaches', (done) => {
      const withinTransaction = MockWithinTransaction<any>()
      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close: jest.fn(),
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        prepareStatement,
        notifyAboutChanges,
      })

      const attachmentsManager = new AttachmentsManager(storage, testIDSupport)
      attachmentsManager
        .insertAttaches(int64(301), [
          // tslint:disable-next-line:max-line-length
          new MessageBodyAttach(
            'hid 1',
            'displayName 1',
            int64(1),
            'downloadUrl 1',
            true,
            true,
            false,
            'attachClass 1',
            'mimeType 1',
            'contentId 1',
          ),
          new MessageBodyAttach(
            'hid 1.1',
            'displayName 1.1',
            null,
            'downloadUrl 1.1',
            true,
            true,
            false,
            'attachClass 1.1',
            'mimeType 1.1',
            'contentId 1.1',
          ),
          // tslint:disable-next-line:max-line-length
          new MessageBodyAttach(
            'hid 2',
            'displayName 2',
            int64(2),
            'downloadUrl 2',
            true,
            true,
            true,
            'attachClass 2',
            'mimeType 2',
            'contentId 2',
          ),
        ])
        .then(() => {
          expect(prepareStatement).toBeCalledTimes(2)
          // tslint:disable-next-line:max-line-length
          expect(prepareStatement.mock.calls[0][0]).toStrictEqual(
            `INSERT INTO ${EntityKind.attachment} (mid, hid, display_name, attachClass, size, mime_type, preview_support, is_disk, download_url, download_manager_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`,
          )
          // tslint:disable-next-line:max-line-length
          expect(prepareStatement.mock.calls[1][0]).toStrictEqual(
            `INSERT INTO ${EntityKind.inline_attach} (mid, hid, display_name, content_id) VALUES (?, ?, ?, ?);`,
          )

          expect(execute).toBeCalledTimes(3)
          // tslint:disable-next-line:max-line-length
          expect(execute.mock.calls[0][0]).toStrictEqual([
            idstr(301),
            'hid 1',
            'displayName 1',
            'attachClass 1',
            int64ToString(int64(1)),
            'mimeType 1',
            1,
            1,
            'downloadUrl 1',
            null,
          ])
          // tslint:disable-next-line: max-line-length
          expect(execute.mock.calls[1][0]).toStrictEqual([
            idstr(301),
            'hid 1.1',
            'displayName 1.1',
            'attachClass 1.1',
            int64ToString(int64(0)),
            'mimeType 1.1',
            1,
            1,
            'downloadUrl 1.1',
            null,
          ])
          expect(execute.mock.calls[2][0]).toStrictEqual([idstr(301), 'hid 2', 'displayName 2', 'contentId 2'])

          expect(notifyAboutChanges.mock.calls[0][0]).toStrictEqual([EntityKind.attachment])
          expect(notifyAboutChanges.mock.calls[1][0]).toStrictEqual([EntityKind.inline_attach])

          done()
        })
    })
  })
  describe('fetch', () => {
    it('should fetch attachments by mid', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(
          resolve(
            MockCursorWithArray([
              [idstr(1), 'hid1', 'displayName1', 'attachClass1', int64(1000), 'mimetype1', true, true, 'downloadurl1'],
              [
                idstr(1),
                'hid2',
                'displayName2',
                'attachClass2',
                int64(2000),
                'mimetype2',
                false,
                false,
                'downloadurl2',
              ],
            ]),
          ),
        ),
      })
      expect.assertions(4)
      new AttachmentsManager(storage, testIDSupport).fetch(int64(1)).then((res) => {
        expect(storage.runQuery).toBeCalledWith(`SELECT * FROM ${EntityKind.attachment} WHERE mid = ?;`, [idstr(1)])
        expect(res).toHaveLength(2)
        expect(res[0]).toEqual(
          new MessageBodyAttach(
            'hid1',
            'displayName1',
            int64(1000),
            'downloadurl1',
            true,
            true,
            false,
            'attachClass1',
            'mimetype1',
            null,
          ),
        )
        expect(res[1]).toEqual(
          new MessageBodyAttach(
            'hid2',
            'displayName2',
            int64(2000),
            'downloadurl2',
            false,
            false,
            false,
            'attachClass2',
            'mimetype2',
            null,
          ),
        )
        done()
      })
    })
  })
  describe('clearPinnedAttachesExceptDisk', () => {
    it('should return immediately if input mids list is empty', (done) => {
      const withinTransaction = MockWithinTransaction<any>()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const attachmentsManager = new AttachmentsManager(storage, testIDSupport)
      attachmentsManager.clearPinnedAttachesExceptDisk([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should delete attaches by mid and notify about changes', (done) => {
      const withinTransaction = MockWithinTransaction<any>()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const attachmentsManager = new AttachmentsManager(storage, testIDSupport)
      attachmentsManager.clearPinnedAttachesExceptDisk([int64(123), int64(456)], true).then((_) => {
        expect(storage.runStatement).toBeCalledTimes(2)
        expect(storage.runStatement).nthCalledWith(1, 'DELETE FROM attachment WHERE mid IN (123, 456) AND is_disk = 0;')
        expect(storage.runStatement).nthCalledWith(2, 'DELETE FROM inline_attach WHERE mid IN (123, 456);')
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toHaveBeenNthCalledWith(1, ['attachment', 'inline_attach'])
        done()
      })
    })
  })
})
