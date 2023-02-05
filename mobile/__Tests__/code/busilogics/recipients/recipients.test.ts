import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { getVoid } from '../../../../../common/code/result/result'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { MockStorage, MockStorageStatement } from '../../../../../xmail/__tests__/__helpers__/mock-patches'
import { TestIDSupport } from '../../../../../xmail/__tests__/__helpers__/test-id-support'
import { RecipientType, recipientTypeToInt32 } from '../../../../../xmail/../mapi/code/api/entities/recipient/recipient'
import { Recipients } from '../../../../code/busilogics/recipients/recipients'

const testIDSupport = new TestIDSupport()

describe(Recipients, () => {
  describe('deleteRecipientsByMids', () => {
    it('shoudl return immeiately if input mids list is empty', (done) => {
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        notifyAboutChanges,
        runStatement,
      })
      const recipients = new Recipients(storage, testIDSupport)
      recipients.deleteRecipientsByMids([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should delete recipients from DB and notify', (done) => {
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        notifyAboutChanges,
        runStatement,
      })
      const recipients = new Recipients(storage, testIDSupport)
      recipients.deleteRecipientsByMids([int64(1234), int64(56789)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith('DELETE FROM recipients WHERE mid IN (1234, 56789);')
        expect(storage.notifyAboutChanges).toBeCalledWith(['recipients'])
        done()
      })
    })
  })
  describe(Recipients.prototype.insertRecipientsForSearch, () => {
    it('should insert recipients with non-null name', (done) => {
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const statement = MockStorageStatement()
      const storage = MockStorage({
        notifyAboutChanges,
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const recipients = new Recipients(storage, testIDSupport)
      recipients
        .insertRecipientsForSearch(int64(1234), 'some@yandex.ru', recipientTypeToInt32(RecipientType.cc), 'somebody')
        .then((_) => {
          expect(storage.prepareStatement).toBeCalledWith(
            `INSERT OR REPLACE INTO ${EntityKind.recipients} (mid, email, type, name) VALUES (?, ?, ?, ?);`,
          )
          expect(statement.execute).toBeCalledWith(['1234', 'some@yandex.ru', 3, 'somebody'])
          expect(statement.close).toBeCalled()
          expect(storage.notifyAboutChanges).toBeCalledWith(['recipients'])
          done()
        })
    })
    it('should insert recipients with null name', (done) => {
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const statement = MockStorageStatement()
      const storage = MockStorage({
        notifyAboutChanges,
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const recipients = new Recipients(storage, testIDSupport)
      recipients
        .insertRecipientsForSearch(int64(1234), 'some@yandex.ru', recipientTypeToInt32(RecipientType.cc), null)
        .then((_) => {
          expect(storage.prepareStatement).toBeCalledWith(
            `INSERT OR REPLACE INTO ${EntityKind.recipients} (mid, email, type, name) VALUES (?, ?, ?, ?);`,
          )
          expect(statement.execute).toBeCalledWith(['1234', 'some@yandex.ru', 3, null])
          expect(statement.close).toBeCalled()
          expect(storage.notifyAboutChanges).toBeCalledWith(['recipients'])
          done()
        })
    })
  })
})
