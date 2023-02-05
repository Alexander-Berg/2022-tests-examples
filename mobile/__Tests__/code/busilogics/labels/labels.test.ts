import { reject, resolve } from '../../../../../../common/xpromise-support'
import { Int32, int32ToInt64, int64, int64ToString, stringToInt32, YSError } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { ID } from '../../../../../mapi/../mapi/code/api/common/id'
import { DeltaApiLabel } from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-label'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { Label, LabelType, labelTypeToInt32 } from '../../../../../mapi/code/api/entities/label/label'
import { containsReadLabel, MessageMeta } from '../../../../../mapi/code/api/entities/message/message-meta'
import { MessageTypeFlags } from '../../../../../mapi/code/api/entities/message/message-type'
import { Storage } from '../../../../code/api/storage/storage'
import { StorageStatement } from '../../../../code/api/storage/storage-statement'
import { Labels, MessageLabelEntry } from '../../../../code/busilogics/labels/labels'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import {
  MockCursorWithArray,
  MockStorage,
  MockStorageStatement,
  MockWithinTransaction,
} from '../../../__helpers__/mock-patches'
import { rejected } from '../../../__helpers__/test-failure'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import label from '../../../../../mapi/__tests__/code/api/entities/delta-api/entities/label.json'

const sampleLabels = [
  new Label('lid1', LabelType.important, 'label 1', 10, 100, 0xaabbcc, int64(1000)),
  new Label('lid2', LabelType.system, 'label 2', 20, 200, 0xabcdef, int64(2000)),
  new Label('lid3', LabelType.user, 'label 3', 30, 300, 0xffeedd, int64(3000)),
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

describe(Labels, () => {
  describe(containsReadLabel, () => {
    it('should return true if contains READ label', () => {
      expect(containsReadLabel(['1', 'FAKE_SEEN_LBL', '2'])).toBe(true)
      expect(containsReadLabel(['1', '2'])).toBe(false)
    })
  })
  describe(Labels.containsAttachmentsLabel, () => {
    it('should return true if contains ATTACH label', () => {
      expect(Labels.containsAttachmentsLabel(['1', 'FAKE_ATTACHED_LBL', '2'])).toBe(true)
      expect(Labels.containsAttachmentsLabel(['1', '2'])).toBe(false)
    })
  })
  describe('replaceLabels', () => {
    it("should run transaction depending on 'transactioned' parameter (true)", (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>().mockReturnValue(
        reject(new YSError('NO MATTER')),
      )
      const storage = MockStorage({
        withinTransaction,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).failed((_) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, false).failed((_) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels).failed((_) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.replaceLabels(sampleLabels, true).failed((error) => {
        expect(withinTransaction).toBeCalledWith(true, expect.any(Function))
        expect(error).toStrictEqual(new YSError('TRANSACTION CREATION FAILED'))
        done()
      })
    })
    it('should delete old labels', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).failed((_) => {
        expect(runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.label};`)
        done()
      })
    })
    it('should fail if old labels deletion fails', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(reject(new YSError('DELETION FAILED')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.replaceLabels(sampleLabels, true).failed((error) => {
        expect(runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.label};`)
        expect(error).toStrictEqual(new YSError('DELETION FAILED'))
        done()
      })
    })
    it('should run insertion if deletion of old labels succeeds', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement: Storage['prepareStatement'] = jest.fn().mockReturnValue(reject(new YSError('NO MATTER')))
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).failed((_) => {
        // tslint:disable-next-line: max-line-length
        expect(prepareStatement).toBeCalledWith(
          `INSERT INTO ${EntityKind.label} (lid, type, name, unread_counter, total_counter, color, symbol) VALUES (?, ?, ?, ?, ?, ?, ?);`,
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).failed((error) => {
        // tslint:disable-next-line: max-line-length
        expect(error).toStrictEqual(new YSError('NO MATTER'))
        done()
      })
    })
    it('should create insertion executions by the number of labels to store', (done) => {
      const labelToArray = (item: Label): readonly any[] => [
        item.lid,
        item.type,
        item.name,
        item.unreadCounter,
        item.totalCounter,
        item.color,
        int64ToString(item.symbol),
      ]
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
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
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(4)
      labels.replaceLabels(sampleLabels, true).then((_) => {
        expect(execute).toBeCalledTimes(3)
        expect((execute as jest.Mock).mock.calls[0][0]).toStrictEqual(labelToArray(sampleLabels[0]))
        expect((execute as jest.Mock).mock.calls[1][0]).toStrictEqual(labelToArray(sampleLabels[1]))
        expect((execute as jest.Mock).mock.calls[2][0]).toStrictEqual(labelToArray(sampleLabels[2]))
        done()
      })
    })
    it('should notify about changes', (done) => {
      const withinTransaction: Storage['withinTransaction'] = MockWithinTransaction<any>()
      const runStatement: Storage['runStatement'] = jest.fn().mockReturnValue(resolve(getVoid()))
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
        runStatement,
        prepareStatement,
        notifyAboutChanges,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).then((_) => {
        expect(notifyAboutChanges).toBeCalledWith([EntityKind.label])
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.replaceLabels(sampleLabels, true).failed((error) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).then((_) => {
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
      const storage = MockStorage({
        withinTransaction,
        runStatement,
        prepareStatement,
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.replaceLabels(sampleLabels, true).failed((_) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.replaceLabels(sampleLabels, true).then((value) => {
        expect(value).not.toBeNull()
        expect(value).toBe(getVoid())
        done()
      })
    })
    it('should return with resolution immediatelly if empty set of labels', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(4)
      labels.replaceLabels([], true).then((value) => {
        expect(value).not.toBeNull()
        expect(value).toBe(getVoid())
        expect(storage.withinTransaction).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })
  describe('cleanUp', () => {
    it('should fail if statement run fails', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.cleanUp().failed((e) => {
        expect(e!.message).toBe('FAILED')
        done()
      })
    })
    it(`should run deletion statement on ${EntityKind.labels_messages}`, (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.cleanUp().then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          expect.stringContaining(`DELETE FROM ${EntityKind.labels_messages}`),
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
        done()
      })
    })
  })
  describe('insertMessageLabels', () => {
    it('should return immediatelly if passed messages list is empty', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(4)
      labels.insertMessageLabels([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.withinTransaction).not.toBeCalled()
        done()
      })
    })
    it('should insert lids for every message', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(4)
      labels
        .insertMessageLabels([
          new MessageMeta(
            int64(301),
            int64(101),
            int64(401),
            ['11', '12'],
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
            int64(402),
            ['21'],
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
            int64(303),
            int64(103),
            int64(403),
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
          expect(storage.runStatement).toBeCalledWith(
            `DELETE FROM ${EntityKind.labels_messages} WHERE mid IN (301, 302, 303);`,
          )
          expect((statement.execute as any).mock.calls[0][0]).toStrictEqual(['11', idstr(301), idstr(401)])
          expect((statement.execute as any).mock.calls[1][0]).toStrictEqual(['12', idstr(301), idstr(401)])
          expect((statement.execute as any).mock.calls[2][0]).toStrictEqual(['21', idstr(302), idstr(402)])
          done()
        })
    })
    it('should call close disregarding the execution result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels
        .insertMessageLabels([
          new MessageMeta(
            int64(301),
            int64(101),
            int64(401),
            ['11', '12'],
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
            int64(402),
            ['21'],
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
    it('should call close disregarding the execution result (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest
          .fn()
          .mockReturnValueOnce(resolve(getVoid()))
          .mockReturnValueOnce(rejected('FAILED'))
          .mockReturnValueOnce(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        close: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels
        .insertMessageLabels([
          new MessageMeta(
            int64(301),
            int64(101),
            int64(401),
            ['11', '12'],
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
            int64(402),
            ['21'],
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
            int64(303),
            int64(103),
            int64(403),
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
          expect(e!.message).toBe('FAILED')
          expect(statement.close).toBeCalled()
          done()
        })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels
        .insertMessageLabels([
          new MessageMeta(
            int64(301),
            int64(101),
            int64(401),
            ['11', '12'],
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
            int64(402),
            ['21'],
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
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
          done()
        })
    })
  })
  describe('insertOrAbortLabels', () => {
    it('should return immediatelly if empty set is passed', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.insertOrAbortLabels([], true).then((_) => {
        expect(storage.withinTransaction).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should create transaction based on the argument passed (true)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      const sample = new Label('', LabelType.user, null, 0, 0, 0, int64(0))
      labels.insertOrAbortLabels([sample], true).failed((e) => {
        expect(e.message).toBe('NO MATTER')
        expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it('should create transaction based on the argument passed (false)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      const sample = new Label('', LabelType.user, null, 0, 0, 0, int64(0))
      labels.insertOrAbortLabels([sample], false).failed((e) => {
        expect(e.message).toBe('NO MATTER')
        expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it('should create insertion statements for labels passed in', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      const samples = [
        new Label('lid1', LabelType.important, 'Important', 10, 100, 0xaabbcc, int64(0)),
        new Label('lid2', LabelType.user, 'name2', 5, 50, 0xccbbaa, int64(0)),
      ]
      expect.assertions(5)
      labels.insertOrAbortLabels(samples, true).then((_) => {
        expect(storage.prepareStatement).toBeCalled()
        expect(statement.execute).toBeCalledTimes(2)
        expect((statement.execute as any).mock.calls[0][0]).toEqual([
          samples[0].lid,
          labelTypeToInt32(samples[0].type),
          samples[0].name,
          samples[0].unreadCounter,
          samples[0].totalCounter,
          samples[0].color,
          samples[0].symbol,
        ])
        expect((statement.execute as any).mock.calls[1][0]).toEqual([
          samples[1].lid,
          labelTypeToInt32(samples[1].type),
          samples[1].name,
          samples[1].unreadCounter,
          samples[1].totalCounter,
          samples[1].color,
          samples[1].symbol,
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
      const labels = new Labels(storage, testIDSupport)
      const samples = [
        new Label('lid1', LabelType.important, 'Important', 10, 100, 0xaabbcc, int64(0)),
        new Label('lid2', LabelType.user, 'name2', 5, 50, 0xccbbaa, int64(0)),
      ]
      expect.assertions(1)
      labels.insertOrAbortLabels(samples, true).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.label])
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
      const labels = new Labels(storage, testIDSupport)
      const samples = [
        new Label('lid1', LabelType.important, 'Important', 10, 100, 0xaabbcc, int64(0)),
        new Label('lid2', LabelType.user, 'name2', 5, 50, 0xccbbaa, int64(0)),
      ]
      expect.assertions(2)
      labels.insertOrAbortLabels(samples, true).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('deleteByIDs', () => {
    it('should return immediatelly if empty set is passed', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.deleteByIDs([], true).then((_) => {
        expect(storage.withinTransaction).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should create transaction based on the argument passed (true)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.deleteByIDs(['lid1'], true).failed((e) => {
        expect(e.message).toBe('NO MATTER')
        expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it('should create transaction based on the argument passed (false)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.deleteByIDs(['lid1'], false).failed((e) => {
        expect(e.message).toBe('NO MATTER')
        expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it('should create deletion statements for labels IDs passed in', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      const lids = ['1', '2']
      expect.assertions(4)
      labels.deleteByIDs(lids, true).then((_) => {
        expect(storage.prepareStatement).toBeCalled()
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.label])
        expect(statement.execute).toBeCalledWith(lids)
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close statement if any of the operation fails', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const labels = new Labels(storage, testIDSupport)
      const lids = ['1', '2']
      expect.assertions(2)
      labels.deleteByIDs(lids, true).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
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

      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.updateWithDeltaApi(DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!, true).failed((e) => {
        expect(e.message).toBe('FAILED')
        done()
      })
    })
    it('should fail if label update fails', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.updateWithDeltaApi(DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!, true).failed((e) => {
        expect(e.message).toBe('FAILED')
        done()
      })
    })
    it('should create transaction with transactioned (false)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.updateWithDeltaApi(DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!, false).failed((e) => {
        expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
        expect(e.message).toBe('NO MATTER')
        done()
      })
    })
    it('should create transaction with transactioned (true)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockReturnValue(rejected('NO MATTER')),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.updateWithDeltaApi(DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!, true).failed((e) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.updateWithDeltaApi(DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!, true).then((_) => {
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
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.updateWithDeltaApi(DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!, true).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should update label', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      const sample = DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!
      expect.assertions(1)
      labels.updateWithDeltaApi(sample, true).then((_) => {
        expect(statement.execute).toBeCalledWith([
          sample.lid,
          LabelType.important,
          sample.name,
          0,
          sample.messagesCount,
          stringToInt32(sample.color, 16)!,
          0,
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
      const labels = new Labels(storage, testIDSupport)
      const sample = DeltaApiLabel.fromJSONItem(JSONItemFromJSON(label))!
      expect.assertions(1)
      labels.updateWithDeltaApi(sample, true).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.label])
        done()
      })
    })
    it('should put 0 as color if the color in DeltaApiLabel is null', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      const nullColor = { ...label, color: null }
      const sample = DeltaApiLabel.fromJSONItem(JSONItemFromJSON(nullColor))!
      expect.assertions(1)
      labels.updateWithDeltaApi(sample, true).then((_) => {
        expect(statement.execute).toBeCalledWith([
          sample.lid,
          LabelType.important,
          sample.name,
          0,
          sample.messagesCount,
          0,
          0,
        ])
        done()
      })
    })
  })
  describe('insertMessageLabelEntries', () => {
    it('should return immediatelly if the passed set of entries is empty', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.insertMessageLabelEntries([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should return pass triple MID-LID-TID into the statement', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        withinTransaction: MockWithinTransaction<any>(),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(5)
      labels
        .insertMessageLabelEntries([
          new MessageLabelEntry(int64(301), 'lid1', int64(401)),
          new MessageLabelEntry(int64(301), 'lid1', null),
        ])
        .then((_) => {
          expect(storage.prepareStatement).toBeCalled()
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
          expect(statement.execute).toBeCalledTimes(2)
          expect((statement.execute as any).mock.calls[0][0]).toEqual(['lid1', idstr(301), idstr(401)])
          expect((statement.execute as any).mock.calls[1][0]).toEqual(['lid1', idstr(301), null])
          done()
        })
    })
  })
  describe('deleteLabelsByFidAndMids', () => {
    it('should return immediatelly if empty mids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.deleteLabelsByFidAndMids(int64(1), []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should run deletion script and notify about changes when done', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.deleteLabelsByFidAndMids(int64(1), [int64(2), int64(3)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.labels_messages} WHERE mid IN (SELECT m.mid FROM ${EntityKind.message_meta} AS m WHERE m.fid = 1 AND m.mid IN (2, 3));`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
        done()
      })
    })
  })
  describe('deleteForMids', () => {
    it('should return immediatelly if empty mids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.deleteForMids([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should run deletion script and notify about changes when done', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.deleteForMids([int64(1), int64(2)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.labels_messages} WHERE mid IN (1, 2);`)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
        done()
      })
    })
  })
  describe(Labels.prototype.markWithLabel, () => {
    it('should return immediatelly if empty mids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.markWithLabel([], ['lid']).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should return immediatelly if empty lids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.markWithLabel([int64(1)], []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should mark with label', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(4)
      labels.markWithLabel([int64(1), int64(2)], ['lbl_1', 'lbl_2']).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `INSERT INTO ${EntityKind.labels_messages} SELECT lids, mids, tids FROM (SELECT label.lid AS lids FROM label WHERE lid IN (?, ?)) CROSS JOIN (SELECT message_meta.mid AS mids, message_meta.tid AS tids FROM message_meta WHERE mid IN (1, 2));`,
        )
        expect(statement.execute).toBeCalledWith(['lbl_1', 'lbl_2'])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe(Labels.prototype.unmarkWithLabels, () => {
    it('should return immediatelly if empty mids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.unmarkWithLabels([], ['lid']).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should return immediatelly if empty lids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(1)
      labels.unmarkWithLabels([int64(1)], []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should mark with label', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(4)
      labels.unmarkWithLabels([int64(1), int64(2)], ['lbl_1', 'lbl_2']).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.labels_messages} WHERE lid IN (?, ?) AND mid IN (1, 2);`,
        )
        expect(statement.execute).toBeCalledWith(['lbl_1', 'lbl_2'])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe(Labels.prototype.getLabelsIDsByLidsAndTypes, () => {
    it('should return immediatelly if empty lids', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.getLabelsIDsByLidsAndTypes([], [LabelType.important]).then((res) => {
        expect(res).toStrictEqual([])
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should return immediatelly if empty types', (done) => {
      const storage = MockStorage()
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.getLabelsIDsByLidsAndTypes(['lbl'], []).then((res) => {
        expect(res).toStrictEqual([])
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should run query', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([['#lbl_1'], ['#lbl_2']]))),
      })
      const labels = new Labels(storage, testIDSupport)
      expect.assertions(2)
      labels.getLabelsIDsByLidsAndTypes(['lbl_1', 'lbl_2'], [LabelType.important, LabelType.system]).then((res) => {
        expect(res).toEqual(['#lbl_1', '#lbl_2'])
        expect(
          storage.runQuery,
        ).toBeCalledWith(`SELECT lid FROM ${EntityKind.label} as l WHERE l.lid IN (?, ?) AND l.type IN (6, 3);`, [
          'lbl_1',
          'lbl_2',
        ])
        done()
      })
    })
  })
})
