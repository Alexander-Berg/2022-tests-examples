import {
  int32ToRecipientType,
  Recipient,
  recipientFromJSONItem,
  recipientToJSONItem,
  RecipientType,
  recipientTypeToInt32,
} from '../../../../../code/api/entities/recipient/recipient'
import { JSONItemFromJSON } from '../../../../../../common/__tests__/__helpers__/json-helpers'

describe(Recipient, () => {
  it('should convert RecipientType to Int32', () => {
    expect(recipientTypeToInt32(RecipientType.to)).toBe(RecipientType.to.valueOf())
    expect(recipientTypeToInt32(RecipientType.from)).toBe(RecipientType.from.valueOf())
    expect(recipientTypeToInt32(RecipientType.cc)).toBe(RecipientType.cc.valueOf())
    expect(recipientTypeToInt32(RecipientType.bcc)).toBe(RecipientType.bcc.valueOf())
    expect(recipientTypeToInt32(RecipientType.replyTo)).toBe(RecipientType.replyTo.valueOf())
  })
  it('should convert Int32 to RecipientType', () => {
    expect(int32ToRecipientType(RecipientType.to.valueOf())).toBe(RecipientType.to)
    expect(int32ToRecipientType(RecipientType.from.valueOf())).toBe(RecipientType.from)
    expect(int32ToRecipientType(RecipientType.cc.valueOf())).toBe(RecipientType.cc)
    expect(int32ToRecipientType(RecipientType.bcc.valueOf())).toBe(RecipientType.bcc)
    expect(int32ToRecipientType(RecipientType.replyTo.valueOf())).toBe(RecipientType.replyTo)
    expect(int32ToRecipientType(100)).toBeNull()
  })
  it('should provide RecipientTypes with specific values', () => {
    expect(RecipientType.to).toBe(1)
    expect(RecipientType.from).toBe(2)
    expect(RecipientType.cc).toBe(3)
    expect(RecipientType.bcc).toBe(4)
    expect(RecipientType.replyTo).toBe(5)
  })
  it('should deserialize Recipient from JSONItem', () => {
    expect(
      recipientFromJSONItem(
        JSONItemFromJSON({
          email: 'mail@yandex.ru',
          name: 'name',
          type: 1,
        })!,
      ),
    ).toStrictEqual(new Recipient('mail@yandex.ru', 'name', RecipientType.to))
    expect(
      recipientFromJSONItem(
        JSONItemFromJSON({
          email: 'mail@yandex.ru',
          name: 'name',
          type: 100,
        })!,
      ),
    ).toBeNull()
    expect(recipientFromJSONItem(JSONItemFromJSON([])!)).toBeNull()
  })
  it('should serialize Recipient to JSONItem', () => {
    expect(recipientToJSONItem(new Recipient('mail@yandex.ru', 'name', RecipientType.to))).toStrictEqual(
      JSONItemFromJSON({
        email: 'mail@yandex.ru',
        name: 'name',
        type: 1,
      })!,
    )
    expect(recipientToJSONItem(new Recipient('mail@yandex.ru', null, RecipientType.to))).toStrictEqual(
      JSONItemFromJSON({
        email: 'mail@yandex.ru',
        type: 1,
      })!,
    )
  })
})
