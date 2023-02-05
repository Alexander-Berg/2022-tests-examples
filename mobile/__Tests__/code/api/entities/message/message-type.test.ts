import { Int32 } from '../../../../../../../common/ys'
import {
  MessageTypeFlags,
  messageTypeMaskFromServerMessageTypes,
} from '../../../../../code/api/entities/message/message-type'

// tslint:disable: no-bitwise

describe(MessageTypeFlags, () => {
  it('should be convertible from server type', () => {
    const values = {
      1: MessageTypeFlags.delivery,
      2: MessageTypeFlags.registration,
      3: MessageTypeFlags.social,
      4: MessageTypeFlags.people,
      5: MessageTypeFlags.eticket,
      6: MessageTypeFlags.eshop,
      7: MessageTypeFlags.notification,
      8: MessageTypeFlags.bounce,
      9: MessageTypeFlags.official,
      10: MessageTypeFlags.script,
      11: MessageTypeFlags.dating,
      12: MessageTypeFlags.greeting,
      13: MessageTypeFlags.news,
      14: MessageTypeFlags.sGrouponsite,
      15: MessageTypeFlags.sDatingsite,
      16: MessageTypeFlags.sETicket,
      17: MessageTypeFlags.sBank,
      18: MessageTypeFlags.sSocial,
      19: MessageTypeFlags.sTravel,
      20: MessageTypeFlags.sZDTicket,
      21: MessageTypeFlags.sRealty,
      23: MessageTypeFlags.sEShop,
      24: MessageTypeFlags.sCompany,
      35: MessageTypeFlags.sHotels,
      64: MessageTypeFlags.transact,
      65: MessageTypeFlags.personal,
      100: MessageTypeFlags.tNews,
      101: MessageTypeFlags.tSocial,
      102: MessageTypeFlags.tNotification,
      103: MessageTypeFlags.tPeople,
    }
    const testing: Int32[] = []
    let expected: Int32 = 0
    expect(messageTypeMaskFromServerMessageTypes([])).toBe(expected)
    for (const [key, value] of Object.entries(values)) {
      testing.push(Number.parseInt(key, 10))
      expected |= value
      const actual = messageTypeMaskFromServerMessageTypes(testing)
      expect(actual).toBe(expected)
    }
  })
  it('should skip mask application for unknown server types', () => {
    expect(messageTypeMaskFromServerMessageTypes([1000])).toBe(0)
    expect(messageTypeMaskFromServerMessageTypes([1, 2, 99])).toBe(
      MessageTypeFlags.delivery | MessageTypeFlags.registration,
    )
  })
})
