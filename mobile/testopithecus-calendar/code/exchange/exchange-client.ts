import {
  Appointment,
  AppointmentSchema,
  Attendee,
  CalendarView,
  ConfigurationApi,
  ConflictResolutionMode,
  DateTime,
  DeleteMode,
  EmailAddress,
  EventType,
  ExchangeService,
  ExchangeVersion,
  FolderId,
  ItemId,
  Mailbox,
  PropertySet,
  SendCancellationsMode,
  SendInvitationsMode,
  SendInvitationsOrCancellationsMode,
  Uri,
  WebCredentials,
  WellKnownFolderName,
} from 'ews-javascript-api'
import { Log } from '../../../xpackages/common/code/logging/logger'
import { CalendarEnv, CalendarEvent, Email, EventId } from '../model/calendar-features'
import { ntlmAuthXhrApi } from 'ews-javascript-api-auth'

export class ExchangeClient {
  public static readonly testing = new ExchangeClient(
    CalendarEnv.corpTest,
    'https://amber.winadminhdsandbox.yandex.net/EWS/exchange.asmx',
    'msft\\testuser2013',
    '1111111',
    'msft.yandex-team.ru',
    false,
  )
  public static readonly production = new ExchangeClient(
    CalendarEnv.corpProd,
    'https://amber-2013.yandex-team.ru/ews/exchange.asmx',
    'ld\\invitebot2013',
    '***', // https://yav.yandex-team.ru/secret/sec-01efeed6zvg8syx6rrh6rwjwka
    'ld.yandex.ru',
    true,
  )

  private readonly ews: ExchangeService

  private constructor(
    public readonly env: CalendarEnv,
    url: string,
    private userName: string,
    private password: string,
    private readonly domain: string,
    private readonly supportsNTLM: boolean,
  ) {
    this.ews = new ExchangeService(ExchangeVersion.Exchange2010)
    this.ews.Credentials = new WebCredentials(userName, password)
    this.ews.KeepAlive = true
    this.ews.Url = new Uri(url)
    this.ews.ClientRequestId = 'testopithecus'
  }

  public activate(): ExchangeClient {
    if (this.supportsNTLM) {
      // noinspection JSPotentiallyInvalidConstructorUsage
      ConfigurationApi.ConfigureXHR(new ntlmAuthXhrApi(this.userName, this.password))
    }
    return this
  }

  public async findCalendarItems(from: Date, to: Date, email?: Email): Promise<Map<EventId, CalendarEvent>> {
    const fromm = DateTime.Parse(from.toISOString())
    const endd = DateTime.Parse(to.toISOString())
    const parentFolderId = this.getCalendarFolder(email)
    Log.info(`Exchange: FindAppointments(${parentFolderId.ToString()}, ${fromm}, ${endd})`)
    const foundItems = await this.ews.FindAppointments(parentFolderId, new CalendarView(fromm, endd))

    const result = new Map()
    for (const appointment of foundItems.Items) {
      const eventId = appointment.Id.UniqueId
      const e = this.toCalendarEvent(appointment)
      result.set(eventId, e)
    }
    return result
  }

  public async getEvent(id: EventId): Promise<CalendarEvent> {
    return this.toCalendarEvent(await this.getAppointment(id))
  }

  public async createCalendarItem(event: CalendarEvent, creatorEmail?: Email): Promise<EventId> {
    const appointment = new Appointment(this.ews)
    this.fill(event, appointment)
    const folder = this.getCalendarFolder(creatorEmail)
    Log.info(`Exchange: Save ${this.toString(appointment)} to ${folder.ToString()}`)
    await appointment.Save(folder, SendInvitationsMode.SendOnlyToAll)
    const exchangeId = appointment.Id.UniqueId
    Log.info(`Exchange: Created event id = '${exchangeId}'`)
    return exchangeId
  }

  public async updateCalendarItem(updaterEmail: Email, id: EventId, newEventData: CalendarEvent): Promise<boolean> {
    const appointment = await this.getAppointment(id, true)
    this.fill(newEventData, appointment)
    Log.info(`Exchange: Update id=${id} by ${this.toString(appointment)}`)
    try {
      await appointment.Update(
        ConflictResolutionMode.AlwaysOverwrite,
        SendInvitationsOrCancellationsMode.SendOnlyToChanged,
      )
    } catch (e) {
      Log.error(e.message)
      if (e.message.includes('Reload the item and try again')) {
        return this.updateCalendarItem(updaterEmail, id, newEventData)
      }
      return false
    }
    return true
  }

  public async deleteItem(id: EventId): Promise<boolean> {
    const appointment = await this.getAppointment(id)
    Log.info(`Exchange: Delete(${this.toString(appointment)})`)
    await appointment.Delete(DeleteMode.HardDelete, SendCancellationsMode.SendOnlyToAll)
    return true
  }

  public async subscribe(): Promise<ExchangeSubscription> {
    const folderId: FolderId = this.getCalendarFolder('calendartestuser@yandex-team.ru')
    const subscription = await this.ews.SubscribeToPullNotifications(
      [folderId],
      10,
      '',
      EventType.Created,
      EventType.Deleted,
      EventType.Modified,
      EventType.Moved,
    )
    return new ExchangeSubscription(subscription.Id, subscription.Watermark)
  }

  public async pull(subscription: ExchangeSubscription): Promise<ExchangeNotification[]> {
    const events = await this.ews.GetEvents(subscription.id, subscription.watermark)
    return events.ItemEvents.map((e) => new ExchangeNotification(e.ItemId.UniqueId, e.EventType))
  }

  private async getAppointment(id: EventId, forUpdate = false): Promise<Appointment> {
    Log.info(`Appointment.Bind(${id}, forUpdate=${forUpdate})`)
    if (!forUpdate) {
      return await Appointment.Bind(this.ews, new ItemId(id))
    }
    const propertiesToUpdate = new PropertySet(
      AppointmentSchema.Subject,
      AppointmentSchema.Start,
      AppointmentSchema.End,
      AppointmentSchema.RequiredAttendees,
    )
    return await Appointment.Bind(this.ews, new ItemId(id), propertiesToUpdate)
  }

  private fill(event: CalendarEvent, appointment: Appointment): void {
    appointment.Subject = event.name
    appointment.Start = new DateTime(event.start.getTime())
    appointment.End = new DateTime(event.end.getTime())
    appointment.RequiredAttendees.Clear()
    event.attendees.forEach((a) => appointment.RequiredAttendees.Add(a))
  }

  private toCalendarEvent(appointment: Appointment): CalendarEvent {
    const attendees: Attendee[] = [
      ...appointment.RequiredAttendees.GetEnumerator(),
      ...appointment.OptionalAttendees.GetEnumerator(),
      ...appointment.Resources.GetEnumerator(),
    ]
    return new CalendarEvent(
      appointment.Subject,
      appointment.Start.MomentDate.toDate(),
      appointment.End.MomentDate.toDate(),
      attendees.map((a) => this.getYandexEmail(a)),
      this.getYandexEmail(appointment.Organizer),
      undefined,
      appointment.LastModifiedTime.MomentDate.toDate(),
    )
  }

  private getSmtpAddress(email: Email): Email {
    return email.replace('@yandex-team.ru', `@${this.domain}`)
  }

  private getYandexEmail(attendee: EmailAddress): Email {
    if (attendee.Address.endsWith('@yandex-team.ru')) {
      return attendee.Address
    }
    const firstName = attendee.Name.split(' ')[0].toLowerCase()
    return `${firstName}@yandex-team.ru`
  }

  private getCalendarFolder(email?: Email): FolderId {
    if (email) {
      return new FolderId(WellKnownFolderName.Calendar, new Mailbox(this.getSmtpAddress(email)))
    }
    return new FolderId(WellKnownFolderName.Calendar)
  }

  private toString(appointment: Appointment): string {
    return `Appointment(subject=${appointment.Subject}, start=${appointment.Start}, end=${
      appointment.End
    }, attendees=${appointment.RequiredAttendees.GetEnumerator()})`
  }
}

export class ExchangeNotification {
  public constructor(public readonly id: string, public readonly type: EventType) {}

  public toString(): string {
    return `Notification(${this.id}, ${EventType[this.type]})`
  }
}

export class ExchangeSubscription {
  public constructor(public readonly id: string, public readonly watermark: string) {}

  public toString(): string {
    return `Subscription(${this.id}, ${this.watermark})`
  }
}
