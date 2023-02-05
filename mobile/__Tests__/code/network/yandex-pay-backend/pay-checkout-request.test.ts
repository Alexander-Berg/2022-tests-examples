import { JsonRequestEncoding, NetworkMethod } from '../../../../../common/code/network/network-request'
import { JSONItemFromJSON } from '../../../../../common/__tests__/__helpers__/json-helpers'
import { AuthMethods } from '../../../../code/models/auth-methods'
import { CardNetworks } from '../../../../code/models/card-networks'
import { CountryCodes } from '../../../../code/models/country-codes'
import { CurrencyCodes } from '../../../../code/models/currency-codes'
import { Merchant } from '../../../../code/models/merchant'
import { Order } from '../../../../code/models/order'
import { OrderItem } from '../../../../code/models/order-item'
import { OrderItemQuantity } from '../../../../code/models/order-item-quantity'
import { OrderItemTypes } from '../../../../code/models/order-item-types'
import { OrderTotal } from '../../../../code/models/order-total'
import { PaymentMethod } from '../../../../code/models/payment-method'
import { PaymentMethodTypes } from '../../../../code/models/payment-method-types'
import { PaymentSheet } from '../../../../code/models/payment-sheet'
import { PayCheckoutRequest } from '../../../../code/network/yandex-pay-backend/pay-checkout-request'

describe(PayCheckoutRequest, () => {
  it('should represent "checkout" request', () => {
    const request = new PayCheckoutRequest(
      'cardId',
      'merchantOrigin',
      new PaymentSheet(
        new Merchant('merchantId', 'merchantName'),
        new Order('orderRequestGroupId', new OrderTotal('1000.00', 'orderRequestGroupLabel'), [
          new OrderItem('Item1', '700.00', OrderItemTypes.Pickup, new OrderItemQuantity('2', 'quantityLabel1')),
          new OrderItem('Item2', '300.00', OrderItemTypes.Shipping, new OrderItemQuantity('4', 'quantityLabel2')),
        ]),
        CurrencyCodes.rub,
        CountryCodes.ru,
        [
          new PaymentMethod(
            [AuthMethods.cloudToken, AuthMethods.panOnly],
            PaymentMethodTypes.card,
            'Gateway1',
            [CardNetworks.amex, CardNetworks.discover],
            'GatewayMerchantId1',
          ),
          new PaymentMethod(
            [AuthMethods.cloudToken],
            PaymentMethodTypes.card,
            'Gateway2',
            [CardNetworks.jcb, CardNetworks.maestro],
            'GatewayMerchantId2',
          ),
        ],
      ),
    )
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.targetPath()).toBe('api/mobile/v1/checkout')
    expect(request.urlExtra().asMap().size).toBe(0)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        card_id: 'cardId',
        merchant_origin: 'merchantOrigin',
        sheet: {
          version: 2,
          currency_code: 'RUB',
          country_code: 'RU',
          merchant: {
            id: 'merchantId',
            name: 'merchantName',
          },
          order: {
            id: 'orderRequestGroupId',
            total: {
              label: 'orderRequestGroupLabel',
              amount: '1000.00',
            },
            items: [
              {
                label: 'Item1',
                amount: '700.00',
                type: 'PICKUP',
                quantity: {
                  count: '2',
                  label: 'quantityLabel1',
                },
              },
              {
                label: 'Item2',
                amount: '300.00',
                type: 'SHIPPING',
                quantity: {
                  count: '4',
                  label: 'quantityLabel2',
                },
              },
            ],
          },
          payment_methods: [
            {
              allowed_auth_methods: ['CLOUD_TOKEN', 'PAN_ONLY'],
              type: 'CARD',
              gateway: 'Gateway1',
              allowed_card_networks: ['AMEX', 'DISCOVER'],
              gateway_merchant_id: 'GatewayMerchantId1',
            },
            {
              allowed_auth_methods: ['CLOUD_TOKEN'],
              type: 'CARD',
              gateway: 'Gateway2',
              allowed_card_networks: ['JCB', 'MAESTRO'],
              gateway_merchant_id: 'GatewayMerchantId2',
            },
          ],
        },
      }),
    )
  })
})
