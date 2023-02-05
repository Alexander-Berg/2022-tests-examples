import { ConsoleLog } from '../../xpackages/common/__tests__/__helpers__/console-log'
import { DefaultJSONSerializer } from '../../xpackages/common/__tests__/__helpers__/default-json'
import { MBTPlatform } from '../../xpackages/testopithecus-common/code/mbt/test/mbt-test'
import { TestopithecusTestRunner } from '../../xpackages/testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OAuthApplicationCredentialsRegistry } from '../../xpackages/testopithecus-common/code/users/oauth-service'
import { createSyncNetwork } from '../../xpackages/testopithecus/__tests__/test-utils'
import { onProcessStart } from '../../xpackages/testopithecus-common/__tests__/code/test-utils'
import { CalendarBackend } from '../code/backend/calendar-backend'
import { CalendarClient } from '../code/backend/calendar-client'
import { ExchangeApplication } from '../code/exchange/exchange-application'
import { ExchangeClient } from '../code/exchange/exchange-client'
import { EventListActions } from '../code/model/calendar-actions'
import { CalendarComponent } from '../code/model/calendar-components'
import { CalendarModel } from '../code/model/calendar-model'
import { CalendarPreparerProvider } from '../code/model/calendar-preparer'
import { MultiCalendar } from '../code/multi-calendar'
import { AllCalendarTests } from '../code/tests/calendar-tests'
import { runRandomWalk } from './calendar.test'

onProcessStart()

describe('Run all tests on calendar backend', () => {
  const registry = AllCalendarTests.get
  // .required(new NoComposeFullCoverageTest(ConsoleLog.LOGGER))
  // registry.debug(new InboxTopBarDisplayTest(AccountType2.YandexTeam))

  for (const test of registry.getTestsPossibleToRun(
    MBTPlatform.MobileAPI,
    CalendarModel.allSupportedFeatures,
    ExchangeApplication.allSupportedFeatures,
  )) {
    const network = createSyncNetwork()
    const jsonSerializer = new DefaultJSONSerializer()
    const backend = new CalendarBackend(CalendarClient.corpTest)
    const exchange = new ExchangeApplication(ExchangeClient.testing)
    // const application = new ExchangeApplication(exchangeClient)
    const application = new MultiCalendar([backend, exchange], 20)
    // const application = backend
    const testRunner = new TestopithecusTestRunner(
      MBTPlatform.MobileAPI,
      test,
      registry,
      new CalendarPreparerProvider([backend, exchange]),
      new OAuthApplicationCredentialsRegistry(),
      network,
      jsonSerializer,
      ConsoleLog.LOGGER,
    )

    afterEach(() => {
      testRunner.finish()
    })

    if (testRunner.isEnabled(CalendarModel.allSupportedFeatures, ExchangeApplication.allSupportedFeatures)) {
      it(test.description, async () => {
        const accounts = await testRunner.lockAndPrepareAccountData()
        await testRunner.runTest(accounts, new CalendarComponent(), application)
      })
    } else {
      // tslint:disable-next-line:no-empty
      it.skip(test.description, () => {})
    }
  }
})

describe('Случайное блуждание', () => {
  it('должно находить баги синхронизации событий нулевой длины (GREG-911, GREG-916)', async () => {
    await runRandomWalk(new EventListActions(10, true, false), 5)
  })

  it('должно находить баг GREG-917', async () => {
    await runRandomWalk(new EventListActions(10, false, false), 0)
  })

  it('должно находить баг, когда удаленный участиник встречи вновь в ней появляется', async () => {
    await runRandomWalk(new EventListActions(10, false, true), 5)
  })
})
