import fs from 'fs'
import { StaffClient } from '../staff-client'
import { CalendarEnv, CalendarUser } from './calendar-features'

export class CalendarUsers {
  public static readonly calendartestuser = new CalendarUser('calendartestuser@yandex-team.ru', '1120000000004717')
  public static readonly robotMailcorp1 = new CalendarUser('robot-mailcorp-1@yandex-team.ru', '1120000000038012')
  public static readonly robotMailcorp2 = new CalendarUser('robot-mailcorp-2@yandex-team.ru', '1120000000038013')
  public static readonly robotMailcorp3 = new CalendarUser('robot-mailcorp-3@yandex-team.ru', '1120000000038014')
  public static readonly robotMailcorp4 = new CalendarUser('robot-mailcorp-4@yandex-team.ru', '1120000000038015')
  public static readonly robotMailcorp5 = new CalendarUser('robot-mailcorp-5@yandex-team.ru', '1120000000038016')
  public static readonly robotMailcorp6 = new CalendarUser('robot-mailcorp-6@yandex-team.ru', '1120000000038017')
  public static readonly robotMailcorp7 = new CalendarUser('robot-mailcorp-7@yandex-team.ru', '1120000000038018')
  public static readonly robotMailcorp8 = new CalendarUser('robot-mailcorp-8@yandex-team.ru', '1120000000038019')

  public static readonly mailings = {
    'robot-mailcorp-3-4-5@yandex-team.ru': [
      CalendarUsers.robotMailcorp3,
      CalendarUsers.robotMailcorp4,
      CalendarUsers.robotMailcorp5,
    ],
    'robot-mailcorp-4-5-6@yandex-team.ru': [
      CalendarUsers.robotMailcorp4,
      CalendarUsers.robotMailcorp5,
      CalendarUsers.robotMailcorp6,
    ],
  }

  public static readonly ewsers = [
    CalendarUsers.calendartestuser,
    CalendarUsers.robotMailcorp1,
    CalendarUsers.robotMailcorp2,
    CalendarUsers.robotMailcorp3,
  ]
  public static readonly notEwsers = [
    CalendarUsers.robotMailcorp4,
    CalendarUsers.robotMailcorp5,
    CalendarUsers.robotMailcorp6,
    CalendarUsers.robotMailcorp7,
    CalendarUsers.robotMailcorp8,
  ]
  public static readonly corpAll = [...CalendarUsers.ewsers, ...CalendarUsers.notEwsers]

  public static readonly other = [
    new CalendarUser('pistch@yandex-team.ru', '1120000000089819'),
    new CalendarUser('olga-ganchikova@yandex-team.ru', '1120000000005899'),
  ]

  public static publicAll(): CalendarUser[] {
    return fs
      .readFileSync(`${__dirname}/accs_new.txt`)
      .toString()
      .trim()
      .split('\n')
      .map((lines) => {
        const [uid, login, _] = lines.split(' ')
        return new CalendarUser(login + '@yandex.ru', uid)
      })
  }

  public static async corp(limit: number): Promise<CalendarUser[]> {
    const total = 38961
    const page = Math.round(total / limit) - 1
    const persons = await new StaffClient().persons(page, limit)
    return persons.map((p) => new CalendarUser(p.getEmail(), p.uid))
  }

  public static all(env: CalendarEnv): CalendarUser[] {
    return env === CalendarEnv.corpTest || env === CalendarEnv.corpProd ? this.corpAll : this.publicAll()
  }

  public static byEmail(email: string): CalendarUser | undefined {
    return [...this.publicAll(), ...this.corpAll, ...this.other].filter((u) => u.email === email)[0]
  }

  private constructor() {}
}
