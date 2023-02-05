import { mapJSONItemFromObject } from '../../../../common/__tests__/__helpers__/json-helpers'
import { Order } from '../../../code/models/order'
import { OrderItem } from '../../../code/models/order-item'
import { OrderTotal } from '../../../code/models/order-total'

describe(Order, () => {
  it('should be serializable into MapJSONItem', () => {
    const value1 = new Order('id1', new OrderTotal('100.00', 'total'), [])
    expect(value1.toMapJSONItem()).toStrictEqual(
      mapJSONItemFromObject({
        id: 'id1',
        total: {
          amount: '100.00',
          label: 'total',
        },
      }),
    )
    const value2 = new Order('id2', new OrderTotal('200.00', null), [
      new OrderItem('item1', '150.00', null, null),
      new OrderItem('item2', '50.00', null, null),
    ])
    expect(value2.toMapJSONItem()).toStrictEqual(
      mapJSONItemFromObject({
        id: 'id2',
        total: {
          amount: '200.00',
        },
        items: [
          {
            label: 'item1',
            amount: '150.00',
          },
          {
            label: 'item2',
            amount: '50.00',
          },
        ],
      }),
    )
  })
})
