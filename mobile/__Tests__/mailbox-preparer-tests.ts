import { DefaultJSONSerializer } from '../../common/__tests__/__helpers__/default-json'
import { ConsoleLog } from '../../common/__tests__/__helpers__/console-log'
import {
  AttachmentSpec,
  MailAccountSpec,
  MailboxBuilder,
  MailboxPreparerProvider,
  MessageSpec,
  UserSpec,
} from '../code/mail/mailbox-preparer'
import { MBTPlatform } from '../../testopithecus-common/code/mbt/test/mbt-test'
import { DefaultFolderName } from '../code/mail/model/folder-data-model'
import { DefaultImapProvider } from './pod/default-imap'
import { SyncSleepImpl } from '../../testopithecus-common/__tests__/code/pod/sleep'
import { PRIVATE_BACKEND_CONFIG } from './private-backend-config'
import { createSyncNetwork, getOAuthAccount } from './test-utils'

describe('IMAP model preparer', () => {
  it('should prepare model correctly', (done) => {
    const delegate = new MailboxPreparerProvider(
      MBTPlatform.MobileAPI,
      new DefaultJSONSerializer(),
      createSyncNetwork(),
      ConsoleLog.LOGGER,
      SyncSleepImpl.instance,
      new DefaultImapProvider(),
    )
    const mailbox = new MailboxBuilder(
      MailAccountSpec.fromUserAccount(PRIVATE_BACKEND_CONFIG.account, 'imap.yandex.ru'),
      delegate,
    )
      .addMessageToFolder(
        DefaultFolderName.inbox,
        MessageSpec.builder()
          .addAttachments([AttachmentSpec.withName('log')])
          .withSender(new UserSpec('katya@yandex.ru', 'Катя'))
          .withSubject('Важные дела')
          .withTextBody('Будут завтра')
          .withTimestamp(new Date('2019-09-11T17:03:06.504Z'))
          .build(),
      )
      .addMessageToFolder(
        DefaultFolderName.inbox,
        MessageSpec.builder()
          .withSender(new UserSpec('dasha@yandex.ru', 'Даша'))
          .withSubject('Ты где?')
          .withTextBody('Все уже тут')
          .withTimestamp(new Date('2019-09-11T17:03:06.504Z'))
          .build(),
      )
      .addMessageToFolder(
        DefaultFolderName.inbox,
        MessageSpec.builder()
          .withSender(new UserSpec('masha@yandex.ru', 'Маша'))
          .withSubject('Привет')
          .withTextBody('Как дела?')
          .withTimestamp(new Date('2019-09-10T17:03:06.504Z'))
          .build(),
      )
      .saveQueryToZeroSuggest('test')
    mailbox
      .prepare(getOAuthAccount(PRIVATE_BACKEND_CONFIG.account, PRIVATE_BACKEND_CONFIG.accountType))
      .then(() => done())
  })
})
