import 'mocha'
import { onProcessStart } from '../../../testopithecus-common/__tests__/code/test-utils'
import { ConsoleLog } from '../../../common/__tests__/__helpers__/console-log'
import { DefaultJSONSerializer } from '../../../common/__tests__/__helpers__/default-json'
import { EventusRegistry } from '../../../eventus-common/code/eventus-registry'
import { SyncSleepImpl } from '../../../testopithecus-common/__tests__/code/pod/sleep'
import { MBTPlatform } from '../../../testopithecus-common/code/mbt/test/mbt-test'
import { TestopithecusTestRunner, TestPlan } from '../../../testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { MailboxClient, MailboxClientHandler } from '../../code/client/mailbox-client'
import { PublicBackendConfig } from '../../code/client/public-backend-config'
import { MobileMailBackend } from '../../code/mail/backend/mobile-mail-backend'
import { LoginComponent } from '../../code/mail/components/login-component'
import { MailboxDownloader } from '../../code/mail/mailbox-downloader'
import { MailboxPreparerProvider } from '../../code/mail/mailbox-preparer'
import { MailboxModel } from '../../code/mail/model/mail-model'
import { NoComposeFullCoverageTest } from '../../code/tests/full-coverage-tests'
import { RandomWalkTest } from '../../code/tests/random-walk-test'
import { AllMailTests } from '../../code/tests/register-your-test-here'
// import { SwipeToReadThreadTest } from '../../code/tests/short-swipe-tests'
import { DefaultImapProvider } from '../pod/default-imap'
import { TestStackReporter } from '../test-stack-reporter'
import { applyEnv, createNetworkClient, createSyncNetwork } from '../test-utils'

onProcessStart()

describe('Run all tests on Mobile API', async () => {
  await runAllTests(false)
})

export async function runAllTests(mockMode: boolean): Promise<void> {
  applyEnv()
  setupRegistry()
  const registry = AllMailTests.get
    .regularAll(RandomWalkTest.generate(10, ConsoleLog.LOGGER))
    .regular(new NoComposeFullCoverageTest(ConsoleLog.LOGGER))
  // registry.debug(new SwipeToReadThreadTest())
  const supportedFeatures = mockMode ? MailboxModel.allSupportedFeatures : MobileMailBackend.allSupportedFeatures

  for (const test of registry.getTestsPossibleToRun(
    MBTPlatform.MobileAPI,
    MailboxModel.allSupportedFeatures,
    supportedFeatures,
  )) {
    const network = createSyncNetwork()
    const jsonSerializer = new DefaultJSONSerializer()
    const imap = new DefaultImapProvider()
    const testRunner = new TestopithecusTestRunner(
      MBTPlatform.MobileAPI,
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

    afterEach(() => {
      testRunner.finish()
    })

    if (testRunner.isEnabled(MailboxModel.allSupportedFeatures, supportedFeatures)) {
      it(test.description, async () => {
        const accounts = await testRunner.lockAndPrepareAccountData()
        if (accounts.length > 0) {
          const clients: MailboxClient[] = []
          accounts.forEach((account) => {
            const mailboxClient = createNetworkClient(account)
            clients.push(mailboxClient)
          })
          const mailboxClientHandler = new MailboxClientHandler(clients)
          const mailBackend = new MobileMailBackend(mailboxClientHandler)
          const application = !mockMode ? mailBackend : null

          const model = await new MailboxDownloader(clients, ConsoleLog.LOGGER).takeAppModel()
          const plan = test.scenario(
            accounts.map((a) => a.account),
            model,
            supportedFeatures,
          )
          const logs = emulateActualLogs(plan)
          await testRunner.runTest(accounts, new LoginComponent(), application)
          testRunner.validateLogs(logs)
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
}

function emulateActualLogs(plan: TestPlan): string {
  const actualEvents = plan.getExpectedEvents()
  const reporter = new TestStackReporter()
  EventusRegistry.setEventReporter(reporter)
  actualEvents.forEach((e) => e.report())
  return reporter.events
    .map((e) => {
      const valueDict: { [key: string]: any } = {}
      e.attributes.forEach((v, k) => {
        if (typeof v === 'bigint') {
          valueDict[k] = Number(v)
        } else {
          valueDict[k] = v
        }
      })
      return JSON.stringify({
        name: e.name,
        value: valueDict,
      })
    })
    .join('\n')
}

export function setupRegistry(): void {
  ConsoleLog.setup()
}
