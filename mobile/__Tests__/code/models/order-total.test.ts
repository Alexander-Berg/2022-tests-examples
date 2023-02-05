import { mapJSONItemFromObject } from '../../../../common/__tests__/__helpers__/json-helpers'
import { OrderTotal } from '../../../code/models/order-total'

describe(OrderTotal, () => {
  it('should be serializable into MapJSONItem', () => {
    const value1 = new OrderTotal('100.00', 'label1')
    expect(value1.toMapJSONItem()).toStrictEqual(
      mapJSONItemFromObject({
        amount: '100.00',
        label: 'label1',
      }),
    )
    const value2 = new OrderTotal('200.00', null)
    const actual = value2.toMapJSONItem()
    expect(actual).toStrictEqual(
      mapJSONItemFromObject({
        amount: '200.00',
      }),
    )
    expect(actual.hasKey('label')).toBe(false)
  })
})
