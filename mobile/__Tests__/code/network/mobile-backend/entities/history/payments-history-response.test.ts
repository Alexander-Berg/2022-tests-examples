import { PaymentsHistoryResponse } from '../../../../../../code/network/mobile-backend/entities/history/payments-history-response'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import {
  HistoryItem,
  HistoryBasketItem,
} from '../../../../../../code/network/mobile-backend/entities/history/history-entities'
import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'

export const sample = JSON.parse(`
{
  "items": [
    {
      "url": "primaryKey",
      "token": "purchaseToken",
      "timestamp": "timestamp",
      "status": "status",
      "basket": [
        {
          "order_id": "orderID",
          "order_ts": "orderTs",
          "product_id": "productID",
          "product_type": "productType",
          "product_name": "productName",
          "orig_amount": "originalAmount",
          "paid_amount": "paidAmount",
          "current_qty": "currentQuantity"
        }
      ],
      "app_id": "appID",
      "merchant_id": "merchantID",
      "receipt": "receiptURL",
      "uid": "uid"
    }
  ]
}
`)

describe(PaymentsHistoryResponse, () => {
  it('should parse PaymentsHistoryResponse', () => {
    const item = JSONItemFromJSON(sample)
    const response = PaymentsHistoryResponse.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new PaymentsHistoryResponse([
        new HistoryItem(
          'primaryKey',
          'purchaseToken',
          'timestamp',
          'status',
          [
            new HistoryBasketItem(
              'orderID',
              'orderTs',
              'productID',
              'productType',
              'productName',
              'originalAmount',
              'paidAmount',
              'currentQuantity',
            ),
          ],
          'appID',
          'merchantID',
          'receiptURL',
          'uid',
        ),
      ]),
    )
  })

  it('should fail to parse PaymentsHistoryResponse with malformed "items"', () => {
    const sampleCopy = Object.assign({}, sample, { items: {} })
    const item = JSONItemFromJSON(sampleCopy)

    const response = PaymentsHistoryResponse.fromJsonItem(item)
    expect(response.isError()).toBe(true)
  })

  it('should fail to parse PaymentsHistoryResponse', () => {
    const response = PaymentsHistoryResponse.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
