import { Uid } from '../../xpackages/testopithecus-common/code/users/user-pool'
import { range } from '../../common/ys'
import { Log } from '../../xpackages/common/code/logging/logger'
import { onProcessStart } from '../../xpackages/testopithecus-common/__tests__/code/test-utils'
import assert from 'assert'
import { AssertAction } from '../../xpackages/testopithecus-common/code/mbt/actions/assert-action'
import { StateMachine } from '../../xpackages/testopithecus-common/code/mbt/state-machine'
import {
  CyclicActions,
  ListActions,
  MBTComponentActions,
  UserBehaviour,
} from '../../xpackages/testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import {
  RandomActionChooser,
  UserBehaviourWalkStrategy,
} from '../../xpackages/testopithecus-common/code/mbt/walk/user-behaviour-walk-strategy'
import { ConsoleLog } from '../../xpackages/common/__tests__/__helpers__/console-log'
import { CalendarBackend, EventListBackend } from '../code/backend/calendar-backend'
import { CalendarClient } from '../code/backend/calendar-client'
import { ExchangeApplication, ExchangeEventList } from '../code/exchange/exchange-application'
import { ExchangeClient } from '../code/exchange/exchange-client'
import { EventListActions } from '../code/model/calendar-actions'
import { CalendarComponent } from '../code/model/calendar-components'
import { CalendarEvent, CalendarUser, EventId } from '../code/model/calendar-features'
import { CalendarDownloader } from '../code/model/calendar-preparer'
import { CalendarUsers } from '../code/model/calendar-users'
import { clearAll, findByNameAsync } from '../code/model/calendar-utils'
import { Timestamps } from '../code/model/timestamps'
import { MultiCalendar } from '../code/multi-calendar'

onProcessStart()

describe('Простые тесты', () => {
  it.skip('GREG-911:  Нулевая встреча не удаляется из exchange при удалении из нашего календаря', async () => {
    const user = CalendarUsers.calendartestuser
    // await clear(new ExchangeApplication(ExchangeClient.testing), user, Timestamps.getDate(-100))
    // await clear(new CalendarBackend(CalendarClient.corpTest), user, Timestamps.getDate(-100))
    const from = Timestamps.getDate(1)
    const to = Timestamps.getDate(1)
    const name = `${from}`
    const event = new CalendarEvent(name, from, to, [])
    await ExchangeClient.testing.createCalendarItem(event, user.email)
    let backId
    while (!backId) {
      backId = await findByNameAsync(new EventListBackend(CalendarClient.corpTest), user, name)
    }
    await CalendarClient.corpTest.deleteEvent(user.uid, backId)
    const exchangeId = await findByNameAsync(new ExchangeEventList(ExchangeClient.testing), user, name)
    assert.strictEqual(exchangeId, undefined)
  })

  it('мы должны уметь получать uid по email', async () => {
    const userInfo = await CalendarClient.corpTest.getUserOrResourceInfo(
      CalendarUsers.calendartestuser.uid,
      CalendarUsers.robotMailcorp1.email,
    )
    assert.strictEqual(userInfo.uid, '1120000000038012')
  })

  it('мы должны уметь получать email по uid', async () => {
    const userSettings = await CalendarClient.publicTest.getUserSettings('1100156894')
    assert.strictEqual(userSettings.email, 'yndx-cal-test-2163@yandex.ru')
  })
})

describe('Нагрузочные тесты', () => {
  it.skip('мы должны уметь создавать много больших событий одновременно на корпе', async () => {
    const eventSize = 800
    const accounts = await CalendarUsers.corp(eventSize)
    const eventIds = await createBigEventsInParallel(CalendarClient.corpTest, accounts, 4, 100 * 1000)
    await attachToEvent(eventIds[0], CalendarUsers.calendartestuser.uid)
  })

  it.skip('мы должны уметь создавать много больших событий одновременно на паблике', async () => {
    const eventSize = 800
    const accounts = CalendarUsers.publicAll().slice(0, eventSize)
    await createBigEventsInParallel(CalendarClient.publicTest, accounts, 3, 100 * 1000)
  })
})

async function createBigEventsInParallel(
  client: CalendarClient,
  accounts: CalendarUser[],
  parallel: number,
  timeoutMs: number,
): Promise<EventId[]> {
  const createBigEvent = async (order: number): Promise<EventId> => {
    const bigEvent = new CalendarEvent(
      `Большое событие из тестов ${new Date()} ${Math.random()}`,
      Timestamps.getDate(0),
      Timestamps.getDate(1),
      accounts.map((a) => a.email),
    )
    const back = new EventListBackend(client)
    const eventId = await back.createEvent(accounts[order % accounts.length], bigEvent)
    Log.info(`Событие создал за ${(Date.now() - start) / 1000} с`)
    return eventId
  }

  const start = Date.now()
  const eventIds = await Promise.all(Array.from(range(0, parallel)).map((i) => createBigEvent(i)))
  assert.strictEqual(Date.now() - start < timeoutMs, true)
  return eventIds
}

async function attachToEvent(bigEventId: EventId, uid: Uid): Promise<void> {
  const client = CalendarClient.corpTest
  const timeoutMs = 10 * 1000
  const defaultLayerId = await new EventListBackend(client).getDefaultLayer(uid)
  await CalendarClient.corpTest.detachEvent(CalendarUsers.calendartestuser.uid, bigEventId, defaultLayerId)
  const start = Date.now()
  const attached = await CalendarClient.corpTest.attachEvent(CalendarUsers.calendartestuser.uid, bigEventId)
  console.log(`Finished in ${Date.now() - start} ms`)
  assert.strictEqual(attached, true)
  assert.strictEqual(Date.now() - start < timeoutMs, true)
}

export async function runRandomWalk(possibleActions: MBTComponentActions, maxBackendRetries: number): Promise<void> {
  // const users = [CalendarUsers.robotMailcorp3, CalendarUsers.robotMailcorp4]
  const testUsers = [CalendarUsers.calendartestuser]
  const backendClient = CalendarClient.corpTest
  backendClient.maxRetries = maxBackendRetries
  const backend = new CalendarBackend(backendClient)
  const exchangeClient = ExchangeClient.testing.activate()
  const exchange = new ExchangeApplication(exchangeClient)
  const cleanFrom = Timestamps.getDate(-10)
  const cleanTo = Timestamps.getDate(Timestamps.size + 10)
  await clearAll(backend, testUsers, cleanFrom, cleanTo)
  await clearAll(exchange, testUsers, cleanFrom, cleanTo)
  // const application = new ExchangeApplication(exchangeClient)
  const syncronizationDelaySec = 20
  const application = new MultiCalendar([exchange, backend], syncronizationDelaySec)
  const model = await new CalendarDownloader(exchange, testUsers).takeAppModel()
  const actions = CyclicActions.of(possibleActions, ListActions.single(new AssertAction()))
  const stepsLimit = 1000
  const walkStrategy = new UserBehaviourWalkStrategy(
    new UserBehaviour().setActionProvider(CalendarComponent.type, actions),
    new RandomActionChooser(),
    stepsLimit,
  )
  const stateMachine = new StateMachine(model, application, walkStrategy, ConsoleLog.LOGGER)
  await stateMachine.go(new CalendarComponent())
}

describe('Случайное блуждание', () => {
  it('должно проверять синхронизацию, обходя известные баги', async () => {
    await runRandomWalk(new EventListActions(10, false, false), 5)
  })
})
