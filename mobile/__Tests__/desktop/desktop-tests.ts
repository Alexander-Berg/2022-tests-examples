import 'spectron'
import { Application } from 'spectron'
import { Nullable } from '../../../../common/ys'
import { ConsoleLog } from '../../../common/__tests__/__helpers__/console-log'
import { DefaultJSONSerializer } from '../../../common/__tests__/__helpers__/default-json'
import { PublicBackendConfig } from '../../code/client/public-backend-config'
import { LoginComponent } from '../../code/mail/components/login-component'
import { MailboxPreparerProvider } from '../../code/mail/mailbox-preparer'
import { MailboxModel } from '../../code/mail/model/mail-model'
import { MBTPlatform } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestopithecusTestRunner } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { RandomWalkTest } from '../../code/tests/random-walk-test'
import { AllMailTests } from '../../code/tests/register-your-test-here'
// import { InboxTopBarDisplayTest } from '../../code/tests/inbox-top-bar-display-test'
import { setupRegistry } from '../backend/mobile-mail-backend-tests'
import { DefaultImapProvider } from '../pod/default-imap'
import { SyncSleepImpl } from '../../../testopithecus-common/__tests__/code/pod/sleep'
import { createSyncNetwork } from '../test-utils'
import { DesktopApplication } from './desktop-application'

process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'
process.on('unhandledRejection', (e) => {
  throw e
})

// TODO: remove skip to show
describe.skip('Run all tests on Desktop', () => {
  setupRegistry()

  let mail: Nullable<Application> = null

  const registry = AllMailTests.get.regularAll(RandomWalkTest.generate(1, ConsoleLog.LOGGER))
  // registry.debug(new InboxTopBarDisplayTest(AccountType.YandexTeam))

  for (const test of registry.getTestsPossibleToRun(
    MBTPlatform.Desktop,
    MailboxModel.allSupportedFeatures,
    DesktopApplication.allSupportedFeatures,
  )) {
    const network = createSyncNetwork()
    const jsonSerializer = new DefaultJSONSerializer()
    const imap = new DefaultImapProvider()
    const testRunner = new TestopithecusTestRunner(
      MBTPlatform.Desktop,
      test,
      registry,
      new MailboxPreparerProvider(
        MBTPlatform.MobileAPI,
        jsonSerializer,
        network,
        ConsoleLog.LOGGER,
        SyncSleepImpl.instance,
        imap,
      ),
      PublicBackendConfig.mailApplicationCredentials,
      network,
      jsonSerializer,
      ConsoleLog.LOGGER,
    )

    afterEach(async () => {
      testRunner.finish()
      if (mail !== null && mail.isRunning()) {
        await mail.stop()
      }
    })

    if (testRunner.isEnabled(MailboxModel.allSupportedFeatures, DesktopApplication.allSupportedFeatures)) {
      it(test.description, async () => {
        mail = new Application({
          path: '/Applications/Yandex.Mail.app/Contents/MacOS/Yandex.Mail',
        })
        await mail.start()

        // TODO: clear state https://medium.com/how-to-electron/how-to-reset-application-data-in-electron-48bba70b5a49

        const accounts = await testRunner.lockAndPrepareAccountData()

        if (await mail.client.isExisting('[href="yamail://logout"]')) {
          await mail.client.element('[href="yamail://logout"]').click()
          await mail.start()
        }

        if (accounts.length > 0) {
          const application = new DesktopApplication(mail, mail.client)
          await testRunner.runTest(accounts, new LoginComponent(), application)
          console.log('Test finished')
        } else {
          console.log(`Skipping test '${test.description}'`)
          // TODO: this.skip()
        }
      })
    } else {
      // tslint:disable-next-line:no-empty
      it.skip(test.description, () => {})
    }
  }
})
