import { Log } from '../../../xpackages/common/code/logging/logger'
import { valuesArray } from '../../../xpackages/testopithecus-common/code/utils/utils'
import { Throwing, YSError } from '../../../common/ys'
import { App, MBTComponent, MBTComponentType } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import {
  CalendarEvent,
  CalendarUser,
  EventList,
  EventListFeature,
  SyncronizationDelayFeature,
  UserListFeature,
} from './calendar-features'
import { findByNameAsync } from './calendar-utils'
import { Timestamps } from './timestamps'

export class CalendarComponent implements MBTComponent {
  public static type: MBTComponentType = 'Calendar'

  public async assertMatches(model: App, application: App): Throwing<Promise<void>> {
    const start = Timestamps.getFirstDate()
    const finish = Timestamps.getLastDate()
    const expectedEventList: EventList = EventListFeature.get.forceCast(model)
    const actualEventList: EventList = EventListFeature.get.forceCast(application)
    const allUsers = UserListFeature.get.forceCast(model).getAllUsers()
    for (const user of allUsers) {
      const modelEvents = await expectedEventList.getEvents(user, start, finish)
      const actualEvents = await actualEventList.getEvents(user, start, finish)
      const syncDelaySec = SyncronizationDelayFeature.get.castIfSupported(application)?.getDelaySec() ?? 0
      const now = new Date()
      const shouldBeSyncronized = (e: CalendarEvent): boolean => {
        return now.getTime() - e.lastUpdateTs.getTime() > syncDelaySec * 1000
      }
      let modelInSync = 0
      for (const id of modelEvents.keys()) {
        const e = modelEvents.get(id)!
        if (shouldBeSyncronized(e)) {
          if (!valuesArray(actualEvents).find((ae) => ae.equals(e))) {
            Log.warn(`События нет на бэке, но оно уже есть в модели: ${e.toString()}`)
            if (!(await this.waitForEventExistance(e, actualEventList, user, 100))) {
              throw new YSError(
                `Событие id=${id} ${e.toString()} уже должно было синхронизироваться, а его все еще нет (сейчас ${now})`,
              )
            }
          } else {
            modelInSync += 1
          }
        }
      }
      Log.info(`${modelInSync} событий из модели есть на бэке`)
      let backInSync = 0
      for (const id of actualEvents.keys()) {
        const e = actualEvents.get(id)!
        if (!valuesArray(modelEvents).find((me) => me.name === e.name)) {
          if (shouldBeSyncronized(e)) {
            Log.warn(`События нет в модели, но оно еще не удалено на бэке: ${e.toString()}`)
            if (!(await this.waitForEventAbsence(e, actualEventList, user, 100))) {
              throw new YSError(`Событие id=${id} ${e.toString()} есть, но его уже не должно быть (сейчас ${now})`)
            }
          }
        } else {
          backInSync += 1
        }
      }
      Log.info(`${backInSync} событий с бэка есть в модели`)
    }
  }

  getComponentType(): MBTComponentType {
    return CalendarComponent.type
  }

  tostring(): string {
    return 'CalendarComponent'
  }

  private async waitForEventExistance(
    expectedEvent: CalendarEvent,
    eventList: EventList,
    user: CalendarUser,
    tries: number,
  ): Promise<boolean> {
    Log.warn(`Начинаем ждать событие '${expectedEvent.toString()}' за ${tries} скачиваний календаря`)
    for (let i = 0; i < tries; i++) {
      const eventId = await findByNameAsync(eventList, user, expectedEvent.name)
      if (eventId) {
        const actualEvent = await eventList.demandEvent(eventId, user)
        if (expectedEvent.equals(actualEvent)) {
          return true
        }
        Log.warn(
          `Событие ${eventId} еще не засинкалось. 
           Ожидание: ${expectedEvent.toString()}
           Реальнть: ${actualEvent.toString()}`,
        )
      }
    }
    Log.error(`За ${tries} скачиваний календаря событие '${expectedEvent.toString()}' так и нет`)
    return false
  }

  private async waitForEventAbsence(
    expectedEvent: CalendarEvent,
    eventList: EventList,
    user: CalendarUser,
    tries: number,
  ): Promise<boolean> {
    Log.warn(`Начинаем ждать отсутствие события '${expectedEvent.toString()}' за ${tries} скачиваний календаря`)
    for (let i = 0; i < tries; i++) {
      const eventId = await findByNameAsync(eventList, user, expectedEvent.name)
      if (!eventId) {
        return true
      }
    }
    Log.error(`За ${tries} скачиваний календаря событие '${expectedEvent.toString()}' все еще есть`)
    return false
  }
}
