import { ConsoleLog } from '../../common/__tests__/__helpers__/console-log'
import * as assert from 'assert'
import { int64 } from '../../../common/ys'
import { MailboxDownloader } from '../code/mail/mailbox-downloader'
import { DefaultFolderName } from '../code/mail/model/folder-data-model'
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config'
import { createBackend, createMailboxPreparer, createNetworkClient, getOAuthAccount } from './test-utils'

// These tests almost do not depend on platform
describe('Mailbox client should', () => {
  const client = createNetworkClient()

  it('download all folders', () => {
    client.getFolderList()
  })

  it('download all messages in folders', () => {
    const folders = client.getFolderList()
    folders.forEach((folder) => client.getMessagesInFolder(folder.fid, 5))
  })

  it('download all threads in folders', () => {
    const folders = client.getFolderList()
    folders.forEach((folder) => client.getThreadsInFolder(folder.fid, 5))
  })

  it('not fall on not existing folder', () => {
    const messages = client.getThreadsInFolder(int64(0), 5)
    assert.strictEqual(messages.length, 0)
  })

  it('get settings', () => {
    client.getSettings()
  })

  it('send message with text', () => {
    client.sendMessage('yandex-team-15929.11479@yandex.ru', 'Как ', 'Привет, как дела?')
  })

  it('send mark nonexisting as read', () => {
    client.markMessageAsRead(int64(1))
  })

  it('send mark nonexisting as unread', () => {
    client.markMessageAsUnread(int64(1))
  })
})

describe('Mailbox downloader should', () => {
  it('download model', async () => {
    const client = createNetworkClient()
    const downloader = new MailboxDownloader([client], ConsoleLog.LOGGER)
    await downloader.takeAppModel()
  })
})

describe('Mailbox backend', () => {
  const backend = createBackend()
  const preparer = createMailboxPreparer(PRIVATE_BACKEND_CONFIG.account, 'imap.yandex.ru').nextMessage('subj')

  beforeEach(async () => {
    await preparer.prepare(getOAuthAccount(PRIVATE_BACKEND_CONFIG.account, PRIVATE_BACKEND_CONFIG.accountType))
  })

  it('should delete top message', () => {
    backend.deleteMessage.deleteMessage(0)
  })

  it('should mark top message as undread', () => {
    backend.markable.markAsUnread(0)
  })

  it('should label top message as important', () => {
    backend.markableImportant.markAsImportant(0)
  })

  it('should create folder', () => {
    backend.creatableFolder.createFolder('New Folder1')
  })

  it('should move message to folder', () => {
    backend.creatableFolder.createFolder('New Folder1')
    backend.movableToFolder.moveMessageToFolder(0, 'New Folder1')
  })

  it('should move to spam by order', () => {
    backend.spamable.moveToSpam(0)
  })

  it('should move to archive by order', () => {
    backend.archive.archiveMessage(0)
  })

  it('should show all messages from delete folder', () => {
    backend.movableToFolder.moveMessageToFolder(0, DefaultFolderName.trash)
    backend.folderNavigator.goToFolder(DefaultFolderName.trash, [])
    assert.strictEqual(backend.messageListDisplay.isInThreadMode(), false)
    const msg = backend.messageListDisplay.getMessageList(10)
    assert.strictEqual(msg.length, 1)
  })
})
