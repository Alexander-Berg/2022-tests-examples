import { Log } from '../../../xpackages/common/code/logging/logger'
import { App } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import {
  CalendarEvent,
  CalendarUser,
  EventId,
  EventList,
  EventListFeature,
  SyncEventList,
  UserListFeature,
} from './calendar-features'
import { Timestamps } from './timestamps'

export async function getEvents(
  eventList: EventList,
  users: CalendarUser[],
  from: Date,
  to: Date,
): Promise<Map<EventId, CalendarEvent>> {
  const events = new Map()
  for (const u of users) {
    ;(await eventList.getEvents(u, from, to)).forEach((e, id) => {
      events.set(id, e)
    })
  }
  return events
}

export async function dump(application: App, model: App): Promise<string> {
  const allUsers = UserListFeature.get.forceCast(model).getAllUsers()
  const eventList = EventListFeature.get.forceCast(application)
  let s = ''
  const events = await getEvents(eventList, allUsers, Timestamps.getFirstDate(), Timestamps.getLastDate())
  events.forEach((e, id) => {
    s += `id=${id} name=${e.name} start=${e.start} end=${e.end}\n`
  })
  return s
}

export async function clearAll(
  backend: App,
  users: CalendarUser[],
  from = Timestamps.getFirstDate(),
  to = Timestamps.getLastDate(),
): Promise<void> {
  for (const user of users) {
    await clear(backend, user, from, to)
  }
}

export async function clear(
  backend: App,
  user: CalendarUser,
  from = Timestamps.getFirstDate(),
  to = Timestamps.getLastDate(),
): Promise<void> {
  const client = EventListFeature.get.forceCast(backend)
  const events = await client.getEvents(user, from, to)
  Log.info(`У тестового пользователя ${events.size} событий, удаляем их`)
  for (const eventId of events.keys()) {
    if (!(await client.deleteEvent(eventId, user))) {
      throw new Error(`Не получилось удалить событие с id '${eventId}'`)
    }
  }
}

export async function findByNameAsync(
  eventList: EventList,
  user: CalendarUser,
  name: string,
): Promise<EventId | undefined> {
  const events = await eventList.getEvents(user, Timestamps.getFirstDate(), Timestamps.getLastDate())
  return findByName(events, name)
}

export function findByNameSync(eventList: SyncEventList, user: CalendarUser, name: string): EventId | undefined {
  const events = eventList.getEventsSync(user, Timestamps.getFirstDate(), Timestamps.getLastDate())
  return findByName(events, name)
}

function findByName(events: Map<EventId, CalendarEvent>, name: string): EventId | undefined {
  for (const id of events.keys()) {
    const event = events.get(id)!
    if (event.name === name) {
      return id
    }
  }
  return undefined
}
