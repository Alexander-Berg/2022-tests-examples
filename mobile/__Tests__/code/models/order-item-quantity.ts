import { mapJSONItemFromObject } from '../../../../common/__tests__/__helpers__/json-helpers'
import { OrderItemQuantity } from '../../../code/models/order-item-quantity'

describe(OrderItemQuantity, () => {
  it('should be serializable into MapJSONItem', () => {
    const value1 = new OrderItemQuantity('5', 'label1')
    expect(value1.toMapJSONItem()).toStrictEqual(
      mapJSONItemFromObject({
        count: '5',
        label: 'label1',
      }),
    )
    const value2 = new OrderItemQuantity('13', null)
    const actual = value2.toMapJSONItem()
    expect(actual).toStrictEqual(
      mapJSONItemFromObject({
        count: '13',
      }),
    )
    expect(actual.hasKey('label')).toBe(false)
  })
})
