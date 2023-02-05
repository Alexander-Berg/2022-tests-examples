import { JSONItemFromJSON } from '../../../../../../../common/__tests__/__helpers__/json-helpers'
import { JsonRequestEncoding, NetworkMethod } from '../../../../../../../common/code/network/network-request'
import { SupplyNewCardRequest } from '../../../../../../code/network/diehard-backend/entities/supply/supply-new-card-request'

describe(SupplyNewCardRequest, () => {
  it('should build SupplyNewCardRequest request', () => {
    const request = new SupplyNewCardRequest(
      'token',
      'purchaseToken',
      'email@ya.ru',
      '1234567812345678',
      '12',
      '21',
      '123',
      true,
    )
    expect(request.encoding()).toBeInstanceOf(JsonRequestEncoding)
    expect(request.headersExtra().asMap().size).toBe(0)
    expect(request.method()).toBe(NetworkMethod.post)
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          purchase_token: 'purchaseToken',
          email: 'email@ya.ru',
          card_number: '1234567812345678',
          expiration_month: '12',
          expiration_year: '21',
          cvn: '123',
          bind_card: 1,
          payment_method: 'new_card',
        },
      }),
    )
    expect(request.targetPath()).toBe('supply_payment_data')
    expect(request.urlExtra().asMap().size).toBe(0)
  })

  it('should build not stored SupplyNewCardRequest request', () => {
    const request = new SupplyNewCardRequest(
      'token',
      'purchaseToken',
      'email@ya.ru',
      '1234567812345678',
      '12',
      '21',
      '123',
      false,
    )
    expect(request.params()).toStrictEqual(
      JSONItemFromJSON({
        params: {
          token: 'token',
          purchase_token: 'purchaseToken',
          email: 'email@ya.ru',
          card_number: '1234567812345678',
          expiration_month: '12',
          expiration_year: '21',
          cvn: '123',
          bind_card: 0,
          payment_method: 'new_card',
        },
      }),
    )
  })
})
