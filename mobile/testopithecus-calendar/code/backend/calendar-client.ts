import { TvmClient } from 'alice/code/tvm-client'
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios'
import { Log } from '../../../xpackages/common/code/logging/logger'
import { Uid } from '../../../xpackages/testopithecus-common/code/users/user-pool'
import { CalendarEnv, CalendarEvent, CalendarUser, Email, EventId } from '../model/calendar-features'

export class CalendarClient {
  public static readonly corpTest = new CalendarClient(
    CalendarEnv.corpTest,
    'https://calendar-api.testing.yandex-team.ru',
    2011068,
  ) // tvmtool.conf https://yav.yandex-team.ru/secret/sec-01eay90w7qqw9eq9hg9khz9928
  public static readonly publicTest = new CalendarClient(
    CalendarEnv.publicTest,
    'https://calendar-api.testing.yandex.ru',
    2011056,
  ) // tvmtool.conf https://yav.yandex-team.ru/secret/sec-01ebnzevt4dnx53ewnv0sewmrc
  public static readonly corpProd = new CalendarClient(
    CalendarEnv.corpProd,
    'https://calendar-api.tools.yandex.net', // 'http://iva8-d6ef645252a8.qloud-c.yandex.net:22296',
    2011072,
  ) // tvmtool.conf https://yav.yandex-team.ru/secret/sec-01ecq2kf9vaph8m28kftaawand
  public static readonly publicProd = new CalendarClient(
    CalendarEnv.publicProd,
    'https://calendar-api.yandex.net',
    2011066,
  ) // tvmtool.conf https://yav.yandex-team.ru/secret/sec-01ecq52d7v500q5w65frm9tpfq

  private readonly axios: AxiosInstance
  private readonly tvmClient = TvmClient.local

  private constructor(
    public readonly env: CalendarEnv,
    host: string,
    private readonly tvmId: number,
    public maxRetries = 5,
  ) {
    this.axios = axios.create({
      baseURL: `${host}/internal`,
    })
    this.axios.interceptors.response.use(
      (r) => r,
      (e) => {
        if (e.response) {
          console.error(`Ошибка в теле ответа: ${JSON.stringify(e.response.data)}`)
        } else {
          console.error(`Ошибка: ${e.message}`)
        }
        return Promise.reject(e)
      },
    )
  }

  public getEnv(): CalendarEnv {
    return this.env
  }

  public async getEvent(id: EventId, uid: Uid): Promise<CalendarEvent> {
    const data = await this.get(`/get-event?eventId=${id}&uid=${uid}`)
    return this.parseEvent(data)
  }

  public async getEvents(uid: Uid, from: Date, to: Date): Promise<Map<EventId, CalendarEvent>> {
    const fromm = this.toMoscowISO(from)
    const too = this.toMoscowISO(to)
    const data = await this.get(`/get-events?uid=${uid}&from=${fromm}&to=${too}`)
    if (!data.events) {
      throw new Error(`В ответе нет списка событий, ответ: ${JSON.stringify(data)}`)
    }
    const result = new Map()
    for (const e of data.events) {
      result.set(`${e.id}`, this.parseEvent(e))
    }
    Log.info(`Скачены события: ${Array.from(result.keys())}`)
    return result
  }

  public async createEvent(uid: Uid, event: CalendarEvent): Promise<EventId> {
    const data = await this.post(`/create-event?uid=${uid}`, this.toJsonBody(event))
    const showEventId = data.showEventId
    if (showEventId) {
      Log.info(`Backend: Created event id = ${showEventId}`)
      return `${showEventId}`
    }
    throw new Error(`Не получилось создать событие, причина: ${JSON.stringify(data)}`)
  }

  public async updateEvent(uid: Uid, id: EventId, newEventData: CalendarEvent): Promise<boolean> {
    const data = await this.post(`/update-event?uid=${uid}&id=${id}`, this.toJsonBody(newEventData))
    const updated = !!data.showEventId
    if (!updated) {
      Log.warn(`Событие не обновлено, ответ сервера: ${JSON.stringify(data)}`)
    }
    return updated
  }

  public async deleteEvent(uid: Uid, id: EventId): Promise<boolean> {
    const data = await this.post(`/delete-event?uid=${uid}&id=${id}`, {})
    if (data.status === 'ok') {
      Log.info(`Событие ${id} удалено с бэка календаря`)
      return true
    }
    Log.warn(`Не получилось удалить событие у пользователя ${uid} с id ${id}, причина: ${JSON.stringify(data)}`)
    return false
  }

  public async getUserLayers(uid: Uid): Promise<CalendarLayer[]> {
    const data = await this.post(`/get-user-layers?uid=${uid}`, {})
    return data.layers.map((layer: any) => new CalendarLayer(layer.id, layer.name, layer.isDefault))
  }

  public async attachEvent(uid: Uid, id: EventId): Promise<boolean> {
    const data = await this.post(`/attach-event?uid=${uid}&id=${id}`, {})
    if (data.status === 'ok') {
      Log.info(`Пользователь ${uid} успешно добавил себе событие ${id}`)
      return true
    }
    Log.warn(`Не получилось добавить событие ${id} пользователю ${uid}, причина: ${data.error.message}`)
    return false
  }

  public async detachEvent(uid: Uid, id: EventId, layerId: number): Promise<boolean> {
    const data = await this.post(`/detach-event?uid=${uid}&id=${id}&layerId=${layerId}`, {})
    if (data.status === 'ok') {
      Log.info(`Событие ${id} удалено из календаря ${layerId} пользователя ${uid}`)
      return true
    }
    const cause = JSON.stringify(data)
    Log.warn(`Никак не удалить событие из календаря ${layerId} у пользователя ${uid} с id ${id}, причина: ${cause}`)
    return false
  }

  public async findUsersAndResources(uid: Uid, loginOrEmails: string[]): Promise<any> {
    return await this.get(`/find-users-and-resources?uid=${uid}&loginOrEmails=${loginOrEmails.join(',')}`)
  }

  public async updateLayer(uid: Uid, layerId: number, applyNotificationsToEvents: boolean): Promise<any> {
    return await this.post(
      `/update-layer?uid=${uid}&id=${layerId}&applyNotificationsToEvents=${applyNotificationsToEvents}`,
      { notifications: [] },
    )
  }

  public async getUserSettings(uid: Uid): Promise<CalendarUser> {
    const data = await this.get(`/get-user-settings?uid=${uid}`)
    return new CalendarUser(data.email, data.uid)
  }

  public async getUserOrResourceInfo(viewerUid: Uid, email: Email): Promise<CalendarUser> {
    const data = await this.get(`/get-user-or-resource-info?uid=${viewerUid}&email=${email}`)
    return new CalendarUser(data.email, `${data.uid}`)
  }

  public async getResourcesSchedule(uid: Uid, officeId: number, from: Date, to: Date): Promise<any> {
    const fromm = this.toMoscowISO(from)
    const too = this.toMoscowISO(to)
    return await this.get(`/get-resources-schedule?uid=${uid}&officeId=${officeId}&from=${fromm}&to=${too}`)
  }

  private async get(url: string, retries = this.maxRetries): Promise<any> {
    Log.info(`GET ${url}`)
    const resp = await this.axios.get(url, await this.provideConfig())
    if (!resp.data.error) {
      return resp.data
    }
    const msg = `Бэк ответил ошибкой: ${JSON.stringify(resp.data)}, осталось попыток: ${retries}`
    if (retries > 0) {
      // TODO: fix GREG-917
      Log.error(msg)
      return await this.get(url, retries - 1)
    }
    throw new Error(msg)
  }

  private async post(url: string, data: any): Promise<any> {
    Log.info(`POST ${url} -d ${JSON.stringify(data)}`)
    const resp = await this.axios.post(url, data, await this.provideConfig())
    if (resp.data.error) {
      throw new Error(`Бэк ответил ошибкой: ${JSON.stringify(resp.data)}`)
    }
    return resp.data
  }

  private async provideConfig(): Promise<AxiosRequestConfig> {
    return {
      headers: {
        'X-Ya-Service-Ticket': await this.getServiceTicket(),
      },
    }
  }

  private async getServiceTicket(): Promise<string> {
    return await this.tvmClient.getServiceTicket([this.tvmId], this.tvmId)
  }

  private parseEvent(json: any): CalendarEvent {
    const attendees = json.attendees.map((a: any) => a.email)
    return new CalendarEvent(json.name, new Date(json.startTs), new Date(json.endTs), attendees, json.organizer?.email)
  }

  private toJsonBody(event: CalendarEvent): any {
    return {
      type: 'user',
      startTs: this.toMoscowISO(event.start),
      endTs: this.toMoscowISO(event.end),
      name: event.name,
      availability: 'available',
      attendees: event.attendees,
      participantsCanEdit: true,
    }
  }

  private toMoscowISO(d: Date): string {
    const s = new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString()
    return s.slice(0, s.length - 5)
  }
}

export class CalendarLayer {
  constructor(public readonly id: number, public readonly name: string, public readonly isDefault: boolean) {}
}
