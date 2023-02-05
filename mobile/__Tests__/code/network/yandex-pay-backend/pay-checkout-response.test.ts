import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { CardNetworks } from '../../../../code/models/card-networks'
import { PaymentMethodTypes } from '../../../../code/models/payment-method-types'
import { APIError } from '../../../../code/network/yandex-pay-backend/api-errors'
import { PayCheckoutResponse } from '../../../../code/network/yandex-pay-backend/pay-checkout-response'

describe(PayCheckoutResponse, () => {
  it('should be deserializable from JSON response', () => {
    const response = PayCheckoutResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          payment_token: 'token',
          payment_method_info: {
            type: 'CARD',
            card_last4: '1234',
            card_network: 'AMEX',
          },
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(
      new PayCheckoutResponse('success', 200, 'token', PaymentMethodTypes.card, '1234', CardNetworks.amex),
    )
  })
  it('should omit optional fields if none provided in JSON response', () => {
    const response = PayCheckoutResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'success',
        code: 200,
        data: {
          payment_token: 'token',
          payment_method_info: {
            type: 'CARD',
          },
        },
      }),
    )
    expect(response.isError()).toBe(false)
    expect(response.getValue()).toStrictEqual(
      new PayCheckoutResponse('success', 200, 'token', PaymentMethodTypes.card, null, null),
    )
  })
  it('should deserialize error if status is not success', () => {
    const response = PayCheckoutResponse.fromJSONItem(
      JSONItemFromJSON({
        status: 'fail',
        code: 501,
        data: {},
      }),
    )
    expect(response.isError()).toBe(true)
    expect(response.getError()).toStrictEqual(new APIError('fail', 501, null, 'Checkout'))
  })
})
