import { resolve } from '../../../../../../common/xpromise-support'
import { getVoid } from '../../../../../common/code/result/result'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { NotDeletedCommandFiles } from '../../../../code/busilogics/commands/not-deleted-command-files'
import { MockCursorWithArray, MockStorage, MockStorageStatement } from '../../../__helpers__/mock-patches'

describe(NotDeletedCommandFiles, () => {
  describe('fetchAll', () => {
    it('should fetch all items from the table', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([['file1'], ['file2']]))),
      })
      const ndcf = new NotDeletedCommandFiles(storage)
      ndcf.fetchAll().then((res) => {
        expect(res).toStrictEqual(['file1', 'file2'])
        done()
      })
    })
  })
  describe('store', () => {
    it('should store passed argument in the table', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(resolve(getVoid())),
        close: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const ndcf = new NotDeletedCommandFiles(storage)
      ndcf.store('file1').then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `INSERT OR IGNORE INTO ${EntityKind.not_deleted_command_files} (file) VALUES (?);`,
        )
        expect(statement.execute).toBeCalledWith(['file1'])
        expect(statement.close).toBeCalled()
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.not_deleted_command_files])
        done()
      })
    })
  })
  describe('delete', () => {
    it('should return immediatelly if the argument is empty', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const ndcf = new NotDeletedCommandFiles(storage)
      ndcf.delete(new Set<string>()).then((res) => {
        expect(storage.prepareStatement).not.toBeCalled()
        done()
      })
    })
    it('should delete arguments from the table', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(resolve(getVoid())),
        close: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const ndcf = new NotDeletedCommandFiles(storage)
      ndcf
        .delete(
          new Set<string>(['file1', 'file2']),
        )
        .then((_) => {
          expect(storage.prepareStatement).toBeCalledWith(
            `DELETE FROM ${EntityKind.not_deleted_command_files} WHERE file IN (?, ?);`,
          )
          expect(statement.execute).toBeCalledWith(['file1', 'file2'])
          expect(statement.close).toBeCalled()
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.not_deleted_command_files])
          done()
        })
    })
  })
})
