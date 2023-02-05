import { resolve } from '../../../../../../common/xpromise-support'
import { Int32, int32ToInt64, int64 } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { FolderType, folderTypeToInt32 } from '../../../../../mapi/code/api/entities/folder/folder'
import { messageMetaFromJSONItem } from '../../../../../mapi/code/api/entities/message/message-meta'
import { MessageRequestItem } from '../../../../../mapi/code/api/entities/message/message-request-item'
import { MessagesRequestPack } from '../../../../../mapi/code/api/entities/message/messages-request-pack'
import { ThreadInFolder } from '../../../../../mapi/code/api/entities/thread/thread-in-folder'
import { ThreadMeta } from '../../../../../mapi/code/api/entities/thread/thread-meta'
import { Network } from '../../../../../common/code/network/network'
import { IDSupport } from '../../../../code/api/common/id-support'
import { Storage } from '../../../../code/api/storage/storage'
import { Threads } from '../../../../code/busilogics/threads/threads'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MockCursorWithArray, MockStorage, MockStorageStatement } from '../../../__helpers__/mock-patches'
import { rejected } from '../../../__helpers__/test-failure'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import { clone } from '../../../../../common/__tests__/__helpers__/utils'
import sample from '../../../../../mapi/__tests__/code/api/entities/thread/sample.json'

const testIDSupport = new TestIDSupport()

function idstr(value: Int32): string {
  return testIDSupport.toDBValue(int32ToInt64(value))
}

function makeThreads({
  network,
  storage,
  idSupport,
}: {
  network?: Network
  storage?: Storage
  idSupport?: IDSupport
}): Threads {
  return new Threads(network || MockNetwork(), storage || MockStorage(), idSupport || testIDSupport)
}

describe(Threads, () => {
  describe('loadThreadsInFolderFromNetwork', () => {
    it('should load threads from network', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(sample))),
      })
      const threads = makeThreads({ network })
      expect.assertions(2)
      threads.loadThreadsInFolderFromNetwork(int64(1), 100).then((response) => {
        const pack: MessagesRequestPack = (network.execute as any).mock.calls[0][0]
        expect(pack.requests).toHaveLength(1)
        expect(pack.requests[0].params()).toStrictEqual(
          JSONItemFromJSON({
            fid: idstr(1),
            first: 0,
            last: 100,
            threaded: true,
            returnIfModified: true,
            md5: '',
          }),
        )
        done()
      })
    })
    it('should fail on parsing error', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON({}))),
      })
      const threads = makeThreads({ network })
      expect.assertions(1)
      threads.loadThreadsInFolderFromNetwork(int64(1), 100).failed((error) => {
        expect(error.message).toStrictEqual('JSON Item parsing failed for entity ThreadResponse')
        done()
      })
    })
  })
  describe('getTidsInFolder', () => {
    it('should return tids of by folders', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)]]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.getTidsInFolder(int64(1)).then((result) => {
        expect(result).toEqual([int64(1), int64(2)])
        done()
      })
    })
    it('should use passed mid in the query', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.getTidsInFolder(int64(101)).then((_) => {
        expect(storage.runQuery).toBeCalledWith(`SELECT tid FROM ${EntityKind.thread} WHERE fid = ?;`, [idstr(101)])
        done()
      })
    })
  })
  describe('fetchScnsByTids', () => {
    it('should return immediatelly if passed set is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.fetchScnsByTids([]).then((result) => {
        expect(result).toStrictEqual(new Map())
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should use passed mid in the query', (done) => {
      const tids = [int64(401), int64(int64(402))]
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(
          resolve(
            MockCursorWithArray([
              [tids[0], 1],
              [tids[1], 2],
            ]),
          ),
        ),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.fetchScnsByTids(tids).then((result) => {
        expect(storage.runQuery).toBeCalledWith('SELECT * FROM thread_scn AS s WHERE s.tid IN (401, 402);', [])
        expect(result).toStrictEqual(
          new Map([
            [tids[0], 1],
            [tids[1], 2],
          ]),
        )
        done()
      })
    })
  })
  describe('fetchMetasInThreads', () => {
    it('should return immediatelly if passed batches set is empty', (done) => {
      const network = MockNetwork()
      const threads = makeThreads({})
      expect.assertions(2)
      threads.fetchMetasInThreads([]).then((result) => {
        expect(result).toStrictEqual([])
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should run message requests for every batch passed in', (done) => {
      const network = MockNetwork({
        execute: jest
          .fn()
          .mockReturnValueOnce(resolve(JSONItemFromJSON([sample[2]])))
          .mockReturnValueOnce(resolve(JSONItemFromJSON([sample[0]]))),
      })
      const threads = makeThreads({ network })
      expect.assertions(3)
      const tids = [[int64(401), int64(402)], [int64(403)]]
      threads.fetchMetasInThreads(tids).then((result) => {
        expect((network.execute as any).mock.calls[0][0]).toStrictEqual(
          new MessagesRequestPack(
            [
              MessageRequestItem.messagesInThread(int64(401), 0, Threads.MESSAGES_IN_THREAD_MAX),
              MessageRequestItem.messagesInThread(int64(402), 0, Threads.MESSAGES_IN_THREAD_MAX),
            ],
            true,
          ),
        )
        expect((network.execute as any).mock.calls[1][0]).toStrictEqual(
          new MessagesRequestPack(
            [MessageRequestItem.messagesInThread(int64(403), 0, Threads.MESSAGES_IN_THREAD_MAX)],
            true,
          ),
        )
        expect(result).toStrictEqual([
          (sample[2].messageBatch.messages as any).map((m: any) => messageMetaFromJSONItem(JSONItemFromJSON(m))!),
          [messageMetaFromJSONItem(JSONItemFromJSON(sample[0].messageBatch.messages[0]))!],
        ])
        done()
      })
    })
    it('should skip message response if it is malformed', (done) => {
      const copy = clone(sample)
      copy[0] = []
      const network = MockNetwork({
        execute: jest
          .fn()
          .mockReturnValueOnce(resolve(JSONItemFromJSON([copy[2]])))
          .mockReturnValueOnce(resolve(JSONItemFromJSON([copy[0]]))),
      })
      const threads = makeThreads({ network })
      expect.assertions(3)
      const tids = [[int64(401), int64(402)], [int64(403)]]
      threads.fetchMetasInThreads(tids).then((result) => {
        expect((network.execute as any).mock.calls[0][0]).toStrictEqual(
          new MessagesRequestPack(
            [
              MessageRequestItem.messagesInThread(int64(401), 0, Threads.MESSAGES_IN_THREAD_MAX),
              MessageRequestItem.messagesInThread(int64(402), 0, Threads.MESSAGES_IN_THREAD_MAX),
            ],
            true,
          ),
        )
        expect((network.execute as any).mock.calls[1][0]).toStrictEqual(
          new MessagesRequestPack(
            [MessageRequestItem.messagesInThread(int64(403), 0, Threads.MESSAGES_IN_THREAD_MAX)],
            true,
          ),
        )
        expect(result).toStrictEqual([
          (sample[2].messageBatch.messages as any).map((m: any) => messageMetaFromJSONItem(JSONItemFromJSON(m))!),
        ])
        done()
      })
    })
    it('should skip message response if it fails', (done) => {
      const copy: typeof sample = clone(sample)
      const network = MockNetwork({
        execute: jest
          .fn()
          .mockReturnValueOnce(resolve(JSONItemFromJSON([copy[2]])))
          .mockReturnValueOnce(resolve(JSONItemFromJSON({}))),
      })
      const threads = makeThreads({ network })
      expect.assertions(3)
      const tids = [[int64(401), int64(402)], [int64(403)]]
      threads.fetchMetasInThreads(tids).then((result) => {
        expect((network.execute as any).mock.calls[0][0]).toStrictEqual(
          new MessagesRequestPack(
            [
              MessageRequestItem.messagesInThread(int64(401), 0, Threads.MESSAGES_IN_THREAD_MAX),
              MessageRequestItem.messagesInThread(int64(402), 0, Threads.MESSAGES_IN_THREAD_MAX),
            ],
            true,
          ),
        )
        expect((network.execute as any).mock.calls[1][0]).toStrictEqual(
          new MessagesRequestPack(
            [MessageRequestItem.messagesInThread(int64(403), 0, Threads.MESSAGES_IN_THREAD_MAX)],
            true,
          ),
        )
        expect(result).toStrictEqual([
          (sample[2].messageBatch.messages as any).map((m: any) => messageMetaFromJSONItem(JSONItemFromJSON(m))!),
        ])
        done()
      })
    })
  })
  describe('fetchDraftInSyncIds', () => {
    it('should fetch mids of drafts', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(301)], [int64(302)]]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.fetchDraftInSyncIds().then((res) => {
        expect(res).toStrictEqual([int64(301), int64(302)])
        expect(storage.runQuery).toBeCalledWith(`SELECT mid FROM ${EntityKind.draft_entry};`, [])
        done()
      })
    })
  })
  describe('getThreadsCountLoadedInFolder', () => {
    it('should count number of threads in fid', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[1000]]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.getThreadsCountLoadedInFolder(int64(101)).then((res) => {
        expect(storage.runQuery).toBeCalledWith(`SELECT count(*) FROM ${EntityKind.thread} WHERE fid = ?;`, [
          idstr(101),
        ])
        expect(res).toBe(1000)
        done()
      })
    })
  })
  describe('selectNonExistingTidsInFolder', () => {
    it('should return empty set if tids passed in is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.selectNonExistingTidsInFolder(int64(101), new Set()).then((res) => {
        expect(res).toStrictEqual(new Set())
        done()
      })
    })
    it('should return subset of passed tids excluding existing ones with fid', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(3)], [int64(4)], [int64(5)]]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.selectNonExistingTidsInFolder(int64(101), new Set([int64(1), int64(2), int64(3)])).then((res) => {
        // tslint:disable-next-line:max-line-length
        expect(storage.runQuery).toBeCalledWith('SELECT tid FROM thread WHERE fid = ? AND tid IN (1, 2, 3);', [
          idstr(101),
        ])
        expect(res).toStrictEqual(new Set([int64(1), int64(2)]))
        done()
      })
    })
  })
  describe('deleteThreadsInFolderExceptTids', () => {
    it('should just remove the entries from Thread table if skipTids are empty', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.deleteThreadsInFolderExceptTids(int64(101), []).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(`DELETE FROM ${EntityKind.thread} WHERE fid = ?;`)
        expect(statement.execute).toBeCalledWith([idstr(101)])
        done()
      })
    })
    it('should just remove entries from Thread table excluding threads with tids in skipTids', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.deleteThreadsInFolderExceptTids(int64(101), [int64(401), int64(402)]).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.thread} WHERE fid = ? AND tid NOT IN (401, 402);`,
        )
        expect(statement.execute).toBeCalledWith([idstr(101)])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.deleteThreadsInFolderExceptTids(int64(101), [int64(401), int64(402)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.deleteThreadsInFolderExceptTids(int64(101), [int64(401), int64(402)]).then((_) => {
        expect(statement.close).toBeCalled()
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
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.deleteThreadsInFolderExceptTids(int64(101), [int64(401), int64(402)]).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('insertThreads', () => {
    it('should return immediatelly if the set of threads is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.insertThreads([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should insert threads', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(3)
      threads
        .insertThreads([
          new ThreadInFolder(int64(401), int64(101), int64(301)),
          new ThreadInFolder(int64(402), int64(102), int64(302)),
        ])
        .then((_) => {
          expect(statement.execute).toBeCalledTimes(2)
          expect((statement.execute as any).mock.calls[0][0]).toEqual([idstr(401), idstr(101), idstr(301)])
          expect((statement.execute as any).mock.calls[1][0]).toEqual([idstr(402), idstr(102), idstr(302)])
          done()
        })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.insertThreads([new ThreadInFolder(int64(401), int64(101), int64(301))]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread])
        done()
      })
    })
    it('should close statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.insertThreads([new ThreadInFolder(int64(401), int64(101), int64(301))]).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close statement disregarding the result (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('FAILED')),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads
        .insertThreads([
          new ThreadInFolder(int64(401), int64(101), int64(301)),
          new ThreadInFolder(int64(402), int64(102), int64(302)),
        ])
        .failed((e) => {
          expect(e.message).toBe('FAILED')
          expect(statement.close).toBeCalled()
          done()
        })
    })
  })
  describe('fetchOldestTopMidTimestampByFid', () => {
    it('should return the timestamp and include fid in query', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[12345]]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.fetchOldestTopMidTimestampByFid(int64(101)).then((res) => {
        expect(res).toBe(12345)
        expect(storage.runQuery).toBeCalledWith(expect.any(String), [idstr(101)])
        done()
      })
    })
  })
  describe('rebuildThreadCounter', () => {
    it('should return immediatelly if the set of tids is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.rebuildThreadCounter([]).then((_) => {
        expect(storage.runQuery).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should fetch fids of non-threaded folders and use them in statement', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)], [int64(3)]]))),
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(3)
      threads.rebuildThreadCounter([int64(401), int64(402)]).then((_) => {
        expect(storage.runQuery).toBeCalledWith(
          'SELECT fid FROM folder WHERE type IN (?, ?, ?);',
          [FolderType.outgoing, FolderType.trash, FolderType.spam].map((type) => folderTypeToInt32(type)),
        )
        expect(storage.prepareStatement).toBeCalledWith(
          'INSERT INTO thread_counters SELECT t.tid, ifnull(total_counter, 0), ifnull(unread_counter, 0) FROM thread AS t LEFT OUTER JOIN (SELECT m.tid, COUNT(*) as total_counter, MIN(1, SUM(m.unread)) as unread_counter FROM message_meta AS m WHERE m.fid NOT IN (?, ?, ?) GROUP BY m.tid) AS calc ON t.tid = calc.tid WHERE t.tid IN (401, 402);',
        )
        expect(statement.execute).toBeCalledWith([idstr(1), idstr(2), idstr(3)])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)], [int64(3)]]))),
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.rebuildThreadCounter([int64(401), int64(402)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread_counters])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)], [int64(3)]]))),
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.rebuildThreadCounter([int64(401), int64(402)]).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close the statement disregarding the result (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)], [int64(3)]]))),
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.rebuildThreadCounter([int64(401), int64(402)]).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('updateTops', () => {
    it('should return immediatelly if the set of tids is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.updateTops([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should use passed tids in the statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      const tids = [int64(401), int64(402)]
      expect.assertions(1)
      threads.updateTops(tids).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          'UPDATE thread SET top_mid = ifnull((SELECT mid FROM message_meta AS m WHERE m.tid = thread.tid AND m.fid = thread.fid ORDER BY m.timestamp DESC LIMIT 1), -1) WHERE thread.tid IN (401, 402);',
        )
        done()
      })
    })
    it('should notify about changes', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      const tids = [int64(401), int64(402)]
      expect.assertions(1)
      threads.updateTops(tids).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread])
        done()
      })
    })
  })
  describe('insertScn', () => {
    it('should return immediatelly if the set of tids is empty', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.insertScn([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should use passed tids in the statement', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      const metas = [
        new ThreadMeta(int64(101), int64(401), int64(101), int64(301), 10),
        new ThreadMeta(int64(102), int64(402), int64(102), int64(302), 20),
      ]
      expect.assertions(3)
      threads.insertScn(metas).then((_) => {
        expect(statement.execute).toBeCalledTimes(2)
        expect((statement.execute as any).mock.calls[0][0]).toEqual([idstr(401), idstr(101)])
        expect((statement.execute as any).mock.calls[1][0]).toEqual([idstr(402), idstr(102)])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      const metas = [
        new ThreadMeta(int64(101), int64(401), int64(101), int64(301), 10),
        new ThreadMeta(int64(102), int64(402), int64(102), int64(302), 20),
      ]
      expect.assertions(1)
      threads.insertScn(metas).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread_scn])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      const metas = [
        new ThreadMeta(int64(101), int64(401), int64(101), int64(301), 10),
        new ThreadMeta(int64(102), int64(402), int64(102), int64(302), 20),
      ]
      expect.assertions(1)
      threads.insertScn(metas).then((_) => {
        expect(statement.close).toBeCalled()
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
      const threads = makeThreads({ storage })
      const metas = [
        new ThreadMeta(int64(101), int64(401), int64(101), int64(301), 10),
        new ThreadMeta(int64(102), int64(402), int64(102), int64(302), 20),
      ]
      expect.assertions(2)
      threads.insertScn(metas).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('killOrphansScn', () => {
    it('should run statement to cleanup invalid thread-scn relations', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.killOrphansScn().then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.thread_scn} WHERE tid NOT IN (SELECT tid FROM ${EntityKind.thread});`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread_scn])
        done()
      })
    })
  })
  describe('cleanupThreadsAfterMessageDeletion', () => {
    it('should run statement to cleanup obsolete threads', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.cleanupThreadsAfterMessageDeletion().then((_) => {
        expect(storage.runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.thread} WHERE top_mid = ${idstr(-1)};`)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread])
        done()
      })
    })
  })
  describe('getTidsByMids', () => {
    it('should return immediatelly for empty set passed', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.getTidsByMids([]).then((res) => {
        expect(res).toHaveLength(0)
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should use passed mids in the query', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(401)], [int64(402)]]))),
      })
      const threads = makeThreads({ storage })
      const mids = [int64(301), int64(302)]
      expect.assertions(2)
      threads.getTidsByMids(mids).then((res) => {
        expect(res).toEqual([int64(401), int64(402)])
        expect(storage.runQuery).toBeCalledWith(
          'SELECT tid FROM message_meta WHERE mid IN (301, 302) AND tid IS NOT NULL;',
          [],
        )
        done()
      })
    })
  })
  describe('fetchMinimalTid', () => {
    it('should return minimal tid among threads', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)]]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.fetchMinimalTid().then((res) => {
        expect(res).toBe(int64(1))
        expect(storage.runQuery).toBeCalledWith(`SELECT MIN(tid) FROM ${EntityKind.thread};`, [])
        done()
      })
    })
    it('should return null if empty table', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.fetchMinimalTid().then((res) => {
        expect(res).toBeNull()
        expect(storage.runQuery).toBeCalledWith(`SELECT MIN(tid) FROM ${EntityKind.thread};`, [])
        done()
      })
    })
  })
  describe('generateThreadsAfterMoveFromFolder', () => {
    it('should return immediatelly if the passed mids set is empty', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.generateThreadsAfterMoveFromFolder(int64(101), []).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should run thread restoration sql', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.generateThreadsAfterMoveFromFolder(int64(101), [int64(301), int64(302)]).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          'INSERT INTO thread SELECT t.tid, (SELECT ?), t.top_mid FROM thread AS t WHERE t.tid IN (SELECT DISTINCT (tid) FROM message_meta AS m WHERE m.mid IN (301, 302)) GROUP BY t.tid;',
        )
        expect(statement.execute).toBeCalledWith([idstr(101)])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.generateThreadsAfterMoveFromFolder(int64(101), [int64(301), int64(302)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.thread])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const threads = makeThreads({ storage })
      expect.assertions(1)
      threads.generateThreadsAfterMoveFromFolder(int64(101), [int64(301), int64(302)]).then((_) => {
        expect(statement.close).toBeCalled()
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
      const threads = makeThreads({ storage })
      expect.assertions(2)
      threads.generateThreadsAfterMoveFromFolder(int64(101), [int64(301), int64(302)]).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('topMidForTidAndFid', () => {
    it('should return top mid for a thread with fid and tid', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(301)]]))),
      })
      const threads = makeThreads({ storage })
      threads.topMidForTidAndFid(int64(401), int64(101)).then((res) => {
        expect(storage.runQuery).toBeCalledWith(`SELECT top_mid FROM ${EntityKind.thread} WHERE tid = ? AND fid = ?;`, [
          idstr(401),
          idstr(101),
        ])
        expect(res).toBe(int64(301))
        done()
      })
    })
    it('should return null if top mid is not found', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const threads = makeThreads({ storage })
      threads.topMidForTidAndFid(int64(401), int64(101)).then((res) => {
        expect(storage.runQuery).toBeCalledWith(`SELECT top_mid FROM ${EntityKind.thread} WHERE tid = ? AND fid = ?;`, [
          idstr(401),
          idstr(101),
        ])
        expect(res).toBeNull()
        done()
      })
    })
  })
})
