import { mapJSONItemFromObject } from '../../../../common/__tests__/__helpers__/json-helpers'
import { OrderItem } from '../../../code/models/order-item'
import { OrderItemQuantity } from '../../../code/models/order-item-quantity'
import { OrderItemTypes } from '../../../code/models/order-item-types'

describe(OrderItem, () => {
  it('should be serializable into MapJSONItem with all the fields filled in', () => {
    const value = new OrderItem('item', '100.00', OrderItemTypes.Pickup, new OrderItemQuantity('3', 'quantityLabel'))
    expect(value.toMapJSONItem()).toStrictEqual(
      mapJSONItemFromObject({
        amount: '100.00',
        label: 'item',
        type: 'PICKUP',
        quantity: {
          count: '3',
          label: 'quantityLabel',
        },
      }),
    )
  })
  it('should be serializable into MapJSONItem with empty quantity', () => {
    const value = new OrderItem('item', '200.00', OrderItemTypes.Shipping, null)
    const actual = value.toMapJSONItem()
    expect(actual).toStrictEqual(
      mapJSONItemFromObject({
        label: 'item',
        amount: '200.00',
        type: 'SHIPPING',
      }),
    )
    expect(actual.hasKey('quantity')).toBe(false)
  })
  it('should be serializable into MapJSONItem with empty orderType', () => {
    const value = new OrderItem('item', '300.00', null, new OrderItemQuantity('3', 'quantityLabel'))
    const actual = value.toMapJSONItem()
    expect(actual).toStrictEqual(
      mapJSONItemFromObject({
        label: 'item',
        amount: '300.00',
        quantity: {
          count: '3',
          label: 'quantityLabel',
        },
      }),
    )
    expect(actual.hasKey('type')).toBe(false)
  })
})
