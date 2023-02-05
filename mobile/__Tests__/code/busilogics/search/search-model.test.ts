import { resolve } from '../../../../../../common/xpromise-support'
import { int64 } from '../../../../../../common/ys'
import { MockNetwork } from '../../../../../common/__tests__/__helpers__/mock-patches'
import { getVoid } from '../../../../../common/code/result/result'
import { YSPair } from '../../../../../common/code/utils/tuples'
import { CustomContainerType } from '../../../../../mapi/../mapi/code/api/entities/custom-container'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { MessageMeta } from '../../../../../mapi/code/api/entities/message/message-meta'
import { MessageRequestItem } from '../../../../../mapi/code/api/entities/message/message-request-item'
import { MessageTypeFlags } from '../../../../../mapi/code/api/entities/message/message-type'
import { MessagesRequestPack } from '../../../../../mapi/code/api/entities/message/messages-request-pack'
import { messageMetasFromJSONItem } from '../../../../../mapi/code/api/entities/message/messages-response'
import { SearchRequest } from '../../../../../mapi/code/api/entities/search/search-request'
import { searchResponseFromJSONItem } from '../../../../../mapi/code/api/entities/search/search-response'
import { Network } from '../../../../../common/code/network/network'
import { IDSupport } from '../../../../code/api/common/id-support'
import { Storage } from '../../../../code/api/storage/storage'
import { SearchModel } from '../../../../code/busilogics/search/search-model'
import { Collections } from '../../../../../common/code/utils/collections'
import { arrayJSONItemFromArray, JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { MockCursorWithArray, MockStorage, MockStorageStatement } from '../../../__helpers__/mock-patches'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import messagesResponse from '../../../../../mapi/__tests__/code/api/entities/message/sample.json'
import searchResponse from '../../../../../mapi/__tests__/code/api/entities/search/sample.json'

function makeSearchModel({
  storage,
  network,
  idSupport,
}: {
  storage?: Storage
  network?: Network
  idSupport?: IDSupport
}): SearchModel {
  return new SearchModel(storage || MockStorage(), network || MockNetwork(), idSupport || new TestIDSupport())
}

describe(SearchModel, () => {
  describe('makeFolderSearchID', () => {
    it('should return Folder Search ID', () => {
      const model = makeSearchModel({})
      expect(model.makeFolderSearchID(int64(0))).toBe('s_f_0')
      expect(model.makeFolderSearchID(int64(101))).toBe('s_f_101')
    })
  })
  describe('makeLabelContentsId', () => {
    it('should return Label Search ID', () => {
      const model = makeSearchModel({})
      expect(model.makeLabelContentsId('lbl')).toBe('l_lbl')
    })
  })
  describe('makeLabelSearchId', () => {
    it('should return Label Search ID', () => {
      const model = makeSearchModel({})
      expect(model.makeLabelSearchId('lbl')).toBe('s_l_lbl')
    })
  })
  describe('makeCustomContentsSearchId', () => {
    it('should return Folder Search ID', () => {
      const model = makeSearchModel({})
      expect(model.makeCustomContentsSearchId(CustomContainerType.unread)).toBe('c_unread')
      expect(model.makeCustomContentsSearchId(CustomContainerType.with_attachment)).toBe('c_with_attachment')
    })
  })
  describe('updateMessagesShowFor', () => {
    it('should return immediatelly if the passed mids set is empty', (done) => {
      const storage = MockStorage()
      const model = makeSearchModel({ storage })
      model.updateMessagesShowFor('s_f_101', []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should set show_for for the passed mids set', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const model = makeSearchModel({ storage })
      model.updateMessagesShowFor('s_f_101', [int64(301), int64(302)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(expect.stringContaining('SET show_for = "s_f_101"'))
        expect(storage.runStatement).toBeCalledWith(expect.stringContaining('mid IN (301, 302)'))
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('getSearchResultsForFolderInMids', () => {
    it('should return immediatelly if the passed mids set is empty', (done) => {
      const storage = MockStorage()
      const search = makeSearchModel({ storage })
      search.getSearchResultsForFolderInMids(int64(101), []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should pick messages which show_for and mids are equal to params', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(301)], [int64(302)]]))),
      })
      const search = makeSearchModel({ storage })
      search.getSearchResultsForFolderInMids(int64(101), [int64(301), int64(302)]).then((_) => {
        expect(storage.runQuery).toBeCalledWith(
          'SELECT mid FROM message_meta WHERE show_for = "s_f_101" AND mid IN (301, 302);',
          [],
        )
        done()
      })
    })
  })
  describe('deleteSearchOnly', () => {
    it('should execute statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const model = makeSearchModel({ storage })
      expect.assertions(2)
      model.deleteSearchOnly([int64(301), int64(302)], 's_f_101').then((_) => {
        // tslint:disable-next-line:max-line-length
        expect(storage.runStatement).toBeCalledWith(
          'DELETE FROM message_meta WHERE search_only = 1 AND mid NOT IN (301, 302) AND show_for = "s_f_101";',
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('clearMessagesShowFor', () => {
    it('should run statement', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const model = makeSearchModel({ storage })
      model.clearMessagesShowFor().then((_) => {
        // tslint:disable-next-line:max-line-length
        expect(storage.runStatement).toBeCalledWith(
          'UPDATE message_meta SET show_for = NULL WHERE show_for IS NOT NULL',
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('filterExistingMids', () => {
    it('should return immediatelly if the passed mids set is empty', (done) => {
      const storage = MockStorage()
      const search = makeSearchModel({ storage })
      expect.assertions(2)
      search.filterExistingMids([]).then((result) => {
        expect(result).toStrictEqual([])
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should filter existing mids', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(101)], [int64(102)]]))),
      })
      expect.assertions(2)
      const search = makeSearchModel({ storage })
      search.filterExistingMids([int64(101), int64(102), int64(103)]).then((result) => {
        expect(storage.runQuery).toBeCalledWith('SELECT mid FROM message_meta WHERE mid IN (101, 102, 103);', [])
        expect(result).toStrictEqual([int64(101), int64(102)])
        done()
      })
    })
  })
  describe('getMessageCountFor', () => {
    it('should execute query', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[101]]))),
      })
      const search = makeSearchModel({ storage })
      expect.assertions(2)
      search.getMessageCountFor('s_id').then((result) => {
        expect(storage.runQuery).toBeCalledWith(
          // tslint:disable-next-line:max-line-length
          'SELECT COUNT(*) FROM message_meta LEFT OUTER JOIN (SELECT labels_messages.mid AS lmid, group_concat(labels_messages.lid) AS lids FROM labels_messages GROUP BY labels_messages.mid) ON (message_meta.mid = lmid) LEFT OUTER JOIN attachment ON (attachment.mid = message_meta.mid AND attachment.hid = (SELECT hid from attachment WHERE attachment.mid = message_meta.mid ORDER BY attachment.preview_support DESC LIMIT 1)) WHERE message_meta.show_for = ?;',
          ['s_id'],
        )
        expect(result).toBe(101)
        done()
      })
    })
  })
  describe('loadMessagesInLabel', () => {
    it('should execute request successfully', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON(messagesResponse)))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(2)
      search.loadMessagesInLabel('lid', 100).then((result) => {
        expect(executeMock).toBeCalledWith(
          new MessagesRequestPack([MessageRequestItem.messagesWithLabel('lid', 0, 100)], true),
        )
        expect(result).toStrictEqual(
          Collections.flatten(
            messagesResponse.map((item): readonly MessageMeta[] => {
              return messageMetasFromJSONItem(arrayJSONItemFromArray(item.messageBatch.messages))
            }),
          ),
        )
        done()
      })
    })
    it('should skip response if it is malformed', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({})))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(1)
      search.loadMessagesInLabel('lid', 100).then((result) => {
        expect(result).toStrictEqual([])
        done()
      })
    })
    it('should skip response if parsing fails', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({})))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(1)
      search.loadMessagesInLabel('lid', 100).then((result) => {
        expect(result).toStrictEqual([])
        done()
      })
    })
  })
  describe('loadUnread', () => {
    it('should execute request successfully', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON(searchResponse)))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(2)
      search.loadUnread(100).then((result) => {
        expect(executeMock).toBeCalledWith(SearchRequest.loadOnlyNew(0, 100))
        expect(result).toStrictEqual(searchResponseFromJSONItem(JSONItemFromJSON(searchResponse))!.messages)
        done()
      })
    })
    it('should skip response if it is malformed', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({})))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(1)
      search.loadUnread(100).then((result) => {
        expect(result).toStrictEqual([])
        done()
      })
    })
    it('should skip response if it fails', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({})))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(1)
      search.loadUnread(100).then((result) => {
        expect(result).toStrictEqual([])
        done()
      })
    })
  })
  describe('loadWithAttachments', () => {
    it('should execute request successfully', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON(searchResponse)))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(2)
      search.loadWithAttachments(100).then((result) => {
        expect(executeMock).toBeCalledWith(SearchRequest.loadWithAttachments(0, 100))
        expect(result).toStrictEqual(searchResponseFromJSONItem(JSONItemFromJSON(searchResponse))!.messages)
        done()
      })
    })
    it('should skip response if it is malformed', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({})))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(1)
      search.loadWithAttachments(100).then((result) => {
        expect(result).toStrictEqual([])
        done()
      })
    })
    it('should skip response if it fails', (done) => {
      const executeMock = jest.fn().mockReturnValue(resolve(JSONItemFromJSON({})))
      const network = MockNetwork({
        execute: executeMock,
      })
      const search = makeSearchModel({ network })
      expect.assertions(1)
      search.loadWithAttachments(100).then((result) => {
        expect(result).toStrictEqual([])
        done()
      })
    })
  })
  describe('getDelta', () => {
    it('should return immediatelly if the passed metas set is empty', (done) => {
      const storage = MockStorage()
      const search = makeSearchModel({ storage })
      expect.assertions(2)
      search.getDelta([]).then((result) => {
        expect(result).toStrictEqual(new YSPair([], []))
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should calculate delta with metas', (done) => {
      const storage = MockStorage()
      const search = makeSearchModel({ storage })
      const filterExistingMidsMock = jest
        .spyOn(search, 'filterExistingMids')
        .mockReturnValue(resolve([int64(301), int64(302)]))
      expect.assertions(2)
      search
        .getDelta([
          new MessageMeta(
            int64(301),
            int64(101),
            null,
            [],
            true,
            null,
            '',
            '',
            '',
            false,
            false,
            null,
            int64(12345),
            false,
            null,
            MessageTypeFlags.people | MessageTypeFlags.tPeople,
          ),
          new MessageMeta(
            int64(302),
            int64(101),
            null,
            [],
            true,
            null,
            '',
            '',
            '',
            false,
            false,
            null,
            int64(23456),
            false,
            null,
            MessageTypeFlags.people | MessageTypeFlags.tPeople,
          ),
          new MessageMeta(
            int64(303),
            int64(101),
            null,
            [],
            true,
            null,
            '',
            '',
            '',
            false,
            false,
            null,
            int64(34567),
            false,
            null,
            MessageTypeFlags.people | MessageTypeFlags.tPeople,
          ),
        ])
        .then((result) => {
          expect(result).toStrictEqual(
            new YSPair(
              [int64(301), int64(302)],
              [
                new MessageMeta(
                  int64(303),
                  int64(101),
                  null,
                  [],
                  true,
                  null,
                  '',
                  '',
                  '',
                  false,
                  false,
                  null,
                  int64(34567),
                  false,
                  null,
                  MessageTypeFlags.people | MessageTypeFlags.tPeople,
                ),
              ],
            ),
          )
          expect(filterExistingMidsMock).toBeCalledWith([int64(301), int64(302), int64(303)])
          done()
        })
    })
  })
  describe('modifyMessageMetaWithSearchId', () => {
    const search = makeSearchModel({})
    expect(
      search.modifyMessageMetaWithSearchId(
        's_id',
        new MessageMeta(
          int64(301),
          int64(101),
          int64(501),
          [],
          true,
          null,
          '',
          '',
          '',
          false,
          false,
          null,
          int64(12345),
          false,
          null,
          MessageTypeFlags.people | MessageTypeFlags.tPeople,
        ),
      ),
    ).toStrictEqual(
      new MessageMeta(
        int64(301),
        int64(101),
        null,
        [],
        true,
        null,
        '',
        '',
        '',
        false,
        true,
        's_id',
        int64(12345),
        false,
        null,
        MessageTypeFlags.people | MessageTypeFlags.tPeople,
      ),
    )
  })
  describe(SearchModel.prototype.dropUnreadShowFor, () => {
    it('should return immediatelly if empty mids are passed', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const search = makeSearchModel({ storage })
      search.dropUnreadShowFor([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should clean message meta of show_for and search_only', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const search = makeSearchModel({ storage })
      search.dropUnreadShowFor([int64(1), int64(2)]).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `UPDATE ${EntityKind.message_meta} SET show_for = NULL, search_only = 0 WHERE mid IN (1, 2) AND show_for IN (?);`,
        )
        expect(statement.execute).toBeCalledWith(['c_unread'])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe(SearchModel.prototype.dropLabelsShowFor, () => {
    it('should return immediatelly if empty mids are passed', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const search = makeSearchModel({ storage })
      search.dropLabelsShowFor([], ['label']).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should return immediatelly if empty labels are passed', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const search = makeSearchModel({ storage })
      search.dropLabelsShowFor([int64(1)], []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should clean message meta of show_for and search_only', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const search = makeSearchModel({ storage })
      search.dropLabelsShowFor([int64(1), int64(2)], ['lbl_1', 'lbl_2']).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `UPDATE ${EntityKind.message_meta} SET show_for = NULL, search_only = 0 WHERE mid IN (1, 2) AND show_for IN (?, ?, ?, ?);`,
        )
        expect(statement.execute).toBeCalledWith(['l_lbl_1', 'l_lbl_2', 's_l_lbl_1', 's_l_lbl_2'])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
})
