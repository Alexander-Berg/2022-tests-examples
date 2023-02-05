import { ArrayJSONItem } from '../../../../../../../common/code/json/json-types'
import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import {
  HistoryBasketItem,
  HistoryItem,
} from '../../../../../../code/network/mobile-backend/entities/history/history-entities'

describe(HistoryBasketItem, () => {
  const sample = JSON.parse(`
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
  `)

  it('should parse HistoryBasketItem', () => {
    const item = JSONItemFromJSON(sample)
    const response = HistoryBasketItem.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
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
    )
  })

  it('should fail to parse HistoryBasketItem', () => {
    const response = HistoryBasketItem.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})

describe(HistoryItem, () => {
  const sample = JSON.parse(`
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
  `)

  it('should parse HistoryItem', () => {
    const item = JSONItemFromJSON(sample)
    const response = HistoryItem.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
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
    )
  })

  it('should parse HistoryItem with malformed "basket"', () => {
    const sampleCopy = Object.assign({}, sample, { basket: {} })
    const item = JSONItemFromJSON(sampleCopy)

    const response = HistoryItem.fromJsonItem(item)
    expect(response.getValue()).toStrictEqual(
      new HistoryItem(
        'primaryKey',
        'purchaseToken',
        'timestamp',
        'status',
        [],
        'appID',
        'merchantID',
        'receiptURL',
        'uid',
      ),
    )
  })

  it('should fail to parse HistoryItem', () => {
    const response = HistoryItem.fromJsonItem(new ArrayJSONItem())
    expect(response.isError()).toBe(true)
  })
})
