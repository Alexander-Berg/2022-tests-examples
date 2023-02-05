import { Log } from '../../../xpackages/common/code/logging/logger'
import { XPromise } from '../../../xpackages/common/code/promise/xpromise'
import { App } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import {
  AccountDataPreparer,
  AccountDataPreparerProvider,
} from '../../../xpackages/testopithecus-common/code/mbt/test/account-data-preparer'
import { AccountType2 } from '../../../xpackages/testopithecus-common/code/mbt/test/mbt-test'
import {
  AppModel,
  AppModelProvider,
} from '../../../xpackages/testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { OAuthUserAccount, Uid, UserAccount } from '../../../xpackages/testopithecus-common/code/users/user-pool'
import { promise } from '../../../common/xpromise-support'
import { CalendarEvent, CalendarUser, EventListFeature } from './calendar-features'
import { CalendarModel } from './calendar-model'
import { clear, getEvents } from './calendar-utils'
import { Timestamps } from './timestamps'

export class CalendarPreparer implements AccountDataPreparer {
  private startEvents: CalendarEvent[] = []

  constructor(private lockedAccount: Uid, private readonly backends: App[]) {}

  public addEvent(e: CalendarEvent): CalendarPreparer {
    this.startEvents.push(e)
    return this
  }

  prepare(account: OAuthUserAccount): XPromise<void> {
    return promise((resolve, reject) => {
      this.prepareImpl(account)
        .then((_) => resolve())
        .catch((e) => reject(e))
    })
  }

  async prepareImpl(account: OAuthUserAccount): Promise<void> {
    const user = CalendarUser.from(account.account)
    for (const backend of this.backends) {
      await clear(backend, user)
    }
    const client = EventListFeature.get.forceCast(this.backends[0])
    for (const e of this.startEvents) {
      await client.createEvent(user, e)
    }
    Log.info(`Создали ${this.startEvents.length} событий у тестового пользователя`)
  }
}

export class CalendarPreparerProvider implements AccountDataPreparerProvider<CalendarPreparer> {
  constructor(private readonly backends: App[]) {}

  public provide(lockedAccount: UserAccount, _: AccountType2): CalendarPreparer {
    return new CalendarPreparer(lockedAccount.uid, this.backends)
  }

  public provideModelDownloader(
    fulfilledPreparers: CalendarPreparer[],
    accountsWithTokens: OAuthUserAccount[],
  ): AppModelProvider {
    return new CalendarDownloader(
      this.backends[0],
      accountsWithTokens.map((a) => CalendarUser.from(a.account)),
    )
  }
}

export class CalendarDownloader implements AppModelProvider {
  constructor(private readonly backend: App, private readonly lockedUsers: CalendarUser[]) {}

  async takeAppModel(): Promise<AppModel> {
    const start = Timestamps.getFirstDate()
    const end = Timestamps.getLastDate()
    const eventList = EventListFeature.get.forceCast(this.backend)
    const events = await getEvents(eventList, this.lockedUsers, start, end)
    Log.info(`В начальном состоянии модели ${events.size} событий`)
    return new CalendarModel(eventList.getEnv(), events, this.lockedUsers)
  }
}
