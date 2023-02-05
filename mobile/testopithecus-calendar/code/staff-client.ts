import { TvmClient } from 'alice/code/tvm-client'
import { Uid } from '../../xpackages/testopithecus-common/code/users/user-pool'
import axios, { AxiosInstance } from 'axios'

export class StaffClient {
  private readonly axios: AxiosInstance
  private readonly tvmClient = TvmClient.local

  constructor() {
    this.axios = axios.create({
      baseURL: 'https://staff-api.yandex-team.ru/v3',
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

  public async persons(page: number, limit: number): Promise<Person[]> {
    const serviceTicket = await this.getServiceTicket()
    const resp = await this.axios.get(`/persons?_page=${page}&_limit=${limit}&official.is_dismissed=false`, {
      headers: {
        'X-Ya-Service-Ticket': serviceTicket,
      },
    })
    return resp.data.result.map((el: any) => new Person(el.login, el.uid))
  }

  private async getServiceTicket(): Promise<string> {
    return await this.tvmClient.getServiceTicket([2001974], 2011068)
  }
}

export class Person {
  constructor(public readonly login: string, public readonly uid: Uid) {}

  public getEmail(): string {
    return `${this.login}@yandex-team.ru`
  }
}
