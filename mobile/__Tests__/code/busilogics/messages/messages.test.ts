import { resolve } from '../../../../../../common/xpromise-support'
import { int64, int64ToString } from '../../../../../../common/ys'
import {
  MockFileSystem,
  MockJSONSerializer,
  MockNetwork,
} from '../../../../../common/__tests__/__helpers__/mock-patches'
import { ArrayJSONItem } from '../../../../../common/code/json/json-types'
import { getVoid, Result } from '../../../../../common/code/result/result'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { YSPair } from '../../../../../common/code/utils/tuples'
import { ID } from '../../../../../mapi/code/api/common/id'
import { ArchiveMessagesNetworkRequest } from '../../../../../mapi/code/api/entities/actions/archive-messages-network-request'
import { DeleteMessagesNetworkRequest } from '../../../../../mapi/code/api/entities/actions/delete-messages-network-request'
import { MarkReadNetworkRequest } from '../../../../../mapi/code/api/entities/actions/mark-read-network-request'
import { MarkWithLabelsNetworkRequest } from '../../../../../mapi/code/api/entities/actions/mark-with-labels-network-request'
import { MoveToFolderNetworkRequest } from '../../../../../mapi/code/api/entities/actions/move-to-folder-network-request'
import {
  MessageBodyAttach,
  MessageBodyInfo,
  MessageBodyPart,
  MessageBodyPayload,
  messageBodyResponseFromJSONItem,
} from '../../../../../mapi/code/api/entities/body/message-body'
import { MessageBodyRequest } from '../../../../../mapi/code/api/entities/body/message-body-request'
import {
  DeltaApiAttachment,
  DeltaApiEnvelope,
  DeltaApiRecipient,
  DeltaApiSubject,
} from '../../../../../mapi/code/api/entities/delta-api/entities/delta-api-envelope'
import { EntityKind } from '../../../../../mapi/code/api/entities/entity-kind'
import { Folder, FolderType } from '../../../../../mapi/code/api/entities/folder/folder'
import {
  Attachment,
  Attachments,
  deltaApiEnvelopeToMessageMeta,
  MessageMeta,
} from '../../../../../mapi/code/api/entities/message/message-meta'
import { MessageRequestItem } from '../../../../../mapi/code/api/entities/message/message-request-item'
import { MessageTypeFlags } from '../../../../../mapi/code/api/entities/message/message-type'
import { MessagesRequestPack } from '../../../../../mapi/code/api/entities/message/messages-request-pack'
import { messageResponseFromJSONItem } from '../../../../../mapi/code/api/entities/message/messages-response'
import { EmailWithName } from '../../../../../mapi/code/api/entities/recipient/email'
import {
  Recipient,
  recipientFromJSONItem,
  RecipientType,
} from '../../../../../mapi/code/api/entities/recipient/recipient'
import { ResetFreshRequest } from '../../../../../mapi/code/api/entities/reset-fresh/reset-fresh-request'
import { NetworkStatus, NetworkStatusCode } from '../../../../../mapi/code/api/entities/status/network-status'
import { ThreadInFolder } from '../../../../../mapi/code/api/entities/thread/thread-in-folder'
import messageBodyResponseSample from '../../../../../mapi/__tests__/code/api/entities/body/sample-message-body.json'
import { createMockInstance } from '../../../../../common/__tests__/__helpers__/utils'
import { MessageBodyMeta } from '../../../../code/api/entities/body/message-body-meta'
import { AttachmentSizes } from '../../../../code/busilogics/draft/attachment-sizes'
import { MessageLabelEntry } from '../../../../code/busilogics/labels/labels'
import { Messages, MessagesSettings, MessagesSettingsBuilder } from '../../../../code/busilogics/messages/messages'
import { AccountSettingsKeys } from '../../../../code/busilogics/settings/settings-saver'
import { Models } from '../../../../code/models'
import { Registry } from '../../../../code/registry'
import {
  MockCursorWithArray,
  MockHighPrecisionTimer,
  MockStorage,
  MockStorageStatement,
  MockWithinTransaction,
} from '../../../__helpers__/mock-patches'
import {
  idstr,
  makeAttachmentsManager,
  makeBodies,
  makeCleanup,
  makeFolders,
  makeLabels,
  makeMessages,
  makeMessagesSettings,
  makeSearch,
  makeThreads,
} from '../../../__helpers__/models'
import { MockSharedPreferences } from '../../../../../common/__tests__/__helpers__/preferences-mock'
import { rejected } from '../../../__helpers__/test-failure'
import { TestIDSupport } from '../../../__helpers__/test-id-support'
import responseSample from './sample.json'

const testIDSupport = new TestIDSupport()

describe(Messages, () => {
  describe('updateMessagesWithBodyMeta', () => {
    it('should not run transaction on empty input', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )

      expect.assertions(2)
      messages.updateMessagesWithBodyMeta([]).then(() => {
        expect(storage.withinTransaction).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should insert MessageBodyMeta and update attaches', (done) => {
      const withinTransaction = MockWithinTransaction<any>()
      const execute = jest.fn().mockReturnValue(resolve(getVoid()))
      const prepareStatement = jest.fn().mockReturnValue(
        resolve({
          execute,
          close: jest.fn(),
        }),
      )
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        withinTransaction,
        prepareStatement,
        notifyAboutChanges,
      })
      const serialize = jest.fn((recipientItems: ArrayJSONItem) => {
        const parsedItems = recipientItems
          .asArray()
          .map((item) => recipientFromJSONItem(item)!)
          .map((rec) => EmailWithName.fromNameAndEmail(rec.name, rec.email).asString())
        return new Result(parsedItems.join(' | '), null)
      })
      const jsonSerializer = MockJSONSerializer({
        serialize,
      })
      const clearAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
      const insertAttaches = jest.fn().mockReturnValue(resolve(getVoid()))
      const attachmentsManager = makeAttachmentsManager({
        patch: {
          clearAttaches,
          insertAttaches,
        },
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        jsonSerializer,
        testIDSupport,
        makeMessagesSettings({ storage, params: { attachmentsManager } }),
      )

      messages
        .updateMessagesWithBodyMeta([
          {
            info: {
              mid: int64(1),
              attachments: [
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
              ],
              recipients: [
                new Recipient('to_1@yandex.ru', 'name 1', RecipientType.to),
                new Recipient('from_2@yandex.ru', 'name 2', RecipientType.from),
              ],
              rfcId: 'rfcId 1',
              references: 'reference 1',
            } as MessageBodyInfo,
            body: [],
            contentType: 'text/plain',
            lang: 'ru',
            quickReply: false,
            smartReplies: [],
          } as MessageBodyPayload,
          {
            info: {
              mid: int64(2),
              attachments: [
                // tslint:disable-next-line:max-line-length
                new MessageBodyAttach(
                  'hid 3',
                  'displayName 3',
                  int64(3),
                  'downloadUrl 3',
                  true,
                  true,
                  false,
                  'attachClass 3',
                  'mimeType 3',
                  'contentId 3',
                ),
                // tslint:disable-next-line:max-line-length
                new MessageBodyAttach(
                  'hid 4',
                  'displayName 4',
                  int64(4),
                  'downloadUrl 4',
                  true,
                  true,
                  true,
                  'attachClass 4',
                  'mimeType 4',
                  'contentId 4',
                ),
              ],
              recipients: [
                new Recipient('to_3@yandex.ru', 'name 3', RecipientType.to),
                new Recipient('from_4@yandex.ru', 'name 4', RecipientType.from),
              ],
              rfcId: 'rfcId 2',
              references: 'reference 2',
            } as MessageBodyInfo,
            body: [],
            contentType: 'text/html',
            lang: 'en',
            quickReply: false,
            smartReplies: [],
          } as MessageBodyPayload,
        ])
        .then(() => {
          expect(prepareStatement).toBeCalledTimes(1)
          expect(prepareStatement).toBeCalledWith(
            expect.stringContaining(
              // tslint:disable-next-line: max-line-length
              `INSERT OR REPLACE INTO ${EntityKind.message_body_meta} (mid, recipients, rfc_id, reference, contentType, lang, quick_reply_enabled) VALUES (?, ?, ?, ?, ?, ?, ?);`,
            ),
          )
          expect(notifyAboutChanges).toBeCalledTimes(1)
          expect(notifyAboutChanges).toBeCalledWith([EntityKind.message_body_meta])

          expect(execute).toBeCalledTimes(2)
          // tslint:disable-next-line:max-line-length
          expect(execute.mock.calls[0][0]).toStrictEqual([
            idstr(1),
            '"name 1" <to_1@yandex.ru> | "name 2" <from_2@yandex.ru>',
            'rfcId 1',
            'reference 1',
            'text/plain',
            'ru',
            0,
          ])
          // tslint:disable-next-line:max-line-length
          expect(execute.mock.calls[1][0]).toStrictEqual([
            idstr(2),
            '"name 3" <to_3@yandex.ru> | "name 4" <from_4@yandex.ru>',
            'rfcId 2',
            'reference 2',
            'text/html',
            'en',
            0,
          ])

          expect(clearAttaches).toBeCalledTimes(1)
          expect(clearAttaches).toBeCalledWith([int64(1), int64(2)])

          expect(insertAttaches).toBeCalledTimes(2)
          expect(insertAttaches.mock.calls[0]).toEqual([
            int64(1),
            [
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
            ],
          ])
          expect(insertAttaches.mock.calls[1]).toEqual([
            int64(2),
            [
              // tslint:disable-next-line:max-line-length
              new MessageBodyAttach(
                'hid 3',
                'displayName 3',
                int64(3),
                'downloadUrl 3',
                true,
                true,
                false,
                'attachClass 3',
                'mimeType 3',
                'contentId 3',
              ),
              // tslint:disable-next-line:max-line-length
              new MessageBodyAttach(
                'hid 4',
                'displayName 4',
                int64(4),
                'downloadUrl 4',
                true,
                true,
                true,
                'attachClass 4',
                'mimeType 4',
                'contentId 4',
              ),
            ],
          ])

          done()
        })
    })
  })

  describe('deleteFolderMessagesConnectionForFid', () => {
    it('should fail if statement cannot be created', (done) => {
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteFolderMessagesConnectionForFid(int64(1), [int64(2)]).failed((e) => {
        expect(e!.message).toBe('FAILED')
        done()
      })
    })
    it('should run a deletion script to remove all messages if mid-to-keep list is empty', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.deleteFolderMessagesConnectionForFid(int64(1), []).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(`DELETE FROM ${EntityKind.folder_messages} WHERE fid = ?;`)
        expect(statement.execute).toBeCalledWith([idstr(1)])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
        done()
      })
    })
    it('should run a deletion script to remove mids not in passed in from a certain folder', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.deleteFolderMessagesConnectionForFid(int64(1), [int64(401), int64(402)]).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.folder_messages} WHERE fid = ? AND mid NOT IN (401, 402);`,
        )
        expect(statement.execute).toBeCalledWith([idstr(1)])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.folder_messages])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteFolderMessagesConnectionForFid(int64(1), [int64(2)]).then((_) => {
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
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.deleteFolderMessagesConnectionForFid(int64(1), [int64(2)]).failed((e) => {
        expect(e!.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('removeNotSyncedMessageIDs', () => {
    it('should fail if statement run fails', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.removeNotSyncedMessageIDs(int64(1)).failed((e) => {
        expect(e!.message).toBe('FAILED')
        done()
      })
    })
    it('should run a deletion script to not synced messages by fid', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.removeNotSyncedMessageIDs(int64(1)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.not_synced_messages} WHERE fid = 1;`)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.not_synced_messages])
        done()
      })
    })
  })
  describe('deleteMessagesByTidExceptMids', () => {
    it('should fail if statement cannot be created', (done) => {
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteMessagesByTidExceptMids(int64(1), [int64(2)]).failed((e) => {
        expect(e!.message).toBe('FAILED')
        done()
      })
    })
    it('should run a deletion script to remove all messages if drafts list is empty', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.deleteMessagesByTidExceptMids(int64(1), []).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(`DELETE FROM ${EntityKind.message_meta} WHERE tid = ?;`)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        expect(statement.execute).toBeCalledWith([idstr(1)])
        done()
      })
    })
    it('should run a deletion script to remove mids except of drafts in sync', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.deleteMessagesByTidExceptMids(int64(2), [int64(301), int64(302)]).then((_) => {
        expect(storage.prepareStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.message_meta} WHERE tid = ? AND mid NOT IN (301, 302);`,
        )
        expect(statement.execute).toBeCalledWith([idstr(2)])
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteMessagesByTidExceptMids(int64(1), [int64(2)]).then((_) => {
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
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.deleteMessagesByTidExceptMids(int64(2), [int64(3)]).failed((e) => {
        expect(e!.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe(Messages.prototype.deleteMessagesByMidsNotInFid, () => {
    it('should return immediatelly if set of mids is empty', (done) => {
      const storage = MockStorage()
      const messages = makeMessages({ storage })
      messages.deleteMessagesByMidsNotInFid([], int64(1)).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should run deletion statement for mids and fid', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = makeMessages({ storage })
      messages.deleteMessagesByMidsNotInFid([int64(1), int64(2)], int64(3)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.message_meta} WHERE mid IN (1, 2) AND fid != 3;`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('insertMessages', () => {
    it('should return immediatelly if the parameter is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertMessages([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should generate insertion statements for passed messages', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'Name',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview',
              'https://ya.ru/download',
              true,
              '12345',
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]
      expect.assertions(3)
      messages.insertMessages(entities).then((_) => {
        expect(statement.execute).toBeCalledTimes(2)
        expect((statement.execute as any).mock.calls[0][0]).toStrictEqual([
          idstr(entities[0].mid),
          idstr(entities[0].fid),
          entities[0].tid !== null ? idstr(entities[0].tid) : null,
          entities[0].subjectEmpty,
          entities[0].subjectPrefix,
          entities[0].subjectText,
          entities[0].firstLine,
          entities[0].sender,
          entities[0].unread,
          entities[0].searchOnly,
          entities[0].showFor,
          int64ToString(entities[0].timestamp),
          entities[0].hasAttach,
          entities[0].typeMask,
        ])
        expect((statement.execute as any).mock.calls[1][0]).toStrictEqual([
          idstr(entities[1].mid),
          idstr(entities[1].fid),
          entities[1].tid !== null ? idstr(entities[1].tid) : null,
          entities[1].subjectEmpty,
          entities[1].subjectPrefix,
          entities[1].subjectText,
          entities[1].firstLine,
          entities[1].sender,
          entities[1].unread,
          entities[1].searchOnly,
          entities[1].showFor,
          int64ToString(entities[1].timestamp),
          entities[1].hasAttach,
          entities[1].typeMask,
        ])
        done()
      })
    })
    it('should generate insertion statements for passed messages', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'Name',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview',
              'https://ya.ru/download',
              true,
              '12345',
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]
      expect.assertions(2)
      messages.insertMessages(entities).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
    it('should close statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'Name',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview',
              'https://ya.ru/download',
              true,
              '12345',
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]
      expect.assertions(1)
      messages.insertMessages(entities).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close statement disregarding the result (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(rejected('FAILED')),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'Name',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview',
              'https://ya.ru/download',
              true,
              '12345',
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]
      expect.assertions(2)
      messages.insertMessages(entities).failed((e) => {
        expect(e!.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('insertCurrentMessageTimestamps', () => {
    it('should return immediatelly if a set of messages passed in is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertCurrentMessageTimestamps([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should insert with current timestamp', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(12345)
      messages.insertCurrentMessageTimestamps([int64(301)]).then((_) => {
        expect(statement.execute).toBeCalledWith([idstr(301), int64ToString(int64(12345))])
        nowSpy.mockRestore()
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(12345)
      messages.insertCurrentMessageTimestamps([int64(301), int64(302)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_timestamp])
        nowSpy.mockRestore()
        done()
      })
    })
    it('should insert all mids passed in', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(12345)
      messages.insertCurrentMessageTimestamps([int64(301), int64(302)]).then((_) => {
        expect((statement.execute as any).mock.calls[0][0]).toStrictEqual([idstr(301), int64ToString(int64(12345))])
        expect((statement.execute as any).mock.calls[1][0]).toStrictEqual([idstr(302), int64ToString(int64(12345))])
        nowSpy.mockRestore()
        done()
      })
    })
    it('should close statement disregarding results (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.insertCurrentMessageTimestamps([int64(301), int64(302)]).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close statement disregarding results (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('ERROR')),
      })
      const storage = MockStorage({ prepareStatement: jest.fn().mockReturnValue(resolve(statement)) })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertCurrentMessageTimestamps([int64(301), int64(302)]).failed((e) => {
        expect(e!.message).toBe('ERROR')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('insertOldMessageTimestamps', () => {
    it('should return immediatelly if a set of messages passed in is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertOldMessageTimestamps([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should insert with "old" timestamp (0)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.insertOldMessageTimestamps([int64(301)]).then((_) => {
        expect(statement.execute).toBeCalledWith([idstr(301), int64ToString(int64(0))])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertOldMessageTimestamps([int64(301), int64(302)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_timestamp])
        done()
      })
    })
    it('should insert all mids passed in', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertOldMessageTimestamps([int64(301), int64(302)]).then((_) => {
        expect((statement.execute as any).mock.calls[0][0]).toStrictEqual([idstr(301), int64ToString(int64(0))])
        expect((statement.execute as any).mock.calls[1][0]).toStrictEqual([idstr(302), int64ToString(int64(0))])
        done()
      })
    })
    it('should close statement disregarding results (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.insertOldMessageTimestamps([int64(301), int64(302)]).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close statement disregarding results (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValueOnce(resolve(getVoid())).mockReturnValueOnce(rejected('ERROR')),
      })
      const storage = MockStorage({ prepareStatement: jest.fn().mockReturnValue(resolve(statement)) })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.insertOldMessageTimestamps([int64(301), int64(302)]).failed((e) => {
        expect(e!.message).toBe('ERROR')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('updateMessageAttaches', () => {
    it('should return immediatelly if a set of messages passed in is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.updateMessageAttaches([]).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should delete old attachments with the passed mids', (done) => {
      const runStatementMock = jest.fn().mockReturnValueOnce(resolve(getVoid()))
      const insertionStatement = MockStorageStatement()
      const storage = MockStorage({
        runStatement: runStatementMock,
        prepareStatement: jest.fn().mockReturnValueOnce(resolve(insertionStatement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'Name',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview',
              'https://ya.ru/download',
              true,
              '12345',
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]

      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.updateMessageAttaches(entities).then((_) => {
        expect(runStatementMock).toBeCalledWith('DELETE FROM attachment WHERE mid IN (301, 302);')
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.attachment])
        done()
      })
    })
    it('should delete old attachments with the passed mids', (done) => {
      const runStatementMock = jest.fn().mockReturnValueOnce(resolve(getVoid()))
      const insertionStatement = MockStorageStatement()
      const storage = MockStorage({
        runStatement: runStatementMock,
        prepareStatement: jest.fn().mockReturnValueOnce(resolve(insertionStatement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'Name',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview',
              'https://ya.ru/download',
              true,
              '12345',
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]

      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.updateMessageAttaches(entities).then((_) => {
        expect(runStatementMock).toBeCalledWith('DELETE FROM attachment WHERE mid IN (301, 302);')
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.attachment])
        done()
      })
    })
    it('should insert new attachments with the passed mids', (done) => {
      const runStatementMock = jest.fn().mockReturnValueOnce(resolve(getVoid()))
      const insertionStatement = MockStorageStatement()
      const storage = MockStorage({
        runStatement: runStatementMock,
        prepareStatement: jest.fn().mockReturnValueOnce(resolve(insertionStatement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const entities = [
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['lid1', 'lid2'],
          false,
          'RE:',
          'Hello',
          'First line',
          'hello world <hello@world.com>',
          true,
          true,
          'search',
          int64(12345),
          true,
          new Attachments([
            new Attachment(
              '1.1',
              'name1.png',
              'image',
              false,
              int64(10000),
              'image/png',
              true,
              'https://ya.ru/preview1',
              'https://ya.ru/download1',
              false,
              null,
            ),
            new Attachment(
              '1.2',
              'name2.png',
              'image',
              false,
              int64(20000),
              'image/png',
              false,
              'https://ya.ru/preview2',
              'https://ya.ru/download2',
              true,
              '12345',
            ),
            new Attachment(
              '1.3',
              'name3.png',
              'image',
              true,
              int64(30000),
              'image/png',
              false,
              'https://ya.ru/preview3',
              'https://ya.ru/download3',
              true,
              '54321',
            ),
            new Attachment(
              '1.4',
              'name4.png',
              'image',
              true,
              int64(40000),
              'text/word',
              true,
              'https://ya.ru/preview4',
              'https://ya.ru/download4',
              false,
              null,
            ),
            new Attachment(
              '1.5',
              'name5.png',
              'image',
              false,
              int64(50000),
              'text/word',
              false,
              'https://ya.ru/preview5',
              'https://ya.ru/download5',
              false,
              null,
            ),
            new Attachment(
              '1.6',
              'name6.unknown',
              'image',
              false,
              int64(60000),
              'text/word',
              false,
              'https://ya.ru/preview6',
              'https://ya.ru/download6',
              false,
              null,
            ),
          ]),
          MessageTypeFlags.personal,
        ),
        new MessageMeta(
          int64(302),
          int64(102),
          null,
          ['lid2', 'lid3'],
          true,
          null,
          'Bye',
          'line',
          'hello world <hello@world.com>',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.personal,
        ),
      ]

      Registry.registerFileSystem(MockFileSystem())

      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(9)
      messages.updateMessageAttaches(entities).then((_) => {
        expect((insertionStatement.execute as any).mock.calls).toHaveLength(6)
        expect((insertionStatement.execute as any).mock.calls[0][0]).toStrictEqual([
          idstr(301),
          '1.1',
          'name1.png',
          'image',
          int64ToString(int64(10000)),
          'image/png',
          true,
          false,
          'https://ya.ru/download1',
        ])
        expect((insertionStatement.execute as any).mock.calls[1][0]).toStrictEqual([
          idstr(301),
          '1.2',
          'name2.png',
          'image',
          int64ToString(int64(20000)),
          'image/png',
          true,
          false,
          'https://ya.ru/download2',
        ])
        expect((insertionStatement.execute as any).mock.calls[2][0]).toStrictEqual([
          idstr(301),
          '1.3',
          'name3.png',
          'image',
          int64ToString(int64(30000)),
          'image/png',
          false,
          true,
          'https://ya.ru/download3',
        ])
        expect((insertionStatement.execute as any).mock.calls[3][0]).toStrictEqual([
          idstr(301),
          '1.4',
          'name4.png',
          'image',
          int64ToString(int64(40000)),
          'text/word',
          true,
          true,
          'https://ya.ru/download4',
        ])
        expect((insertionStatement.execute as any).mock.calls[4][0]).toStrictEqual([
          idstr(301),
          '1.5',
          'name5.png',
          'image',
          int64ToString(int64(50000)),
          'text/word',
          false,
          false,
          'https://ya.ru/download5',
        ])
        expect((insertionStatement.execute as any).mock.calls[5][0]).toStrictEqual([
          idstr(301),
          '1.6',
          'name6.unknown',
          'image',
          int64ToString(int64(60000)),
          'text/word',
          false,
          false,
          'https://ya.ru/download6',
        ])
        expect(storage.notifyAboutChanges).toBeCalledTimes(1)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.attachment])
        Registry.drop()
        done()
      })
    })
  })
  describe('updateMessagesWithBodyMeta', () => {
    it('should delete message body metas', (done) => {
      const runStatement = jest.fn().mockReturnValue(resolve(getVoid()))
      const notifyAboutChanges = jest.fn().mockReturnValue(resolve(getVoid()))
      const storage = MockStorage({
        runStatement,
        notifyAboutChanges,
      })

      const jsonSerializer = MockJSONSerializer()
      const messages = new Messages(
        MockNetwork(),
        storage,
        jsonSerializer,
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.deleteMessageBodyMetas([int64(1), int64(2), int64(3)]).then(() => {
        expect(runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.message_body_meta} WHERE mid IN (1, 2, 3);`)
        expect(notifyAboutChanges).toBeCalledWith([EntityKind.message_body_meta])
        done()
      })
    })
  })
  describe('selectNonExistingMidsInFolder', () => {
    it('should return immediatelly if the passed in set is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.selectNonExistingMidsInFolder(int64(1), new Set()).then((res) => {
        expect(res).toBeInstanceOf(Set)
        expect(res!.size).toBe(0)
        done()
      })
    })
    it('should use the mids passed in', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)], [int64(3)]]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.selectNonExistingMidsInFolder(int64(1), new Set([int64(3), int64(4)])).then((res) => {
        expect(
          storage.runQuery,
        ).toBeCalledWith('SELECT mid FROM folder_messages AS f WHERE f.fid = ? AND f.mid IN (3, 4);', [idstr(1)])
        expect(res!.size).toBe(1)
        expect(res!).toStrictEqual(new Set([int64(4)]))
        done()
      })
    })
  })
  describe('loadMessagesInFolder', () => {
    Models.setupInstance(
      'body-dir',
      MockNetwork(),
      MockStorage(),
      MockJSONSerializer(),
      MockFileSystem(),
      new TestIDSupport(),
      MockHighPrecisionTimer(),
      new MockSharedPreferences(),
      'attaches-temp',
      createMockInstance(AttachmentSizes),
    )
    it('should run network request', (done) => {
      const network = MockNetwork({ execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(responseSample))) })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.loadMessagesInFolder(int64(1), 30).then((res) => {
        expect(network.execute).toBeCalledWith(
          new MessagesRequestPack([MessageRequestItem.messagesInFolder(int64(1), 0, 30)], true),
        )
        expect(res!).toStrictEqual(messageResponseFromJSONItem(JSONItemFromJSON(responseSample))!)
        Models.drop()
        done()
      })
    })
    it('should fail on parsing error', (done) => {
      const network = MockNetwork({ execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON({}))) })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.loadMessagesInFolder(int64(1), 30).failed((error) => {
        expect(error.message).toStrictEqual('JSON Item parsing failed for entity MessageResponse')
        done()
      })
    })
  })
  describe('getOldestMessageTimestampByFid', () => {
    it('should fetch oldest message timestamp in folder by fid', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[1000]]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.getOldestMessageTimestampByFid(int64(1)).then((res) => {
        expect(storage.runQuery).toBeCalledWith(expect.any(String), [idstr(1)])
        expect(res!).toBe(1000)
        done()
      })
    })
  })
  describe(Messages.prototype.getFidsOfMessages, () => {
    it('should return empty array for empty argument', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.getFidsOfMessages([]).then((res) => {
        expect(res).toHaveLength(0)
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should return an array of fids for array of mids', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(1)], [int64(2)]]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.getFidsOfMessages([int64(3), int64(4), int64(5)]).then((res) => {
        expect(res).toStrictEqual([int64(1), int64(2)])
        expect(storage.runQuery).toBeCalledWith(
          `SELECT DISTINCT fid FROM ${EntityKind.message_meta} WHERE mid IN (3, 4, 5);`,
          [],
        )
        done()
      })
    })
  })
  describe('filterMessagesWithoutBody', () => {
    it('should return immediatelly if empty list is passed', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.filterMessagesWithoutBody([]).then((res) => {
        expect(res).toStrictEqual([])
        expect(storage.runQuery).not.toBeCalled()
        done()
      })
    })
    it('should return filtered messages', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(2)], [int64(3)]]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.filterMessagesWithoutBody([int64(1), int64(2), int64(3)]).then((res) => {
        expect(res).toStrictEqual([int64(1)])
        expect(storage.runQuery).toBeCalledWith(
          `SELECT mid FROM ${EntityKind.message_body_meta} WHERE mid IN (1, 2, 3);`,
          [],
        )
        done()
      })
    })
  })
  describe(Messages.prototype.executeMessageBodiesRequest, () => {
    it('should return immediatelly with empty array if mids is empty', (done) => {
      const network = MockNetwork()
      const messages = new Messages(network, MockStorage(), MockJSONSerializer(), testIDSupport, makeMessagesSettings())
      messages.executeMessageBodiesRequest([]).then((res) => {
        expect(res).toHaveLength(0)
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should fail on parsing error', (done) => {
      const network = MockNetwork({ execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON({}))) })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.executeMessageBodiesRequest([int64(1), int64(2)]).failed((error) => {
        expect(network.execute).toBeCalledWith(new MessageBodyRequest([int64(1), int64(2)]))
        expect(error!.message!).toStrictEqual('JSON Item parsing failed for entity MessageBodyResponse')
        done()
      })
    })
    it('should return message body payloads for network status "Ok"', (done) => {
      Models.setupInstance(
        'body-dir',
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        MockFileSystem(),
        new TestIDSupport(),
        MockHighPrecisionTimer(),
        new MockSharedPreferences(),
        'attaches-temp',
        createMockInstance(AttachmentSizes),
      )
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(resolve(JSONItemFromJSON(messageBodyResponseSample))),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.executeMessageBodiesRequest([int64(1), int64(2)]).then((res) => {
        expect(network.execute).toBeCalledWith(new MessageBodyRequest([int64(1), int64(2)]))
        const parsedValue = messageBodyResponseFromJSONItem(JSONItemFromJSON(messageBodyResponseSample))!
          .slice(0, 2)
          .map((val) => val.payload!)
        expect(res).toStrictEqual(parsedValue)
        Models.drop()
        done()
      })
    })
  })
  describe(Messages.prototype.loadMessageBodies, () => {
    it('returns immediatelly if mids is empty', (done) => {
      const messages = new Messages(
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings(),
      )
      const filterMessagesWithoutBodySpy = jest.spyOn(messages, 'filterMessagesWithoutBody')
      messages.loadMessageBodies([]).then((res) => {
        expect(res).toHaveLength(0)
        expect(filterMessagesWithoutBodySpy).not.toBeCalled()
        done()
      })
    })
    it('filters message bodies before executing request', (done) => {
      const result = messageBodyResponseFromJSONItem(JSONItemFromJSON(messageBodyResponseSample))!
        .slice(0, 2)
        .map((val) => val.payload!)
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const filterMessagesSpy = jest
        .spyOn(messages, 'filterMessagesWithoutBody')
        .mockReturnValue(resolve([int64(2), int64(3)]))
      const executeBodiesRequestSpy = jest
        .spyOn(messages, 'executeMessageBodiesRequest')
        .mockReturnValue(resolve(result))

      expect.assertions(3)
      messages.loadMessageBodies([int64(1), int64(2), int64(3)]).then((res) => {
        expect(res).toBe(result)
        expect(filterMessagesSpy).toBeCalledWith([int64(1), int64(2), int64(3)])
        expect(executeBodiesRequestSpy).toBeCalledWith([int64(2), int64(3)])
        done()
      })
    })
  })
  describe(Messages.prototype.sendMarkReadAction, () => {
    it('should return immediatelly with OK if passed mids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMarkReadAction([], true).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should send Mark As Read request', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMarkReadAction([int64(1), int64(2)], true).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).toBeCalledWith(new MarkReadNetworkRequest([int64(1), int64(2)], [], true))
        done()
      })
    })
  })
  describe(Messages.prototype.sendMarkWithLabelsAction, () => {
    it('should return immediatelly with OK if passed mids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMarkWithLabelsAction([], ['lid'], true).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should return immediatelly with OK if passed lids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMarkWithLabelsAction([int64(1)], [], true).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should send Mark With Labels request', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMarkWithLabelsAction([int64(1), int64(2)], ['lbl1', 'lbl2'], true).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).toBeCalledWith(
          new MarkWithLabelsNetworkRequest([int64(1), int64(2)], [], ['lbl1', 'lbl2'], true),
        )
        done()
      })
    })
  })
  describe(Messages.prototype.sendMoveToFolderAction, () => {
    it('should return immediatelly with OK if passed mids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMoveToFolderAction([], int64(1), int64(2)).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should send Move To Folder request', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendMoveToFolderAction([int64(1), int64(2)], int64(3), int64(4)).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).toBeCalledWith(
          new MoveToFolderNetworkRequest([int64(1), int64(2)], [], int64(3), int64(4)),
        )
        done()
      })
    })
  })
  describe(Messages.prototype.sendDeleteMessagesInFolderAction, () => {
    it('should return immediatelly with OK if passed mids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendDeleteMessagesInFolderAction([], int64(1)).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should send Delete Messages in Folder request', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendDeleteMessagesInFolderAction([int64(1), int64(2)], int64(3)).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).toBeCalledWith(new DeleteMessagesNetworkRequest([int64(1), int64(2)], [], int64(3)))
        done()
      })
    })
  })
  describe(Messages.prototype.sendDeleteMessagesAction, () => {
    it('should return immediatelly with OK if passed mids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendDeleteMessagesAction([], int64(3)).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
    it('should send Delete Messages request', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendDeleteMessagesAction([int64(1), int64(2)], int64(3)).then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).toBeCalledWith(new DeleteMessagesNetworkRequest([int64(1), int64(2)], [], int64(3)))
        done()
      })
    })
  })
  describe(Messages.prototype.sendArchiveMessagesAction, () => {
    it('should return immediatelly with OK if passed mids are empty', (done) => {
      const network = MockNetwork({
        execute: jest.fn().mockReturnValue(
          resolve(
            JSONItemFromJSON({
              status: 1,
            }),
          ),
        ),
      })
      const storage = MockStorage()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.sendArchiveMessagesAction([], 'Archive').then((status) => {
        expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
        expect(network.execute).not.toBeCalled()
        done()
      })
    })
  })
  it('should send Archive Messages request', (done) => {
    const network = MockNetwork({
      execute: jest.fn().mockReturnValue(
        resolve(
          JSONItemFromJSON({
            status: 1,
          }),
        ),
      ),
    })
    const storage = MockStorage()
    const messages = new Messages(
      network,
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage, network }),
    )
    messages.sendArchiveMessagesAction([int64(1), int64(2)], 'Archive').then((status) => {
      expect(status).toStrictEqual(new NetworkStatus(NetworkStatusCode.ok))
      expect(network.execute).toBeCalledWith(new ArchiveMessagesNetworkRequest([int64(1), int64(2)], [], 'Archive'))
      done()
    })
  })
  describe('deleteMessages', () => {
    afterEach(jest.restoreAllMocks)
    it('should create transaction based on flag passed in (true)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockImplementation().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteMessages([int64(301), int64(302)], true).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
        done()
      })
    })
    it('should create transaction based on flag passed in (false)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>().mockImplementation().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteMessages([int64(301), int64(302)], false).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
        done()
      })
    })
    it('should return immediatelly if empty set was passed', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteMessages([], true).then((_) => {
        expect(storage.withinTransaction).not.toBeCalled()
        done()
      })
    })
    it('should do necessary cleanup operations', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const fm = makeFolders({ storage })
      const cl = makeCleanup({ storage, threads: makeThreads({ storage }), folders: fm })
      jest.spyOn(fm, 'cleanUpFolderMessagesConnection').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cl, 'removeOrphans').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cl, 'rebuildAggregates').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders: fm, cleanup: cl } }),
      )
      jest.spyOn(messages, 'deleteEntriesByMids').mockReturnValue(resolve(getVoid()))
      expect.assertions(4)
      const mids = [int64(301), int64(302)]
      messages.deleteMessages(mids, true).then((_) => {
        expect(messages.deleteEntriesByMids).toBeCalledWith(mids)
        expect(fm.cleanUpFolderMessagesConnection).toBeCalledWith(mids)
        expect(cl.removeOrphans).toBeCalledWith(mids)
        expect(cl.rebuildAggregates).toBeCalledWith(mids)
        done()
      })
    })
    it('should fail if any of the necessary cleanup operations fails', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const fm = makeFolders({ storage })
      const cl = makeCleanup({ storage, threads: makeThreads({ storage }), folders: fm })
      jest.spyOn(fm, 'cleanUpFolderMessagesConnection').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cl, 'removeOrphans').mockReturnValue(rejected('FAILED'))
      jest.spyOn(cl, 'rebuildAggregates').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders: fm, cleanup: cl } }),
      )
      jest.spyOn(messages, 'deleteEntriesByMids').mockReturnValue(resolve(getVoid()))
      expect.assertions(1)
      messages.deleteMessages([int64(301), int64(302)], true).failed((e) => {
        expect(e.message).toBe('FAILED')
        done()
      })
    })
  })
  describe('deleteEntriesByMids', () => {
    it('should return immediatelly if empty set was passed', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.deleteEntriesByMids([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should include passed mids in SQL query', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteEntriesByMids([int64(1), int64(2)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith('DELETE FROM message_meta WHERE mid IN (1, 2);')
        done()
      })
    })
    it('should notify about changes', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteEntriesByMids([int64(1), int64(2)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('changeLabels', () => {
    describe('if the message does not exist in the Storage', () => {
      afterEach(jest.restoreAllMocks)
      it('should insert entry into labels_messages', (done) => {
        const storage = MockStorage({
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const labels = makeLabels({ storage })
        const insertMessageLabelEntriesSpy = jest
          .spyOn(labels, 'insertMessageLabelEntries')
          .mockReturnValue(resolve(getVoid()))
        const messages = new Messages(
          MockNetwork(),
          storage,
          MockJSONSerializer(),
          testIDSupport,
          makeMessagesSettings({ storage, params: { labels } }),
        )
        const messageMetaByMidSpy = jest.spyOn(messages as any, 'messageMetaByMid').mockReturnValue(resolve(null))
        expect.assertions(5)
        messages.changeLabels(int64(301), ['lid1', 'lid2']).then((_) => {
          expect(messageMetaByMidSpy).toBeCalledWith(int64(301))
          expect(storage.runStatement).toBeCalledWith(
            `DELETE FROM ${EntityKind.labels_messages} WHERE mid = ${idstr(301)};`,
          )
          expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
          expect(insertMessageLabelEntriesSpy).toBeCalledWith([
            new MessageLabelEntry(int64(301), 'lid1', null),
            new MessageLabelEntry(int64(301), 'lid2', null),
          ])
          done()
        })
      })
      it('should only remove old entries from labels_messages if set of labels is empty', (done) => {
        const storage = MockStorage({
          prepareStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
          withinTransaction: MockWithinTransaction<any>(),
          notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        })
        const labels = makeLabels({ storage })
        const insertMessageLabelEntriesSpy = jest
          .spyOn(labels, 'insertMessageLabelEntries')
          .mockReturnValue(resolve(getVoid()))
        const messages = new Messages(
          MockNetwork(),
          storage,
          MockJSONSerializer(),
          testIDSupport,
          makeMessagesSettings({ storage, params: { labels } }),
        )
        const messageMetaByMidSpy = jest.spyOn(messages as any, 'messageMetaByMid').mockReturnValue(resolve(null))
        expect.assertions(7)
        messages.changeLabels(int64(301), []).then((_) => {
          expect(messageMetaByMidSpy).toBeCalledWith(int64(301))
          expect(storage.prepareStatement).not.toBeCalled()
          expect(storage.runStatement).toBeCalledWith(expect.stringContaining('mid = 301'))
          expect(storage.withinTransaction).toBeCalledTimes(1)
          expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
          expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.labels_messages])
          expect(insertMessageLabelEntriesSpy).toBeCalledWith([])
          done()
        })
      })
    })
    describe('if the message exists in the Storage', () => {
      it('should update message with labels', (done) => {
        const storage = MockStorage()
        const messages = new Messages(
          MockNetwork(),
          storage,
          MockJSONSerializer(),
          testIDSupport,
          makeMessagesSettings({ storage }),
        )
        const msg = new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          [],
          false,
          null,
          'subject',
          'firstline',
          'from@yandex.ru',
          true,
          true,
          null,
          int64(12345),
          false,
          null,
          MessageTypeFlags.people,
        )
        const updateMessageWithLabelsSpy = jest
          .spyOn(messages, 'updateMessageWithLabels')
          .mockReturnValue(resolve(getVoid()))
        jest.spyOn(messages as any, 'messageMetaByMid').mockReturnValue(resolve(msg))
        expect.assertions(1)
        messages.changeLabels(int64(301), ['lid1', 'lid2']).then((_) => {
          expect(updateMessageWithLabelsSpy).toBeCalledWith(msg, ['lid1', 'lid2'])
          done()
        })
      })
    })
  })
  describe(Messages.prototype.moveMessage, () => {
    afterEach(jest.restoreAllMocks)
    it('should fetch message by mid and update messages, cleanup, update dependent tables', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const folders = makeFolders({ storage })
      const threads = makeThreads({ storage })
      const cleanup = makeCleanup({ storage, threads, folders })
      const cleanUpFolderMessagesConnectionSpy = jest
        .spyOn(folders, 'cleanUpFolderMessagesConnection')
        .mockReturnValue(resolve(getVoid()))
      const insertFolderMessagesConnectionMidFidsSpy = jest
        .spyOn(folders, 'insertFolderMessagesConnectionMidFids')
        .mockReturnValue(resolve(getVoid()))
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([int64(401), int64(402)]))
      const generateThreadsAfterMoveFromFolderSpy = jest
        .spyOn(threads, 'generateThreadsAfterMoveFromFolder')
        .mockReturnValue(resolve(getVoid()))
      const updateTopsSpy = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const cleanUpFolderThreadsByTopsSpy = jest
        .spyOn(cleanup, 'cleanUpFolderThreadsByTops')
        .mockReturnValue(resolve(getVoid()))
      const rebuildCountersSpy = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, threads, cleanup } }),
      )
      const resetMessagesTimestampSpy = jest
        .spyOn(messages as any, 'resetMessagesTimestamp')
        .mockReturnValue(resolve(getVoid()))
      const updateMessagesFidSpy = jest.spyOn(messages as any, 'updateMessagesFid').mockReturnValue(resolve(getVoid()))
      const messageMetaByMidSpy = jest
        .spyOn(messages as any, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(301),
              int64(102),
              int64(402),
              [],
              false,
              null,
              'subject',
              'firstline',
              'from@yandex.ru',
              true,
              true,
              null,
              int64(12345),
              false,
              null,
              MessageTypeFlags.people,
            ),
          ),
        )
      const restoreThreadsIfNeededSpy = jest
        .spyOn(messages as any, 'restoreThreadsIfNeeded')
        .mockReturnValue(resolve(getVoid()))
      const updateSearchShowForSpy = jest
        .spyOn(messages as any, 'updateSearchShowFor')
        .mockReturnValue(resolve(getVoid()))
      messages.moveMessage(int64(101), int64(401), int64(301), ['lid1', 'lid2']).then((_) => {
        expect(messageMetaByMidSpy).toBeCalledWith(int64(301))
        expect(resetMessagesTimestampSpy).toBeCalledWith([int64(301)])
        expect(updateMessagesFidSpy).toBeCalledWith([int64(301)], int64(101))
        expect(cleanUpFolderMessagesConnectionSpy).toBeCalledWith([int64(301)], int64(102))
        expect(insertFolderMessagesConnectionMidFidsSpy).toBeCalledWith([new YSPair(int64(301), int64(101))])
        expect(restoreThreadsIfNeededSpy).toBeCalledWith(int64(101), [int64(301)])
        expect(getTidsByMidsSpy).toBeCalledWith([int64(301)])
        expect(generateThreadsAfterMoveFromFolderSpy).toBeCalledWith(int64(101), [int64(301)])
        expect(updateTopsSpy).toBeCalledWith([int64(401), int64(402)])
        expect(cleanUpFolderThreadsByTopsSpy).toBeCalled()
        expect(rebuildCountersSpy).toBeCalledWith([int64(301)])
        expect(updateSearchShowForSpy).toBeCalledWith(int64(102), int64(101), [int64(301)])
        done()
      })
    })
    it('should be able to use transactions', (done) => {
      const storage = MockStorage({
        withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const useTransactions = true
      messages.moveMessage(int64(101), int64(401), int64(301), ['lid1', 'lid2'], useTransactions).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(useTransactions, expect.any(Function))
        done()
      })
    })
    it('should be able to not use transactions', (done) => {
      const storage = MockStorage({
        withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const useTransactions = false
      messages.moveMessage(int64(101), int64(401), int64(301), ['lid1', 'lid2'], useTransactions).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(useTransactions, expect.any(Function))
        done()
      })
    })
    it('should return immediatelly if no message with the specified mid found', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const updateMessagesFidSpy = jest.spyOn(messages as any, 'updateMessagesFid').mockReturnValue(resolve(getVoid()))
      const messageMetaByMidSpy = jest.spyOn(messages as any, 'messageMetaByMid').mockReturnValue(resolve(null))
      messages.moveMessage(int64(101), int64(401), int64(301), ['lid1', 'lid2']).then((_) => {
        expect(messageMetaByMidSpy).toBeCalledWith(int64(301))
        expect(updateMessagesFidSpy).not.toBeCalled()
        done()
      })
    })
  })
  describe(Messages.prototype.moveMessages, () => {
    afterEach(jest.restoreAllMocks)
    it('should return immediatelly if the mids list is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(MockNetwork(), storage, MockJSONSerializer(), testIDSupport, makeMessagesSettings())
      messages.moveMessages([], int64(1), int64(2), true).then((_) => {
        expect(storage.withinTransaction).not.toBeCalled()
        done()
      })
    })
    it('should update messages, cleanup, update dependent tables', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const mids = [int64(301), int64(302)]
      const targetFid = int64(101)
      const currentFid = int64(102)
      const folders = makeFolders({ storage })
      const threads = makeThreads({ storage })
      const cleanup = makeCleanup({ storage, threads, folders })
      const cleanUpFolderMessagesConnectionSpy = jest
        .spyOn(folders, 'cleanUpFolderMessagesConnection')
        .mockReturnValue(resolve(getVoid()))
      const insertFolderMessagesConnectionMidsSpy = jest
        .spyOn(folders, 'insertFolderMessagesConnectionMids')
        .mockReturnValue(resolve(getVoid()))
      const getTidsByMidsSpy = jest.spyOn(threads, 'getTidsByMids').mockReturnValue(resolve([int64(401), int64(402)]))
      const generateThreadsAfterMoveFromFolderSpy = jest
        .spyOn(threads, 'generateThreadsAfterMoveFromFolder')
        .mockReturnValue(resolve(getVoid()))
      const updateTopsSpy = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const cleanUpFolderThreadsByTopsSpy = jest
        .spyOn(cleanup, 'cleanUpFolderThreadsByTops')
        .mockReturnValue(resolve(getVoid()))
      const rebuildCountersSpy = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, threads, cleanup } }),
      )
      const updateMessageFidSpy = jest.spyOn(messages as any, 'updateMessagesFid').mockReturnValue(resolve(getVoid()))
      const restoreThreadsIfNeededSpy = jest
        .spyOn(messages as any, 'restoreThreadsIfNeeded')
        .mockReturnValue(resolve(getVoid()))
      messages.moveMessages(mids, targetFid, currentFid).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(false, expect.any(Function))
        expect(updateMessageFidSpy).toBeCalledWith(mids, targetFid)
        expect(cleanUpFolderMessagesConnectionSpy).toBeCalledWith(mids, currentFid)
        expect(insertFolderMessagesConnectionMidsSpy).toBeCalledWith(mids, targetFid)
        expect(restoreThreadsIfNeededSpy).toBeCalledWith(targetFid, mids)
        expect(getTidsByMidsSpy).toBeCalledWith(mids)
        expect(generateThreadsAfterMoveFromFolderSpy).toBeCalledWith(targetFid, mids)
        expect(updateTopsSpy).toBeCalledWith([int64(401), int64(402)])
        expect(cleanUpFolderThreadsByTopsSpy).toBeCalled()
        expect(rebuildCountersSpy).toBeCalledWith(mids)
        done()
      })
    })
    it('should be able to use transactions', (done) => {
      const storage = MockStorage({
        withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const useTransactions = true
      messages.moveMessages([int64(301), int64(302)], int64(101), int64(102), useTransactions).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(useTransactions, expect.any(Function))
        done()
      })
    })
    it('should be able to not use transactions', (done) => {
      const storage = MockStorage({
        withinTransaction: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      const useTransactions = false
      messages.moveMessages([int64(301), int64(302)], int64(101), int64(102), useTransactions).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(useTransactions, expect.any(Function))
        done()
      })
    })
  })
  describe('saveDraft', () => {
    // tslint:disable-next-line: max-line-length
    it('should insert thread if needed, store draft, attaches, update timestamp, update tops, labels, counters, load body, remove old bodies', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const folders = makeFolders({ storage })
      const labels = makeLabels({ storage })
      const bodies = makeBodies()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, labels, threads, bodies } }),
      )
      const insertThreadIfNeeded = jest.spyOn(messages, 'insertThreadIfNeeded').mockReturnValue(resolve(getVoid()))
      const insertMessages = jest.spyOn(messages, 'insertMessages').mockReturnValue(resolve(getVoid()))
      const updateMessageAttaches = jest.spyOn(messages, 'updateMessageAttaches').mockReturnValue(resolve(getVoid()))
      const insertCurrentMessageTimestamps = jest
        .spyOn(messages, 'insertCurrentMessageTimestamps')
        .mockReturnValue(resolve(getVoid()))
      const updateTops = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const deleteOldDrafts = jest.spyOn(messages, 'deleteOldDrafts').mockReturnValue(resolve(getVoid()))
      const bs = [
        new MessageBodyPayload(
          new MessageBodyInfo(
            int64(301),
            [],
            [new Recipient('rec@yandex.com', 'Recipient', RecipientType.to)],
            'rfcid',
            'references',
          ),
          [new MessageBodyPart('hid1', 'The Content', 'text', 'en')],
          'contenttype',
          'en',
          false,
          [],
        ),
      ]
      const loadMessageBodies = jest.spyOn(messages, 'loadMessageBodies').mockReturnValue(resolve(bs))
      const insertMessageLabels = jest.spyOn(labels, 'insertMessageLabels').mockReturnValue(resolve(getVoid()))
      const insertFolderMessagesConnectionMessages = jest
        .spyOn(folders, 'insertFolderMessagesConnectionMessages')
        .mockReturnValue(resolve(getVoid()))
      const updateCountersForSingleFolder = jest
        .spyOn(folders, 'updateCountersForSingleFolder')
        .mockReturnValue(resolve(getVoid()))
      const storeMessageBodies = jest.spyOn(bodies, 'storeMessageBodies').mockReturnValue(resolve(getVoid()))
      const updateBodiesTimestamps = jest.spyOn(bodies, 'updateBodiesTimestamps').mockReturnValue(resolve(getVoid()))
      const updateMessagesWithBodyMeta = jest
        .spyOn(messages, 'updateMessagesWithBodyMeta')
        .mockReturnValue(resolve(getVoid()))
      const e = new DeltaApiEnvelope(
        int64(301),
        int64(101),
        int64(401),
        12345,
        int64(54321),
        int64(54321),
        [new DeltaApiRecipient('from', 'domain.com', 'From')],
        [new DeltaApiRecipient('replyTo', 'domain.com', 'Reply')],
        'Pre Subject',
        new DeltaApiSubject('type', 'Pre', 'Subject', 'Post', false),
        [new DeltaApiRecipient('cc', 'domain.com', 'Cc')],
        [new DeltaApiRecipient('bcc', 'domain.com', 'Bcc')],
        [new DeltaApiRecipient('to', 'domain.com', 'To')],
        null,
        null,
        null,
        'firstLine',
        'inreplyto@domain.com',
        'references',
        'rfcid',
        12345,
        0,
        0,
        1,
        1000,
        [new DeltaApiAttachment('hid1', 'image/png', 'image.png', 1000)],
        ['lid1', 'lid2', 'FAKE_SEEN_LBL'],
        [4, 103],
      )
      const expectedMessages = [deltaApiEnvelopeToMessageMeta(e)]
      messages.saveDraft(e).then((_) => {
        expect(insertThreadIfNeeded).toBeCalledWith(int64(401), int64(101), int64(301))
        expect(insertMessages).toBeCalledWith(expectedMessages)
        expect(updateMessageAttaches).toBeCalledWith(expectedMessages)
        expect(insertCurrentMessageTimestamps).toBeCalledWith([int64(301)])
        expect(updateTops).toBeCalledWith([int64(401)])
        expect(insertMessageLabels).toBeCalledWith(expectedMessages)
        expect(insertFolderMessagesConnectionMessages).toBeCalledWith(expectedMessages)
        expect(updateCountersForSingleFolder).toBeCalledWith(int64(101))
        expect(deleteOldDrafts).toBeCalledWith(expectedMessages)
        expect(loadMessageBodies).toBeCalledWith([int64(301)])
        expect(storeMessageBodies).toBeCalledWith(bs)
        expect(updateBodiesTimestamps).toBeCalledWith(bs, new Map([[int64(101), int64(54321)]]))
        expect(updateMessagesWithBodyMeta).toBeCalledWith(bs)
        done()
      })
    })
    // tslint:disable-next-line: max-line-length
    it('should insert thread if needed, store draft, attaches, update timestamp, update tops, labels, counters, load body, remove old bodies (empty tid)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const folders = makeFolders({ storage })
      const labels = makeLabels({ storage })
      const bodies = makeBodies()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, labels, threads, bodies } }),
      )
      const insertThreadIfNeeded = jest.spyOn(messages, 'insertThreadIfNeeded').mockReturnValue(resolve(getVoid()))
      const insertMessages = jest.spyOn(messages, 'insertMessages').mockReturnValue(resolve(getVoid()))
      const updateMessageAttaches = jest.spyOn(messages, 'updateMessageAttaches').mockReturnValue(resolve(getVoid()))
      const insertCurrentMessageTimestamps = jest
        .spyOn(messages, 'insertCurrentMessageTimestamps')
        .mockReturnValue(resolve(getVoid()))
      const updateTops = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const deleteOldDrafts = jest.spyOn(messages, 'deleteOldDrafts').mockReturnValue(resolve(getVoid()))
      const bs = [
        new MessageBodyPayload(
          new MessageBodyInfo(
            int64(301),
            [],
            [new Recipient('rec@yandex.com', 'Recipient', RecipientType.to)],
            'rfcid',
            'references',
          ),
          [new MessageBodyPart('hid1', 'The Content', 'text', 'en')],
          'contenttype',
          'en',
          false,
          [],
        ),
      ]
      const loadMessageBodies = jest.spyOn(messages, 'loadMessageBodies').mockReturnValue(resolve(bs))
      const insertMessageLabels = jest.spyOn(labels, 'insertMessageLabels').mockReturnValue(resolve(getVoid()))
      const insertFolderMessagesConnectionMessages = jest
        .spyOn(folders, 'insertFolderMessagesConnectionMessages')
        .mockReturnValue(resolve(getVoid()))
      const updateCountersForSingleFolder = jest
        .spyOn(folders, 'updateCountersForSingleFolder')
        .mockReturnValue(resolve(getVoid()))
      const storeMessageBodies = jest.spyOn(bodies, 'storeMessageBodies').mockReturnValue(resolve(getVoid()))
      const updateBodiesTimestamps = jest.spyOn(bodies, 'updateBodiesTimestamps').mockReturnValue(resolve(getVoid()))
      const updateMessagesWithBodyMeta = jest
        .spyOn(messages, 'updateMessagesWithBodyMeta')
        .mockReturnValue(resolve(getVoid()))
      const e = new DeltaApiEnvelope(
        int64(301),
        int64(101),
        null,
        12345,
        int64(54321),
        int64(54321),
        [new DeltaApiRecipient('from', 'domain.com', 'From')],
        [new DeltaApiRecipient('replyTo', 'domain.com', 'Reply')],
        'Pre Subject',
        new DeltaApiSubject('type', 'Pre', 'Subject', 'Post', false),
        [new DeltaApiRecipient('cc', 'domain.com', 'Cc')],
        [new DeltaApiRecipient('bcc', 'domain.com', 'Bcc')],
        [new DeltaApiRecipient('to', 'domain.com', 'To')],
        null,
        null,
        null,
        'firstLine',
        'inreplyto@domain.com',
        'references',
        'rfcid',
        12345,
        0,
        0,
        1,
        1000,
        [new DeltaApiAttachment('hid1', 'image/png', 'image.png', 1000)],
        ['lid1', 'lid2', 'FAKE_SEEN_LBL'],
        [4, 103],
      )
      const expectedMessages = [deltaApiEnvelopeToMessageMeta(e)]
      messages.saveDraft(e).then((_) => {
        expect(insertThreadIfNeeded).toBeCalledWith(null, int64(101), int64(301))
        expect(insertMessages).toBeCalledWith(expectedMessages)
        expect(updateMessageAttaches).toBeCalledWith(expectedMessages)
        expect(insertCurrentMessageTimestamps).toBeCalledWith([int64(301)])
        expect(updateTops).toBeCalledWith([])
        expect(insertMessageLabels).toBeCalledWith(expectedMessages)
        expect(insertFolderMessagesConnectionMessages).toBeCalledWith(expectedMessages)
        expect(updateCountersForSingleFolder).toBeCalledWith(int64(101))
        expect(deleteOldDrafts).toBeCalledWith(expectedMessages)
        expect(loadMessageBodies).toBeCalledWith([int64(301)])
        expect(storeMessageBodies).toBeCalledWith(bs)
        expect(updateBodiesTimestamps).toBeCalledWith(bs, new Map([[int64(101), int64(54321)]]))
        expect(updateMessagesWithBodyMeta).toBeCalledWith(bs)
        done()
      })
    })
  })
  describe('attachToThread', () => {
    afterEach(jest.restoreAllMocks)
    it('should do nothing if the modified message does not exist', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads } }),
      )
      jest.spyOn(messages, 'messageMetaByMid').mockReturnValue(resolve(null))
      jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(null))
      messages.attachToThread(int64(301), int64(401), []).then((_) => {
        expect(threads.topMidForTidAndFid).not.toBeCalled()
        done()
      })
    })
    it('should do nothing if the modified message is already in the thread', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads } }),
      )
      const tid = int64(401)
      jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(301),
              int64(101),
              tid,
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
          ),
        )
      jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(null))
      messages.attachToThread(int64(301), tid, []).then((_) => {
        expect(threads.topMidForTidAndFid).not.toBeCalled()
        done()
      })
    })
    it('should create thread if it does not exist', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const cleanup = makeCleanup({ storage, threads })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads, cleanup } }),
      )
      jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(301),
              int64(101),
              int64(401),
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
          ),
        )
      jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(null))
      jest.spyOn(messages, 'updateMessageTid').mockReturnValue(resolve(getVoid()))
      jest.spyOn(messages, 'updateMessageWithLabels').mockReturnValue(resolve(getVoid()))
      const insertThreadsSpy = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      jest.spyOn(threads, 'generateThreadsAfterMoveFromFolder').mockReturnValue(resolve(getVoid()))
      jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cleanup, 'cleanUpFolderThreadsByTops').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
      messages.attachToThread(int64(301), int64(402), []).then((_) => {
        expect(insertThreadsSpy).toBeCalledWith([new ThreadInFolder(int64(402), int64(101), int64(301))])
        done()
      })
    })
    it('should update existing thread if it exists', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const cleanup = makeCleanup({ storage, threads })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads, cleanup } }),
      )
      jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(301),
              int64(101),
              int64(401),
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
          ),
        )
      jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(int64(303)))
      jest.spyOn(messages, 'updateMessageTid').mockReturnValue(resolve(getVoid()))
      jest.spyOn(messages, 'updateMessageWithLabels').mockReturnValue(resolve(getVoid()))
      const insertThreadsSpy = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      jest.spyOn(threads, 'generateThreadsAfterMoveFromFolder').mockReturnValue(resolve(getVoid()))
      jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cleanup, 'cleanUpFolderThreadsByTops').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
      messages.attachToThread(int64(301), int64(402), []).then((_) => {
        expect(insertThreadsSpy).toBeCalledWith([new ThreadInFolder(int64(402), int64(101), int64(303))])
        done()
      })
    })
    it('should update message, thread, top mids, labels, and counters', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const cleanup = makeCleanup({ storage, threads })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads, cleanup } }),
      )
      const mid = int64(301)
      const fid = int64(101)
      const newTid = int64(402)
      const oldTid = int64(401)
      const lids = ['lid1', 'lid2']
      const topMid = int64(303)
      const messageMeta = new MessageMeta(
        mid,
        fid,
        oldTid,
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
      )
      const messageMetaByMid = jest.spyOn(messages, 'messageMetaByMid').mockReturnValue(resolve(messageMeta))
      const topMidForTidAndFid = jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(topMid))
      const updateMessageTid = jest.spyOn(messages, 'updateMessageTid').mockReturnValue(resolve(getVoid()))
      const updateMessageWithLabels = jest
        .spyOn(messages, 'updateMessageWithLabels')
        .mockReturnValue(resolve(getVoid()))
      const insertThreads = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      const generateThreadsAfterMoveFromFolder = jest
        .spyOn(threads, 'generateThreadsAfterMoveFromFolder')
        .mockReturnValue(resolve(getVoid()))
      const updateTops = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const cleanUpFolderThreadsByTops = jest
        .spyOn(cleanup, 'cleanUpFolderThreadsByTops')
        .mockReturnValue(resolve(getVoid()))
      const rebuildCounters = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))

      messages.attachToThread(mid, newTid, lids).then((_) => {
        expect(messageMetaByMid).toBeCalledWith(mid)
        expect(topMidForTidAndFid).toBeCalledWith(newTid, fid)
        expect(updateMessageTid).toBeCalledWith(mid, newTid)
        expect(updateMessageWithLabels).toBeCalledWith(messageMeta, lids)
        expect(insertThreads).toBeCalledWith([new ThreadInFolder(int64(402), int64(101), int64(303))])
        expect(generateThreadsAfterMoveFromFolder).toBeCalledWith(fid, [mid])
        expect(updateTops).toBeCalledWith([oldTid, newTid])
        expect(cleanUpFolderThreadsByTops).toBeCalled()
        expect(rebuildCounters).toBeCalledWith([mid])
        done()
      })
    })
    it('should update top mids for new thread only if the message was not in thread', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const cleanup = makeCleanup({ storage, threads })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads, cleanup } }),
      )
      const mid = int64(301)
      const newTid = int64(402)
      jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              mid,
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
          ),
        )
      jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(int64(303)))
      jest.spyOn(messages, 'updateMessageTid').mockReturnValue(resolve(getVoid()))
      jest.spyOn(messages, 'updateMessageWithLabels').mockReturnValue(resolve(getVoid()))
      jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      jest.spyOn(threads, 'generateThreadsAfterMoveFromFolder').mockReturnValue(resolve(getVoid()))
      const updateTops = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cleanup, 'cleanUpFolderThreadsByTops').mockReturnValue(resolve(getVoid()))
      jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))

      messages.attachToThread(mid, newTid, ['lid1', 'lid2']).then((_) => {
        expect(updateTops).toBeCalledWith([newTid])
        done()
      })
    })
  })
  describe('updateMessageWithLabels', () => {
    describe('if message read status should be changed', () => {
      it('should update read status, rebuild counters, and update labels', (done) => {
        const storage = MockStorage()
        const cleanup = makeCleanup({ storage })
        const messages = new Messages(
          MockNetwork(),
          storage,
          MockJSONSerializer(),
          testIDSupport,
          makeMessagesSettings({ storage, params: { cleanup } }),
        )
        const message = new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          [],
          false,
          null,
          'Subject',
          'firstline',
          'from@yandex.ru',
          true, // unread
          false,
          null,
          int64(12345),
          false,
          null,
          MessageTypeFlags.people,
        )
        const updateMessageReadSpy = jest.spyOn(messages, 'updateMessageRead').mockReturnValue(resolve(getVoid()))
        const rebuildCountersSpy = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
        const markMessageWithLabelsSpy = jest
          .spyOn(messages as any, 'markMessageWithLabels')
          .mockReturnValue(resolve(getVoid()))
        expect.assertions(3)
        messages.updateMessageWithLabels(message, ['FAKE_SEEN_LBL', 'lid1']).then((_) => {
          expect(updateMessageReadSpy).toBeCalledWith(int64(301), false)
          expect(rebuildCountersSpy).toBeCalledWith([message.mid])
          expect(markMessageWithLabelsSpy).toBeCalledWith(message.mid, ['FAKE_SEEN_LBL', 'lid1'], message.tid)
          done()
        })
      })
    })
    describe('if message read status is preserved', () => {
      it('should only update labels', (done) => {
        const storage = MockStorage()
        const messages = new Messages(
          MockNetwork(),
          storage,
          MockJSONSerializer(),
          testIDSupport,
          makeMessagesSettings({ storage }),
        )
        const updateMessagesReadSpy = jest.spyOn(messages, 'updateMessagesRead').mockReturnValue(resolve(getVoid()))
        const message = new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          [],
          false,
          null,
          'Subject',
          'firstline',
          'from@yandex.ru',
          false, // unread
          false,
          null,
          int64(12345),
          false,
          null,
          MessageTypeFlags.people,
        )
        const markMessageWithLabelsSpy = jest
          .spyOn(messages as any, 'markMessageWithLabels')
          .mockReturnValue(resolve(getVoid()))
        expect.assertions(2)
        messages.updateMessageWithLabels(message, ['FAKE_SEEN_LBL', 'lid1']).then((_) => {
          expect(updateMessagesReadSpy).not.toBeCalled()
          expect(markMessageWithLabelsSpy).toBeCalledWith(message.mid, ['FAKE_SEEN_LBL', 'lid1'], message.tid)
          done()
        })
      })
    })
  })
  describe('messageMetaByMid', () => {
    it('should return message meta if found by mid', (done) => {
      const storage = MockStorage({
        runQuery: jest
          .fn()
          .mockReturnValue(
            resolve(
              MockCursorWithArray([
                [
                  int64(301),
                  int64(101),
                  int64(401),
                  true,
                  'RE:',
                  'SUBJECT1',
                  'firstLine1',
                  'sender',
                  true,
                  true,
                  'lid1',
                  int64(12345),
                  true,
                  MessageTypeFlags.people | MessageTypeFlags.personal,
                  'lid1,lid2,lid3',
                ],
              ]),
            ),
          ),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.messageMetaByMid(int64(301)).then((res) => {
        expect(res).toStrictEqual(
          new MessageMeta(
            int64(301),
            int64(101),
            int64(401),
            ['lid1', 'lid2', 'lid3'],
            true,
            'RE:',
            'SUBJECT1',
            'firstLine1',
            'sender',
            true,
            true,
            'lid1',
            int64(12345),
            true,
            null,
            MessageTypeFlags.people | MessageTypeFlags.personal,
          ),
        )
        expect(storage.runQuery).toBeCalledWith(expect.any(String), [idstr(301)])
        done()
      })
    })
    it('should return null if meta not found by mid', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.messageMetaByMid(int64(301)).then((res) => {
        expect(res).toBeNull()
        expect(storage.runQuery).toBeCalledWith(expect.any(String), [idstr(301)])
        done()
      })
    })
  })
  describe('resetMessagesTimestamp', () => {
    it('should reset timestamps for messages with passed mids', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.resetMessagesTimestamp([int64(301), int64(302)]).then((_) => {
        expect((statement.execute as any).mock.calls[0][0]).toEqual([idstr(301)])
        expect((statement.execute as any).mock.calls[1][0]).toEqual([idstr(302)])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.resetMessagesTimestamp([int64(301), int64(302)]).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_timestamp])
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.resetMessagesTimestamp([int64(301), int64(302)]).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close the statement disregarding the result (negative)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValueOnce(rejected('FAILED')).mockReturnValueOnce(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.resetMessagesTimestamp([int64(301), int64(302)]).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('resetTimestampInFidsExceptMids', () => {
    it('should return early on empty fids', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.resetTimestampInFidsExceptMids([], [int64(201), int64(202)]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should reset timestamps for messages with passed mids', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
        withinTransaction: MockWithinTransaction<any>(),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(3)
      messages.resetTimestampInFidsExceptMids([int64(101), int64(102)], [int64(201), int64(202)]).then((_) => {
        expect(storage.withinTransaction).toBeCalledWith(true, expect.any(Function))
        // tslint:disable-next-line:max-line-length
        expect(storage.runStatement).toBeCalledWith(
          'UPDATE message_timestamp SET timestamp = 0 WHERE mid IN (SELECT mm.mid FROM message_meta AS mm WHERE mm.fid IN (101, 102) AND mm.mid NOT IN (201, 202));',
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_timestamp])
        done()
      })
    })
  })
  describe(Messages.prototype.updateMessageRead, () => {
    it('should set unread flag in message with mid', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessageRead(int64(301), true).then((_) => {
        expect(statement.execute).toBeCalledWith([true, idstr(301)])
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessageRead(int64(301), true).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
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
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.updateMessageRead(int64(301), true).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessageRead(int64(301), true).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe(Messages.prototype.updateMessagesRead, () => {
    it('should return immediatelly if mids array is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessagesRead([], true).then((_) => {
        expect(storage.prepareStatement).not.toBeCalled()
        done()
      })
    })
    it('should set unread flag in messages with mids', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.updateMessagesRead([int64(301), int64(302)], true).then((_) => {
        expect(statement.execute).toBeCalledWith([true])
        expect(storage.prepareStatement).toBeCalledWith(
          `UPDATE ${EntityKind.message_meta} SET unread = ? WHERE mid IN (301, 302);`,
        )
        done()
      })
    })
    it('should notify about changes', (done) => {
      const statement = MockStorageStatement()
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessagesRead([int64(301), int64(302)], true).then((_) => {
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
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
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.updateMessagesRead([int64(301), int64(302)], true).failed((e) => {
        expect(e.message).toBe('FAILED')
        expect(statement.close).toBeCalled()
        done()
      })
    })
    it('should close the statement disregarding the result (positive)', (done) => {
      const statement = MockStorageStatement({
        execute: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const storage = MockStorage({
        prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessagesRead([int64(301), int64(302)], true).then((_) => {
        expect(statement.close).toBeCalled()
        done()
      })
    })
  })
  describe('messageMetaFromCursor', () => {
    it('should map message metas from cursor', () => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect(
        messages.messageMetaFromCursor(
          MockCursorWithArray([
            [
              int64(301),
              int64(101),
              int64(401),
              true,
              'RE:',
              'SUB1',
              'FL1',
              's@ya.ru',
              true,
              true,
              'lid1',
              int64(12345),
              true,
              MessageTypeFlags.people | MessageTypeFlags.tPeople,
              '1,2,3',
            ],
            [
              int64(0),
              int64(0),
              null,
              false,
              '',
              '',
              '',
              '',
              false,
              false,
              null,
              int64(54321),
              false,
              MessageTypeFlags.dating | MessageTypeFlags.sDatingsite,
              null,
            ],
          ]),
        ),
      ).toEqual([
        new MessageMeta(
          int64(301),
          int64(101),
          int64(401),
          ['1', '2', '3'],
          true,
          'RE:',
          'SUB1',
          'FL1',
          's@ya.ru',
          true,
          true,
          'lid1',
          int64(12345),
          true,
          null,
          MessageTypeFlags.people | MessageTypeFlags.tPeople,
        ),
        new MessageMeta(
          int64(0),
          int64(0),
          null,
          [],
          false,
          '',
          '',
          '',
          '',
          false,
          false,
          null,
          int64(54321),
          false,
          null,
          MessageTypeFlags.dating | MessageTypeFlags.sDatingsite,
        ),
      ])
    })
  })
  describe('updateSearchShowFor', () => {
    it('should return immediatelly if mids is empty', (done) => {
      const storage = MockStorage()
      const search = makeSearch({ storage })
      jest.spyOn(search, 'getSearchResultsForFolderInMids').mockReturnValue(resolve([]))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { search } }),
      )
      expect.assertions(1)
      messages.updateSearchShowFor(int64(101), int64(102), []).then((_) => {
        expect(search.getSearchResultsForFolderInMids).not.toBeCalled()
        done()
      })
    })
    it('should set showFor columns to new folder id', (done) => {
      const storage = MockStorage()
      const search = makeSearch({ storage })
      const searchMids = [int64(1), int64(2)]
      jest.spyOn(search, 'getSearchResultsForFolderInMids').mockReturnValue(resolve(searchMids))
      jest.spyOn(search, 'updateMessagesShowFor').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { search } }),
      )
      const [currentFid, targetFid, mids] = [int64(101), int64(102), [int64(301), int64(302)]]
      expect.assertions(2)
      messages.updateSearchShowFor(currentFid, targetFid, mids).then((_) => {
        expect(search.getSearchResultsForFolderInMids).toBeCalledWith(currentFid, mids)
        expect(search.updateMessagesShowFor).toBeCalledWith(search.makeFolderSearchID(targetFid), searchMids)
        done()
      })
    })
  })
  describe(Messages.prototype.updateMessagesFid, () => {
    it('should return immediatelly if mids is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.updateMessagesFid([], int64(101)).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should update fid of messages by mids', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.updateMessagesFid([int64(301), int64(302)], int64(101)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `UPDATE ${EntityKind.message_meta} SET fid = 101 WHERE mid IN (301, 302);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('updateMessageTid', () => {
    it('should update tid of message by mid', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.updateMessageTid(int64(301), int64(401)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(`UPDATE ${EntityKind.message_meta} SET tid = 401 WHERE mid = 301;`)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('fetchMessageMetasByMids', () => {
    it('should return immediatelly if passed mids set is empty', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.fetchMessageMetasByMids([]).then((res) => {
        expect(storage.runQuery).not.toBeCalled()
        expect(res).toHaveLength(0)
        done()
      })
    })
    it('should return messages by mids', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(
          resolve(
            MockCursorWithArray([
              [
                int64(301),
                int64(101),
                int64(401),
                true,
                'RE:',
                'SUB1',
                'FL1',
                's@ya.ru',
                true,
                true,
                'lid1',
                int64(12345),
                true,
                MessageTypeFlags.people | MessageTypeFlags.tPeople,
                '1,2,3',
              ],
              [
                int64(0),
                int64(0),
                null,
                false,
                '',
                '',
                '',
                '',
                false,
                false,
                null,
                int64(54321),
                false,
                MessageTypeFlags.dating | MessageTypeFlags.sDatingsite,
                null,
              ],
            ]),
          ),
        ),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.fetchMessageMetasByMids([int64(301), int64(302)]).then((res) => {
        expect(storage.runQuery).toBeCalledWith(expect.any(String), [])
        expect(res).toEqual([
          new MessageMeta(
            int64(301),
            int64(101),
            int64(401),
            ['1', '2', '3'],
            true,
            'RE:',
            'SUB1',
            'FL1',
            's@ya.ru',
            true,
            true,
            'lid1',
            int64(12345),
            true,
            null,
            MessageTypeFlags.people | MessageTypeFlags.tPeople,
          ),
          new MessageMeta(
            int64(0),
            int64(0),
            null,
            [],
            false,
            '',
            '',
            '',
            '',
            false,
            false,
            null,
            int64(54321),
            false,
            null,
            MessageTypeFlags.dating | MessageTypeFlags.sDatingsite,
          ),
        ])
        done()
      })
    })
  })
  describe(Messages.prototype.restoreThreadsIfNeeded, () => {
    afterEach(jest.restoreAllMocks)
    it('should not restore if folder is non-threaded', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({
        storage,
        sharedPreferences: new MockSharedPreferences(new Map([[AccountSettingsKeys.threadMode.toString(), false]])),
      })
      const fetchFolderByIDSpy = jest.spyOn(folders, 'fetchFolderByID').mockReturnValue(
        resolve(
          new Folder(
            int64(101),
            FolderType.spam, // Non-threaded folder
            'Spam',
            0,
            null,
            0,
            0,
          ),
        ),
      )
      const threads = makeThreads({ storage })
      const fetchMinimalTidSpy = jest.spyOn(threads, 'fetchMinimalTid').mockReturnValue(resolve(int64(301)))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, threads } }),
      )
      messages.restoreThreadsIfNeeded(int64(101), [int64(301)]).then((_) => {
        expect(fetchMinimalTidSpy).not.toBeCalled()
        expect(fetchFolderByIDSpy).toBeCalledWith(int64(101))
        done()
      })
    })
    it('should restore threads for messages (no fake tids in db)', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({
        storage,
        sharedPreferences: new MockSharedPreferences(new Map([[AccountSettingsKeys.threadMode.toString(), true]])),
      })
      const fetchFolderByIDSpy = jest.spyOn(folders, 'fetchFolderByID').mockReturnValue(
        resolve(
          new Folder(
            int64(101),
            FolderType.inbox, // Threaded folder
            'Inbox',
            0,
            null,
            0,
            0,
          ),
        ),
      )
      const threads = makeThreads({ storage })
      const fetchMinimalTidSpy = jest.spyOn(threads, 'fetchMinimalTid').mockReturnValue(resolve(int64(100)))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, threads } }),
      )
      const fetchMessageMetasByMidsSpy = jest
        .spyOn(messages, 'fetchMessageMetasByMids')
        .mockReturnValue(
          resolve([
            new MessageMeta(
              int64(301),
              int64(101),
              int64(401),
              [],
              true,
              'RE:',
              'SUB1',
              'FL1',
              's1@ya.ru',
              true,
              true,
              'lid1',
              int64(12345),
              true,
              null,
              MessageTypeFlags.people | MessageTypeFlags.tPeople,
            ),
            new MessageMeta(
              int64(302),
              int64(101),
              null,
              [],
              true,
              'FW:',
              'SUB2',
              'FL2',
              's2@ya.ru',
              true,
              true,
              'lid2',
              int64(54321),
              true,
              null,
              MessageTypeFlags.social | MessageTypeFlags.sSocial,
            ),
          ]),
        )
      const updateMessageTidSpy = jest.spyOn(messages, 'updateMessageTid').mockReturnValue(resolve(getVoid()))
      const insertThreadsSpy = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      messages.restoreThreadsIfNeeded(int64(101), [int64(301)]).then((_) => {
        expect(fetchMinimalTidSpy).toBeCalled()
        expect(fetchFolderByIDSpy).toBeCalledWith(int64(101))
        expect(fetchMessageMetasByMidsSpy).toBeCalledWith([int64(301)])
        expect(updateMessageTidSpy.mock.calls[0]).toEqual([int64(302), int64(-3)])
        expect(insertThreadsSpy).toBeCalledWith([new ThreadInFolder(int64(-3), int64(101), int64(302))])
        done()
      })
    })
    it('should restore threads for messages (some fake tids in db)', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({
        storage,
        sharedPreferences: new MockSharedPreferences(new Map([[AccountSettingsKeys.threadMode.toString(), true]])),
      })
      const fetchFolderByIDSpy = jest.spyOn(folders, 'fetchFolderByID').mockReturnValue(
        resolve(
          new Folder(
            int64(101),
            FolderType.inbox, // Threaded folder
            'Inbox',
            0,
            null,
            0,
            0,
          ),
        ),
      )
      const threads = makeThreads({ storage })
      const fetchMinimalTidSpy = jest.spyOn(threads, 'fetchMinimalTid').mockReturnValue(resolve(int64(-100)))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders, threads } }),
      )
      const fetchMessageMetasByMidsSpy = jest
        .spyOn(messages, 'fetchMessageMetasByMids')
        .mockReturnValue(
          resolve([
            new MessageMeta(
              int64(301),
              int64(101),
              int64(401),
              [],
              true,
              'RE:',
              'SUB1',
              'FL1',
              's1@ya.ru',
              true,
              true,
              'lid1',
              int64(12345),
              true,
              null,
              MessageTypeFlags.people | MessageTypeFlags.tPeople,
            ),
            new MessageMeta(
              int64(302),
              int64(101),
              null,
              [],
              true,
              'FW:',
              'SUB2',
              'FL2',
              's2@ya.ru',
              true,
              true,
              'lid2',
              int64(54321),
              true,
              null,
              MessageTypeFlags.social | MessageTypeFlags.sSocial,
            ),
          ]),
        )
      const updateMessageTidSpy = jest.spyOn(messages, 'updateMessageTid').mockReturnValue(resolve(getVoid()))
      const insertThreadsSpy = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      messages.restoreThreadsIfNeeded(int64(101), [int64(301)]).then((_) => {
        expect(fetchMinimalTidSpy).toBeCalled()
        expect(fetchFolderByIDSpy).toBeCalledWith(int64(101))
        expect(fetchMessageMetasByMidsSpy).toBeCalledWith([int64(301)])
        expect(updateMessageTidSpy.mock.calls[0]).toEqual([int64(302), int64(-101)])
        expect(insertThreadsSpy).toBeCalledWith([new ThreadInFolder(int64(-101), int64(101), int64(302))])
        done()
      })
    })
  })
  describe('insertThreadIfNeeded', () => {
    afterEach(jest.restoreAllMocks)
    it('should return immediatelly if tid is null', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const topMidForTidAndFid = jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(null))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads } }),
      )
      messages.insertThreadIfNeeded(null, int64(101), int64(301)).then((_) => {
        expect(topMidForTidAndFid).not.toBeCalled()
        done()
      })
    })
    it('should insert new thread if not found in the db', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const topMidForTidAndFid = jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(null))
      const insertThreads = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads } }),
      )
      messages.insertThreadIfNeeded(int64(401), int64(101), int64(301)).then((_) => {
        expect(topMidForTidAndFid).toBeCalledWith(int64(401), int64(101))
        expect(insertThreads).toBeCalledWith([new ThreadInFolder(int64(401), int64(101), int64(301))])
        done()
      })
    })
    it('should not insert new thread if it is already in the db', (done) => {
      const storage = MockStorage()
      const threads = makeThreads({ storage })
      const topMidForTidAndFid = jest.spyOn(threads, 'topMidForTidAndFid').mockReturnValue(resolve(int64(302)))
      const insertThreads = jest.spyOn(threads, 'insertThreads').mockReturnValue(resolve(getVoid()))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads } }),
      )
      messages.insertThreadIfNeeded(int64(401), int64(101), int64(302)).then((_) => {
        expect(topMidForTidAndFid).toBeCalledWith(int64(401), int64(101))
        expect(insertThreads).not.toBeCalled()
        done()
      })
    })
  })
  describe('storeIncoming', () => {
    afterEach(jest.restoreAllMocks)
    // tslint:disable-next-line: max-line-length
    it('should insert thread if needed, store message, attaches, update timestamp, update tops, labels, counters, load body (drafty)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const folders = makeFolders({ storage })
      const labels = makeLabels({ storage })
      const cleanup = makeCleanup({ storage, threads, folders })
      const bodies = makeBodies()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads, folders, labels, bodies, cleanup } }),
      )
      const insertThreadIfNeeded = jest.spyOn(messages, 'insertThreadIfNeeded').mockReturnValue(resolve(getVoid()))
      const insertMessages = jest.spyOn(messages, 'insertMessages').mockReturnValue(resolve(getVoid()))
      const updateMessageAttaches = jest.spyOn(messages, 'updateMessageAttaches').mockReturnValue(resolve(getVoid()))
      const insertCurrentMessageTimestamps = jest
        .spyOn(messages, 'insertCurrentMessageTimestamps')
        .mockReturnValue(resolve(getVoid()))
      const updateTops = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const deleteMessageBodyMetas = jest.spyOn(messages, 'deleteMessageBodyMetas').mockReturnValue(resolve(getVoid()))
      const bs = [
        new MessageBodyPayload(
          new MessageBodyInfo(
            int64(301),
            [],
            [new Recipient('rec@yandex.com', 'Recipient', RecipientType.to)],
            'rfcid',
            'references',
          ),
          [new MessageBodyPart('hid1', 'The Content', 'text', 'en')],
          'contenttype',
          'en',
          false,
          [],
        ),
      ]
      const loadMessageBodies = jest.spyOn(messages, 'loadMessageBodies').mockReturnValue(resolve(bs))
      const insertMessageLabels = jest.spyOn(labels, 'insertMessageLabels').mockReturnValue(resolve(getVoid()))
      const insertFolderMessagesConnectionMessages = jest
        .spyOn(folders, 'insertFolderMessagesConnectionMessages')
        .mockReturnValue(resolve(getVoid()))
      const rebuildCounters = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
      const fetchFidsOfFoldersWithUpgradableBodies = jest
        .spyOn(folders, 'fetchFidsOfFoldersWithUpgradableBodies')
        .mockReturnValue(
          resolve(
            new Set<ID>([int64(101)]),
          ),
        )
      const storeMessageBodies = jest.spyOn(bodies, 'storeMessageBodies').mockReturnValue(resolve(getVoid()))
      const updateBodiesTimestamps = jest.spyOn(bodies, 'updateBodiesTimestamps').mockReturnValue(resolve(getVoid()))
      const updateMessagesWithBodyMeta = jest
        .spyOn(messages, 'updateMessagesWithBodyMeta')
        .mockReturnValue(resolve(getVoid()))
      const deleteMessageDirectory = jest.spyOn(bodies, 'deleteMessageDirectory').mockReturnValue(resolve(getVoid()))
      const e = new DeltaApiEnvelope(
        int64(301),
        int64(101),
        int64(401),
        12345,
        int64(54321),
        int64(54321),
        [new DeltaApiRecipient('from', 'domain.com', 'From')],
        [new DeltaApiRecipient('replyTo', 'domain.com', 'Reply')],
        'Pre Subject',
        new DeltaApiSubject('type', 'Pre', 'Subject', 'Post', false),
        [new DeltaApiRecipient('cc', 'domain.com', 'Cc')],
        [new DeltaApiRecipient('bcc', 'domain.com', 'Bcc')],
        [new DeltaApiRecipient('to', 'domain.com', 'To')],
        null,
        null,
        null,
        'firstLine',
        'inreplyto@domain.com',
        'references',
        'rfcid',
        12345,
        0,
        0,
        1,
        1000,
        [new DeltaApiAttachment('hid1', 'image/png', 'image.png', 1000)],
        ['lid1', 'lid2', 'FAKE_SEEN_LBL'],
        [4, 103],
      )
      const expectedMessages = [deltaApiEnvelopeToMessageMeta(e)]
      messages.storeIncoming(e).then((_) => {
        expect(insertThreadIfNeeded).toBeCalledWith(int64(401), int64(101), int64(301))
        expect(insertMessages).toBeCalledWith(expectedMessages)
        expect(updateMessageAttaches).toBeCalledWith(expectedMessages)
        expect(insertCurrentMessageTimestamps).toBeCalledWith([int64(301)])
        expect(updateTops).toBeCalledWith([int64(401)])
        expect(insertMessageLabels).toBeCalledWith(expectedMessages)
        expect(insertFolderMessagesConnectionMessages).toBeCalledWith(expectedMessages)
        expect(rebuildCounters).toBeCalledWith([int64(301)])
        expect(fetchFidsOfFoldersWithUpgradableBodies).toBeCalled()
        expect(deleteMessageBodyMetas).toBeCalledWith([int64(301)])
        expect(deleteMessageDirectory).toBeCalledWith(int64(301))
        expect(loadMessageBodies).toBeCalledWith([int64(301)])
        expect(storeMessageBodies).toBeCalledWith(bs)
        expect(updateBodiesTimestamps).toBeCalledWith(bs, new Map([[int64(301), int64(54321)]]))
        expect(updateMessagesWithBodyMeta).toBeCalledWith(bs)
        done()
      })
    })
    // tslint:disable-next-line: max-line-length
    it('should insert thread if needed, store message, attaches, update timestamp, update tops, labels, counters, load body (non-drafty)', (done) => {
      const storage = MockStorage({
        withinTransaction: MockWithinTransaction<any>(),
      })
      const threads = makeThreads({ storage })
      const folders = makeFolders({ storage })
      const labels = makeLabels({ storage })
      const cleanup = makeCleanup({ storage, threads, folders })
      const bodies = makeBodies()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { threads, folders, labels, bodies, cleanup } }),
      )
      const insertThreadIfNeeded = jest.spyOn(messages, 'insertThreadIfNeeded').mockReturnValue(resolve(getVoid()))
      const insertMessages = jest.spyOn(messages, 'insertMessages').mockReturnValue(resolve(getVoid()))
      const updateMessageAttaches = jest.spyOn(messages, 'updateMessageAttaches').mockReturnValue(resolve(getVoid()))
      const insertCurrentMessageTimestamps = jest
        .spyOn(messages, 'insertCurrentMessageTimestamps')
        .mockReturnValue(resolve(getVoid()))
      const updateTops = jest.spyOn(threads, 'updateTops').mockReturnValue(resolve(getVoid()))
      const deleteMessageBodyMetas = jest.spyOn(messages, 'deleteMessageBodyMetas').mockReturnValue(resolve(getVoid()))
      const bs = [
        new MessageBodyPayload(
          new MessageBodyInfo(
            int64(301),
            [],
            [new Recipient('rec@yandex.com', 'Recipient', RecipientType.to)],
            'rfcid',
            'references',
          ),
          [new MessageBodyPart('hid1', 'The Content', 'text', 'en')],
          'contenttype',
          'en',
          false,
          [],
        ),
      ]
      const loadMessageBodies = jest.spyOn(messages, 'loadMessageBodies').mockReturnValue(resolve(bs))
      const insertMessageLabels = jest.spyOn(labels, 'insertMessageLabels').mockReturnValue(resolve(getVoid()))
      const insertFolderMessagesConnectionMessages = jest
        .spyOn(folders, 'insertFolderMessagesConnectionMessages')
        .mockReturnValue(resolve(getVoid()))
      const rebuildCounters = jest.spyOn(cleanup, 'rebuildCounters').mockReturnValue(resolve(getVoid()))
      const fetchFidsOfFoldersWithUpgradableBodies = jest
        .spyOn(folders, 'fetchFidsOfFoldersWithUpgradableBodies')
        .mockReturnValue(
          resolve(
            new Set<ID>([int64(103)]),
          ),
        )
      const storeMessageBodies = jest.spyOn(bodies, 'storeMessageBodies').mockReturnValue(resolve(getVoid()))
      const updateBodiesTimestamps = jest.spyOn(bodies, 'updateBodiesTimestamps').mockReturnValue(resolve(getVoid()))
      const updateMessagesWithBodyMeta = jest
        .spyOn(messages, 'updateMessagesWithBodyMeta')
        .mockReturnValue(resolve(getVoid()))
      const deleteMessageDirectory = jest.spyOn(bodies, 'deleteMessageDirectory').mockReturnValue(resolve(getVoid()))
      const e = new DeltaApiEnvelope(
        int64(301),
        int64(101),
        null,
        12345,
        int64(54321),
        int64(54321),
        [new DeltaApiRecipient('from', 'domain.com', 'From')],
        [new DeltaApiRecipient('replyTo', 'domain.com', 'Reply')],
        'Pre Subject',
        new DeltaApiSubject('type', 'Pre', 'Subject', 'Post', false),
        [new DeltaApiRecipient('cc', 'domain.com', 'Cc')],
        [new DeltaApiRecipient('bcc', 'domain.com', 'Bcc')],
        [new DeltaApiRecipient('to', 'domain.com', 'To')],
        null,
        null,
        null,
        'firstLine',
        'inreplyto@domain.com',
        'references',
        'rfcid',
        12345,
        0,
        0,
        1,
        1000,
        [new DeltaApiAttachment('hid1', 'image/png', 'image.png', 1000)],
        ['lid1', 'lid2', 'FAKE_SEEN_LBL'],
        [4, 103],
      )
      const expectedMessages = [deltaApiEnvelopeToMessageMeta(e)]
      messages.storeIncoming(e).then((_) => {
        expect(insertThreadIfNeeded).toBeCalledWith(null, int64(101), int64(301))
        expect(insertMessages).toBeCalledWith(expectedMessages)
        expect(updateMessageAttaches).toBeCalledWith(expectedMessages)
        expect(insertCurrentMessageTimestamps).toBeCalledWith([int64(301)])
        expect(updateTops).toBeCalledWith([])
        expect(insertMessageLabels).toBeCalledWith(expectedMessages)
        expect(insertFolderMessagesConnectionMessages).toBeCalledWith(expectedMessages)
        expect(rebuildCounters).toBeCalledWith([int64(301)])
        expect(fetchFidsOfFoldersWithUpgradableBodies).toBeCalled()
        expect(deleteMessageBodyMetas).not.toBeCalled()
        expect(deleteMessageDirectory).not.toBeCalled()
        expect(loadMessageBodies).toBeCalledWith([int64(301)])
        expect(storeMessageBodies).toBeCalledWith(bs)
        expect(updateBodiesTimestamps).toBeCalledWith(bs, new Map([[int64(301), int64(54321)]]))
        expect(updateMessagesWithBodyMeta).toBeCalledWith(bs)
        done()
      })
    })
  })
  describe(Messages.prototype.deleteOldDrafts, () => {
    afterEach(jest.restoreAllMocks)
    it('should return immediatelly if mids is empty', (done) => {
      const bodies = makeBodies()
      const deleteOldDraftBodiesSpy = jest.spyOn(bodies, 'deleteOldDraftBodies')
      const messages = new Messages(
        MockNetwork(),
        MockStorage(),
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ params: { bodies } }),
      )
      messages.deleteOldDrafts([]).then((_) => {
        expect(deleteOldDraftBodiesSpy).not.toBeCalled()
        done()
      })
    })
    it('should delete old draft bodies and then their metas', (done) => {
      const storage = MockStorage()
      const bodies = makeBodies()
      const deleteOldDraftBodies = jest.spyOn(bodies, 'deleteOldDraftBodies').mockReturnValue(resolve([int64(301)]))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { bodies } }),
      )
      const deleteMessageBodyMetas = jest.spyOn(messages, 'deleteMessageBodyMetas').mockReturnValue(resolve(getVoid()))
      messages
        .deleteOldDrafts([
          new MessageMeta(
            int64(302),
            int64(101),
            null,
            [],
            true,
            'FW:',
            'SUB2',
            'FL2',
            's2@ya.ru',
            true,
            true,
            'lid2',
            int64(54321),
            true,
            null,
            MessageTypeFlags.social | MessageTypeFlags.sSocial,
          ),
        ])
        .then((_) => {
          expect(deleteOldDraftBodies).toBeCalledWith(new Map([[int64(302), int64(54321)]]))
          expect(deleteMessageBodyMetas).toBeCalledWith([int64(301)])
          done()
        })
    })
  })
  describe('moveMessageToTab', () => {
    it('should run only for tabs of supported name/type', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({ storage })
      const fetchFirstFidByType = jest.spyOn(folders, 'fetchFirstFidByType')
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders } }),
      )
      messages.moveMessageToTab(int64(301), 'unknown').then((_) => {
        expect(fetchFirstFidByType).not.toBeCalled()
        done()
      })
    })
    it('should only move messages if the target Tab is found', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({ storage })
      const fetchFoldersByType = jest.spyOn(folders, 'fetchFoldersByType').mockReturnValue(resolve([]))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders } }),
      )
      const messageMetaByMid = jest.spyOn(messages, 'messageMetaByMid')
      messages.moveMessageToTab(int64(301), 'news').then((_) => {
        expect(fetchFoldersByType).toBeCalledWith(FolderType.tab_news)
        expect(messageMetaByMid).not.toBeCalled()
        done()
      })
    })
    it('should not move message if the message is not found', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({ storage })
      const fetchFoldersByType = jest
        .spyOn(folders, 'fetchFoldersByType')
        .mockReturnValue(resolve([new Folder(int64(-100), FolderType.tab_news, 'news', 0, null, 10, 100)]))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders } }),
      )
      const messageMetaByMid = jest.spyOn(messages, 'messageMetaByMid').mockReturnValue(resolve(null))
      const moveMessage = jest.spyOn(messages, 'moveMessage')
      messages.moveMessageToTab(int64(301), 'news').then((_) => {
        expect(fetchFoldersByType).toBeCalledWith(FolderType.tab_news)
        expect(messageMetaByMid).toBeCalledWith(int64(301))
        expect(moveMessage).not.toBeCalled()
        done()
      })
    })
    it('should move messages if the target Tab and the message are found', (done) => {
      const storage = MockStorage()
      const folders = makeFolders({ storage })
      const fetchFoldersByType = jest
        .spyOn(folders, 'fetchFoldersByType')
        .mockReturnValue(resolve([new Folder(int64(-100), FolderType.tab_news, 'news', 0, null, 10, 100)]))
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, params: { folders } }),
      )
      const messageMetaByMid = jest
        .spyOn(messages, 'messageMetaByMid')
        .mockReturnValue(
          resolve(
            new MessageMeta(
              int64(301),
              int64(1),
              int64(401),
              ['lid1'],
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
              MessageTypeFlags.people,
            ),
          ),
        )
      const moveMessage = jest.spyOn(messages, 'moveMessage').mockReturnValue(resolve(getVoid()))
      const transactioned = false
      messages.moveMessageToTab(int64(301), 'news', transactioned).then((_) => {
        expect(fetchFoldersByType).toBeCalledWith(FolderType.tab_news)
        expect(messageMetaByMid).toBeCalledWith(int64(301))
        expect(moveMessage).toBeCalledWith(int64(-100), int64(401), int64(301), ['lid1'], transactioned)
        done()
      })
    })
  })
  describe('deleteMessagesByFidAndMids', () => {
    it('should return immediatelly if passed mids is empty', (done) => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(1)
      messages.deleteMessagesByFidAndMids(int64(1), []).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        done()
      })
    })
    it('should run deletion script and notify when done', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.deleteMessagesByFidAndMids(int64(1), [int64(2), int64(3)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.message_meta} WHERE fid = 1 AND mid IN (2, 3);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('deleteMessagesByFid', () => {
    it('should run deletion script and notify when done', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect.assertions(2)
      messages.deleteMessagesByFid(int64(13)).then((_) => {
        expect(storage.runStatement).toBeCalledWith(`DELETE FROM ${EntityKind.message_meta} WHERE fid = 13;`)
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
        done()
      })
    })
  })
  describe('getAllMids', () => {
    it('should return all mids from messages', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([[int64(54345), int64(898898)]]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.getAllMids().then((res) => {
        expect(storage.runQuery).toBeCalledWith(`SELECT mid FROM ${EntityKind.message_meta};`, [])
        done()
      })
    })
  })
  describe('scheduleResetFreshRequest', () => {
    it('should schedule "reset_fresh" request', () => {
      const storage = MockStorage()
      const network = MockNetwork()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network }),
      )
      messages.scheduleResetFreshRequest()
      expect(network.execute).toBeCalledWith(new ResetFreshRequest())
    })
  })
  describe('updateBody', () => {
    it('should call storeMessageBodyContent of MessageBodyStore when updateBody', (done) => {
      const storage = MockStorage()
      const bodies = makeBodies()
      bodies.storeMessageBodyContent = jest.fn().mockReturnValue(resolve(getVoid()))
      const network = MockNetwork()
      const messages = new Messages(
        network,
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage, network, params: { bodies } }),
      )
      messages.updateBody(int64(123), 'some new content').then((_) => {
        expect(bodies.storeMessageBodyContent).toBeCalledWith(int64(123), 'some new content')
        done()
      })
    })
  })
  describe('fetchMessagesBodyMetasByMids', () => {
    it('should return immediately if mids array is empty', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(resolve(MockCursorWithArray([]))),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.fetchMessagesBodyMetasByMids([]).then((_) => {
        expect(storage.runQuery).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
    it('should fetch messages bodies and map to MessageBodyMeta', (done) => {
      const storage = MockStorage({
        runQuery: jest.fn().mockReturnValue(
          resolve(
            MockCursorWithArray([
              [int64(123), 'some recipients', 'someRfcId', 'someRef', 'text/plain', 'en'],
              [int64(456), 'some recipients2', 'someRfcId2', 'someRef2', 'text/html', 'ru'],
            ]),
          ),
        ),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.fetchMessagesBodyMetasByMids([int64(1234), int64(5678)]).then((messageBodiesMetas) => {
        expect(storage.runQuery).toBeCalledWith('SELECT * FROM message_body_meta WHERE mid IN (1234, 5678);', [])
        expect(messageBodiesMetas.length).toBe(2)
        expect(messageBodiesMetas).toStrictEqual([
          new MessageBodyMeta(int64(123), 'some recipients', 'someRfcId', 'someRef', 'text/plain', 'en'),
          new MessageBodyMeta(int64(456), 'some recipients2', 'someRfcId2', 'someRef2', 'text/html', 'ru'),
        ])
        done()
      })
    })
  })
  describe('deleteMessageBodyMetasByMids', () => {
    it('should delete message bodies metas for specified mids', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.deleteMessageBodyMetasByMids([int64(123), int64(456)]).then((_) => {
        expect(storage.runStatement).toBeCalledWith(
          `DELETE FROM ${EntityKind.message_body_meta} WHERE mid IN (123, 456);`,
        )
        expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_body_meta])
        done()
      })
    })
    it('should return immediately if input messages array is empty', (done) => {
      const storage = MockStorage({
        runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
        notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
      })
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      messages.deleteMessageBodyMetasByMids([]).then((_) => {
        expect(storage.runStatement).not.toBeCalled()
        expect(storage.notifyAboutChanges).not.toBeCalled()
        done()
      })
    })
  })
})

describe(MessagesSettings, () => {
  it('should provide a builder', () => {
    expect(MessagesSettings.builder()).toBeInstanceOf(MessagesSettingsBuilder)
  })
})
describe(MessagesSettingsBuilder, () => {
  it('should be creatable', () => {
    const builder = MessagesSettingsBuilder.create()
    expect(builder).toBeInstanceOf(MessagesSettingsBuilder)
  })
  it('should build MessageSettings if all properties are set', () => {
    const builder = MessagesSettingsBuilder.create()
    const storage = MockStorage()

    const attachmentsManager = makeAttachmentsManager({ storage })
    const folders = makeFolders({ storage })
    const threads = makeThreads({ storage })
    const labels = makeLabels({ storage })
    const search = makeSearch({ storage })
    const cleanup = makeCleanup({ storage, threads, folders })
    const bodies = makeBodies()

    const result = builder
      .setAttachmentsManager(attachmentsManager)
      .setCleanup(cleanup)
      .setFolders(folders)
      .setLabels(labels)
      .setSearch(search)
      .setThreads(threads)
      .setBodies(bodies)
      .build()
    expect(result).toStrictEqual(
      makeMessagesSettings({
        storage,
        params: { attachmentsManager, folders, threads, labels, search, cleanup, bodies },
      }),
    )
  })
  it('should throw if AttachmentManager is not provided', () => {
    const builder = MessagesSettingsBuilder.create()
    expect(() => builder.build()).toThrowError('Attachments Manager needs to be set to create MessagesSettings')
  })
  it('should throw if Folders model is not provided', () => {
    const builder = MessagesSettingsBuilder.create().setAttachmentsManager(makeAttachmentsManager())
    expect(() => builder.build()).toThrowError('Folders Model needs to be set to create MessagesSettings')
  })
  it('should throw if Labels model is not provided', () => {
    const storage = MockStorage()
    const builder = MessagesSettingsBuilder.create()
      .setAttachmentsManager(makeAttachmentsManager({ storage }))
      .setFolders(makeFolders({ storage }))
    expect(() => builder.build()).toThrowError('Labels Model needs to be set to create MessagesSettings')
  })
  it('should throw if Threads model is not provided', () => {
    const storage = MockStorage()
    const builder = MessagesSettingsBuilder.create()
      .setAttachmentsManager(makeAttachmentsManager({ storage }))
      .setFolders(makeFolders({ storage }))
      .setLabels(makeLabels({ storage }))
    expect(() => builder.build()).toThrowError('Threads Model needs to be set to create MessagesSettings')
  })
  it('should throw if Search model is not provided', () => {
    const storage = MockStorage()
    const builder = MessagesSettingsBuilder.create()
      .setAttachmentsManager(makeAttachmentsManager({ storage }))
      .setFolders(makeFolders({ storage }))
      .setLabels(makeLabels({ storage }))
      .setThreads(makeThreads({ storage }))
    expect(() => builder.build()).toThrowError('Search Model needs to be set to create MessagesSettings')
  })
  it('should throw if Cleanup model is not provided', () => {
    const storage = MockStorage()
    const builder = MessagesSettingsBuilder.create()
      .setAttachmentsManager(makeAttachmentsManager({ storage }))
      .setFolders(makeFolders({ storage }))
      .setLabels(makeLabels({ storage }))
      .setThreads(makeThreads({ storage }))
      .setSearch(makeSearch({ storage }))
    expect(() => builder.build()).toThrowError('Cleanup Model needs to be set to create MessagesSettings')
  })
  it('should throw if Bodies Store is not provided', () => {
    const storage = MockStorage()
    const threads = makeThreads({ storage })
    const folders = makeFolders({ storage })
    const builder = MessagesSettingsBuilder.create()
      .setAttachmentsManager(makeAttachmentsManager({ storage }))
      .setFolders(folders)
      .setLabels(makeLabels({ storage }))
      .setThreads(threads)
      .setSearch(makeSearch({ storage }))
      .setCleanup(makeCleanup({ storage, threads, folders }))
    expect(() => builder.build()).toThrowError('Body Store needs to be set to create MessagesSettings')
  })
  it('should not throw if all the models are provided', () => {
    const storage = MockStorage()
    const threads = makeThreads({ storage })
    const folders = makeFolders({ storage })
    const builder = MessagesSettingsBuilder.create()
      .setAttachmentsManager(makeAttachmentsManager({ storage }))
      .setFolders(folders)
      .setLabels(makeLabels({ storage }))
      .setThreads(threads)
      .setSearch(makeSearch({ storage }))
      .setCleanup(makeCleanup({ storage, threads, folders }))
      .setBodies(makeBodies())
    expect(() => builder.build()).not.toThrow()
  })

  it('should update message body in db with new mid', (done) => {
    const storage = MockStorage({
      runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    messages.updateMessageBodyMid(int64(111), int64(222)).then((_) => {
      expect(storage.runStatement).toBeCalledWith('UPDATE message_body_meta SET mid = 111 WHERE mid = 222;')
      expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_body_meta])
      done()
    })
  })
  describe('messageBodyMetaFromCursor', () => {
    it('should map cursor to message body meta', () => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect(
        messages.messageBodyMetaFromCursor(
          MockCursorWithArray([
            [
              int64(301),
              'me@yandex.ru, you@yandex.ru, him@yandex.ru, they@gmail.com',
              'someRfcId',
              'someReference',
              null,
              'ru',
            ],
            [int64(402), 'nobody@yandex.ru', 'someRfcId2', 'someReference2', 'text/plain', 'en'],
          ]),
        ),
      ).toEqual([
        new MessageBodyMeta(
          int64(301),
          'me@yandex.ru, you@yandex.ru, him@yandex.ru, they@gmail.com',
          'someRfcId',
          'someReference',
          'text/html',
          'ru',
        ),
        new MessageBodyMeta(int64(402), 'nobody@yandex.ru', 'someRfcId2', 'someReference2', 'text/plain', 'en'),
      ])
    })
    it('should map cursor to message body meta with some nulled fields', () => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect(
        messages.messageBodyMetaFromCursor(MockCursorWithArray([[int64(301), null, null, null, null, null]])),
      ).toEqual([new MessageBodyMeta(int64(301), null, null, null, 'text/html', null)])
    })
  })
  it('should insert message bodies metas to DB', (done) => {
    const statement = MockStorageStatement()
    const storage = MockStorage({
      prepareStatement: jest.fn().mockReturnValue(resolve(statement)),
      notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    // expect.assertions(1)
    messages.insertBodyMetaFlat(int64(301), '{}', 'someRfcId', 'someRefs', 'text/html', 'ru').then((_) => {
      expect(storage.prepareStatement).toBeCalledWith(
        'INSERT OR REPLACE INTO message_body_meta (mid, recipients, rfc_id, reference, contentType, lang) VALUES (?, ?, ?, ?, ?, ?);',
      )
      expect(statement.execute).toBeCalledWith([idstr(301), '{}', 'someRfcId', 'someRefs', 'text/html', 'ru'])
      expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_body_meta])
      done()
    })
  })
  it('should update mid from old to new', (done) => {
    const storage = MockStorage({
      runStatement: jest.fn().mockReturnValue(resolve(getVoid())),
      notifyAboutChanges: jest.fn().mockReturnValue(resolve(getVoid())),
    })
    const messages = new Messages(
      MockNetwork(),
      storage,
      MockJSONSerializer(),
      testIDSupport,
      makeMessagesSettings({ storage }),
    )
    messages.updateMid(int64(111), int64(222)).then((_) => {
      expect(storage.runStatement).toBeCalledWith('UPDATE message_meta SET mid = 111 WHERE mid = 222;')
      expect(storage.notifyAboutChanges).toBeCalledWith([EntityKind.message_meta])
      done()
    })
  })
  describe('isRealMid', () => {
    it('should return true for non-negative mid value', () => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect(messages.isRealMid(int64(12345))).toBe(true)
      expect(messages.isRealMid(int64(0))).toBe(true)
    })
    it('should return false for negative mid value', () => {
      const storage = MockStorage()
      const messages = new Messages(
        MockNetwork(),
        storage,
        MockJSONSerializer(),
        testIDSupport,
        makeMessagesSettings({ storage }),
      )
      expect(messages.isRealMid(int64(-12345))).toBe(false)
    })
  })
})
