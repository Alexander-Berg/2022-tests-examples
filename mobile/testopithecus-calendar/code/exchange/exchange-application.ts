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
import { ExchangeClient } from './exchange-client'

export class ExchangeEventList implements EventList {
  constructor(private readonly client: ExchangeClient) {
    this.client = client
  }

  public getEnv(): CalendarEnv {
    return this.client.env
  }

  public async demandEvent(id: EventId, _: CalendarUser): Promise<CalendarEvent> {
    return await this.client.getEvent(id)
  }

  public async getEvents(user: CalendarUser, from: Date, to: Date): Promise<Map<EventId, CalendarEvent>> {
    return await this.client.findCalendarItems(from, to, user.email)
  }

  public async createEvent(creator: CalendarUser, event: CalendarEvent, _?: EventId): Promise<EventId> {
    return await this.client.createCalendarItem(event, creator.email)
  }

  public async updateEvent(performer: CalendarUser, id: EventId, newEventData: CalendarEvent): Promise<boolean> {
    return await this.client.updateCalendarItem(performer.email, id, newEventData)
  }

  public async deleteEvent(id: EventId, _: CalendarUser): Promise<boolean> {
    return await this.client.deleteItem(id)
  }
}

export class ExchangeApplication implements App {
  public static readonly allSupportedFeatures: FeatureID[] = [EventListFeature.get.name]

  supportedFeatures: FeatureID[] = ExchangeApplication.allSupportedFeatures

  public readonly eventList: ExchangeEventList

  constructor(client: ExchangeClient) {
    this.eventList = new ExchangeEventList(client)
  }

  getFeature(feature: FeatureID): any {
    return new FeatureRegistry().register(EventListFeature.get, this.eventList).get(feature)
  }

  public async dump(model: App): Promise<string> {
    return await dump(this, model)
  }
}
