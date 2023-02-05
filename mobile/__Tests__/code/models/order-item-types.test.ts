import { orderItemTypeFromString, OrderItemTypes, orderItemTypeToString } from '../../../code/models/order-item-types'

describe('OrderItemTypes', () => {
  const map: Readonly<{ readonly [key: string]: OrderItemTypes }> = {
    PICKUP: OrderItemTypes.Pickup,
    SHIPPING: OrderItemTypes.Shipping,
    DISCOUNT: OrderItemTypes.Discount,
    PROMOCODE: OrderItemTypes.Promocode,
  }
  it.each(Object.entries(map))('should be deserializable from string %s to OrderItemTypes %s', (key, value) => {
    expect(orderItemTypeFromString(key)).toBe(value)
  })
  it('should deserialize into null for unknown string value', () => {
    expect(orderItemTypeFromString('unknown')).toBeNull()
  })
  it.each(Object.entries(map))('should be serializable from OrderItemTypes %s to string %s', (key, value) => {
    expect(orderItemTypeToString(value)).toBe(key)
  })
})
