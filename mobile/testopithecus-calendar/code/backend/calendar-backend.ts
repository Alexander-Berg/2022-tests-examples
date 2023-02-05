import { Uid } from '../../../xpackages/testopithecus-common/code/users/user-pool'
import { Log } from '../../../xpackages/common/code/logging/logger'
import { App, FeatureID, FeatureRegistry } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import {
  CalendarEnv,
  CalendarEvent,
  CalendarUser,
  EventId,
  EventList,
  EventListFeature,
} from '../model/calendar-features'
import { dump } from '../model/calendar-utils'
import { CalendarClient } from './calendar-client'

export class EventListBackend implements EventList {
  private static readonly createEventAttendeesStartChunkSize = 400

  constructor(private readonly client: CalendarClient) {}

  public getEnv(): CalendarEnv {
    return this.client.env
  }

  public async getEvents(user: CalendarUser, from: Date, to: Date): Promise<Map<EventId, CalendarEvent>> {
    return await this.client.getEvents(user.uid, from, to)
  }

  public async demandEvent(id: EventId, viewer: CalendarUser): Promise<CalendarEvent> {
    const event = await this.client.getEvent(id, viewer.uid)
    if (event.organizer) {
      return event
    }
    const user = await this.client.getUserSettings(viewer.uid)
    return new CalendarEvent(
      event.name,
      event.start,
      event.end,
      event.attendees,
      user.email,
      event.layerId,
      event.lastUpdateTs,
    )
  }

  public async createEvent(creator: CalendarUser, event: CalendarEvent, _?: EventId): Promise<EventId> {
    const chunkSize = EventListBackend.createEventAttendeesStartChunkSize
    const tempEvent = event.copyForUpdate()
    tempEvent.attendees.length = Math.min(chunkSize, tempEvent.attendees.length)
    const creatorUid = creator.uid
    const id = await this.client.createEvent(creatorUid, tempEvent)
    for (let i = tempEvent.attendees.length; i < event.attendees.length; i += chunkSize) {
      Log.info(`Текущий размер чанка: ${chunkSize}`)
      tempEvent.attendees.push(...event.attendees.slice(i, i + chunkSize))
      await this.client.updateEvent(creator.uid, id, tempEvent)
    }
    return id
  }

  public async updateEvent(performer: CalendarUser, id: EventId, newEventData: CalendarEvent): Promise<boolean> {
    return await this.client.updateEvent(performer.uid, id, newEventData)
  }

  public async deleteEvent(id: EventId, viewer: CalendarUser): Promise<boolean> {
    const event = await this.demandEvent(id, viewer)
    const organizerEmail = event.organizer!
    const organizer = await this.client.getUserOrResourceInfo(viewer.uid, organizerEmail)
    try {
      return await this.client.deleteEvent(organizer.uid, id)
    } catch (e) {
      const defaultLayerId = await this.getDefaultLayer(organizer.uid)
      return await this.client.detachEvent(
        organizer.uid,
        id,
        event.layerId ? parseInt(event.layerId, 10) : defaultLayerId,
      )
    }
  }

  public async attachEvent(user: CalendarUser, id: EventId): Promise<boolean> {
    return await this.client.attachEvent(user.uid, id)
  }

  public async getDefaultLayer(uid: Uid): Promise<number> {
    const layers = await this.client.getUserLayers(uid)
    const defaultLayer = layers.filter((layer) => layer.isDefault)[0]
    if (!defaultLayer) {
      throw new Error(`У пользователя ${uid} нет дефолтного слоя!`)
    }
    return defaultLayer.id
  }
}

export class CalendarBackend implements App {
  public static readonly allSupportedFeatures: FeatureID[] = [EventListFeature.get.name]

  supportedFeatures: FeatureID[] = CalendarBackend.allSupportedFeatures

  public readonly eventList: EventListBackend

  constructor(client: CalendarClient) {
    this.eventList = new EventListBackend(client)
  }

  getFeature(feature: FeatureID): any {
    return new FeatureRegistry().register(EventListFeature.get, this.eventList).get(feature)
  }

  async dump(model: App): Promise<string> {
    return await dump(this, model)
  }
}
