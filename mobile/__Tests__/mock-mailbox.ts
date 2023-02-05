import * as assert from 'assert'
import { Int32, int32ToString, int64, int64ToInt32 } from '../../../common/ys'
import { ConsoleLog } from '../../common/__tests__/__helpers__/console-log'
import { DefaultJSONSerializer } from '../../common/__tests__/__helpers__/default-json'
import { Contact } from '../../mapi/code/api/entities/contact/contact'
import { SignaturePlace } from '../../mapi/code/api/entities/settings/settings-entities'
import { AccountType2, MBTPlatform } from '../../testopithecus-common/code/mbt/test/mbt-test'
import { AppModelProvider } from '../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OAuthUserAccount, UserAccount } from '../../testopithecus-common/code/users/user-pool'
import { keysArray, requireNonNull } from '../../testopithecus-common/code/utils/utils'
import { assertBooleanEquals } from '../code/../../testopithecus-common/code/utils/assert'
import { MailboxClient } from '../code/client/mailbox-client'
import {
  ComposeEmailProvider,
  ComposeFieldType,
  ComposeRecipientFieldType,
  Draft,
  Yabble,
  YabbleType,
} from '../code/mail/feature/compose/compose-features'
import { FolderName, LabelName } from '../code/mail/feature/folder-list-features'
import { ShtorkaBannerType, TabBarItem } from '../code/mail/feature/tab-bar-feature'
import { LanguageName } from '../code/mail/feature/translator-features'
import { DefaultFolderName } from '../code/mail/model/folder-data-model'
import {
  AccountMailboxData,
  AccountSettingsModel,
  FullMessage,
  MailAppModelHandler,
  MailboxModel,
  Message,
  MessageId,
} from '../code/mail/model/mail-model'
import { MessageActionItem } from '../code/mail/model/messages-list/context-menu-model'
import { MessageListDatabase, MessageListDatabaseFilter } from '../code/mail/model/supplementary/message-list-database'
import { TranslatorLanguageName } from '../code/mail/model/translator-models'
import { createSyncNetwork } from './test-utils'

export class MockMailboxProvider implements AppModelProvider {
  public readonly model: MailboxModel

  private constructor(accountDataHandler: MailAppModelHandler) {
    accountDataHandler.logInToAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'))
    this.model = new MailboxModel(accountDataHandler)
  }

  public static emptyFoldersOneAccount(): MockMailboxProvider {
    const inbox = DefaultFolderName.inbox
    const trash = DefaultFolderName.trash
    const draft = DefaultFolderName.draft
    const spam = DefaultFolderName.spam
    const sent = DefaultFolderName.sent
    const subscriptions = DefaultFolderName.mailingLists
    const socialMedia = DefaultFolderName.socialNetworks

    const folderToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>([
      [inbox, new Set<MessageId>()],
      [trash, new Set<MessageId>()],
      [sent, new Set<MessageId>()],
      [draft, new Set<MessageId>()],
      [spam, new Set<MessageId>()],
    ])

    const tabsToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>([
      [inbox, new Set<MessageId>()],
      [subscriptions, new Set<MessageId>()],
      [socialMedia, new Set<MessageId>()],
    ])

    const labelToMessages: Map<LabelName, Set<MessageId>> = new Map<LabelName, Set<MessageId>>()

    const accountData = new AccountMailboxData(
      new MailboxClient(
        MBTPlatform.MobileAPI,
        new OAuthUserAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'), null, AccountType2.Yandex),
        createSyncNetwork(),
        new DefaultJSONSerializer(),
        ConsoleLog.LOGGER,
      ),
      new MessageListDatabase(new Map<MessageId, FullMessage>(), folderToMessages, labelToMessages, tabsToMessages, []),
      'mock-mailbox@yandex.ru',
      ['mock-mailbox@yandex.ru'],
      [],
      [],
      new AccountSettingsModel(true, false, '--\nSent from Yandex Mail for mobile', SignaturePlace.afterReply, [inbox]),
      [],
      [],
      true,
    )
    const accountDataHandler = new MailAppModelHandler([accountData])
    accountDataHandler.logInToAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'))
    return new MockMailboxProvider(accountDataHandler)
  }

  public static exampleOneAccount(): MockMailboxProvider {
    ConsoleLog.setup()
    const messages = new Map<MessageId, FullMessage>()
    messages.set(
      int64(6),
      new FullMessage(
        new Message('from4@yandex.ru', 'subject5', int64(5), 'Как дела?', 4),
        new Set<string>(),
        'Как дела?',
        TranslatorLanguageName.russian,
        new Map<LanguageName, string>().set(TranslatorLanguageName.english, 'How are you doing?'),
        true,
        ['Хорошо.', 'Плохо.', 'Нормально.'],
      ),
    )
    messages.set(int64(5), new FullMessage(new Message('from4@yandex.ru', 'subject4', int64(4), 'firstLine4', 4, true)))
    messages.set(int64(3), new FullMessage(new Message('from3@yandex.ru', 'subject3', int64(2), 'firstLine3', 4, true)))
    messages.set(int64(1), new FullMessage(new Message('from1@yandex.ru', 'subject1', int64(0), 'firstLine1', 4)))
    messages.set(
      int64(4),
      new FullMessage(
        new Message('from3@yandex.ru', 'subject3', int64(3), 'Message body', null, true),
        new Set<string>(),
        'Message body',
        TranslatorLanguageName.english,
        new Map<LanguageName, string>().set(TranslatorLanguageName.russian, 'Тело письма'),
        true,
        [],
      ),
    )
    messages.set(int64(2), new FullMessage(new Message('from2@yandex.ru', 'subject2', int64(1), 'firstLine2', null)))
    messages.set(int64(8), new FullMessage(new Message('from7@yandex.ru', 'subject6', int64(7), 'firstLine6', 2)))
    messages.set(int64(7), new FullMessage(new Message('from7@yandex.ru', 'subject6', int64(6), 'firstLine6', 2)))
    messages.set(int64(10), new FullMessage(new Message('from8@yandex.ru', 'subject7', int64(9), 'firstLine7', 2)))
    messages.set(int64(9), new FullMessage(new Message('from8@yandex.ru', 'subject7', int64(8), 'firstLine7', 2)))
    messages.set(int64(11), new FullMessage(new Message('from11@yandex.ru', 'subject5', int64(8), 'firstLine5', 2)))

    const preloadedSearch = new Map<MessageId, FullMessage>()
    for (const mid of messages.keys()) {
      preloadedSearch.set(mid, messages.get(mid)!.copy())
    }
    const inbox = DefaultFolderName.inbox
    const trash = DefaultFolderName.trash
    const draft = DefaultFolderName.draft
    const spam = DefaultFolderName.spam
    const sent = DefaultFolderName.sent
    const subscriptions = DefaultFolderName.mailingLists
    const socialMedia = DefaultFolderName.socialNetworks

    const sentSet: Set<MessageId> = new Set<MessageId>([int64(7), int64(8), int64(11)])
    const inboxSet: Set<MessageId> = new Set<MessageId>([int64(1), int64(2), int64(3), int64(4), int64(5), int64(6)])
    const trashSet: Set<MessageId> = new Set<MessageId>([int64(9), int64(10)])
    const threads = [
      new Set([int64(1), int64(3), int64(5), int64(6)]),
      new Set([int64(7), int64(8)]),
      new Set([int64(9), int64(10)]),
    ]
    const folderToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>([
      [inbox, inboxSet],
      [trash, trashSet],
      [sent, sentSet],
      [draft, new Set<MessageId>()],
      [spam, new Set<MessageId>()],
    ])

    const labelToMessages: Map<LabelName, Set<MessageId>> = new Map<LabelName, Set<MessageId>>()

    const tabsToMessages: Map<FolderName, Set<MessageId>> = new Map<FolderName, Set<MessageId>>([
      [inbox, new Set<MessageId>()],
      [subscriptions, new Set<MessageId>()],
      [socialMedia, new Set<MessageId>()],
    ])

    const contacts = [
      new Contact('testtest', 'testtest@yandex.ru'),
      new Contact('checktest', 'checktest@yandex.ru'),
      new Contact('maintest', 'maintest@yandex.ru'),
      new Contact('имя отличается от email', 'different-name@yandex.ru'),
    ]
    const accountSettings = new AccountSettingsModel(
      true,
      false,
      '--\nSent from Yandex Mail for mobile',
      SignaturePlace.afterReply,
      [inbox],
    )
    const accountData = new AccountMailboxData(
      new MailboxClient(
        MBTPlatform.MobileAPI,
        new OAuthUserAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'), null, AccountType2.Yandex),
        createSyncNetwork(),
        new DefaultJSONSerializer(),
        ConsoleLog.LOGGER,
      ),
      new MessageListDatabase(messages, folderToMessages, labelToMessages, tabsToMessages, threads),
      'mock-mailbox@yandex.ru',
      ['mock-mailbox@yandex.ru', 'mock-mailbox@yandex.com', 'mock-mailbox@ya.ru'],
      contacts,
      [],
      accountSettings,
      ['Quick', 'Links'],
      [
        TranslatorLanguageName.afrikaans,
        TranslatorLanguageName.albanian,
        TranslatorLanguageName.amharic,
        TranslatorLanguageName.arabic,
        TranslatorLanguageName.english,
        TranslatorLanguageName.russian,
      ],
      false,
    )
    const accountDataHandler = new MailAppModelHandler([accountData])
    accountDataHandler.logInToAccount(new UserAccount('mock-mailbox@yandex.ru', '123456'))
    return new MockMailboxProvider(accountDataHandler)
  }

  public async takeAppModel(): Promise<MailboxModel> {
    return this.model
  }
}

describe('Model unit tests', () => {
  ConsoleLog.setup()
  describe('Folders', () => {
    it('should create folders in model', (done) => {
      const firstFolderName = 'New Folder'
      const secondFolderName = 'Even newer folder'
      const model = MockMailboxProvider.exampleOneAccount().model
      const foldersInitialSize = model.folderNavigator.getFoldersList().size
      model.manageFolders.enterNameForNewFolder(firstFolderName)
      model.manageFolders.submitNewFolder()
      assert.strictEqual(model.folderNavigator.getFoldersList().size, foldersInitialSize + 1)
      model.manageFolders.enterNameForNewFolder(secondFolderName)
      model.manageFolders.submitNewFolder()
      assert.strictEqual(model.folderNavigator.getFoldersList().size, foldersInitialSize + 2)
      assertBooleanEquals(
        true,
        keysArray(model.folderNavigator.getFoldersList()).filter((f) => f === firstFolderName).length === 1,
        'no first added folder',
      )
      assertBooleanEquals(
        true,
        keysArray(model.folderNavigator.getFoldersList()).filter((f) => f === secondFolderName).length === 1,
        'no second added folder',
      )
      done()
    })
    it('should move message to another folder', (done) => {
      const folderToCreate = 'another folder'
      const model = MockMailboxProvider.exampleOneAccount().model
      model.manageFolders.enterNameForNewFolder(folderToCreate)
      model.manageFolders.submitNewFolder()
      const msgId = model.messageListDisplay.getThreadByOrder(0)[0]!
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.openMoveToFolderScreen()
      model.moveToFolderModel.tapOnFolder(folderToCreate)
      assert.strictEqual(
        model.mailAppModelHandler
          .getCurrentAccount()
          .messagesDB.getMessageIdList(new MessageListDatabaseFilter().withFolder(folderToCreate))
          .includes(msgId),
        true,
        'message was not moved',
      )
      done()
    })
  })
  describe('Expandable threads', () => {
    it('model should mark as unread only one message in thread', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const threadMsg0 = model.readOnlyExpandableThreads.getThreadMessage(0, 0)
      const threadMsg1 = model.readOnlyExpandableThreads.getThreadMessage(0, 1)
      const threadMsg2 = model.readOnlyExpandableThreads.getThreadMessage(0, 2)
      const threadMsg3 = model.readOnlyExpandableThreads.getThreadMessage(0, 3)
      model.expandableThreads.markThreadMessageAsUnRead(0, 1)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
      assert.strictEqual(threadMsg0.head.read, false)
      assert.strictEqual(threadMsg1.head.read, false)
      assert.strictEqual(threadMsg2.head.read, true)
      assert.strictEqual(threadMsg3.head.read, false)
      done()
    })
    it('model should mark as read only one message in thread', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const threadMsg0 = model.readOnlyExpandableThreads.getThreadMessage(0, 0)
      const threadMsg1 = model.readOnlyExpandableThreads.getThreadMessage(0, 1)
      const threadMsg2 = model.readOnlyExpandableThreads.getThreadMessage(0, 2)
      const threadMsg3 = model.readOnlyExpandableThreads.getThreadMessage(0, 3)
      model.expandableThreads.markThreadMessageAsRead(0, 0)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
      assert.strictEqual(threadMsg0.head.read, true)
      assert.strictEqual(threadMsg1.head.read, true)
      assert.strictEqual(threadMsg2.head.read, true)
      assert.strictEqual(threadMsg3.head.read, false)
      done()
    })
    it('model should mark thread as simple message correct', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.markableRead.markAsRead(0)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, true)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.read, true)
      }
      model.markableRead.markAsUnread(0)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.read, false)
      }
      done()
    })
    it('model should label thread as simple message important correct', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.markableImportant.markAsImportant(0)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, true)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.important, true)
      }
      model.markableImportant.markAsUnimportant(0)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, false)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.important, false)
      }
      done()
    })
    it('model should label as important only one message in thread', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const threadMsg0 = model.readOnlyExpandableThreads.getThreadMessage(0, 0)
      const threadMsg1 = model.readOnlyExpandableThreads.getThreadMessage(0, 1)
      const threadMsg2 = model.readOnlyExpandableThreads.getThreadMessage(0, 2)
      const threadMsg3 = model.readOnlyExpandableThreads.getThreadMessage(0, 3)
      model.expandableThreads.markThreadMessageAsImportant(0, 0)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, true)
      assert.strictEqual(threadMsg0.head.important, true)
      assert.strictEqual(threadMsg1.head.important, false)
      assert.strictEqual(threadMsg2.head.important, false)
      assert.strictEqual(threadMsg3.head.important, false)
      done()
    })
  })
  describe('Simple actions', () => {
    it('model should mark simple message correct', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.markableRead.markAsRead(2)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].read, true)
      model.markableRead.markAsUnread(2)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].read, false)
      done()
    })
    it('model should label simple message as important correct', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.markableImportant.markAsImportant(2)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].important, true)
      model.markableImportant.markAsUnimportant(2)
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].important, false)
      done()
    })
    it('should send to spam folder if move to spam and than mark as not spam', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.spammable.moveToSpam(2)
      model.folderNavigator.goToFolder(DefaultFolderName.spam, [])
      const messagesInSpam = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInSpam.length, 1)
      assert.strictEqual(
        Message.matches(
          new Message('from2@yandex.ru', 'subject2', int64(4), 'firstLine2', null, true),
          messagesInSpam[0],
        ),
        true,
      )
      model.spammable.moveFromSpam(0)
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      assert.strictEqual(
        Message.matches(
          new Message('from2@yandex.ru', 'subject2', int64(1), 'firstLine2', null, true),
          model.messageListDisplay.getMessageList(10)[2],
        ),
        true,
      )
      done()
    })
    it('should send to archive folder if move to archive', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.archiveMessage.archiveMessage(0)
      model.folderNavigator.goToFolder(DefaultFolderName.archive, [])
      const messagesInArchive = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInArchive.length, 4)
      assert.strictEqual(
        Message.matches(
          new Message('from4@yandex.ru', 'subject5', int64(5), 'Как дела?', null, false),
          messagesInArchive[0],
        ),
        true,
      )
      done()
    })
    it('should send to trash if delete by short swipe', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.shortSwipe.deleteMessageByShortSwipe(0)
      model.folderNavigator.goToFolder(DefaultFolderName.trash, [])
      const messagesInTrash = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInTrash.length, 6)
      assert.strictEqual(
        Message.matches(
          new Message('from4@yandex.ru', 'subject5', int64(5), 'Как дела?', null, false),
          messagesInTrash[2],
        ),
        true,
      )
      done()
    })
  })
  describe('Message list view', () => {
    it('should see messages from different folders', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInbox = model.messageListDisplay.getMessageList(100)
      assert.strictEqual(messagesInbox.length, 3)
      model.folderNavigator.goToFolder(DefaultFolderName.sent, [])
      const sentMessages = model.messageListDisplay.getMessageList(100)
      assert.strictEqual(sentMessages.length, 2)
      model.folderNavigator.goToFolder(DefaultFolderName.trash, [])
      const trashMessages = model.messageListDisplay.getMessageList(100)
      assert.strictEqual(trashMessages.length, 2)
      done()
    })
    it('model should list display messages correct', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messages = model.messageListDisplay.getMessageList(6)
      assert.strictEqual(messages[0].subject, 'subject5')
      assert.strictEqual(messages[1].subject, 'subject3')
      assert.strictEqual(messages[2].subject, 'subject2')
      assert.strictEqual(messages.length, 3)
      done()
    })
  })
  describe('Mail view active operations', () => {
    it('should move to label list view', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const labelsText = ['test_label_1', 'test_label_2']
      model.messageNavigator.openMessage(0)
      model.contextMenu.openFromMessageView()
      model.contextMenu.openApplyLabelsScreen()
      model.applyLabelModel.selectLabelsToAdd(labelsText)
      model.applyLabelModel.tapOnDoneButton()
      model.messageNavigator.openMessage(1)
      model.contextMenu.openFromMessageView()
      model.contextMenu.openApplyLabelsScreen()
      model.applyLabelModel.selectLabelsToAdd(labelsText)
      model.applyLabelModel.tapOnDoneButton()
      model.folderNavigator.goToLabel(labelsText[0])
      assert.strictEqual(model.messageListDisplay.getMessageList(10).length, 2)
      model.folderNavigator.goToLabel(labelsText[1])
      assert.strictEqual(model.messageListDisplay.getMessageList(10).length, 2)
      done()
    })
    it('should move to filter list view', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.folderNavigator.goToFilterUnread()
      assert.strictEqual(model.messageListDisplay.getMessageList(10).length, 6)
      model.folderNavigator.goToFolder('Inbox', [])
      model.messageNavigator.openMessage(1)
      model.contextMenu.openFromMessageView()
      model.contextMenu.markAsImportant()
      model.folderNavigator.goToFilterImportant()
      assert.strictEqual(model.messageListDisplay.getMessageList(10).length, 1)
      done()
    })
    it('should attach and detach label', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const labelsText = ['test_label_1', 'test_label_2']
      model.messageNavigator.openMessage(0)
      model.contextMenu.openFromMessageView()
      model.contextMenu.openApplyLabelsScreen()
      model.applyLabelModel.selectLabelsToAdd(labelsText)
      model.applyLabelModel.tapOnDoneButton()
      assert.strictEqual(model.messageNavigator.getLabels().has('test_label_1'), true)
      model.folderNavigator.goToLabel('test_label_1')
      assert.strictEqual(model.messageListDisplay.getMessageList(10).length, 1)
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.openApplyLabelsScreen()
      model.applyLabelModel.deselectLabelsToRemove(labelsText)
      model.applyLabelModel.tapOnDoneButton()
      assert.strictEqual(model.messageNavigator.getLabels().has('test_label_1'), false)
      done()
    })
  })
  describe('Group by subject', () => {
    it('should change group mode', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.accountSettingsModel.openAccountSettings(
        requireNonNull(
          model.mailAppModelHandler.accountsManager.currentAccount,
          'Необходимо зайти в настройки аккаунта',
        ),
      )
      assert.strictEqual(model.messageListDisplay.isInThreadMode(), true)
      model.accountSettingsModel.switchGroupBySubject()
      assert.strictEqual(model.messageListDisplay.isInThreadMode(), false)
      done()
    })
    it('should group messages', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.messageListDisplay.getMessageId(1), int64(4))
      model.accountSettingsModel.openAccountSettings(
        requireNonNull(
          model.mailAppModelHandler.accountsManager.currentAccount,
          'Необходимо зайти в настройки аккаунта',
        ),
      )
      model.accountSettingsModel.switchGroupBySubject()
      assert.strictEqual(model.messageListDisplay.getMessageId(1), int64(5))
      done()
    })
  })
  describe('Search', () => {
    it('should mark as read in search', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.search.openSearch()
      model.search.searchAllMessages()
      model.markableRead.markAsRead(1)
      const messagesInInbox = model.messageListDisplay.getMessageList(6)
      assert.strictEqual(messagesInInbox[1].read, true)
      assert.strictEqual(messagesInInbox[2].read, false)
      done()
    })
    it('should mark as read in search and check in message list', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesDB = model.mailAppModelHandler.getCurrentAccount().messagesDB
      let threadInInbox = model.messageListDisplay.getThreadByOrder(0)
      assert.strictEqual(messagesDB.storedMessage(threadInInbox[0]).head.read, false)
      assert.strictEqual(messagesDB.storedMessage(threadInInbox[3]).head.read, false)
      model.search.openSearch()
      model.search.searchAllMessages()
      model.markableRead.markAsRead(3)
      model.search.closeSearch()
      threadInInbox = model.messageListDisplay.getThreadByOrder(0)
      assert.strictEqual(messagesDB.storedMessage(threadInInbox[0]).head.read, true)
      assert.strictEqual(messagesDB.storedMessage(threadInInbox[3]).head.read, false)
      done()
    })
    it('should search by request', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.search.openSearch()
      model.search.searchByQuery('subject6')
      assert.strictEqual(int64ToInt32(model.messageListDisplay.getMessageId(0)), 8)
      assert.strictEqual(int64ToInt32(model.messageListDisplay.getMessageId(1)), 7)
      assert.strictEqual(model.messageListDisplay.getMessageList(20).length, 2)
      done()
    })
  })
  describe('Undo', () => {
    it('should undo after deleting message by short swipe', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInInbox = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInbox.length, 3)
      model.shortSwipe.deleteMessageByShortSwipe(0)
      const messagesInInboxAfterDeleteFirstMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterDeleteFirstMessage.length, 2)
      model.shortSwipe.deleteMessageByShortSwipe(0)
      const messagesInInboxAfterDeleteSecondMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterDeleteSecondMessage.length, 1)
      model.undo.undoDelete()
      const messagesInInboxAfterUndoDeletingSecondMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterUndoDeletingSecondMessage.length, 2)
      assert.strictEqual(Message.matches(messagesInInbox[1], messagesInInboxAfterUndoDeletingSecondMessage[0]), true)
      assert.strictEqual(Message.matches(messagesInInbox[2], messagesInInboxAfterUndoDeletingSecondMessage[1]), true)
      done()
    })
    it('should undo only last action', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInInbox = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInbox.length, 3)
      model.shortSwipe.deleteMessageByShortSwipe(0)
      const messagesInInboxAfterDeleteFirstMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterDeleteFirstMessage.length, 2)
      model.shortSwipe.archiveMessageByShortSwipe(0)
      const messagesInInboxAfterDeleteSecondMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterDeleteSecondMessage.length, 1)
      model.undo.undoArchive()
      const messagesInInboxAfterUndoArchivingSecondMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterUndoArchivingSecondMessage.length, 2)
      assert.strictEqual(Message.matches(messagesInInbox[1], messagesInInboxAfterUndoArchivingSecondMessage[0]), true)
      assert.strictEqual(Message.matches(messagesInInbox[2], messagesInInboxAfterUndoArchivingSecondMessage[1]), true)
      done()
    })
    it('should undo after archiving message by short swipe', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInInbox = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInbox.length, 3)
      model.shortSwipe.archiveMessageByShortSwipe(0)
      const messagesInInboxAfterArchiveFirstMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterArchiveFirstMessage.length, 2)
      model.shortSwipe.archiveMessageByShortSwipe(0)
      const messagesInInboxAfterArchiveSecondMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterArchiveSecondMessage.length, 1)
      model.undo.undoArchive()
      const messagesInInboxAfterUndoArchivingSecondMessage = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterUndoArchivingSecondMessage.length, 2)
      assert.strictEqual(Message.matches(messagesInInbox[1], messagesInInboxAfterUndoArchivingSecondMessage[0]), true)
      assert.strictEqual(Message.matches(messagesInInbox[2], messagesInInboxAfterUndoArchivingSecondMessage[1]), true)
      done()
    })
    it('should undo after deleting messages by group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInInbox = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInbox.length, 3)
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.delete()
      const messagesInInboxAfterDeleteTwoMessages = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterDeleteTwoMessages.length, 1)
      model.undo.undoDelete()
      const messagesInInboxAfterUndoDeletingTwoMessages = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterUndoDeletingTwoMessages.length, 3)
      assert.strictEqual(Message.matches(messagesInInbox[0], messagesInInboxAfterUndoDeletingTwoMessages[0]), true)
      assert.strictEqual(Message.matches(messagesInInbox[1], messagesInInboxAfterUndoDeletingTwoMessages[1]), true)
      assert.strictEqual(Message.matches(messagesInInbox[2], messagesInInboxAfterUndoDeletingTwoMessages[2]), true)
      done()
    })
    it('should undo after archive messages by group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInInbox = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInbox.length, 3)
      model.groupMode.initialMessageSelect(2)
      model.groupMode.selectMessage(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.archive()
      const messagesInInboxAfterDeleteTwoMessages = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterDeleteTwoMessages.length, 1)
      model.undo.undoArchive()
      const messagesInInboxAfterUndoArchivingTwoMessages = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInInboxAfterUndoArchivingTwoMessages.length, 3)
      assert.strictEqual(Message.matches(messagesInInbox[0], messagesInInboxAfterUndoArchivingTwoMessages[0]), true)
      assert.strictEqual(Message.matches(messagesInInbox[1], messagesInInboxAfterUndoArchivingTwoMessages[1]), true)
      assert.strictEqual(Message.matches(messagesInInbox[2], messagesInInboxAfterUndoArchivingTwoMessages[2]), true)
      done()
    })
  })
  describe('Group mode', () => {
    it('should get selected messages count', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.groupMode.initialMessageSelect(0)
      let numberOfSelectedMessages = model.groupMode.getNumberOfSelectedMessages()
      assert.strictEqual(numberOfSelectedMessages, 4)
      model.groupMode.selectMessage(1)
      numberOfSelectedMessages = model.groupMode.getNumberOfSelectedMessages()
      assert.strictEqual(numberOfSelectedMessages, 5)
      model.groupMode.unselectAllMessages()
      numberOfSelectedMessages = model.groupMode.getNumberOfSelectedMessages()
      assert.strictEqual(numberOfSelectedMessages, 0)
      done()
    })
    it('should get selected messages list', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.groupMode.initialMessageSelect(0)
      let selectedMessages = model.groupMode.getSelectedMessages()
      assert.strictEqual(selectedMessages.size, 1)
      assert.deepEqual(selectedMessages, new Set([0]))
      model.groupMode.selectMessage(2)
      selectedMessages = model.groupMode.getSelectedMessages()
      assert.strictEqual(selectedMessages.size, 2)
      assert.deepEqual(selectedMessages, new Set([0, 2]))
      model.groupMode.unselectMessage(0)
      selectedMessages = model.groupMode.getSelectedMessages()
      assert.strictEqual(selectedMessages.size, 1)
      assert.deepEqual(selectedMessages, new Set([2]))
      done()
    })
    it('should mark as read and unread from group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      let messages = model.messageListDisplay.getMessageList(2)
      assert.strictEqual(messages[0].read, false)
      assert.strictEqual(messages[1].read, true)
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.markAsRead()
      messages = model.messageListDisplay.getMessageList(2)
      assert.strictEqual(messages[0].read, true)
      assert.strictEqual(messages[1].read, true)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      model.groupMode.selectMessage(2)
      model.groupMode.markAsUnread()
      messages = model.messageListDisplay.getMessageList(2)
      assert.strictEqual(messages[0].read, false)
      assert.strictEqual(messages[1].read, false)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      done()
    })
    it('should deselect some messages after select', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      model.groupMode.selectMessage(2)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 3)
      model.groupMode.unselectMessage(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.unselectMessage(0)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 1)
      assert.deepStrictEqual(
        model.groupMode.getSelectedMessages(),
        new Set<Int32>([2]),
      )
      done()
    })
    it('should deselect all messages after select', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      model.groupMode.selectMessage(2)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 3)
      model.groupMode.unselectAllMessages()
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      done()
    })
    it('should archive via group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.archive()
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      model.folderNavigator.goToFolder(DefaultFolderName.archive, [])
      const messagesInArchive = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInArchive.length, 5)
      done()
    })
    it('should mark message as important and unimportant via group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.groupMode.initialMessageSelect(1)
      model.groupMode.selectMessage(2)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.markAsImportant()
      let messages = model.messageListDisplay.getMessageList(5)
      assert.strictEqual(messages[1].important, true)
      assert.strictEqual(messages[2].important, true)
      model.groupMode.selectMessage(2)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 1)
      model.groupMode.markAsUnimportant()
      messages = model.messageListDisplay.getMessageList(5)
      assert.strictEqual(messages[2].important, false)
      done()
    })
    it('should move to folder via group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const newFolderName = 'autotest folder'
      const messagesInInbox = model.messageListDisplay.getMessageList(10)
      const firstMessageInInbox = messagesInInbox[0]
      const thirdMessageInInbox = messagesInInbox[2]
      model.manageFolders.enterNameForNewFolder(newFolderName)
      model.manageFolders.submitNewFolder()
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(2)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 2)
      model.groupMode.openMoveToFolderScreen()
      model.moveToFolderModel.tapOnFolder(newFolderName)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      model.folderNavigator.goToFolder(newFolderName, [])
      const messagesInNewFolder = model.messageListDisplay.getMessageList(10)
      const firstMessageInNewFolder = messagesInNewFolder[0]
      const secondMessageInNewFolder = messagesInNewFolder[1]
      assert.strictEqual(messagesInNewFolder.length, 2)
      assert.strictEqual(Message.matches(firstMessageInInbox, firstMessageInNewFolder), true)
      assert.strictEqual(Message.matches(thirdMessageInInbox, secondMessageInNewFolder), true)
      done()
    })
    it('should mark as spam via group operations and then mark as not spam', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      let messagesInInbox = model.messageListDisplay.getMessageList(10)
      let secondMessageInInbox = messagesInInbox[1]
      model.groupMode.initialMessageSelect(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 1)
      model.groupMode.markAsSpam()
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      model.folderNavigator.goToFolder(DefaultFolderName.spam, [])
      let messagesInSpam = model.messageListDisplay.getMessageList(10)
      const firstMessageInSpam = messagesInSpam[0]
      assert.strictEqual(Message.matches(secondMessageInInbox, firstMessageInSpam), true)
      assert.strictEqual(messagesInSpam.length, 1)
      model.groupMode.initialMessageSelect(0)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 1)
      model.groupMode.markAsNotSpam()
      messagesInSpam = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      assert.strictEqual(messagesInSpam.length, 0)
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      messagesInInbox = model.messageListDisplay.getMessageList(10)
      secondMessageInInbox = messagesInInbox[1]
      assert.strictEqual(Message.matches(secondMessageInInbox, firstMessageInSpam), true)
      done()
    })
    it('should apply label by group operations', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const labelNames = ['label1']
      model.groupMode.initialMessageSelect(0)
      model.groupMode.selectMessage(1)
      model.groupMode.openApplyLabelsScreen()
      model.applyLabelModel.selectLabelsToAdd(labelNames)
      model.applyLabelModel.tapOnDoneButton()
      model.folderNavigator.goToLabel(labelNames[0])
      assert.strictEqual(model.messageListDisplay.getMessageList(10).length, 5)
      done()
    })
    it('should select and unselect all loaded messages in folder', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const messagesInInbox = model.messageListDisplay.getMessageList(20)
      model.groupMode.initialMessageSelect(1)
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 1)
      model.groupMode.selectAllMessages()
      assert.strictEqual(model.groupMode.getSelectedMessages().size, messagesInInbox.length)
      model.groupMode.unselectAllMessages()
      assert.strictEqual(model.groupMode.getSelectedMessages().size, 0)
      done()
    })
  })
  describe('Short swipe menu', () => {
    it('should send to trash if delete by short swipe menu', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.deleteMessage()
      model.folderNavigator.goToFolder(DefaultFolderName.trash, [])
      const messagesInTrash = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInTrash.length, 6)
      assert.strictEqual(
        Message.matches(
          new Message('from4@yandex.ru', 'subject5', int64(5), 'Как дела?', null, false),
          messagesInTrash[2],
        ),
        true,
      )
      done()
    })
    it('should mark and unmark important single message by short swipe menu', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.contextMenu.openFromShortSwipe(2)
      model.contextMenu.markAsImportant()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].important, true)
      model.contextMenu.openFromShortSwipe(2)
      model.contextMenu.markAsUnimportant()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].important, false)
      done()
    })
    it('should mark and unmark important thread by short swipe menu', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.markAsImportant()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, true)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.important, true)
      }
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.markAsUnimportant()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].important, false)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.important, false)
      }
      done()
    })
    it('model should mark read and unread simple message by short swipe menu', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.contextMenu.openFromShortSwipe(2)
      model.contextMenu.markAsRead()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].read, true)
      model.contextMenu.openFromShortSwipe(2)
      model.contextMenu.markAsUnread()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[2].read, false)
      done()
    })
    it('model should mark read and unread thread by short swipe menu', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.markAsRead()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, true)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.read, true)
      }
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.markAsUnread()
      assert.strictEqual(model.messageListDisplay.getMessageList(6)[0].read, false)
      for (let i = 0; i < 4; i++) {
        assert.strictEqual(model.readOnlyExpandableThreads.getThreadMessage(0, i).head.read, false)
      }
      done()
    })
    it('should move to folder by short swipe menu', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const newFolderName = 'autotest folder'
      model.manageFolders.enterNameForNewFolder(newFolderName)
      model.manageFolders.submitNewFolder()
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.openMoveToFolderScreen()
      model.moveToFolderModel.tapOnFolder(newFolderName)
      model.folderNavigator.goToFolder(newFolderName, [])
      const messagesInNewFolder = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInNewFolder.length, 1)
      assert.strictEqual(
        Message.matches(
          new Message('from4@yandex.ru', 'subject5', int64(5), 'Как дела?', 4, false),
          messagesInNewFolder[0],
        ),
        true,
      )
      done()
    })
  })
  describe('Advanced search', () => {
    it('should find message with label advanced search', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const newLabelName = 'search label'
      model.createLabel.createLabel(newLabelName)
      model.contextMenu.openFromShortSwipe(1)
      model.contextMenu.openApplyLabelsScreen()
      model.applyLabelModel.selectLabelsToAdd([newLabelName])
      model.applyLabelModel.tapOnDoneButton()
      model.contextMenu.openFromShortSwipe(2)
      model.contextMenu.openApplyLabelsScreen()
      model.applyLabelModel.selectLabelsToAdd([newLabelName])
      model.applyLabelModel.tapOnDoneButton()
      model.search.openSearch()
      model.search.searchByQuery('subject3')
      model.advancedSearch.addLabelToSearch(newLabelName)
      assert.strictEqual(model.messageListDisplay.getMessageList(20).length, 1)
      done()
    })
    it('should find message folder label advanced search', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.search.openSearch()
      model.search.searchByQuery('subject5')
      assert.strictEqual(model.messageListDisplay.getMessageList(20).length, 2)
      model.advancedSearch.addFolderToSearch(DefaultFolderName.sent)
      assert.strictEqual(model.messageListDisplay.getMessageList(20).length, 1)
      done()
    })
  })
  describe('Tabs', () => {
    it('should change tabs mode', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.accountSettingsModel.openAccountSettings(model.mailAppModelHandler.accountsManager.currentAccount!)
      assert.strictEqual(model.folderNavigator.isInTabsMode(), false)
      model.accountSettingsModel.switchSortingEmailsByCategory()
      assert.strictEqual(model.folderNavigator.isInTabsMode(), true)
      done()
    })
    it('should display notification Social Media', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.accountSettingsModel.openAccountSettings(model.mailAppModelHandler.accountsManager.currentAccount!)
      model.accountSettingsModel.switchSortingEmailsByCategory()
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      assert.strictEqual(model.tabs.isDisplayNotificationTabs(DefaultFolderName.socialNetworks), false)
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.openMoveToFolderScreen()
      model.moveToFolderModel.tapOnFolder(DefaultFolderName.socialNetworks)
      assert.strictEqual(model.tabs.isDisplayNotificationTabs(DefaultFolderName.socialNetworks), true)
      done()
    })
    it('should not display notification Subscription Media', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.accountSettingsModel.openAccountSettings(model.mailAppModelHandler.accountsManager.currentAccount!)
      model.accountSettingsModel.switchSortingEmailsByCategory()
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      assert.strictEqual(model.tabs.isDisplayNotificationTabs(DefaultFolderName.mailingLists), false)
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.openMoveToFolderScreen()
      model.moveToFolderModel.tapOnFolder(DefaultFolderName.socialNetworks)
      assert.strictEqual(model.tabs.isDisplayNotificationTabs(DefaultFolderName.mailingLists), false)
      done()
    })
    it('should display notification Social Media on second position', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.accountSettingsModel.openAccountSettings(model.mailAppModelHandler.accountsManager.currentAccount!)
      model.accountSettingsModel.switchSortingEmailsByCategory()
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      model.contextMenu.openFromShortSwipe(1)
      model.contextMenu.openMoveToFolderScreen()
      model.moveToFolderModel.tapOnFolder(DefaultFolderName.socialNetworks)
      assert.strictEqual(model.tabs.getPositionTabsNotification(DefaultFolderName.socialNetworks), 1)
      done()
    })
  })
  describe('Translator', () => {
    it('should not show translate bar if message language is equal to system language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      assert.strictEqual(model.messageNavigator.getOpenedMessage().body, 'Message body')
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), false)
      done()
    })

    it('should force show translate bar', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      assert.strictEqual(model.messageNavigator.getOpenedMessage().body, 'Message body')
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), false)
      model.contextMenu.openFromMessageView()
      assert.strictEqual(model.contextMenu.getAvailableActions().includes(MessageActionItem.showTranslator), true)
      model.contextMenu.showTranslator()
      assert.strictEqual(model.contextMenu.getAvailableActions().includes(MessageActionItem.showTranslator), false)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      done()
    })

    it('should translate message', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.messageNavigator.getOpenedMessage().body, 'Как дела?')
      assert.strictEqual(model.messageNavigator.getOpenedMessage().lang, TranslatorLanguageName.russian)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.auto)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.isMessageTranslated(), false)
      model.translatorBarModel.tapOnTranslateButton()
      assert.strictEqual(model.messageNavigator.getOpenedMessage().body, 'How are you doing?')
      assert.strictEqual(model.messageNavigator.getOpenedMessage().lang, TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.auto)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.isMessageTranslated(), true)
      done()
    })

    it('should revert translation', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTranslateButton()
      assert.strictEqual(model.messageNavigator.getOpenedMessage().body, 'How are you doing?')
      assert.strictEqual(model.messageNavigator.getOpenedMessage().lang, TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.auto)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.isMessageTranslated(), true)
      model.translatorBarModel.tapOnRevertButton()
      assert.strictEqual(model.messageNavigator.getOpenedMessage().body, 'Как дела?')
      assert.strictEqual(model.messageNavigator.getOpenedMessage().lang, TranslatorLanguageName.russian)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.auto)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.isMessageTranslated(), false)
      done()
    })

    it('should close translate bar (auto)', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.auto)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      model.translatorBarModel.tapOnCloseBarButton(true)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), false)
      model.messageNavigator.closeMessage()
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorSettingsModel.getIgnoredTranslationLanguages().length, 0)
      done()
    })

    it('should close translate bar', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.auto)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      model.translatorBarModel.tapOnSourceLanguage()
      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.russian)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.getSourceLanguage(), TranslatorLanguageName.russian)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      model.translatorBarModel.tapOnCloseBarButton(true)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), false)
      model.messageNavigator.closeMessage()
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), false)
      assert.strictEqual(model.translatorSettingsModel.getIgnoredTranslationLanguages().length, 1)
      assert.strictEqual(
        model.translatorSettingsModel.getIgnoredTranslationLanguages()[0],
        TranslatorLanguageName.russian,
      )
      done()
    })

    it('should get all source language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getAllSourceLanguages().length, 6)
      for (const language of model.translatorLanguageListModel.getAllSourceLanguages()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
            TranslatorLanguageName.english,
            TranslatorLanguageName.russian,
          ].includes(language),
          true,
        )
      }
      done()
    })

    it('should get current source language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), null)
      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.afrikaans)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), TranslatorLanguageName.afrikaans)
      done()
    })

    it('should get determined automatically source language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.afrikaans)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      done()
    })

    it('should get recent source languages', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages().length, 0)
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), null)
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.afrikaans)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), TranslatorLanguageName.afrikaans)
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages().length, 0)

      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.albanian)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), TranslatorLanguageName.albanian)
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages().length, 1)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[0],
        TranslatorLanguageName.afrikaans,
      )

      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.arabic)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), TranslatorLanguageName.arabic)
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages().length, 2)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[0],
        TranslatorLanguageName.albanian,
      )
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[1],
        TranslatorLanguageName.afrikaans,
      )

      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.amharic)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), TranslatorLanguageName.amharic)
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages().length, 3)
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages()[0], TranslatorLanguageName.arabic)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[1],
        TranslatorLanguageName.albanian,
      )
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[2],
        TranslatorLanguageName.afrikaans,
      )

      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.english)
      model.translatorBarModel.tapOnSourceLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentSourceLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(
        model.translatorLanguageListModel.getDeterminedAutomaticallySourceLanguage(),
        TranslatorLanguageName.russian,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages().length, 3)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[0],
        TranslatorLanguageName.amharic,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentSourceLanguages()[1], TranslatorLanguageName.arabic)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentSourceLanguages()[2],
        TranslatorLanguageName.albanian,
      )
      done()
    })

    it('should get all target language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getAllTargetLanguages().length, 6)
      for (const language of model.translatorLanguageListModel.getAllTargetLanguages()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
            TranslatorLanguageName.english,
            TranslatorLanguageName.russian,
          ].includes(language),
          true,
        )
      }
      done()
    })

    it('should get current target language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), null)
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.afrikaans, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), TranslatorLanguageName.afrikaans)
      done()
    })

    it('should get default target language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.afrikaans, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      done()
    })

    it('should get recent target languages', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages().length, 0)
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), null)
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.afrikaans, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), TranslatorLanguageName.afrikaans)
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages().length, 0)
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.albanian, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), TranslatorLanguageName.albanian)
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages().length, 1)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[0],
        TranslatorLanguageName.afrikaans,
      )
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.arabic, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), TranslatorLanguageName.arabic)
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages().length, 2)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[0],
        TranslatorLanguageName.albanian,
      )
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[1],
        TranslatorLanguageName.afrikaans,
      )
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.amharic, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), TranslatorLanguageName.amharic)
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages().length, 3)
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages()[0], TranslatorLanguageName.arabic)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[1],
        TranslatorLanguageName.albanian,
      )
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[2],
        TranslatorLanguageName.afrikaans,
      )
      model.translatorLanguageListModel.setTargetLanguage(TranslatorLanguageName.russian, true)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListModel.getCurrentTargetLanguage(), TranslatorLanguageName.russian)
      assert.strictEqual(model.translatorLanguageListModel.getDefaultTargetLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages().length, 3)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[0],
        TranslatorLanguageName.amharic,
      )
      assert.strictEqual(model.translatorLanguageListModel.getRecentTargetLanguages()[1], TranslatorLanguageName.arabic)
      assert.strictEqual(
        model.translatorLanguageListModel.getRecentTargetLanguages()[2],
        TranslatorLanguageName.albanian,
      )
      done()
    })

    it('should focus search bar', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTargetLanguage()
      assert.strictEqual(model.translatorLanguageListSearchModel.isSearchTextFieldFocused(), false)
      model.translatorLanguageListSearchModel.tapOnSearchTextField()
      assert.strictEqual(model.translatorLanguageListSearchModel.isSearchTextFieldFocused(), true)
      model.translatorLanguageListSearchModel.tapOnCancelButton()
      assert.strictEqual(model.translatorLanguageListSearchModel.isSearchTextFieldFocused(), false)
      done()
    })

    it('should search languages', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      model.translatorLanguageListSearchModel.tapOnSearchTextField()
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 6)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
            TranslatorLanguageName.english,
            TranslatorLanguageName.russian,
          ].includes(language),
          true,
        )
      }
      model.translatorLanguageListSearchModel.enterSearchQuery('A')
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 4)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
          ].includes(language),
          true,
        )
      }
      model.translatorLanguageListSearchModel.enterSearchQuery('Ar')
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 1)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(language, TranslatorLanguageName.arabic)
      }
      done()
    })

    it('should clear search field', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      model.translatorLanguageListSearchModel.tapOnSearchTextField()
      model.translatorLanguageListSearchModel.enterSearchQuery('A')
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 4)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
          ].includes(language),
          true,
        )
      }
      model.translatorLanguageListSearchModel.tapOnClearSearchFieldButton()
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 6)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
            TranslatorLanguageName.english,
            TranslatorLanguageName.russian,
          ].includes(language),
          true,
        )
      }
      done()
    })
    it('should clear search field after cancel search', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnTargetLanguage()
      model.translatorLanguageListSearchModel.tapOnSearchTextField()
      model.translatorLanguageListSearchModel.enterSearchQuery('A')
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 4)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
          ].includes(language),
          true,
        )
      }
      model.translatorLanguageListSearchModel.tapOnCancelButton()
      assert.strictEqual(model.translatorLanguageListSearchModel.getSearchedLanguageList().length, 6)
      for (const language of model.translatorLanguageListSearchModel.getSearchedLanguageList()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
            TranslatorLanguageName.english,
            TranslatorLanguageName.russian,
          ].includes(language),
          true,
        )
      }
      assert.strictEqual(model.translatorLanguageListModel.getAllSourceLanguages().length, 6)
      for (const language of model.translatorLanguageListModel.getAllSourceLanguages()) {
        assert.strictEqual(
          [
            TranslatorLanguageName.afrikaans,
            TranslatorLanguageName.albanian,
            TranslatorLanguageName.amharic,
            TranslatorLanguageName.arabic,
            TranslatorLanguageName.english,
            TranslatorLanguageName.russian,
          ].includes(language),
          true,
        )
      }
      done()
    })

    it('should ignored language cell shown', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.translatorSettingsModel.isTranslatorEnabled(), true)
      assert.strictEqual(model.translatorSettingsModel.isIgnoredLanguageCellShown(), false)
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.russian)
      model.translatorBarModel.tapOnCloseBarButton(true)
      assert.strictEqual(model.translatorSettingsModel.isTranslatorEnabled(), true)
      assert.strictEqual(model.translatorSettingsModel.isIgnoredLanguageCellShown(), true)
      assert.strictEqual(model.translatorSettingsModel.getIgnoredTranslationLanguages().length, 1)
      assert.strictEqual(
        model.translatorSettingsModel.getIgnoredTranslationLanguages()[0],
        TranslatorLanguageName.russian,
      )
      done()
    })

    it('should delete language from ignored language list', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.translatorBarModel.tapOnSourceLanguage()
      model.translatorLanguageListModel.setSourceLanguage(TranslatorLanguageName.russian)
      model.translatorBarModel.tapOnCloseBarButton(true)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), false)
      model.messageNavigator.closeMessage()
      assert.strictEqual(model.translatorSettingsModel.isTranslatorEnabled(), true)
      assert.strictEqual(model.translatorSettingsModel.isIgnoredLanguageCellShown(), true)
      assert.strictEqual(model.translatorSettingsModel.getIgnoredTranslationLanguages().length, 1)
      assert.strictEqual(
        model.translatorSettingsModel.getIgnoredTranslationLanguages()[0],
        TranslatorLanguageName.russian,
      )
      model.translatorSettingsModel.removeTranslationLanguageFromIgnored(TranslatorLanguageName.russian)
      assert.strictEqual(model.translatorSettingsModel.isTranslatorEnabled(), true)
      assert.strictEqual(model.translatorSettingsModel.isIgnoredLanguageCellShown(), false)
      assert.strictEqual(model.translatorSettingsModel.getIgnoredTranslationLanguages().length, 0)
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      done()
    })

    it('should get and set default translation language', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.english)
      model.messageNavigator.closeMessage()
      assert.strictEqual(model.translatorSettingsModel.getDefaultTranslationLanguage(), TranslatorLanguageName.english)
      assert.strictEqual(
        model.translatorSettingsModel.getDefaultTranslationLanguageFromGeneralSettingsPage(),
        TranslatorLanguageName.english,
      )
      model.translatorSettingsModel.setDefaultTranslationLanguage(TranslatorLanguageName.arabic)
      assert.strictEqual(model.translatorSettingsModel.getDefaultTranslationLanguage(), TranslatorLanguageName.arabic)
      assert.strictEqual(
        model.translatorSettingsModel.getDefaultTranslationLanguageFromGeneralSettingsPage(),
        TranslatorLanguageName.arabic,
      )
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.translatorBarModel.isTranslatorBarShown(), true)
      assert.strictEqual(model.translatorBarModel.getTargetLanguage(), TranslatorLanguageName.arabic)
      model.messageNavigator.closeMessage()
      done()
    })
  })
  describe('Quick and smart reply', () => {
    it('should show quick reply and not show smart reply if quickReply true and smartReplies = []', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      done()
    })

    it('should show quick reply and smart replies if quickReply true and smartReplies exists', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Хорошо.', 'Плохо.', 'Нормально.'])
      done()
    })

    it('should not show quick reply and not show smart reply if quickReply false and smartReplies = []', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(2)
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      done()
    })

    it('should close smart reply by order', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.smartReplyModel.closeSmartReply(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Плохо.', 'Нормально.'])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      done()
    })

    it('should close all smart replies', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.smartReplyModel.closeAllSmartReplies()
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      done()
    })

    it('should close all smart replies one by one ', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Хорошо.', 'Плохо.', 'Нормально.'])
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      model.smartReplyModel.closeSmartReply(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Плохо.', 'Нормально.'])
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      model.smartReplyModel.closeSmartReply(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Нормально.'])
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      model.smartReplyModel.closeSmartReply(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      done()
    })

    it('should set smart reply to textField', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Хорошо.', 'Плохо.', 'Нормально.'])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      model.smartReplyModel.tapOnSmartReply(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), 'Хорошо.')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), true)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      done()
    })

    it('should set smart reply to textField and then clear textField', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.smartReplyModel.tapOnSmartReply(0)
      model.quickReplyModel.tapOnTextField()
      model.quickReplyModel.setTextFieldValue('')
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Хорошо.', 'Плохо.', 'Нормально.'])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      done()
    })

    it('should show all smart replies after re-opening message (close by order)', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.smartReplyModel.closeSmartReply(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Плохо.', 'Нормально.'])
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      model.messageNavigator.closeMessage()
      model.messageNavigator.openMessage(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Хорошо.', 'Плохо.', 'Нормально.'])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      done()
    })

    it('should show all smart replies after re-opening message (close all)', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(0)
      model.smartReplyModel.closeAllSmartReplies()
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      model.messageNavigator.closeMessage()
      model.messageNavigator.openMessage(0)
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), ['Хорошо.', 'Плохо.', 'Нормально.'])
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), true)
      done()
    })

    it('should paste to quick reply textField', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      model.quickReplyModel.tapOnTextField()
      model.quickReplyModel.pasteTextFieldValue('Ответ')
      assert.deepStrictEqual(model.smartReplyModel.getSmartReplies(), [])
      assert.strictEqual(model.quickReplyModel.isQuickReplyTextFieldExpanded(), false)
      assert.strictEqual(model.quickReplyModel.isQuickReplyShown(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), 'Ответ')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), true)
      assert.strictEqual(model.smartReplyModel.isSmartRepliesShown(), false)
      done()
    })

    it('should expand textField', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      model.quickReplyModel.tapOnTextField()
      model.quickReplyModel.pasteTextFieldValue('Ответ\n\n')
      assert.strictEqual(model.quickReplyModel.isQuickReplyTextFieldExpanded(), true)
      done()
    })

    it('should open compose (empty textField)', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      model.quickReplyModel.tapOnComposeButton()
      assert.strictEqual(
        model.composeModel.getDraft().body,
        '\n\n--\nSent from Yandex Mail for mobile\n\n01.01.1970, 03:00, from3@yandex.ru:\nMessage body',
      )
      done()
    })

    it('should open compose (filled textField)', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      model.quickReplyModel.tapOnTextField()
      model.quickReplyModel.setTextFieldValue('Ответ')
      model.quickReplyModel.tapOnComposeButton()
      assert.strictEqual(
        model.composeModel.getDraft().body,
        'Ответ\n\n--\nSent from Yandex Mail for mobile\n\n01.01.1970, 03:00, from3@yandex.ru:\nMessage body',
      )
      done()
    })

    it('should send message if enter reply manually', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.messageNavigator.openMessage(1)
      model.quickReplyModel.tapOnTextField()
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      model.quickReplyModel.setTextFieldValue('Ответ')
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), true)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), 'Ответ')
      model.quickReplyModel.tapOnSendButton()
      assert.strictEqual(model.quickReplyModel.isSendButtonEnabled(), false)
      assert.strictEqual(model.quickReplyModel.getTextFieldValue(), '')
      model.folderNavigator.goToFolder(DefaultFolderName.sent, [])
      model.messageNavigator.openMessage(0)
      const sentMessage = model.messageNavigator.getOpenedMessage()
      assert.strictEqual(
        sentMessage.body,
        'Ответ\n\n--\nSent from Yandex Mail for mobile\n\n01.01.1970, 03:00, from3@yandex.ru:\nMessage body',
      )
      assert.strictEqual(sentMessage.head.firstLine, 'Ответ')
      assert.strictEqual(sentMessage.head.subject, 'Re: subject3')
      assert.strictEqual(sentMessage.head.threadCounter, 2)
      assert.strictEqual(sentMessage.head.from, 'mock-mailbox@yandex.ru')
      done()
    })
  })

  describe('Tab bar', () => {
    it('should change current item', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      model.tabBarModel.tapOnItem(TabBarItem.telemost)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.telemost)
      model.tabBarModel.tapOnItem(TabBarItem.calendar)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.calendar)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.calendar)
      done()
    })

    it('should not shown in Folder list', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.folderNavigator.openFolderList()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.folderNavigator.closeFolderList()
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.folderNavigator.openFolderList()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.folderNavigator.goToFolder(DefaultFolderName.sent, [])
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.folderNavigator.openFolderList()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.folderNavigator.goToFilterUnread()
      assert.strictEqual(model.tabBarModel.isShown(), true)
      done()
    })

    it('should not shown in Search', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.search.openSearch()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.search.searchAllMessages()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.search.closeSearch()
      assert.strictEqual(model.tabBarModel.isShown(), true)
      done()
    })

    it('should not shown in MailView', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.messageNavigator.openMessage(0)
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.messageNavigator.closeMessage()
      assert.strictEqual(model.tabBarModel.isShown(), true)
      done()
    })

    it('should not shown in Compose', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.composeModel.openCompose()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.composeModel.sendMessage()
      assert.strictEqual(model.tabBarModel.isShown(), true)
      done()
    })

    it('should not shown in Settings', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.strictEqual(model.tabBarModel.isShown(), true)
      model.folderNavigator.openFolderList()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.rootSettingsModel.openRootSettings()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.generalSettingsModel.openGeneralSettings()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.generalSettingsModel.closeGeneralSettings()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.accountSettingsModel.openAccountSettings(0)
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.accountSettingsModel.closeAccountSettings()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.aboutSettingsModel.openAboutSettings()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.rootSettingsModel.closeRootSettings()
      assert.strictEqual(model.tabBarModel.isShown(), false)
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      assert.strictEqual(model.tabBarModel.isShown(), true)
      done()
    })

    it('should return correct item for yandex acc', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const yandexTabBarItemsIOS = [
        TabBarItem.mail,
        TabBarItem.calendar,
        TabBarItem.documents,
        TabBarItem.telemost,
        TabBarItem.more,
      ]
      const yandexTabBarItemsAndroid = [
        TabBarItem.mail,
        TabBarItem.contacts,
        TabBarItem.documents,
        TabBarItem.telemost,
        TabBarItem.more,
      ]
      assert.deepStrictEqual(model.tabBarIOSModel.getItems(), yandexTabBarItemsIOS)
      assert.deepStrictEqual(model.tabBarAndroidModel.getItems(), yandexTabBarItemsAndroid)
      done()
    })

    it('should return correct date', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      assert.deepStrictEqual(model.tabBarIOSModel.getCalendarIconDate(), int32ToString(new Date().getDate()))
      done()
    })
  })

  describe('Shtorka', () => {
    it('should close', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      model.shtorkaModel.closeBySwipe()
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      model.shtorkaModel.closeByTapOver()
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      done()
    })

    it('should show and close promo', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), ShtorkaBannerType.docs)
      model.shtorkaModel.closeBanner()
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      model.shtorkaModel.closeBySwipe()
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), ShtorkaBannerType.scanner)
      model.shtorkaModel.closeBanner()
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      model.shtorkaModel.closeBySwipe()
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), ShtorkaBannerType.mail360)
      model.shtorkaModel.closeBanner()
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      model.shtorkaModel.closeBySwipe()
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      done()
    })

    it('should not show promo if no promote_mail360 = true', (done) => {
      const model = MockMailboxProvider.emptyFoldersOneAccount().model
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), ShtorkaBannerType.docs)
      model.shtorkaModel.closeBanner()
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      model.shtorkaModel.closeByTapOver()
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), ShtorkaBannerType.scanner)
      model.shtorkaModel.closeBanner()
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      model.shtorkaModel.closeBySwipe()
      model.tabBarModel.tapOnItem(TabBarItem.more)
      assert.strictEqual(model.shtorkaModel.getShownBannerType(), null)
      done()
    })

    it('should change current tab bar item', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.mail)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.calendar)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.calendar)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.telemost)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.telemost)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.contacts)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.contacts)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.disk)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.more)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.notes)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.more)
      model.tabBarModel.tapOnItem(TabBarItem.more)
      model.shtorkaModel.tapOnItem(TabBarItem.mail)
      assert.strictEqual(model.tabBarModel.getCurrentItem(), TabBarItem.mail)
      done()
    })
  })

  describe('ComposeNew', () => {
    const composeEmailProvider = ComposeEmailProvider.instance

    it('should be signature in body', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      const body = model.composeModel.getBody()
      assert.strictEqual(body, '\n\n--\nSent from Yandex Mail for mobile')
      done()
    })

    it('should set and delete valid/invalid to/cc/bcc', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail = composeEmailProvider.getRandomValidEmail()
      const invalidEmail = composeEmailProvider.getRandomInvalidEmail()
      model.composeModel.openCompose()
      for (const field of [ComposeRecipientFieldType.to, ComposeRecipientFieldType.cc, ComposeRecipientFieldType.bcc]) {
        for (const yabbleType of [YabbleType.manual, YabbleType.invalid]) {
          const email = yabbleType === YabbleType.invalid ? invalidEmail : validEmail
          const formattedValidEmail = model.composeModel.formatValidEmail(email)
          model.composeModel.setRecipientField(field, email, true)
          let recipients = model.composeModel.getRecipientFieldValue(field)
          assert.strictEqual(recipients.length, 1)
          assert.strictEqual(Yabble.matches(recipients[0], new Yabble(formattedValidEmail, '', yabbleType)), true)
          model.composeModel.deleteRecipientByTapOnCross(field, 0)
          recipients = model.composeModel.getRecipientFieldValue(field)
          assert.strictEqual(recipients.length, 0)
        }
      }
      done()
    })

    it('should paste and delete valid/invalid to/cc/bcc', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail = composeEmailProvider.getRandomValidEmail()
      const invalidEmail = composeEmailProvider.getRandomInvalidEmail()
      model.composeModel.openCompose()
      for (const field of [ComposeRecipientFieldType.to, ComposeRecipientFieldType.cc, ComposeRecipientFieldType.bcc]) {
        for (const yabbleType of [YabbleType.manual, YabbleType.invalid]) {
          const email = yabbleType === YabbleType.invalid ? invalidEmail : validEmail
          const formattedValidEmail = model.composeModel.formatValidEmail(email)
          model.composeModel.pasteToRecipientField(field, email, true)
          let recipients = model.composeModel.getRecipientFieldValue(field)
          assert.strictEqual(recipients.length, 1)
          assert.strictEqual(Yabble.matches(recipients[0], new Yabble(formattedValidEmail, '', yabbleType)), true)
          model.composeModel.deleteRecipientByTapOnCross(field, 0)
          recipients = model.composeModel.getRecipientFieldValue(field)
          assert.strictEqual(recipients.length, 0)
        }
      }
      done()
    })

    it('should generate yabble after change focus', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail = composeEmailProvider.getRandomValidEmail()
      const invalidEmail = composeEmailProvider.getRandomInvalidEmail()
      const formattedValidEmail = model.composeModel.formatValidEmail(validEmail)
      model.composeModel.openCompose()
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, validEmail, false)
      assert.strictEqual(
        Yabble.matches(model.composeModel.getNotGeneratedYabble()!, new Yabble(validEmail, '', YabbleType.new)),
        true,
      )
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(
        Yabble.matches(model.composeModel.getNotGeneratedYabble()!, new Yabble(validEmail, '', YabbleType.new)),
        true,
      )
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.getNotGeneratedYabble(), null)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(
        Yabble.matches(
          model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to)[0],
          new Yabble(formattedValidEmail, '', YabbleType.manual),
        ),
        true,
      )
      model.composeModel.setRecipientField(ComposeRecipientFieldType.cc, invalidEmail, false)
      assert.strictEqual(
        Yabble.matches(model.composeModel.getNotGeneratedYabble()!, new Yabble(invalidEmail, '', YabbleType.new)),
        true,
      )
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc).length, 1)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc).length, 1)
      assert.strictEqual(model.composeModel.getNotGeneratedYabble(), null)
      assert.strictEqual(
        Yabble.matches(
          model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc)[0],
          new Yabble(invalidEmail, '', YabbleType.invalid),
        ),
        true,
      )
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc).length, 1)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.bcc, validEmail, false)
      assert.strictEqual(
        Yabble.matches(model.composeModel.getNotGeneratedYabble()!, new Yabble(validEmail, '', YabbleType.new)),
        true,
      )
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc).length, 1)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.bcc).length, 1)
      model.composeModel.tapOnSubjectField()
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc).length, 1)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.bcc).length, 1)
      assert.strictEqual(model.composeModel.getNotGeneratedYabble(), null)
      assert.strictEqual(
        Yabble.matches(
          model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.bcc)[0],
          new Yabble(formattedValidEmail, '', YabbleType.manual),
        ),
        true,
      )
      done()
    })

    it('should change focus', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.to)
      model.composeModel.expandExtendedRecipientForm()
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.to)
      model.composeModel.tapOnSenderField()
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.to)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.cc)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.bcc)
      model.composeModel.tapOnSubjectField()
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.subject)
      model.composeModel.tapOnBodyField()
      assert.strictEqual(model.composeModel.getFocusedField(), ComposeFieldType.body)
      done()
    })

    it('should expand and minimize cc/bcc/from fields', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail = composeEmailProvider.getRandomValidEmail()
      model.composeModel.openCompose()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), false)
      model.composeModel.expandExtendedRecipientForm()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), true)
      model.composeModel.minimizeExtendedRecipientForm()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), false)
      model.composeModel.expandExtendedRecipientForm()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), true)
      model.composeModel.tapOnBodyField()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), false)
      model.composeModel.expandExtendedRecipientForm()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), true)
      model.composeModel.tapOnSubjectField()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), false)

      model.composeModel.expandExtendedRecipientForm()
      model.composeModel.setRecipientField(ComposeRecipientFieldType.cc, validEmail, true)
      model.composeModel.tapOnBodyField()
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isExtendedRecipientFormShown(), true)
      done()
    })

    it('should generate yabble by tap on Enter', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail = composeEmailProvider.getRandomValidEmail()
      const formattedEmail = model.composeModel.formatValidEmail(validEmail)
      model.composeModel.openCompose()
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, validEmail, false)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(
        Yabble.matches(model.composeModel.getNotGeneratedYabble()!, new Yabble(validEmail, '', YabbleType.new)),
        true,
      )
      model.composeModel.generateYabbleByTapOnEnter()
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(model.composeModel.getNotGeneratedYabble(), null)
      assert.strictEqual(
        Yabble.matches(
          model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to)[0],
          new Yabble(formattedEmail, '', YabbleType.manual),
        ),
        true,
      )
      done()
    })

    it('should delete last yabble by tap on backspace', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail1 = composeEmailProvider.getRandomValidEmail()
      const formattedValidEmail1 = model.composeModel.formatValidEmail(validEmail1)
      const validEmail2 = composeEmailProvider.getRandomValidEmail()
      model.composeModel.openCompose()
      for (const field of [ComposeRecipientFieldType.to, ComposeRecipientFieldType.cc, ComposeRecipientFieldType.bcc]) {
        model.composeModel.setRecipientField(field, validEmail1, false)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 1)
        model.composeModel.deleteLastRecipientByTapOnBackspace(field)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 0)
        model.composeModel.deleteLastRecipientByTapOnBackspace(field)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 0)

        model.composeModel.setRecipientField(field, validEmail1, true)
        model.composeModel.setRecipientField(field, validEmail2, true)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 2)
        model.composeModel.deleteLastRecipientByTapOnBackspace(field)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 1)
        assert.strictEqual(
          Yabble.matches(
            model.composeModel.getRecipientFieldValue(field)[0],
            new Yabble(formattedValidEmail1, '', YabbleType.manual),
          ),
          true,
        )
      }
      done()
    })

    it('should delete last yabble by tap on cross', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      const validEmail1 = composeEmailProvider.getRandomValidEmail()
      const formattedValidEmail1 = model.composeModel.formatValidEmail(validEmail1)
      const validEmail2 = composeEmailProvider.getRandomValidEmail()
      const invalidEmail = composeEmailProvider.getRandomInvalidEmail()
      model.composeModel.openCompose()
      for (const field of [ComposeRecipientFieldType.to, ComposeRecipientFieldType.cc, ComposeRecipientFieldType.bcc]) {
        model.composeModel.setRecipientField(field, validEmail1, true)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 1)
        model.composeModel.setRecipientField(field, validEmail2, true)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 2)
        model.composeModel.setRecipientField(field, invalidEmail, true)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 3)
        model.composeModel.deleteRecipientByTapOnCross(field, 1)
        assert.strictEqual(model.composeModel.getRecipientFieldValue(field).length, 2)
        assert.strictEqual(
          Yabble.matches(
            model.composeModel.getRecipientFieldValue(field)[0],
            new Yabble(formattedValidEmail1, '', YabbleType.manual),
          ),
          true,
        )
        assert.strictEqual(
          Yabble.matches(
            model.composeModel.getRecipientFieldValue(field)[1],
            new Yabble(invalidEmail, '', YabbleType.invalid),
          ),
          true,
        )
      }
      done()
    })

    it('should change from address', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      assert.strictEqual(model.composeModel.getSenderFieldValue(), 'mock-mailbox@yandex.ru')
      model.composeModel.openCompose()
      model.composeModel.tapOnSenderField()
      model.composeModel.tapOnSenderSuggestByIndex(1)
      assert.strictEqual(model.composeModel.getSenderFieldValue(), 'mock-mailbox@yandex.com')
      model.composeModel.tapOnSenderField()
      model.composeModel.tapOnSenderSuggestByEmail('mock-mailbox@ya.ru')
      assert.strictEqual(model.composeModel.getSenderFieldValue(), 'mock-mailbox@ya.ru')
      done()
    })

    it('should show from address suggest', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), false)
      model.composeModel.tapOnSenderField()
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), true)
      assert.deepStrictEqual(model.composeModel.getSenderSuggest(), [
        'mock-mailbox@yandex.ru',
        'mock-mailbox@yandex.com',
        'mock-mailbox@ya.ru',
      ])
      model.composeModel.tapOnSenderField()
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), false)
      model.composeModel.tapOnSenderField()
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), true)
      assert.deepStrictEqual(model.composeModel.getSenderSuggest(), [
        'mock-mailbox@yandex.ru',
        'mock-mailbox@yandex.com',
        'mock-mailbox@ya.ru',
      ])
      model.composeModel.tapOnSenderSuggestByIndex(1)
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), false)
      model.composeModel.tapOnSenderField()
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), true)
      assert.deepStrictEqual(model.composeModel.getSenderSuggest(), [
        'mock-mailbox@yandex.ru',
        'mock-mailbox@yandex.com',
        'mock-mailbox@ya.ru',
      ])
      model.composeModel.tapOnSenderSuggestByEmail('mock-mailbox@ya.ru')
      assert.strictEqual(model.composeModel.isSenderSuggestShown(), false)
      done()
    })

    it('should show to/cc/bcc suggest', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.expandExtendedRecipientForm()
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.minimizeExtendedRecipientForm()
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.tapOnSubjectField()
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      model.composeModel.tapOnBodyField()
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      done()
    })

    it('should show found contacts suggest', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 4)
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[0], new Contact('testtest', 'testtest@yandex.ru')),
        true,
      )
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, 'main', false)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 1)
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[0], new Contact('maintest', 'maintest@yandex.ru')),
        true,
      )
      model.composeModel.deleteLastRecipientByTapOnBackspace(ComposeRecipientFieldType.to)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, 'test@yan', false)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 3)
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[0], new Contact('testtest', 'testtest@yandex.ru')),
        true,
      )
      model.composeModel.tapOnRecipientSuggestByIndex(0)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, 'testtest@yan', false)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 0)
      model.composeModel.deleteLastRecipientByTapOnBackspace(ComposeRecipientFieldType.to)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, 'имя отличается', false)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 1)
      assert.strictEqual(
        Contact.matches(
          model.composeModel.getRecipientSuggest()[0],
          new Contact('имя отличается от email', 'different-name@yandex.ru'),
        ),
        true,
      )
      model.composeModel.deleteLastRecipientByTapOnBackspace(ComposeRecipientFieldType.to)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, 'asdas@', false)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 9)
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[8], new Contact('', 'asdas@inbox.ru')),
        true,
      )
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[2], new Contact('', 'asdas@gmail.com')),
        true,
      )
      model.composeModel.deleteLastRecipientByTapOnBackspace(ComposeRecipientFieldType.to)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, 'asdas@y', false)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 2)
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[0], new Contact('', 'asdas@yandex.ru')),
        true,
      )
      assert.strictEqual(
        Contact.matches(model.composeModel.getRecipientSuggest()[1], new Contact('', 'asdas@yahoo.com')),
        true,
      )
      done()
    })

    it('should generate yabble by tap on suggest by index', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 4)
      model.composeModel.tapOnRecipientSuggestByIndex(0)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to).length, 1)
      assert.strictEqual(
        Yabble.matches(
          model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.to)[0],
          new Yabble('testtest@yandex.ru', 'testtest', YabbleType.suggested),
        ),
        true,
      )
      done()
    })

    it('should generate yabble by tap on suggest by email', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      model.composeModel.expandExtendedRecipientForm()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), true)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 4)
      model.composeModel.tapOnRecipientSuggestByEmail('maintest@yandex.ru')
      assert.strictEqual(model.composeModel.isRecipientSuggestShown(), false)
      assert.strictEqual(model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc).length, 1)
      assert.strictEqual(
        Yabble.matches(
          model.composeModel.getRecipientFieldValue(ComposeRecipientFieldType.cc)[0],
          new Yabble('maintest@yandex.ru', 'maintest', YabbleType.suggested),
        ),
        true,
      )
      done()
    })

    it('should not show used email in suggest', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 4)
      model.composeModel.tapOnRecipientSuggestByIndex(0)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 3)
      model.composeModel.expandExtendedRecipientForm()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 3)
      model.composeModel.tapOnRecipientSuggestByIndex(0)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 2)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 2)
      model.composeModel.tapOnRecipientSuggestByIndex(0)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      assert.strictEqual(model.composeModel.getRecipientSuggest().length, 1)
      done()
    })

    it('should set subject', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      model.composeModel.tapOnSubjectField()
      assert.strictEqual(model.composeModel.getSubject(), '')
      model.composeModel.setSubject('new subject')
      assert.strictEqual(model.composeModel.getSubject(), 'new subject')
      model.composeModel.setSubject('other one')
      assert.strictEqual(model.composeModel.getSubject(), 'other one')
      model.composeModel.setSubject('')
      assert.strictEqual(model.composeModel.getSubject(), '')
      done()
    })

    it('should set body', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      model.composeModel.tapOnBodyField()
      assert.strictEqual(model.composeModel.getBody(), '\n\n--\nSent from Yandex Mail for mobile')
      model.composeModel.setBody('new body')
      assert.strictEqual(model.composeModel.getBody(), 'new body\n\n--\nSent from Yandex Mail for mobile')
      model.composeModel.setBody('other one')
      assert.strictEqual(model.composeModel.getBody(), 'other one\n\n--\nSent from Yandex Mail for mobile')
      model.composeModel.setBody('')
      assert.strictEqual(model.composeModel.getBody(), '\n\n--\nSent from Yandex Mail for mobile')
      model.composeModel.clearBody()
      assert.strictEqual(model.composeModel.getBody(), '\n')
      model.composeModel.setBody('other one')
      assert.strictEqual(model.composeModel.getBody(), 'other one')
      done()
    })

    it('should reply', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.folderNavigator.goToFolder(DefaultFolderName.sent, [])
      const messagesInSentBefore = model.messageListDisplay.getMessageList(10)
      model.folderNavigator.goToFolder(DefaultFolderName.inbox, [])
      model.contextMenu.openFromShortSwipe(0)
      model.contextMenu.openReplyCompose()
      assert.strictEqual(
        model.composeModel.getBody(),
        '\n\n--\nSent from Yandex Mail for mobile\n\n01.01.1970, 03:00, from4@yandex.ru:\nКак дела?',
      )
      model.composeModel.setBody('test body')
      assert.strictEqual(
        model.composeModel.getBody(),
        'test body\n\n--\nSent from Yandex Mail for mobile\n\n01.01.1970, 03:00, from4@yandex.ru:\nКак дела?',
      )
      model.composeModel.sendMessage()
      model.folderNavigator.goToFolder(DefaultFolderName.sent, [])
      const messagesInSent = model.messageListDisplay.getMessageList(10)
      assert.strictEqual(messagesInSentBefore.length + 1, messagesInSent.length)
      const sentMessage = messagesInSent[0]
      assert.strictEqual(
        Message.matches(
          sentMessage,
          new Message('from4@yandex.ru', 'Re: subject5', int64(1), 'test body', 5, false, false, []),
        ),
        true,
      )
      done()
    })

    it('should get To field value with minimized cc bcc from fields', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      const validEmail1 = composeEmailProvider.getRandomValidEmail()
      const validEmail2 = composeEmailProvider.getRandomValidEmail()
      const validEmail3 = composeEmailProvider.getRandomValidEmail()
      const invalidEmail = composeEmailProvider.getRandomInvalidEmail()
      const formattedValidEmail1 = model.composeModel.formatValidEmail(validEmail1)
      const formattedValidEmail2 = model.composeModel.formatValidEmail(validEmail2)
      const formattedValidEmail3 = model.composeModel.formatValidEmail(validEmail3)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, validEmail1, true)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), formattedValidEmail1)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, validEmail2, true)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), `${formattedValidEmail1} and 1 more`)
      model.composeModel.expandExtendedRecipientForm()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.cc, validEmail3, true)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), `${formattedValidEmail1} and 2 more`)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.bcc, invalidEmail, true)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), `${formattedValidEmail1} and 3 more`)
      model.composeModel.deleteRecipientByTapOnCross(ComposeRecipientFieldType.to, 0)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), `${formattedValidEmail2} and 2 more`)
      model.composeModel.deleteRecipientByTapOnCross(ComposeRecipientFieldType.to, 0)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), `${formattedValidEmail3} and 1 more`)
      model.composeModel.deleteRecipientByTapOnCross(ComposeRecipientFieldType.cc, 0)
      assert.strictEqual(model.composeModel.getCompactRecipientFieldValue(), `${invalidEmail}`)
      done()
    })

    it('should save draft', (done) => {
      const model = MockMailboxProvider.exampleOneAccount().model
      model.composeModel.openCompose()
      const validEmail1 = composeEmailProvider.getRandomValidEmail()
      const validEmail2 = composeEmailProvider.getRandomValidEmail()
      const invalidEmail = composeEmailProvider.getRandomInvalidEmail()
      const formattedValidEmail1 = model.composeModel.formatValidEmail(validEmail1)
      const formattedValidEmail2 = model.composeModel.formatValidEmail(validEmail2)
      const subject = 'new subject'
      const body = 'new body'
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.to)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.to, validEmail1, true)
      model.composeModel.expandExtendedRecipientForm()
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.cc)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.cc, validEmail2, true)
      model.composeModel.tapOnRecipientField(ComposeRecipientFieldType.bcc)
      model.composeModel.setRecipientField(ComposeRecipientFieldType.bcc, invalidEmail, true)
      model.composeModel.tapOnBodyField()
      model.composeModel.setBody(body)
      model.composeModel.tapOnSubjectField()
      model.composeModel.setSubject(subject)
      assert.strictEqual(
        Draft.matches(
          model.composeModel.getDraft(),
          new Draft(
            [new Yabble(formattedValidEmail1, '', YabbleType.manual)],
            [new Yabble(formattedValidEmail2, '', YabbleType.manual)],
            [new Yabble(invalidEmail, '', YabbleType.invalid)],
            'mock-mailbox@yandex.ru',
            subject,
            `${body}\n\n--\nSent from Yandex Mail for mobile`,
            [],
          ),
        ),
        true,
      )
      done()
    })
  })
})
