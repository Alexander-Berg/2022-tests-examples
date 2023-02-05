import { copyArray } from '../../../xpackages/testopithecus-common/code/utils/utils'
import { Uid, UserAccount } from '../../../xpackages/testopithecus-common/code/users/user-pool'
import { Feature } from '../../../xpackages/testopithecus-common/code/mbt/mbt-abstractions'

export type EventId = string
export type Email = string

export class CalendarUser {
  constructor(public readonly email: Email, public readonly uid: Uid) {}

  public static from(user: UserAccount): CalendarUser {
    return new CalendarUser(user.login, user.uid)
  }

  public toString(): string {
    return `CalendarUser(${this.email}, ${this.uid})`
  }
}

export class CalendarEvent {
  constructor(
    public readonly name: string,
    public readonly start: Date,
    public readonly end: Date,
    public readonly attendees: Email[] = [],
    public readonly organizer?: Email,
    public readonly layerId?: string,
    public readonly lastUpdateTs: Date = new Date(),
  ) {}

  public toString(): string {
    return `Event(${this.name}, start=${this.start}, end=${this.end}, organizer=${this.organizer}, attendees=${this.attendees}, lastUpdate=${this.lastUpdateTs})`
  }

  public isParticipant(email: Email): boolean {
    return this.getParticipants().has(email)
  }

  public getParticipants(): Set<Email> {
    const participants = new Set(this.attendees)
    const organizer = this.organizer
    if (organizer) {
      participants.add(organizer)
    }
    return participants
  }

  public equals(other: CalendarEvent): boolean {
    const eq = (t1: Date, t2: Date): boolean => Math.abs(t2.getTime() - t1.getTime()) < 60 * 1000
    const isSetsEqual = (a: Set<any>, b: Set<any>): boolean => {
      return a.size === b.size && [...a].every((value) => b.has(value))
    }
    return (
      this.name === other.name &&
      eq(this.start, other.start) &&
      eq(this.end, other.end) &&
      isSetsEqual(this.getParticipants(), other.getParticipants())
    )
  }

  public copyForUpdate(): CalendarEvent {
    return new CalendarEvent(this.name, this.start, this.end, copyArray(this.attendees), this.organizer)
  }
}

export class EventListFeature extends Feature<EventList> {
  public static readonly get: EventListFeature = new EventListFeature()

  private constructor() {
    super('EventList', 'Список евентов с добавлением/удалением')
  }
}

export enum CalendarEnv {
  publicProd,
  publicTest,
  corpProd,
  corpTest,
}

export interface EventList {
  getEnv(): CalendarEnv

  demandEvent(id: EventId, viewer: CalendarUser): Promise<CalendarEvent>

  getEvents(user: CalendarUser, from: Date, to: Date): Promise<Map<EventId, CalendarEvent>>

  createEvent(creator: CalendarUser, event: CalendarEvent, id?: EventId): Promise<EventId>

  updateEvent(performer: CalendarUser, id: EventId, newEventData: CalendarEvent): Promise<boolean>

  deleteEvent(id: EventId, viewer: CalendarUser): Promise<boolean>
}

export class SyncEventListFeature extends Feature<SyncEventList> {
  public static readonly get: SyncEventListFeature = new SyncEventListFeature()

  private constructor() {
    super('SyncEventList', 'Синхронный список евентов')
  }
}

export interface SyncEventList {
  getEventsSync(user: CalendarUser, from: Date, to: Date): Map<EventId, CalendarEvent>
}

export class EventIndexFeature extends Feature<EventIndex> {
  public static readonly get: EventIndexFeature = new EventIndexFeature()

  private constructor() {
    super('EventList', 'Список евентов с добавлением/удалением')
  }
}

export interface EventIndex {
  getEvent(id: EventId): CalendarEvent | undefined
}

export class UserListFeature extends Feature<UserList> {
  public static readonly get: UserListFeature = new UserListFeature()

  private constructor() {
    super('UserList', 'Список всех рассматриваемых аккаунтов')
  }
}

export interface UserList {
  getAllUsers(): CalendarUser[]
}

export class SyncronizationDelayFeature extends Feature<SyncronizationDelay> {
  public static readonly get: SyncronizationDelayFeature = new SyncronizationDelayFeature()

  private constructor() {
    super('SyncronizationDelay', 'Задержка при появлении событий после их создания/изменения')
  }
}

export interface SyncronizationDelay {
  getDelaySec(): number
}
