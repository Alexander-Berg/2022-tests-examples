import { Throwing, YSError } from '../../../common/ys'
import { Log } from '../../../xpackages/common/code/logging/logger'
import { EventusEvent } from '../../../xpackages/eventus-common/code/eventus-event'
import {
  App,
  FeatureID,
  MBTAction,
  MBTActionType,
  MBTComponent,
  MBTHistory,
} from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'
import { MBTComponentActions } from '../../../xpackages/testopithecus-common/code/mbt/walk/behaviour/user-behaviour'
import { assertBooleanEquals } from '../../../xpackages/testopithecus-common/code/utils/assert'
import {
  CalendarEvent,
  CalendarUser,
  EventId,
  EventIndexFeature,
  EventListFeature,
  SyncEventListFeature,
  UserListFeature,
} from './calendar-features'
import { CalendarUsers } from './calendar-users'
import { findByNameAsync, findByNameSync } from './calendar-utils'
import { Timestamps } from './timestamps'

export class CreateEventAction implements MBTAction {
  public static readonly type: MBTActionType = 'CreateEvent'

  constructor(private readonly creator: CalendarUser, private readonly event: CalendarEvent) {}

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return CreateEventAction.type
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return EventListFeature.get.includedAll(modelFeatures, applicationFeatures)
  }

  public canBePerformed(_: App): boolean {
    return true
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const realId = await EventListFeature.get.forceCast(application).createEvent(this.creator, this.event)
    await EventListFeature.get.forceCast(model).createEvent(this.creator, this.event, realId)
    return history.currentComponent
  }

  tostring(): string {
    return `CreateEventAction(${this.event.toString()})`
  }
}

export class DeleteEventAction implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteEvent'

  constructor(private readonly performer: CalendarUser, private readonly id: EventId) {}

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return DeleteEventAction.type
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      SyncEventListFeature.get.included(modelFeatures) &&
      EventListFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): boolean {
    return EventIndexFeature.get.forceCast(model).getEvent(this.id) !== null
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const deletedFromModel = await EventListFeature.get.forceCast(model).deleteEvent(this.id, this.performer)
    const deletedFromApplication = await EventListFeature.get
      .forceCast(application)
      .deleteEvent(this.id, this.performer)
    assertBooleanEquals(
      deletedFromModel,
      deletedFromApplication,
      'Событие должно быть и там и там удалено или не удалено',
    )
    return history.currentComponent
  }

  tostring(): string {
    return `DeleteEvent(${this.id})`
  }
}

export class DeleteEventByNameAction implements MBTAction {
  public static readonly type: MBTActionType = 'DeleteEventByName'

  constructor(private readonly performer: CalendarUser, private readonly name: string) {}

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return DeleteEventByNameAction.type
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      SyncEventListFeature.get.included(modelFeatures) &&
      EventListFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): boolean {
    return !!findByNameSync(SyncEventListFeature.get.forceCast(model), this.performer, this.name)
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelId = findByNameSync(SyncEventListFeature.get.forceCast(model), this.performer, this.name)!
    const applicationEventList = EventListFeature.get.forceCast(application)
    const applicationId = await findByNameAsync(applicationEventList, this.performer, this.name)
    if (!applicationId) {
      Log.warn(`События с именем ${this.name} нет в приложении, пропускаем действие`)
      return history.currentComponent
    }
    const deletedFromModel = await EventListFeature.get.forceCast(model).deleteEvent(modelId, this.performer)
    const deletedFromApplication = await applicationEventList.deleteEvent(applicationId, this.performer)
    assertBooleanEquals(
      deletedFromModel,
      deletedFromApplication,
      'Событие должно быть и там и там удалено или не удалено',
    )
    return history.currentComponent
  }

  tostring(): string {
    return `DeleteEventByName(${this.name},)`
  }
}

export class EditEventAction implements MBTAction {
  public static readonly type: MBTActionType = 'EditEvent'

  constructor(
    private readonly performer: CalendarUser,
    private readonly id: EventId,
    private readonly newEventData: CalendarEvent,
  ) {}

  public events(): EventusEvent[] {
    return []
  }

  public getActionType(): MBTActionType {
    return EditEventAction.type
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      EventIndexFeature.get.included(modelFeatures) &&
      EventListFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): boolean {
    return EventIndexFeature.get.forceCast(model).getEvent(this.id) !== null
  }

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const updateInModel = await EventListFeature.get
      .forceCast(model)
      .updateEvent(this.performer, this.id, this.newEventData)
    const updateInApplication = await EventListFeature.get
      .forceCast(application)
      .updateEvent(this.performer, this.id, this.newEventData)
    assertBooleanEquals(updateInModel, updateInApplication, 'Событие должно быть и там и там изменено')
    return history.currentComponent
  }

  tostring(): string {
    return `EditEvent(${this.id})`
  }
}

export abstract class EditEventByNameActionBase implements MBTAction {
  protected constructor(protected readonly performer: CalendarUser, protected readonly eventName: string) {}

  public events(): EventusEvent[] {
    return []
  }

  public supported(modelFeatures: FeatureID[], applicationFeatures: FeatureID[]): boolean {
    return (
      SyncEventListFeature.get.included(modelFeatures) &&
      EventListFeature.get.includedAll(modelFeatures, applicationFeatures)
    )
  }

  public canBePerformed(model: App): boolean {
    const eventId = findByNameSync(SyncEventListFeature.get.forceCast(model), this.performer, this.eventName)
    if (!eventId) {
      return false
    }
    const event = EventIndexFeature.get.forceCast(model).getEvent(eventId)!
    return this.canBePerformedImpl(event)
  }

  // eslint-disable-next-line
  public canBePerformedImpl(event: CalendarEvent): boolean {
    return true
  }

  public abstract updateNewEventData(newEventData: CalendarEvent): CalendarEvent

  public async perform(model: App, application: App, history: MBTHistory): Throwing<Promise<MBTComponent>> {
    const modelId = findByNameSync(SyncEventListFeature.get.forceCast(model), this.performer, this.eventName)!
    const event = EventIndexFeature.get.forceCast(model).getEvent(modelId)!
    const applicationEventList = EventListFeature.get.forceCast(application)
    const applicationId = await findByNameAsync(applicationEventList, this.performer, this.eventName)
    if (!applicationId) {
      Log.warn(`События с именем ${this.eventName} нет в приложении, пропускаем действие`)
      return history.currentComponent
    }
    const newEventData = this.updateNewEventData(event.copyForUpdate())
    const updateInModel = await EventListFeature.get.forceCast(model).updateEvent(this.performer, modelId, newEventData)
    const updateInApplication = await applicationEventList.updateEvent(this.performer, applicationId, newEventData)
    assertBooleanEquals(updateInModel, updateInApplication, 'Событие должно быть и там и там изменено')
    return history.currentComponent
  }

  abstract tostring(): string

  abstract getActionType(): MBTActionType
}

export class EditEventByNameAction extends EditEventByNameActionBase {
  public static readonly type: MBTActionType = 'EditEventByName'

  constructor(
    performer: CalendarUser,
    name: string,
    private readonly newName?: string,
    private readonly newStart?: Date,
    private readonly newEnd?: Date,
  ) {
    super(performer, name)
  }

  updateNewEventData(newEventData: CalendarEvent): CalendarEvent {
    return new CalendarEvent(
      this.newName ?? newEventData.name,
      this.newStart ?? newEventData.start,
      this.newEnd ?? newEventData.end,
      newEventData.attendees,
      newEventData.organizer,
      newEventData.layerId,
    )
  }

  getActionType(): MBTActionType {
    return EditEventByNameAction.type
  }

  tostring(): string {
    return `EditEventByName(${this.eventName}, name=${this.newName}, start=${this.newStart}, end=${this.newEnd})`
  }
}

export class AddAttendeeByEventNameAction extends EditEventByNameActionBase {
  public static readonly type: MBTActionType = 'AddAttendeeByEventName'

  constructor(performer: CalendarUser, name: string, private readonly attendeeEmail: string) {
    super(performer, name)
  }

  public getActionType(): MBTActionType {
    return AddAttendeeByEventNameAction.type
  }

  canBePerformedImpl(event: CalendarEvent): boolean {
    return !event.attendees.includes(this.attendeeEmail) && event.organizer !== this.attendeeEmail
  }

  updateNewEventData(newEventData: CalendarEvent): CalendarEvent {
    newEventData.attendees.push(this.attendeeEmail)
    return newEventData
  }

  tostring(): string {
    return `AddEventByName(${this.eventName}, ${this.attendeeEmail})`
  }
}

export class DetachEventByNameAction extends EditEventByNameActionBase {
  public static readonly type: MBTActionType = 'DetachEventByName'

  constructor(performer: CalendarUser, eventName: string, private readonly attendeeEmail: string) {
    super(performer, eventName)
  }

  public getActionType(): MBTActionType {
    return DetachEventByNameAction.type
  }

  canBePerformedImpl(event: CalendarEvent): boolean {
    return event.attendees.includes(this.attendeeEmail)
  }

  updateNewEventData(newEventData: CalendarEvent): CalendarEvent {
    const i = newEventData.attendees.indexOf(this.attendeeEmail)
    if (i < 0) {
      throw new YSError(`Участника ${this.attendeeEmail} нет в списке, хотя должен быть!`)
    }
    newEventData.attendees.splice(i, 1)
    return newEventData
  }

  tostring(): string {
    return `DetachEventByName(${this.eventName}, ${this.attendeeEmail})`
  }
}

export class EventListActions implements MBTComponentActions {
  private counter = 0

  constructor(
    private readonly maxEvents: number,
    private readonly allowEmptyEvents: boolean,
    private readonly allowAttendeeDetach: boolean,
  ) {}

  public getActions(model: App): MBTAction[] {
    const users = UserListFeature.get.forceCast(model)
    const eventList = EventListFeature.get.forceCast(model)
    const syncEventList = SyncEventListFeature.get.forceCast(model)
    const actions = []
    for (const performer of users.getAllUsers()) {
      const [randomStart, randomEnd] = Timestamps.randomInterval(this.allowEmptyEvents)
      actions.push(new CreateEventAction(performer, new CalendarEvent(`${this.counter}`, randomStart, randomEnd)))
      const events = syncEventList.getEventsSync(performer, Timestamps.getFirstDate(), Timestamps.getLastDate())
      if (events.size > this.maxEvents) {
        for (const e of events.values()) {
          actions.push(new EditEventByNameAction(performer, e.name, `${this.counter}`, randomStart, randomEnd))
          actions.push(new DeleteEventByNameAction(performer, e.name))
          if (this.allowAttendeeDetach) {
            for (const email of e.attendees) {
              actions.push(new DetachEventByNameAction(performer, e.name, email))
            }
          }
          for (const possibleAttendee of CalendarUsers.all(eventList.getEnv())) {
            actions.push(new AddAttendeeByEventNameAction(performer, e.name, possibleAttendee.email))
          }
          // eslint-disable-next-line
          for (const mailing in CalendarUsers.mailings) {
            actions.push(new AddAttendeeByEventNameAction(performer, e.name, mailing))
          }
        }
      }
      this.counter += 1
    }
    return actions
  }
}
