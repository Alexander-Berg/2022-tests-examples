import { onProcessStart } from '../../xpackages/testopithecus-common/__tests__/code/test-utils'
import { CalendarClient } from '../code/backend/calendar-client'
import { CalendarEvent } from '../code/model/calendar-features'
import * as assert from 'assert'

onProcessStart()

describe('calendar client', () => {
  it('should delete event', async () => {
    const client = CalendarClient.corpTest
    const amosovF = '1120000000022901'
    const id = await client.createEvent(
      amosovF,
      new CalendarEvent('pizza', new Date(), new Date(), ['amosov-f@yandex-team.ru']),
    )
    const updated = await client.updateEvent(amosovF, id, new CalendarEvent('222', new Date(), new Date(), []))
    assert.strictEqual(updated, true)
    const eventData = await client.getEvent(id, amosovF)
    assert.strictEqual(eventData?.name, '222')
    assert.strictEqual(await client.deleteEvent(amosovF, id), true)
  })

  it('should find users and resources', async () => {
    const client = CalendarClient.publicProd
    const resp = await client.findUsersAndResources('1120000000158732', ['anastasia_b@upside.pro'])
    console.log(resp)
  })

  it('should update layer', async () => {
    const client = CalendarClient.corpProd
    const resp = await client.updateLayer('1120000000094850', 97827, true)
    console.log(resp)
  })

  it('should get email by uid', async () => {
    const settings = await CalendarClient.publicTest.getUserSettings('1100154853')
    console.log(settings)
  })
})
