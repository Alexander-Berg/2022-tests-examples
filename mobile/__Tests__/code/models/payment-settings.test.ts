import { PaymentSettings } from '../../../code/models/payment-settings'
import { Acquirer } from '../../../code/network/mobile-backend/entities/init/acquirer'
import { MerchantAddress, MerchantInfo } from '../../../code/network/mobile-backend/entities/init/merchant-info'

describe(PaymentSettings, () => {
  it('build PaymentSettings with licenseURL', () => {
    const card = new PaymentSettings(
      '1000',
      'RUB',
      'licenseURL',
      Acquirer.tinkoff,
      'sandbox',
      new MerchantInfo(
        'Test merchant',
        'с 9 до 6',
        '1234567890123',
        new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '119021'),
      ),
      null,
      'creditFormUrl',
    )
    expect(card.total).toStrictEqual('1000')
    expect(card.currency).toStrictEqual('RUB')
    expect(card.licenseURL).toStrictEqual('licenseURL')
    expect(card.acquirer).toStrictEqual('tinkoff')
    expect(card.environment).toStrictEqual('sandbox')
    expect(card.merchantInfo).toStrictEqual(
      new MerchantInfo(
        'Test merchant',
        'с 9 до 6',
        '1234567890123',
        new MerchantAddress('Москва', 'RUS', '16', 'Льва Толстого', '119021'),
      ),
    )
    expect(card.creditFormUrl).toStrictEqual('creditFormUrl')
  })
  it('build PaymentSettings without licenseURL', () => {
    const card = new PaymentSettings('1000', 'RUB', null, Acquirer.tinkoff, 'sandbox', null, null, 'creditFormUrl')
    expect(card.total).toStrictEqual('1000')
    expect(card.currency).toStrictEqual('RUB')
    expect(card.licenseURL).toBeNull()
    expect(card.acquirer).toStrictEqual('tinkoff')
    expect(card.environment).toStrictEqual('sandbox')
    expect(card.creditFormUrl).toStrictEqual('creditFormUrl')
  })
})
