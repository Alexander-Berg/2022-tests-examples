import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { AuthMethods } from '../../../../code/models/auth-methods'
import { CardNetworks } from '../../../../code/models/card-networks'
import { PaymentMethod } from '../../../../code/models/payment-method'
import { PaymentMethodTypes } from '../../../../code/models/payment-method-types'
import { IsReadyToPayRequest } from '../../../../code/network/yandex-pay-backend/is-ready-to-pay-request'

describe(IsReadyToPayRequest, () => {
  it('should represent "is_ready_to_pay" request', () => {
    const request = new IsReadyToPayRequest('merchantOrigin', 'merchantId', true, [
      new PaymentMethod(
        [AuthMethods.panOnly],
        PaymentMethodTypes.card,
        'gateway1',
        [CardNetworks.amex, CardNetworks.discover],
        'gatewayMerchantId1',
      ),
      new PaymentMethod(
        [AuthMethods.cloudToken, AuthMethods.panOnly],
        PaymentMethodTypes.card,
        'gateway2',
        [CardNetworks.jcb, CardNetworks.maestro],
        'gatewayMerchantId2',
      ),
    ])
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/mobile/v1/is_ready_to_pay')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        merchant_origin: 'merchantOrigin',
        merchant_id: 'merchantId',
        existing_payment_method_required: true,
        payment_methods: [
          {
            allowed_auth_methods: ['PAN_ONLY'],
            type: 'CARD',
            gateway: 'gateway1',
            allowed_card_networks: ['AMEX', 'DISCOVER'],
            gateway_merchant_id: 'gatewayMerchantId1',
          },
          {
            allowed_auth_methods: ['CLOUD_TOKEN', 'PAN_ONLY'],
            type: 'CARD',
            gateway: 'gateway2',
            allowed_card_networks: ['JCB', 'MAESTRO'],
            gateway_merchant_id: 'gatewayMerchantId2',
          },
        ],
      }),
    )
  })
})
