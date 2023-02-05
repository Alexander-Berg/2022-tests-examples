import { copyArray, copyMap } from '../../../xpackages/testopithecus-common/code/utils/utils'
import { App, FeatureID, FeatureRegistry } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import { AppModel } from '../../../xpackages/testopithecus-common/code/mbt/walk/fixed-scenario-strategy'
import { int64, Int64 } from '../../../common/ys'
import {
  CalendarEnv,
  CalendarEvent,
  CalendarUser,
  EventId,
  EventIndex,
  EventIndexFeature,
  EventList,
  EventListFeature,
  SyncEventList,
  SyncEventListFeature,
  UserList,
  UserListFeature,
} from './calendar-features'
import { dump } from './calendar-utils'

export class EventListModel implements EventList, SyncEventList, EventIndex, UserList {
  constructor(
    private readonly env: CalendarEnv,
    public readonly events: Map<EventId, CalendarEvent>,
    public readonly lockedUsers: CalendarUser[],
  ) {}

  public getEnv(): CalendarEnv {
    return this.env
  }

  public getEvent(id: EventId): CalendarEvent | undefined {
    return this.events.get(id)
  }

  public async demandEvent(id: EventId, _: CalendarUser): Promise<CalendarEvent> {
    return this.getEvent(id)!
  }

  public getEventsSync(user: CalendarUser, from: Date, to: Date): Map<EventId, CalendarEvent> {
    const result = new Map()
    const isBetween = (t: Date): boolean => from <= t && t <= to
    this.events.forEach((e, id) => {
      if (e.isParticipant(user.email) && (isBetween(e.start) || isBetween(e.end))) {
        result.set(id, e)
      }
    })
    return result
  }

  public async getEvents(user: CalendarUser, from: Date, to: Date): Promise<Map<EventId, CalendarEvent>> {
    return this.getEventsSync(user, from, to)
  }

  public async createEvent(creator: CalendarUser, event: CalendarEvent, id?: EventId): Promise<EventId> {
    const eventId = id ?? `${this.events.size}`
    this.events.set(
      eventId,
      new CalendarEvent(
        event.name,
        event.start,
        event.end,
        event.attendees,
        creator.email,
        event.layerId,
        event.lastUpdateTs,
      ),
    )
    return eventId
  }

  public async updateEvent(performer: CalendarUser, id: EventId, newEventData: CalendarEvent): Promise<boolean> {
    if (!this.events.has(id)) {
      return false
    }
    this.events.set(id, newEventData)
    return true
  }

  public async deleteEvent(id: EventId, _: CalendarUser): Promise<boolean> {
    return this.events.delete(id)
  }

  public getAllUsers(): CalendarUser[] {
    return this.lockedUsers
  }
}

export class CalendarModel implements AppModel {
  public static readonly allSupportedFeatures: FeatureID[] = [
    EventListFeature.get.name,
    SyncEventListFeature.get.name,
    EventIndexFeature.get.name,
    UserListFeature.get.name,
  ]

  supportedFeatures: FeatureID[] = CalendarModel.allSupportedFeatures

  private readonly eventList: EventListModel

  constructor(env: CalendarEnv, events: Map<EventId, CalendarEvent>, lockedUsers: CalendarUser[]) {
    this.eventList = new EventListModel(env, events, lockedUsers)
  }

  copy(): AppModel {
    return new CalendarModel(
      this.eventList.getEnv(),
      copyMap(this.eventList.events),
      copyArray(this.eventList.lockedUsers),
    )
  }

  getCurrentStateHash(): Int64 {
    return int64(0)
  }

  async dump(model: App): Promise<string> {
    return dump(model, model)
  }

  getFeature(feature: FeatureID): any {
    return new FeatureRegistry()
      .register(EventListFeature.get, this.eventList)
      .register(SyncEventListFeature.get, this.eventList)
      .register(EventIndexFeature.get, this.eventList)
      .register(UserListFeature.get, this.eventList)
      .get(feature)
  }
}
