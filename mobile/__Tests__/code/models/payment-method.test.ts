import { mapJSONItemFromObject } from '../../../../common/__tests__/__helpers__/json-helpers'
import { AuthMethods } from '../../../code/models/auth-methods'
import { CardNetworks } from '../../../code/models/card-networks'
import { PaymentMethod } from '../../../code/models/payment-method'
import { PaymentMethodTypes } from '../../../code/models/payment-method-types'

describe(PaymentMethod, () => {
  it('should be serializable into MapJSONItem', () => {
    const value = new PaymentMethod(
      [AuthMethods.cloudToken, AuthMethods.panOnly],
      PaymentMethodTypes.card,
      'gateway1',
      [CardNetworks.amex, CardNetworks.discover],
      'merchant1',
    )
    expect(value.toMapJSONItem()).toStrictEqual(
      mapJSONItemFromObject({
        allowed_auth_methods: ['CLOUD_TOKEN', 'PAN_ONLY'],
        type: 'CARD',
        gateway: 'gateway1',
        allowed_card_networks: ['AMEX', 'DISCOVER'],
        gateway_merchant_id: 'merchant1',
      }),
    )
  })
})
