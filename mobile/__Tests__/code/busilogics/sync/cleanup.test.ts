import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { StringBuilder } from '../../../../../mapi/code/utils/string-builder'
import { IDSupport } from '../../../../code/api/common/id-support'
import { HighPrecisionTimer } from '../../../../code/api/logging/perf/high-precision-timer'
import { Network } from '../../../../../common/code/network/network'
import { SharedPreferences } from '../../../../../common/code/shared-prefs/shared-preferences'
import { queryValuesFromIds } from '../../../../code/api/storage/query-helpers'
import { Storage } from '../../../../code/api/storage/storage'
import { Folders } from '../../../../code/busilogics/folders/folders'
import { Cleanup } from '../../../../code/busilogics/sync/cleanup'
import { Threads } from '../../../../code/busilogics/threads/threads'
import { MockHighPrecisionTimer, MockStorage } from '../../../__helpers__/mock-patches'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { TestIDSupport } from '../../../__helpers__/test-id-support'

function makeThreads({
  network,
  storage,
  idSupport,
}: {
  network?: Network
  storage?: Storage
  idSupport?: IDSupport
}): Threads {
  return new Threads(network || MockNetwork(), storage || MockStorage(), idSupport || new TestIDSupport())
}

function makeFolders({
  storage,
  prefs,
  idSupport,
  timer,
}: {
  storage?: Storage
  prefs?: SharedPreferences
  idSupport?: IDSupport
  timer?: HighPrecisionTimer
}): Folders {
  return new Folders(
    storage || MockStorage(),
    prefs || new MockSharedPreferences(),
    idSupport || new TestIDSupport(),
    timer || MockHighPrecisionTimer(),
  )
}

function mockCleanup(
  storage: Storage,
  idSupport: IDSupport = new TestIDSupport(),
  threads: Threads = makeThreads({ storage, idSupport }),
  folders: Folders = makeFolders({ storage, idSupport }),
): Cleanup {
  return new Cleanup(storage, threads, folders, idSupport)
}

const testIDSupport = new TestIDSupport()

describe(Cleanup, () => {
  afterEach(jest.restoreAllMocks)
  describe('removeOrphans', () => {
    it('should return immediatelly if the passed set is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([]))
      const cleanup = mockCleanup(storage, testIDSupport, threads)
      expect.assertions(1)
      cleanup.removeOrphans([]).then(() => {
        expect(getTidsByMidsSpy).not.toBeCalled()
        done()
      })
    })
    it('gets tids by mids, then update tops for the tids, and cleanup obsolete threads', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([int64(401), int64(402)]))
      const updateTopsSpy = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const cleanupThreadsAfterMessageDeletionSpy = jest
        .spyOn(threads, 'cleanupThreadsAfterMessageDeletion')
        .mockReturnValue(resolve(getVoid()))
      const cleanup = mockCleanup(storage, testIDSupport, threads)
      expect.assertions(3)
      cleanup.removeOrphans([int64(301), int64(302)]).then(() => {
        expect(getTidsByMidsSpy).toBeCalledWith([int64(301), int64(302)])
        expect(updateTopsSpy).toBeCalledWith([int64(401), int64(402)])
        expect(cleanupThreadsAfterMessageDeletionSpy).toBeCalled()
        done()
      })
    })
  })
  describe('rebuildAggregates', () => {
    it('should return immediatelly if the passed set is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([]))
      const cleanup = mockCleanup(storage, new TestIDSupport(), threads)
      expect.assertions(1)
      cleanup.rebuildAggregates([]).then((_) => {
        expect(getTidsByMidsSpy).not.toBeCalled()
        done()
      })
    })
    it('gets tids by mids, update local counters for all folders and rebuild thread counters', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const folders = makeFolders({ storage })
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([int64(401), int64(402)]))
      const rebuildThreadCounter = jest.spyOn(threads, 'rebuildThreadCounter').mockReturnValue(resolve(getVoid()))
      const updateLocalCountersForAllFoldersSpy = jest
        .spyOn(folders, 'updateLocalCountersForAllFolders')
        .mockReturnValue(resolve(getVoid()))
      const cleanup = mockCleanup(storage, testIDSupport, threads, folders)
      expect.assertions(3)
      cleanup.rebuildAggregates([int64(301), int64(302)]).then(() => {
        expect(getTidsByMidsSpy).toBeCalledWith([int64(301), int64(302)])
        expect(rebuildThreadCounter).toBeCalledWith([int64(401), int64(402)])
        expect(updateLocalCountersForAllFoldersSpy).toBeCalled()
        done()
      })
    })
  })
  describe('rebuildCounters', () => {
    it('should return immediatelly if the passed set is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([]))
      const cleanup = mockCleanup(storage, testIDSupport, threads)
      expect.assertions(1)
      cleanup.rebuildCounters([]).then((_) => {
        expect(getTidsByMidsSpy).not.toBeCalled()
        done()
      })
    })
    it('gets tids by mids, update local counters for all folders and rebuild thread counters', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const folders = makeFolders({ storage })
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([int64(401), int64(402)]))
      const rebuildThreadCounter = jest.spyOn(threads, 'rebuildThreadCounter').mockReturnValue(resolve(getVoid()))
      const updateLocalCountersForAllFoldersSpy = jest
        .spyOn(folders, 'updateLocalCountersForAllFolders')
        .mockReturnValue(resolve(getVoid()))
      const cleanup = mockCleanup(storage, testIDSupport, threads, folders)
      expect.assertions(3)
      cleanup.rebuildCounters([int64(301), int64(302)]).then(() => {
        expect(getTidsByMidsSpy).toBeCalledWith([int64(301), int64(302)])
        expect(rebuildThreadCounter).toBeCalledWith([int64(401), int64(402)])
        expect(updateLocalCountersForAllFoldersSpy).toBeCalled()
        done()
      })
    })
  })
  describe('cleanUpFolderMessagesOrphans', () => {
    it('should run the cleanup statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      expect.assertions(2)
      cleanup.cleanUpFolderMessagesOrphans(int64(101)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          expect.stringContaining(`fid = ${queryValuesFromIds([int64(101)], testIDSupport)}`),
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('cleanupAttachOrphans', () => {
    it('should run the cleanup statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanupAttachOrphans([int64(101), int64(202), int64(303)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.attachment} WHERE mid NOT IN (101, 202, 303);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.attachment])
        done()
      })
    })
    it('should do nothing if mids have length zero', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanupAttachOrphans([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })
  describe('cleanupInlineAttachOrphans', () => {
    it('should run the cleanup statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanupInlineAttachOrphans([int64(101), int64(202), int64(303)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.inline_attach} WHERE mid NOT IN (101, 202, 303);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.inline_attach])
        done()
      })
    })
    it('should do nothing if mids have length zero', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanupInlineAttachOrphans([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })
  describe('cleanupLabelOrphans', () => {
    it('should run the cleanup statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanUpLabelsOrphans([int64(101), int64(202), int64(303)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.labels_messages} WHERE mid NOT IN (101, 202, 303);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
        done()
      })
    })
    it('should do nothing if mids have length zero', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanUpLabelsOrphans([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })
  describe('resetCountersForFolder', () => {
    it('should run the statement resetting counter for folder', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.resetCountersForFolder(int64(10)).then((_) => {
        const expectedSql = new StringBuilder()
        expectedSql.add('INSERT OR REPLACE INTO folder_counters')
        expectedSql.add(' (fid, overflow_total, overflow_unread, local_total, local_unread)')
        expectedSql.add(' VALUES (10, 0, 0, 0, 0);')
        expect(storage.runStatement).toBeCalledWith(expectedSql.build())
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_counters])
        done()
      })
    })
    it('should do nothing if mids have length zero', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      cleanup.cleanUpLabelsOrphans([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })
  describe('cleanUpThreadMessageOrphans', () => {
    it('should run the cleanup statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      expect.assertions(2)
      cleanup.cleanUpThreadMessageOrphans(int64(101)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          expect.stringContaining(`fid = ${queryValuesFromIds([int64(101)], testIDSupport)}`),
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('cleanUpFolderThreadsByTops', () => {
    it('should run the cleanup statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const cleanup = mockCleanup(storage)
      expect.assertions(2)
      cleanup.cleanUpFolderThreadsByTops().then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.thread} WHERE top_mid = ${queryValuesFromIds([int64(-1)], testIDSupport)};`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread])
        done()
      })
    })
  })
})
