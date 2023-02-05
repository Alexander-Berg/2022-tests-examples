import { reject, resolve } from '../../../../../../common/xpromise-support'
import { Int32, int32ToInt64, int64, int64ToString, YSError } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { ID, idFromString } from '../../../../../mapi/../mapi/code/api/common/id'
import { DeltaApiFolder } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-folder'
import { DeltaApiFolderCountersUpdateItem } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-folder-counters-update-item'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import {
  Folder,
  FolderSyncType,
  folderSyncTypeToInt32,
  FolderType,
  folderTypeToInt32,
} from '../../../../../mapi/code/api/entities/folder/folder'
import { MessageMeta } from '../../../../../mapi/code/api/entities/message/message-meta'
import { MessageTypeFlags } from '../../../../../mapi/code/api/entities/message/message-type'
import { StringBuilder } from '../../../../../mapi/code/utils/string-builder'
import { queryValuesFromIds } from '../../../../code/api/storage/query-helpers'
import { StorageStatement } from '../../../../code/api/storage/storage-statement'
import { Folders, folderSyncType } from '../../../../code/busilogics/folders/folders'
import { AccountSettingsKeys } from '../../../../code/busilogics/settings/settings-saver'
import { Registry } from '../../../../code/registry'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import {
  MockCursorWithArray,
  MockHighPrecisionTimer,
  MockStorage,
  MockStorageStatement,
  MockWithinTransaction,
} from '../../../__helpers__/mock-patches'
import { makeFolders } from '../../../__helpers__/models'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { rejected } from '../../../__helpers__/test-failure'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import folder from '../../../../../mapi/__tests__/code/api/entities/delta-api/entities/folder.json'

const sampleFolders = [
  new Folder(int64(101), FolderType.inbox, 'Inbox', 0, null, 100, 1000),
  new Folder(int64(102), FolderType.user, 'User 1', 100, int64(101), 200, 2000),
  new Folder(int64(103), FolderType.trash, 'Trash', 0, null, 300, 3000),
]

const testIDSupport = new TestIDSupport()

function idstr(value: Int32 | ID): string {
  switch (typeof value) {
    case 'bigint':
      return testIDSupport.toDBValue(value)
    case 'number':
      return testIDSupport.toDBValue(int32ToInt64(value))
  }
}
describe(Folders, () => {
  describe('replaceFolders', () => {
    it("should run transaction depending on 'transactioned' parameter (true)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).failed((_) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, false).failed((_) => {
        expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (default, false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders).failed((_) => {
        expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it('should fail if transaction creation fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('TRANSACTION CREATION FAILED')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      folders.replaceFolders(sampleFolders, true).failed((error) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        expect(error).toStrictEqual(new YSError('TRANSACTION CREATION FAILED'))
        done()
      })
    })
    it('should delete old folders', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).failed((_) => {
        expect(runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.folder};`)
        done()
      })
    })
    it('should fail if old folders deletion fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(reject(new YSError('DELETION FAILED')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      folders.replaceFolders(sampleFolders, true).failed((error) => {
        expect(runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.folder};`)
        expect(error).toStrictEqual(new YSError('DELETION FAILED'))
        done()
      })
    })
    it('should run insertion if deletion of old folders succeeds', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).failed((_) => {
        // tslint:disable-next-line: max-line-length
        expect(prepareStatement).toBeCalledWith(
          `INSERT OR REPLACE INTO ${EntityKind.folder} (fid, type, name, position, parent, unread_counter, total_counter) VALUES (?, ?, ?, ?, ?, ?, ?);`,
        )
        done()
      })
    })
    it('should fail if insertion fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).failed((error) => {
        // tslint:disable-next-line: max-line-length
        expect(error).toStrictEqual(new YSError('NO MATTER'))
        done()
      })
    })
    it('should create insertion executions by the number of folders to store', (done) => {
      const folderToArray = (item: Folder): readonly any[] => [
        testIDSupport.toDBValue(item.fid),
        item.type,
        item.name,
        item.position,
        item.parent !== null ? testIDSupport.toDBValue(item.parent) : null,
        item.unreadCounter,
        item.totalCounter,
      ]
      const withinTransaction = MockWithinTransaction<any>()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
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
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(4)
      folders.replaceFolders(sampleFolders, true).then((_) => {
        expect(execute).toBeCalledTimes(3)
        expect(execute.mock.calls[0][0]).toStrictEqual(folderToArray(sampleFolders[0]))
        expect(execute.mock.calls[1][0]).toStrictEqual(folderToArray(sampleFolders[1]))
        expect(execute.mock.calls[2][0]).toStrictEqual(folderToArray(sampleFolders[2]))
        done()
      })
    })
    it('should notify about changes on successfull insertion', (done) => {
      const withinTransaction = MockWithinTransaction<any>()
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
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
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).then((_) => {
        expect(notifyAboutChanges).toBeCalledWith([EntityKind.folder])
        done()
      })
    })
    it('should fail insertion executions if any insertion fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const execute: StorageStatement['execute'] = jest
        .fn()
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(reject(new YSError('INSERTION FAILED')))
        .mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close: jest.fn(),
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      folders.replaceFolders(sampleFolders, true).failed((error) => {
        expect(execute).toBeCalledTimes(3)
        expect(error).toStrictEqual(new YSError('INSERTION FAILED'))
        done()
      })
    })
    it('should close statement after insertions are done successfully', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const close: StorageStatement['close'] = jest.fn()
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).then((_) => {
        expect(close).toBeCalled()
        done()
      })
    })
    it('should close statement after insertions are failed', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(reject(new YSError('INSERTION FAILED')))
      const close: StorageStatement['close'] = jest.fn()
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.replaceFolders(sampleFolders, true).failed((_) => {
        expect(close).toBeCalled()
        done()
      })
    })
    it('should return void if successfull', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const close: StorageStatement['close'] = jest.fn()
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      folders.replaceFolders(sampleFolders, true).then((value) => {
        expect(value).not.toBeNull()
        expect(value).toBe(getVoid())
        done()
      })
    })
    it('should return with resolution immediatelly if empty set of folders', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(4)
      folders.replaceFolders([], true).then((value) => {
        expect(value).not.toBeNull()
        expect(value).toBe(getVoid())
        expect(withinTransaction).not.toBeCalled()
        expect(notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })

  describe('cleanupOrphanFoldersEntities', () => {
    it('should return immediately on empty input', (done) => {
      const folders = new Folders(MockStorage(), new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.cleanupOrphanFoldersEntities([]).then((result) => {
        expect(result).toBe(getVoid())
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (true)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders
        .cleanupOrphanFoldersEntities([new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000)], true)
        .failed((_) => {
          expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
          done()
        })
    })
    it("should run transaction depending on 'transactioned' parameter (false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders
        .cleanupOrphanFoldersEntities([new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000)], false)
        .failed((_) => {
          expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
          done()
        })
    })
    it("should run transaction depending on 'transactioned' parameter (default, false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders
        .cleanupOrphanFoldersEntities([new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000)])
        .failed((_) => {
          expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
          done()
        })
    })
    it('should notify about changes all tables', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders
        .cleanupOrphanFoldersEntities([
          new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000),
          new Folder(int64(2001), FolderType.user, 'user', 100, int64(1), 200, 2000),
        ])
        .then((_) => {
          expect(notifyAboutChanges).toBeCalledTimes(2)
          expect(notifyAboutChanges.mock.calls[0][0]).toStrictEqual([EntityKind.folder_lat])
          expect(notifyAboutChanges.mock.calls[1][0]).toStrictEqual([EntityKind.thread_scn])
          done()
        })
    })
    it('should run statements with fids', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders
        .cleanupOrphanFoldersEntities([
          new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000),
          new Folder(int64(2001), FolderType.user, 'user', 100, int64(1), 200, 2000),
        ])
        .then((_) => {
          expect(runStatement).toBeCalledTimes(2)
          expect((runStatement as any).mock.calls[0][0]).toStrictEqual(
            'DELETE FROM folder_lat WHERE fid NOT IN (1, 2001);',
          )
          expect((runStatement as any).mock.calls[1][0]).toStrictEqual(
            'DELETE FROM thread_scn WHERE tid IN (SELECT t.tid FROM thread AS t WHERE t.fid NOT IN (1, 2001));',
          )
          done()
        })
    })
  })

  describe('cleanupThreadOrMessagesNotInXlist', () => {
    it('should return immediately on empty input', (done) => {
      const folders = new Folders(MockStorage(), new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.cleanupThreadOrMessagesNotInXlist([]).then((result) => {
        expect(result).toBe(getVoid())
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (true)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders
        .cleanupThreadOrMessagesNotInXlist([new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000)], true)
        .failed((_) => {
          expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
          done()
        })
    })
    it("should run transaction depending on 'transactioned' parameter (false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders
        .cleanupThreadOrMessagesNotInXlist([new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000)], false)
        .failed((_) => {
          expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
          done()
        })
    })
    it("should run transaction depending on 'transactioned' parameter (default, false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders
        .cleanupThreadOrMessagesNotInXlist([new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000)])
        .failed((_) => {
          expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
          done()
        })
    })
    it('should notify about changes all tables', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders
        .cleanupThreadOrMessagesNotInXlist([
          new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000),
          new Folder(int64(2001), FolderType.user, 'user', 100, int64(1), 200, 2000),
        ])
        .then((_) => {
          expect(notifyAboutChanges).toBeCalledTimes(2)
          expect(notifyAboutChanges.mock.calls[0][0]).toStrictEqual([EntityKind.thread])
          expect(notifyAboutChanges.mock.calls[1][0]).toStrictEqual([EntityKind.folder_messages])
          done()
        })
    })
    it('should run statements with fids', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders
        .cleanupThreadOrMessagesNotInXlist([
          new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000),
          new Folder(int64(2001), FolderType.user, 'user', 100, int64(1), 200, 2000),
        ])
        .then((_) => {
          expect(runStatement).toBeCalledTimes(2)
          expect((runStatement as any).mock.calls[0][0]).toStrictEqual('DELETE FROM thread WHERE fid NOT IN (1, 2001);')
          expect((runStatement as any).mock.calls[1][0]).toStrictEqual(
            'DELETE FROM folder_messages WHERE fid NOT IN (1, 2001);',
          )
          done()
        })
    })
  })

  describe('updateOverflowCountersForAllFolders', () => {
    it("should run transaction depending on 'transactioned' parameter (true)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.updateOverflowCountersForAllFolders(true).failed((_) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.updateOverflowCountersForAllFolders(false).failed((_) => {
        expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (default, false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.updateOverflowCountersForAllFolders().failed((_) => {
        expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it('should fail if transaction creation fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('TRANSACTION CREATION FAILED')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      folders.updateOverflowCountersForAllFolders(true).failed((error) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        expect(error).toStrictEqual(new YSError('TRANSACTION CREATION FAILED'))
        done()
      })
    })
    it('should run a query to update the counters', (done) => {
      const query = new StringBuilder()
        // tslint:disable-next-line: max-line-length
        .addLine(
          `INSERT INTO ${EntityKind.folder_counters} (fid, overflow_total, overflow_unread, local_total, local_unread)`,
        )
        .addLine('SELECT f.fid,')
        .addLine('max(0, f.total_counter - ifnull(local_total, 0)),')
        .addLine('max(0, f.unread_counter - ifnull(local_unread, 0)),')
        .addLine('ifnull(local_total, 0),')
        .addLine('ifnull(local_unread, 0)')
        .add(`FROM ${EntityKind.folder} AS f LEFT OUTER JOIN folder_counters ON f.fid = folder_counters.fid;`)
        .build()

      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.updateOverflowCountersForAllFolders(true).then((_) => {
        expect(runStatement).toBeCalledWith(query)
        done()
      })
    })
    it('should notify about changes on success', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.updateOverflowCountersForAllFolders(true).then((_) => {
        expect(notifyAboutChanges).toBeCalledWith([EntityKind.folder_counters])
        done()
      })
    })
    it('should fail if the statement fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(reject(new YSError('STATEMENT FAILED')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.updateOverflowCountersForAllFolders(true).failed((error) => {
        expect(error).toStrictEqual(new YSError('STATEMENT FAILED'))
        done()
      })
    })
  })

  describe('insertFolderDefaults', () => {
    it("should run transaction depending on 'transactioned' parameter (true)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.insertFolderDefaults(sampleFolders, false, false, true).failed((_) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.insertFolderDefaults(sampleFolders, false, false, false).failed((_) => {
        expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it("should run transaction depending on 'transactioned' parameter (default, false)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.insertFolderDefaults(sampleFolders, false, false).failed((_) => {
        expect(withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it('should prepare folder-related statements with other tables', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(5)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect(prepareStatement).toBeCalledTimes(4)
        expect(prepareStatement.mock.calls[0][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_lat} (fid, lat) VALUES (?, ?);`,
        )
        expect(prepareStatement.mock.calls[1][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_load_more} (fid) VALUES (?);`,
        )
        expect(prepareStatement.mock.calls[2][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_expand} (fid) VALUES (?);`,
        )
        expect(prepareStatement.mock.calls[3][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_synctype} (fid, sync_type) VALUES (?, ?);`,
        )
        done()
      })
    })
    it('should not create folder lat entities if option is disabled', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer(), false)
      expect.assertions(4)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect(prepareStatement).toBeCalledTimes(3)
        expect(prepareStatement.mock.calls[0][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_load_more} (fid) VALUES (?);`,
        )
        expect(prepareStatement.mock.calls[1][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_expand} (fid) VALUES (?);`,
        )
        expect(prepareStatement.mock.calls[2][0]).toBe(
          `INSERT OR IGNORE INTO ${EntityKind.folder_synctype} (fid, sync_type) VALUES (?, ?);`,
        )
        done()
      })
    })
    it('should notify about changes all tables', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(5)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect(notifyAboutChanges).toBeCalledTimes(4)
        expect(notifyAboutChanges.mock.calls[0][0]).toStrictEqual([EntityKind.folder_lat])
        expect(notifyAboutChanges.mock.calls[1][0]).toStrictEqual([EntityKind.folder_load_more])
        expect(notifyAboutChanges.mock.calls[2][0]).toStrictEqual([EntityKind.folder_expand])
        expect(notifyAboutChanges.mock.calls[3][0]).toStrictEqual([EntityKind.folder_synctype])
        done()
      })
    })
    it('should fail if any operation preparation fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest
        .fn()
        .mockReturnValueOnce(
          resolve({
            execute,
            close: jest.fn(),
          } as StorageStatement),
        )
        .mockReturnValueOnce(
          resolve({
            execute,
            close: jest.fn(),
          } as StorageStatement),
        )
        .mockReturnValueOnce(reject(new YSError('PREPARATION FAILED')))
        .mockReturnValueOnce(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.insertFolderDefaults(sampleFolders, false, false, true).failed((error) => {
        expect(error).toStrictEqual(new YSError('PREPARATION FAILED'))
        done()
      })
    })
    it('should fail if any of operation fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest
        .fn()
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))

        .mockReturnValueOnce(reject(new YSError('FAILED')))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))

        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))

        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.insertFolderDefaults(sampleFolders, false, false, true).failed((error) => {
        expect(error).toStrictEqual(new YSError('FAILED'))
        done()
      })
    })
    it('should call close if any of operation fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest
        .fn()
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))

        .mockReturnValueOnce(reject(new YSError('FAILED')))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))

        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))

        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
        .mockReturnValueOnce(resolve(getVoid()))
      const close = jest.fn()
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      folders.insertFolderDefaults(sampleFolders, false, false, true).failed((error) => {
        expect(error).toStrictEqual(new YSError('FAILED'))
        expect(close).toBeCalledTimes(4)
        done()
      })
    })
    it('should call close if all operations succeed', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const close = jest.fn()
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
        resolve({
          execute,
          close,
        } as StorageStatement),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        prepareStatement,
        notifyAboutChanges,
      })
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect(close).toBeCalledTimes(4)
        done()
      })
    })
    it('last access time modification should be called with Fid and Current Date', (done) => {
      const dateSpy = jest.spyOn(Date, 'now').mockReturnValue(12345)
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect((execute as any).mock.calls[0][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[0].fid), 12345])
        expect((execute as any).mock.calls[1][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[1].fid), 12345])
        expect((execute as any).mock.calls[2][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[2].fid), 12345])
        dateSpy.mockRestore()
        done()
      })
    })
    it('load more modification should be called with Fid', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect((execute as any).mock.calls[3][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[0].fid)])
        expect((execute as any).mock.calls[4][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[1].fid)])
        expect((execute as any).mock.calls[5][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[2].fid)])
        done()
      })
    })
    it('expanded folders should be called with Fid', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        expect((execute as any).mock.calls[6][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[0].fid)])
        expect((execute as any).mock.calls[7][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[1].fid)])
        expect((execute as any).mock.calls[8][0]).toStrictEqual([testIDSupport.toDBValue(sampleFolders[2].fid)])
        done()
      })
    })
    it('sync type for folders should be called with Fid and Sync Type', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const execute: StorageStatement['execute'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(
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
      const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(3)
      folders.insertFolderDefaults(sampleFolders, false, false, true).then((_) => {
        // tslint:disable: max-line-length
        expect((execute as any).mock.calls[9][0]).toStrictEqual([
          testIDSupport.toDBValue(sampleFolders[0].fid),
          FolderSyncType.pushSync,
        ])
        expect((execute as any).mock.calls[10][0]).toStrictEqual([
          testIDSupport.toDBValue(sampleFolders[1].fid),
          FolderSyncType.pushSync,
        ])
        expect((execute as any).mock.calls[11][0]).toStrictEqual([
          testIDSupport.toDBValue(sampleFolders[2].fid),
          FolderSyncType.doNotSync,
        ])
        // tslint:enable: max-line-length
        done()
      })
    })
    describe(Folders.foldersFromCursor, () => {
      it('should build folders having full cursor on Folder table', () => {
        const cursor = MockCursorWithArray([
          [int64(1), folderTypeToInt32(FolderType.inbox), 'inbox', 0, null, 100, 1000],
          [int64(2001), folderTypeToInt32(FolderType.user), 'user', 100, int64(1), 200, 2000],
        ])
        const result = Folders.foldersFromCursor(cursor, testIDSupport)
        expect(result).toStrictEqual([
          new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 100, 1000),
          new Folder(int64(2001), FolderType.user, 'user', 100, int64(1), 200, 2000),
        ])
      })
    })
    describe('isFolderThreaded', () => {
      const threadedFolder = new Folder(int64(1), FolderType.inbox, 'inbox', 0, null, 10, 100)
      const nonThreadedFolder = new Folder(int64(3), FolderType.trash, 'trash', 0, null, 0, 100)
      it("should return false if preferences are set to nonthreaded disregarding folder's type", () => {
        const manager = new Folders(
          MockStorage(),
          new MockSharedPreferences(new Map([[AccountSettingsKeys.threadMode.toString(), false]])),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect(manager.isFolderThreaded(threadedFolder)).toBe(false)
      })
      it("should return false if preferences are set to threaded based on folder's type", () => {
        const manager = new Folders(
          MockStorage(),
          new MockSharedPreferences(new Map([[AccountSettingsKeys.threadMode.toString(), true]])),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect(manager.isFolderThreaded(threadedFolder)).toBe(true)
        expect(manager.isFolderThreaded(nonThreadedFolder)).toBe(false)
      })
    })
    describe('checkWhetherWasLoadedMoreInPeriod', () => {
      afterEach(jest.restoreAllMocks)
      it('should fail if unable to fetch Load More time from the Storage', (done) => {
        const folderManager = new Folders(
          MockStorage({
            runQuery: jest.fn().mockReturnValue(rejected('FAILED')),
          }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        folderManager.checkWhetherWasLoadedMoreInPeriod(int64(1), int64(1000)).failed((e) => {
          expect(e!.message).toBe('FAILED')
          done()
        })
      })
      it('should return true if Load More was made within the period', (done) => {
        jest.spyOn(Registry, 'getHighPrecisionTimer').mockReturnValue({
          getCurrentTimestampInMillis: jest.fn().mockReturnValue(int64(300)),
        })
        const folderManager = new Folders(
          MockStorage({
            runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[100]]))),
          }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        folderManager.checkWhetherWasLoadedMoreInPeriod(int64(1), int64(201)).then((res) => {
          expect(res).toBe(true)
          done()
        })
      })
      it('should return false if Load More was made after the period', (done) => {
        const timer = MockHighPrecisionTimer(() => int64(300))
        const folderManager = new Folders(
          MockStorage({
            runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(100)]]))),
          }),
          new MockSharedPreferences(),
          testIDSupport,
          timer,
        )
        expect.assertions(1)
        folderManager.checkWhetherWasLoadedMoreInPeriod(int64(1), int64(199)).then((res) => {
          expect(res).toBe(false)
          done()
        })
      })
    })
    describe('updateCountersForSingleFolder', () => {
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runStatement: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        manager.updateCountersForSingleFolder(int64(1)).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include fid as parameter in statement', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        manager.updateCountersForSingleFolder(int64(100)).then((_) => {
          expect(storage.runStatement).toBeCalledWith(
            expect.stringContaining(`fid = "${queryValuesFromIds([int64(100)], new TestIDSupport())}"`),
          )
          done()
        })
      })
      it('should notify about changes', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        manager.updateCountersForSingleFolder(int64(100)).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_counters])
          done()
        })
      })
    })
    describe('updateLastAccessTime', () => {
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runStatement: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(() => int64(100)),
        )
        expect.assertions(1)
        manager.updateLastAccessTime(int64(1)).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include fid and timestamp in statement', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(
          storage,
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(() => int64(100)),
        )
        expect.assertions(1)
        manager.updateLastAccessTime(int64(1)).then((_) => {
          expect(storage.runStatement).toBeCalledWith(
            `INSERT OR REPLACE INTO ${EntityKind.folder_lat} (fid, lat) VALUES (1, 100);`,
          )
          done()
        })
      })
      it('should notify about changes', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(
          storage,
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(() => int64(100)),
        )
        expect.assertions(1)
        manager.updateLastAccessTime(int64(1)).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_lat])
          done()
        })
      })
    })
    describe('updateLoadMoreTime', () => {
      afterAll(jest.restoreAllMocks)
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runStatement: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(() => int64(100)),
        )
        expect.assertions(1)
        manager.updateLoadMoreTime(int64(1)).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include fid and timestamp in statement', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(
          storage,
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(() => int64(100)),
        )
        expect.assertions(1)
        manager.updateLoadMoreTime(int64(1)).then((_) => {
          expect(storage.runStatement).toBeCalledWith(
            `INSERT OR REPLACE INTO ${EntityKind.folder_load_more} (fid, load_more_time) VALUES (1, 100);`,
          )
          done()
        })
      })
      it('should notify about changes', (done) => {
        jest.spyOn(Registry, 'getHighPrecisionTimer').mockReturnValue({
          getCurrentTimestampInMillis: jest.fn().mockReturnValue(100),
        })

        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        manager.updateLoadMoreTime(int64(1)).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_load_more])
          done()
        })
      })
    })
    describe('fetchFoldersByType', () => {
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runQuery: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        manager.fetchFoldersByType(FolderType.inbox).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include type in query', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(
            resolve(
              MockCursorWithArray([
                [int64(101), FolderType.user, 'User 1', 0, int64(1), 10, 100],
                [int64(201), FolderType.user, 'User 2', 1, int64(101), 20, 200],
              ]),
            ),
          ),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.fetchFoldersByType(FolderType.inbox).then((res) => {
          expect(storage.runQuery).toBeCalledWith(expect.any(String), [folderTypeToInt32(FolderType.inbox)])
          expect(res).toStrictEqual([
            new Folder(int64(101), FolderType.user, 'User 1', 0, int64(1), 10, 100),
            new Folder(int64(201), FolderType.user, 'User 2', 1, int64(101), 20, 200),
          ])
          done()
        })
      })
    })
    describe('fetchFidsByType', () => {
      it('should return folder ids', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(101)], [int64(201)]]))),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.fetchFidsByType(FolderType.user).then((res) => {
          expect(storage.runQuery).toBeCalledWith(expect.any(String), [folderTypeToInt32(FolderType.user)])
          expect(res).toStrictEqual([int64(101), int64(201)])
          done()
        })
      })
    })
    describe('fetchFirstFidByType', () => {
      it('should return folder id for existing folder', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(101)], [int64(201)]]))),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.fetchFirstFidByType(FolderType.inbox).then((res) => {
          expect(storage.runQuery).toBeCalledWith(expect.any(String), [folderTypeToInt32(FolderType.inbox)])
          expect(res).toBe(int64(101))
          done()
        })
      })
      it('should return null folder id for missing folder', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.fetchFirstFidByType(FolderType.inbox).then((res) => {
          expect(storage.runQuery).toBeCalledWith(expect.any(String), [folderTypeToInt32(FolderType.inbox)])
          expect(res).toBeNull()
          done()
        })
      })
    })
    describe('fetchFidsOfFoldersWithUpgradableBodies', () => {
      it('should return folder ids', (done) => {
        const runQuery = jest
          .fn()
          .mockReturnValueOnce(resolve(MockCursorWithArray([[int64(101)]])))
          .mockReturnValueOnce(resolve(MockCursorWithArray([[int64(201)]])))
        const storage = MockStorage({ runQuery })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(3)
        manager.fetchFidsOfFoldersWithUpgradableBodies().then((res) => {
          expect(runQuery.mock.calls[0]).toEqual([expect.any(String), [folderTypeToInt32(FolderType.draft)]])
          expect(runQuery.mock.calls[1]).toEqual([expect.any(String), [folderTypeToInt32(FolderType.templates)]])
          expect(res).toStrictEqual(new Set([int64(101), int64(201)]))
          done()
        })
      })
    })
    describe('fetchFolderByID', () => {
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runQuery: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        manager.fetchFolderByID(int64(1)).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include ID in query', (done) => {
        const storage = MockStorage({
          runQuery: jest
            .fn()
            .mockReturnValue(
              resolve(MockCursorWithArray([[int64(101), FolderType.user, 'User 1', 0, int64(1), 10, 100]])),
            ),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.fetchFolderByID(int64(1)).then((res) => {
          expect(storage.runQuery).toBeCalledWith(`SELECT * FROM ${EntityKind.folder} WHERE fid = ?;`, [idstr(1)])
          expect(res).toStrictEqual(new Folder(int64(101), FolderType.user, 'User 1', 0, int64(1), 10, 100))
          done()
        })
      })
      it('should return null if Folder with the specified ID is not found', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.fetchFolderByID(int64(1)).then((res) => {
          expect(storage.runQuery).toBeCalledWith(`SELECT * FROM ${EntityKind.folder} WHERE fid = ?;`, [idstr(1)])
          expect(res).toBeNull()
          done()
        })
      })
    })
    describe('getMessagesLoadedInFolder', () => {
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runQuery: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        manager.getMessagesLoadedInFolder(int64(1)).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include FID in query', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[100]]))),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.getMessagesLoadedInFolder(int64(1)).then((res) => {
          expect(storage.runQuery).toBeCalledWith(`SELECT count(*) FROM ${EntityKind.folder_messages} WHERE fid = ?;`, [
            idstr(1),
          ])
          expect(res).toBe(100)
          done()
        })
      })
    })
    describe('getNotSyncedFolderIds', () => {
      it('should fail if the Storage operation fails', (done) => {
        const manager = new Folders(
          MockStorage({ runQuery: jest.fn().mockReturnValue(rejected('FAILED')) }),
          new MockSharedPreferences(),
          testIDSupport,
          MockHighPrecisionTimer(),
        )
        expect.assertions(1)
        manager.getNotSyncedFolderIds([int64(1), int64(2)]).failed((err) => {
          expect(err!.message).toBe('FAILED')
          done()
        })
      })
      it('should include FID in query', (done) => {
        const storage = MockStorage({
          runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(100)]]))),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.getNotSyncedFolderIds([int64(1), int64(2)]).then((res) => {
          expect(storage.runQuery).toBeCalledWith(
            'SELECT DISTINCT fid FROM not_synced_messages WHERE mid IN (1, 2);',
            [],
          )
          expect(res).toStrictEqual([int64(100)])
          done()
        })
      })
    })
    describe(Folders.prototype.insertFolderMessagesConnectionMessages, () => {
      it('should return immediatelly if empty list is passed', (done) => {
        const storage = MockStorage()
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager.insertFolderMessagesConnectionMessages([]).then((_) => {
          expect(storage.prepareStatement).not.toBeCalled()
          expect(storage.notifyAboutChanges).not.toBeCalled()
          done()
        })
      })
      it('should fail if the Storage operation fails', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('FAILED')),
        })
        const storage = MockStorage({ prepareStatement: jest.fn().mockReturnValue(resolve(statement)) })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        manager
          .insertFolderMessagesConnectionMessages([
            new MessageMeta(
              int64(301),
              int64(101),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
            new MessageMeta(
              int64(301),
              int64(101),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
          ])
          .failed((err) => {
            expect(err!.message).toBe('FAILED')
            done()
          })
      })
      it('should save all passed messages', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(3)
        manager
          .insertFolderMessagesConnectionMessages([
            new MessageMeta(
              int64(301),
              int64(101),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
            new MessageMeta(
              int64(302),
              int64(102),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
          ])
          .then((_) => {
            expect(statement.execute).toBeCalledTimes(2)
            expect((statement.execute as any).mock.calls[0][0]).toStrictEqual([idstr(101), idstr(301)])
            expect((statement.execute as any).mock.calls[1][0]).toStrictEqual([idstr(102), idstr(302)])
            done()
          })
      })
      it('should close statement if succeeded', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        manager
          .insertFolderMessagesConnectionMessages([
            new MessageMeta(
              int64(301),
              int64(101),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
          ])
          .then((_) => {
            expect(statement.close).toBeCalled()
            done()
          })
      })
      it('should notify about changes if succeeded', (done) => {
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(MockStorageStatement())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        manager
          .insertFolderMessagesConnectionMessages([
            new MessageMeta(
              int64(301),
              int64(101),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
          ])
          .then((_) => {
            expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
            done()
          })
      })
      it('should close statement if failed', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('FAILED')),
        })
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        })
        const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        manager
          .insertFolderMessagesConnectionMessages([
            new MessageMeta(
              int64(301),
              int64(101),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
            new MessageMeta(
              int64(302),
              int64(102),
              null,
              [],
              false,
              null,
              '',
              '',
              '',
              false,
              false,
              null,
              int64(0),
              false,
              null,
              MessageTypeFlags.people,
            ),
          ])
          .failed((e) => {
            expect(statement.close).toBeCalled()
            expect(e!.message).toBe('FAILED')
            done()
          })
      })
    })
    describe(Folders.prototype.insertFolderMessagesConnectionMids, () => {
      it('should return immediatelly if empty list is passed', (done) => {
        const storage = MockStorage()
        const folders = makeFolders({ storage })
        expect.assertions(2)
        folders.insertFolderMessagesConnectionMids([], int64(1)).then((_) => {
          expect(storage.prepareStatement).not.toBeCalled()
          expect(storage.notifyAboutChanges).not.toBeCalled()
          done()
        })
      })
      it('should fail if the Storage operation fails', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('FAILED')),
        })
        const storage = MockStorage({ prepareStatement: jest.fn().mockReturnValue(resolve(statement)) })
        const folders = makeFolders({ storage })
        expect.assertions(1)
        folders.insertFolderMessagesConnectionMids([int64(301), int64(301)], int64(1)).failed((err) => {
          expect(err.message).toBe('FAILED')
          done()
        })
      })
      it('should modify all passed messages', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = makeFolders({ storage })
        expect.assertions(3)
        folders.insertFolderMessagesConnectionMids([int64(301), int64(302)], int64(1)).then((_) => {
          expect(statement.execute).toBeCalledTimes(2)
          expect(statement.execute).toBeCalledWith([idstr(1), idstr(301)])
          expect(statement.execute).toBeCalledWith([idstr(1), idstr(302)])
          done()
        })
      })
      it('should close statement if succeeded', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = makeFolders({ storage })
        expect.assertions(1)
        folders.insertFolderMessagesConnectionMids([int64(301)], int64(1)).then((_) => {
          expect(statement.close).toBeCalled()
          done()
        })
      })
      it('should notify about changes if succeeded', (done) => {
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(MockStorageStatement())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = makeFolders({ storage })
        expect.assertions(1)
        folders.insertFolderMessagesConnectionMids([int64(301)], int64(1)).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
          done()
        })
      })
      it('should close statement if failed', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('FAILED')),
        })
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        })
        const folders = makeFolders({ storage })
        expect.assertions(2)
        folders.insertFolderMessagesConnectionMids([int64(301), int64(302)], int64(1)).failed((e) => {
          expect(statement.close).toBeCalled()
          expect(e.message).toBe('FAILED')
          done()
        })
      })
    })
    describe('deleteByID', () => {
      it('should return immediatelly if empty set is passed', (done) => {
        const storage = MockStorage()
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.deleteByIDs([], true).then((_) => {
          expect(storage.withinTransaction).not.toBeCalled()
          expect(storage.notifyAboutChanges).not.toBeCalled()
          done()
        })
      })
      it('should create transaction based on the argument passed (true)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.deleteByIDs([int64(101)], true).failed((e) => {
          expect(e.message).toBe('NO MATTER')
          expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
          done()
        })
      })
      it('should create transaction based on the argument passed (false)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.deleteByIDs([int64(101)], false).failed((e) => {
          expect(e.message).toBe('NO MATTER')
          expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
          done()
        })
      })
      it('should run deletion statements for folders IDs passed in', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const fids = [int64(101), int64(102)]
        expect.assertions(1)
        folders.deleteByIDs(fids, true).then((_) => {
          expect(storage.runStatement).toBeCalledWith('DELETE FROM folder WHERE fid IN (101, 102);')
          done()
        })
      })
      it('should notify about changes', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const fids = [int64(101), int64(102)]
        expect.assertions(1)
        folders.deleteByIDs(fids, true).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder])
          done()
        })
      })
      it('should fail if any of the operation fails', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(rejected('FAILED')),
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const fids = [int64(101), int64(102)]
        expect.assertions(1)
        folders.deleteByIDs(fids, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
    })
    describe('insertOrAbortFolders', () => {
      it('should return immediatelly if empty set is passed', (done) => {
        const storage = MockStorage()
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.insertOrAbortFolders([], true).then((_) => {
          expect(storage.withinTransaction).not.toBeCalled()
          expect(storage.notifyAboutChanges).not.toBeCalled()
          done()
        })
      })
      it('should create transaction based on the argument passed (true)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        const sample = new Folder(int64(101), FolderType.inbox, null, 0, null, 0, 0)
        folders.insertOrAbortFolders([sample], true).failed((e) => {
          expect(e.message).toBe('NO MATTER')
          expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
          done()
        })
      })
      it('should create transaction based on the argument passed (false)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        const sample = new Folder(int64(101), FolderType.inbox, null, 0, null, 0, 0)
        folders.insertOrAbortFolders([sample], false).failed((e) => {
          expect(e.message).toBe('NO MATTER')
          expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
          done()
        })
      })
      it('should create insertion statements for folders passed in', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const samples = [
          new Folder(int64(101), FolderType.inbox, 'name1', 1, null, 10, 100),
          new Folder(int64(102), FolderType.user, 'name2', 2, int64(101), 5, 50),
        ]
        expect.assertions(5)
        folders.insertOrAbortFolders(samples, true).then((_) => {
          expect(storage.prepareStatement).toBeCalled()
          expect(statement.execute).toBeCalledTimes(2)
          expect((statement.execute as any).mock.calls[0][0]).toEqual([
            idstr(samples[0].fid),
            folderTypeToInt32(samples[0].type),
            samples[0].name,
            samples[0].position,
            samples[0].parent !== null ? idstr(samples[0].parent) : null,
            samples[0].unreadCounter,
            samples[0].totalCounter,
          ])
          expect((statement.execute as any).mock.calls[1][0]).toEqual([
            idstr(samples[1].fid),
            folderTypeToInt32(samples[1].type),
            samples[1].name,
            samples[1].position,
            samples[1].parent !== null ? idstr(samples[1].parent) : null,
            samples[1].unreadCounter,
            samples[1].totalCounter,
          ])
          expect(statement.close).toBeCalled()
          done()
        })
      })
      it('should notify about changes', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const samples = [
          new Folder(int64(101), FolderType.inbox, 'name1', 1, int64(101), 10, 100),
          new Folder(int64(102), FolderType.user, 'name2', 2, int64(101), 5, 50),
        ]
        expect.assertions(1)
        folders.insertOrAbortFolders(samples, true).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder])
          done()
        })
      })
      it('should close statement if any of the operation fails', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('FAILED')),
        })
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const samples = [
          new Folder(int64(101), FolderType.inbox, 'name1', 1, int64(101), 10, 100),
          new Folder(int64(102), FolderType.user, 'name2', 2, int64(101), 5, 50),
        ]
        expect.assertions(2)
        folders.insertOrAbortFolders(samples, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          expect(statement.close).toBeCalled()
          done()
        })
      })
    })
    describe('createFromDeltaApi', () => {
      afterEach(jest.restoreAllMocks)
      it('should fail if transaction creation fails', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('FAILED')),
        })

        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should fail if folder creation fails', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        jest.spyOn(folders, 'insertOrAbortFolders').mockReturnValue(rejected('FAILED'))
        jest.spyOn(folders, 'insertFolderDefaults').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'cleanupOrphanFoldersEntities').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'updateOverflowCountersForAllFolders').mockReturnValue(resolve(getVoid()))
        expect.assertions(1)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should fail if folder defaults insertion creation fails', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        jest.spyOn(folders, 'insertOrAbortFolders').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'insertFolderDefaults').mockReturnValue(rejected('FAILED'))
        jest.spyOn(folders, 'cleanupOrphanFoldersEntities').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'updateOverflowCountersForAllFolders').mockReturnValue(resolve(getVoid()))
        expect.assertions(1)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should fail if folder cleanup fails', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        jest.spyOn(folders, 'insertOrAbortFolders').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'insertFolderDefaults').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'cleanupOrphanFoldersEntities').mockReturnValue(rejected('FAILED'))
        jest.spyOn(folders, 'updateOverflowCountersForAllFolders').mockReturnValue(resolve(getVoid()))
        expect.assertions(1)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should fail if counters update fails', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        jest.spyOn(folders, 'insertOrAbortFolders').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'insertFolderDefaults').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'cleanupOrphanFoldersEntities').mockReturnValue(resolve(getVoid()))
        jest.spyOn(folders, 'updateOverflowCountersForAllFolders').mockReturnValue(rejected('FAILED'))
        expect.assertions(1)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should create transaction with transactioned (false)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, false).failed((e) => {
          expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
          expect(e.message).toBe('NO MATTER')
          done()
        })
      })
      it('should create transaction with transactioned (true)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
          expect(e.message).toBe('NO MATTER')
          done()
        })
      })
      it('should create folder, insert defauls, do cleanup, recalculate counters', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const insertOrAbortFolders = jest.spyOn(folders, 'insertOrAbortFolders').mockReturnValue(resolve(getVoid()))
        const insertFolderDefaults = jest.spyOn(folders, 'insertFolderDefaults').mockReturnValue(resolve(getVoid()))
        const cleanUpOrphanFolderDefaults = jest
          .spyOn(folders, 'cleanupOrphanFoldersEntities')
          .mockReturnValue(resolve(getVoid()))
        const updateOverflowCountersForAllFolders = jest
          .spyOn(folders, 'updateOverflowCountersForAllFolders')
          .mockReturnValue(resolve(getVoid()))
        const expected = [
          new Folder(
            idFromString(folder.id)!,
            FolderType.inbox,
            folder.name,
            0,
            idFromString(folder.parentId),
            folder.unreadMessagesCount,
            folder.messagesCount,
          ),
        ]
        expect.assertions(4)
        folders.createFromDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).then((_) => {
          expect(insertOrAbortFolders).toBeCalledWith(expected, false)
          expect(insertFolderDefaults).toBeCalledWith(expected, false, false)
          expect(cleanUpOrphanFolderDefaults).toBeCalledWith(expected)
          expect(updateOverflowCountersForAllFolders).toBeCalled()
          done()
        })
      })
    })
    describe('updateWithDeltaApi', () => {
      afterEach(jest.restoreAllMocks)
      it('should fail if transaction creation fails', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('FAILED')),
        })

        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        folders.updateWithDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should fail if folder update fails', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValue(rejected('FAILED')),
        })
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        folders.updateWithDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          done()
        })
      })
      it('should create transaction with transactioned (false)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.updateWithDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, false).failed((e) => {
          expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
          expect(e.message).toBe('NO MATTER')
          done()
        })
      })
      it('should create transaction with transactioned (true)', (done) => {
        const storage = MockStorage({
          withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.updateWithDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
          expect(e.message).toBe('NO MATTER')
          done()
        })
      })
      it('should close statement disregarding execution result (positive)', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(1)
        folders.updateWithDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).then((_) => {
          expect(statement.close).toBeCalled()
          done()
        })
      })
      it('should close statement disregarding execution result (negative)', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValue(rejected('FAILED')),
        })
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        expect.assertions(2)
        folders.updateWithDeltaApi(DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!, true).failed((e) => {
          expect(e.message).toBe('FAILED')
          expect(statement.close).toBeCalled()
          done()
        })
      })
      it('should update folder', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const sample = DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!
        expect.assertions(1)
        folders.updateWithDeltaApi(sample, true).then((_) => {
          expect(statement.execute).toBeCalledWith([
            idstr(sample.fid),
            FolderType.inbox,
            sample.name,
            0,
            sample.parentId !== null ? idstr(sample.parentId) : null,
            sample.unreadMessagesCount,
            sample.messagesCount,
          ])
          done()
        })
      })
      it('should update folder (null parent)', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const data = Object.assign({}, folder, { parentId: '123' })
        const sample = DeltaApiFolder.fromJSONItem(JSONItemFromJSON(data))!
        expect.assertions(1)
        folders.updateWithDeltaApi(sample, true).then((_) => {
          expect(statement.execute).toBeCalledWith([
            idstr(sample.fid),
            FolderType.inbox,
            sample.name,
            0,
            sample.parentId !== null ? idstr(sample.parentId) : null,
            sample.unreadMessagesCount,
            sample.messagesCount,
          ])
          done()
        })
      })
      it('should notify about changes', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        const sample = DeltaApiFolder.fromJSONItem(JSONItemFromJSON(folder))!
        expect.assertions(1)
        folders.updateWithDeltaApi(sample, true).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder])
          done()
        })
      })
    })
    describe('updateLocalCountersForAllFolders', () => {
      it('should run SQL query (no parameters)', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        folders.updateLocalCountersForAllFolders().then((_) => {
          expect(storage.runStatement).toBeCalledWith(expect.any(String))
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_counters])
          done()
        })
      })
    })
    describe('cleanUpFolderMessagesConnection', () => {
      describe('with fid (fid !== null)', () => {
        it('should return immediatelly if the set of mids is empty', (done) => {
          const storage = MockStorage()
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          folders.cleanUpFolderMessagesConnection([], int64(1)).then((_) => {
            expect(storage.prepareStatement).not.toBeCalled()
            expect(storage.notifyAboutChanges).not.toBeCalled()
            done()
          })
        })
        it('should pass mids and fid in the query', (done) => {
          const statement = MockStorageStatement()
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids, int64(101)).then((_) => {
            expect(storage.prepareStatement).toBeCalledWith(
              `DELETE FROM ${EntityKind.folder_messages} WHERE fid = ? AND mid IN (301, 302);`,
            )
            expect(statement.execute).toBeCalledWith([idstr(101)])
            done()
          })
        })
        it('should close the statement disregarding the result (positive)', (done) => {
          const statement = MockStorageStatement()
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids, int64(101)).then((_) => {
            expect(statement.close).toBeCalled()
            done()
          })
        })
        it('should notify about changes', (done) => {
          const statement = MockStorageStatement()
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids, int64(101)).then((_) => {
            expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
            done()
          })
        })
        it('should close the statement disregarding the result (negative)', (done) => {
          const statement = MockStorageStatement({
            execute: jest.fn().mockReturnValue(rejected('FAILED')),
          })
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids, int64(101)).failed((e) => {
            expect(e.message).toBe('FAILED')
            expect(statement.close).toBeCalled()
            done()
          })
        })
      })
      describe('without fid (fid === null)', () => {
        it('should return immediatelly if the set of mids is empty', (done) => {
          const storage = MockStorage()
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          folders.cleanUpFolderMessagesConnection([]).then((_) => {
            expect(storage.prepareStatement).not.toBeCalled()
            expect(storage.notifyAboutChanges).not.toBeCalled()
            done()
          })
        })
        it('should pass mids in the query', (done) => {
          const statement = MockStorageStatement()
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids).then((_) => {
            expect(storage.prepareStatement).toBeCalledWith(
              `DELETE FROM ${EntityKind.folder_messages} WHERE mid IN (301, 302);`,
            )
            expect(statement.execute).toBeCalledWith([])
            done()
          })
        })
        it('should close the statement disregarding the result (positive)', (done) => {
          const statement = MockStorageStatement()
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids).then((_) => {
            expect(statement.close).toBeCalled()
            done()
          })
        })
        it('should notify about changes', (done) => {
          const statement = MockStorageStatement()
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
            notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids).then((_) => {
            expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
            done()
          })
        })
        it('should close the statement disregarding the result (negative)', (done) => {
          const statement = MockStorageStatement({
            execute: jest.fn().mockReturnValue(rejected('FAILED')),
          })
          const storage = MockStorage({
            prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          })
          const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
          const mids = [int64(301), int64(302)]
          folders.cleanUpFolderMessagesConnection(mids).failed((e) => {
            expect(e.message).toBe('FAILED')
            expect(statement.close).toBeCalled()
            done()
          })
        })
      })
    })
  })

  describe('cleanUpAllFolderMessagesConnection', () => {
    describe('with fid (fid !== null)', () => {
      it('should pass fid in the query', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        folders.cleanUpAllFolderMessagesConnection(int64(808)).then((_) => {
          expect(storage.prepareStatement).toBeCalledWith(`DELETE FROM ${EntityKind.folder_messages} WHERE fid = ?;`)
          expect(statement.execute).toBeCalledWith([idstr(808)])
          done()
        })
      })
      it('should close the statement disregarding the result (positive)', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        folders.cleanUpAllFolderMessagesConnection(int64(14)).then((_) => {
          expect(statement.close).toBeCalled()
          done()
        })
      })
      it('should notify about changes', (done) => {
        const statement = MockStorageStatement()
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        folders.cleanUpAllFolderMessagesConnection(int64(22)).then((_) => {
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
          done()
        })
      })
      it('should close the statement disregarding the result (negative)', (done) => {
        const statement = MockStorageStatement({
          execute: jest.fn().mockReturnValue(rejected('FAILED')),
        })
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        })
        const folders = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
        folders.cleanUpAllFolderMessagesConnection(int64(34)).failed((e) => {
          expect(e.message).toBe('FAILED')
          expect(statement.close).toBeCalled()
          done()
        })
      })
    })
  })

  describe('getDefaultFolder', () => {
    it('should fail if the Storage operation fails', (done) => {
      const manager = new Folders(
        MockStorage({ runQuery: jest.fn().mockReturnValue(rejected('FAILED')) }),
        new MockSharedPreferences(),
        testIDSupport,
        MockHighPrecisionTimer(),
      )
      expect.assertions(1)
      manager.getDefaultFolder().failed((err) => {
        expect(err!.message).toBe('FAILED')
        done()
      })
    })
    it('should return folder if it exists', (done) => {
      const storage = MockStorage({
        runQuery: jest
          .fn()
          .mockReturnValue(
            resolve(
              MockCursorWithArray([[idstr(101), FolderType.user, 'User 1', 0, int64ToString(int64(1)), 10, 100]]),
            ),
          ),
      })
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(2)
      manager.getDefaultFolder().then((res) => {
        expect(storage.runQuery).toBeCalledWith('SELECT * FROM folder WHERE type IN (?, ?) LIMIT 1;', [
          folderTypeToInt32(FolderType.inbox),
          folderTypeToInt32(FolderType.tab_relevant),
        ])
        expect(res).toStrictEqual(new Folder(int64(101), FolderType.user, 'User 1', 0, int64(1), 10, 100))
        done()
      })
    })
    it('should return null if folder is missing', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      manager.getDefaultFolder().then((res) => {
        expect(res).toBeNull()
        done()
      })
    })
  })
  describe('getDefaultFid', () => {
    it('should return fid for existing folder', (done) => {
      const manager = new Folders(MockStorage(), new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      jest
        .spyOn(manager, 'getDefaultFolder')
        .mockReturnValue(resolve(new Folder(int64(101), FolderType.user, 'User 1', 0, int64(1), 10, 100)))
      expect.assertions(1)
      manager.getDefaultFid().then((fid) => {
        expect(fid).toBe(int64(101))
        done()
      })
    })
    it('should return null for missing folder', (done) => {
      const manager = new Folders(MockStorage(), new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      jest.spyOn(manager, 'getDefaultFolder').mockReturnValue(resolve(null))
      expect.assertions(1)
      manager.getDefaultFid().then((fid) => {
        expect(fid).toBeNull()
        done()
      })
    })
  })
  describe('updateFolderMessagesConnection', () => {
    it('should update DB and notify about DB update', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      manager.updateFolderMessagesConnection(int64(54321), int64(12345), int64(2)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          'UPDATE folder_messages SET mid = 54321, fid = 2 WHERE mid = 12345;',
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
        done()
      })
    })
  })
  describe('insertFolderCounters', () => {
    it('should return immediatelly if nothing to save', (done) => {
      const storage = MockStorage()
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      expect.assertions(1)
      manager.insertFolderCounters([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        done()
      })
    })
    it('should prepare statement and run for arguments', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      const updateLocalCountersForAllFolders = jest
        .spyOn(manager, 'updateLocalCountersForAllFolders')
        .mockReturnValue(resolve(getVoid()))
      const updateOverflowCountersForAllFolders = jest
        .spyOn(manager, 'updateOverflowCountersForAllFolders')
        .mockReturnValue(resolve(getVoid()))

      expect.assertions(7)
      manager
        .insertFolderCounters([
          DeltaApiFolderCountersUpdateItem.fromJSONItem(
            JSONItemFromJSON({
              fid: '-10',
              tab: 'relevant',
              unread: 10,
              total: 100,
            }),
          )!,
          DeltaApiFolderCountersUpdateItem.fromJSONItem(
            JSONItemFromJSON({
              fid: '7',
              tab: '',
              unread: 20,
              total: 200,
            }),
          )!,
        ])
        .then((_) => {
          expect(storage.prepareStatement).toBeCalledWith(
            `UPDATE ${EntityKind.folder} SET unread_counter = ?, total_counter = ? WHERE fid = ?;`,
          )
          expect(statement.execute).toBeCalledTimes(2)
          expect(statement.execute).toBeCalledWith([10, 100, idstr(-10)])
          expect(statement.execute).toBeCalledWith([20, 200, idstr(7)])
          expect(statement.close).toBeCalled()
          expect(updateLocalCountersForAllFolders).toBeCalled()
          expect(updateOverflowCountersForAllFolders).toBeCalledWith(false)
          done()
        })
    })
    it('should close statement disregarding the result', (done) => {
      const statement = MockStorageStatement({
        execute: jest
          .fn()
          .mockReturnValueOnce(resolve(getVoid()))
          .mockReturnValueOnce(rejected('ERROR'))
          .mockReturnValueOnce(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      const updateLocalCountersForAllFolders = jest
        .spyOn(manager, 'updateLocalCountersForAllFolders')
        .mockReturnValue(resolve(getVoid()))
      const updateOverflowCountersForAllFolders = jest
        .spyOn(manager, 'updateOverflowCountersForAllFolders')
        .mockReturnValue(resolve(getVoid()))
      expect.assertions(4)
      manager
        .insertFolderCounters([
          DeltaApiFolderCountersUpdateItem.fromJSONItem(
            JSONItemFromJSON({
              fid: '-10',
              tab: 'relevant',
              unread: 10,
              total: 100,
            }),
          )!,
          DeltaApiFolderCountersUpdateItem.fromJSONItem(
            JSONItemFromJSON({
              fid: '7',
              tab: '',
              unread: 20,
              total: 200,
            }),
          )!,
          DeltaApiFolderCountersUpdateItem.fromJSONItem(
            JSONItemFromJSON({
              fid: '-11',
              tab: 'news',
              unread: 30,
              total: 300,
            }),
          )!,
        ])
        .failed((e) => {
          expect(e.message).toBe('ERROR')
          expect(statement.close).toBeCalled()
          expect(updateLocalCountersForAllFolders).not.toBeCalled()
          expect(updateOverflowCountersForAllFolders).not.toBeCalled()
          done()
        })
    })
  })
  describe(Folders.prototype.getFoldersSyncType, () => {
    it('should return with empty map if no affected fids are passed', (done) => {
      const storage = MockStorage()
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      manager.getFoldersSyncType(new Set()).then((res) => {
        expect(res.size).toBe(0)
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should return map of ID to its type', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(
          resolve(
            MockCursorWithArray([
              [int64(1), folderSyncTypeToInt32(FolderSyncType.pushSync)],
              [int64(3), folderSyncTypeToInt32(FolderSyncType.doNotSync)],
            ]),
          ),
        ),
      })
      const manager = new Folders(storage, new MockSharedPreferences(), testIDSupport, MockHighPrecisionTimer())
      manager.getFoldersSyncType(new Set([int64(1), int64(3)])).then((res) => {
        expect(res.size).toBe(2)
        expect(res).toStrictEqual(
          new Map([
            [int64(1), FolderSyncType.pushSync],
            [int64(3), FolderSyncType.doNotSync],
          ]),
        )
        expect(storage.runQuery).toBeCalledWith(`SELECT * FROM ${EntityKind.folder_synctype} WHERE fid IN (1, 3);`, [])
        done()
      })
    })
  })
  describe(folderSyncType, () => {
    it('should return Sync And Push for Inbox', () => {
      expect(folderSyncType(new Folder(int64(1), FolderType.inbox, null, 0, null, 0, 0), true, false)).toBe(
        FolderSyncType.pushSync,
      )
      expect(folderSyncType(new Folder(int64(1), FolderType.inbox, null, 0, null, 0, 0), false, true)).toBe(
        FolderSyncType.pushSync,
      )
      expect(folderSyncType(new Folder(int64(1), FolderType.inbox, null, 0, null, 0, 0), false, false)).toBe(
        FolderSyncType.pushSync,
      )
    })
    it('should return Sync And Push for Relevant Tabs', () => {
      expect(folderSyncType(new Folder(int64(1), FolderType.tab_relevant, null, 0, null, 0, 0), true, false)).toBe(
        FolderSyncType.pushSync,
      )
      expect(folderSyncType(new Folder(int64(1), FolderType.tab_relevant, null, 0, null, 0, 0), false, true)).toBe(
        FolderSyncType.pushSync,
      )
      expect(folderSyncType(new Folder(int64(1), FolderType.tab_relevant, null, 0, null, 0, 0), false, false)).toBe(
        FolderSyncType.pushSync,
      )
    })
    it('should return Silent Sync for Social and News tabs', () => {
      for (const tabType of [FolderType.tab_news, FolderType.tab_social]) {
        expect(folderSyncType(new Folder(int64(1), tabType, null, 0, null, 0, 0), true, false)).toBe(
          FolderSyncType.silentSync,
        )
        expect(folderSyncType(new Folder(int64(1), tabType, null, 0, null, 0, 0), false, true)).toBe(
          FolderSyncType.silentSync,
        )
        expect(folderSyncType(new Folder(int64(1), tabType, null, 0, null, 0, 0), false, false)).toBe(
          FolderSyncType.silentSync,
        )
      }
    })
    it('should return Silent Sync for Gmail user folders', () => {
      expect(folderSyncType(new Folder(int64(1), FolderType.user, null, 0, null, 0, 0), true, false)).toBe(
        FolderSyncType.silentSync,
      )
    })
    it('should return No Sync for Corp user folders', () => {
      expect(folderSyncType(new Folder(int64(1), FolderType.user, null, 0, null, 0, 0), false, true)).toBe(
        FolderSyncType.doNotSync,
      )
    })
    it('should return Sync and Push for generic user folders', () => {
      expect(folderSyncType(new Folder(int64(1), FolderType.user, null, 0, null, 0, 0), false, false)).toBe(
        FolderSyncType.pushSync,
      )
    })
    it('should return No Sync for other folders', () => {
      for (let type = FolderType.outgoing; type < FolderType.unsubscribe; ++type) {
        expect(folderSyncType(new Folder(int64(1), type, null, 0, null, 0, 0), false, false)).toBe(
          FolderSyncType.doNotSync,
        )
        expect(folderSyncType(new Folder(int64(1), type, null, 0, null, 0, 0), true, false)).toBe(
          FolderSyncType.doNotSync,
        )
        expect(folderSyncType(new Folder(int64(1), type, null, 0, null, 0, 0), false, true)).toBe(
          FolderSyncType.doNotSync,
        )
      }
    })
  })
})
