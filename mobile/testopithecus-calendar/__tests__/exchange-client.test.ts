import { onProcessStart } from '../../xpackages/testopithecus-common/__tests__/code/test-utils'
import { valuesArray } from '../../xpackages/testopithecus-common/code/utils/utils'
import { ExchangeClient } from '../code/exchange/exchange-client'
import { CalendarEvent } from '../code/model/calendar-features'

onProcessStart()

describe('Exchange client', () => {
  const email = 'calendartestuser@yandex-team.ru'

  it('should pull events', async () => {
    const ews = ExchangeClient.testing
    const subscription = await ews.subscribe()
    console.log(subscription)
    const events = await ews.pull(subscription)
    console.log(events.length)
    for (const e of events) {
      console.log(e.toString())
    }
  })

  it('shold update event', async () => {
    const ews = ExchangeClient.testing
    const id = await ews.createCalendarItem(new CalendarEvent('pizza', new Date(), new Date()), email)
    console.log(id)
    await ews.updateCalendarItem(email, id, new CalendarEvent('pasta', new Date(), new Date()))
    const events = await ews.findCalendarItems(new Date(new Date().getTime() - 100), new Date(), email)
    console.log(valuesArray(events))
  })

  it('should get mailbox', async () => {
    const ews = ExchangeClient.production
    const items = await ews.findCalendarItems(new Date(), new Date(), 'calendartestuser@yandex-team.ru')
    console.log(items)
  })
})
