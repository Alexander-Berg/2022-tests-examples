import bankMock from 'auto-core/react/dataDomain/credit/mocks/bank.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';

import { BankID } from 'auto-core/types/TCreditBroker';

import getCreditProductMappedInfo from './getCreditProductMappedInfo';

const banks = [
    bankMock().withBankID(BankID.ALFABANK).value(),
    bankMock().withBankID(BankID.TINKOFF).value(),
    bankMock().withBankID(BankID.SBERBANK).value(),
    bankMock().withBankID(BankID.RAIFFEISEN).value(),
];

const claims = [
    creditApplicationClaimMock().withProductID('test-1').value(),
    creditApplicationClaimMock().withProductID('test-2').value(),
    creditApplicationClaimMock().withProductID('test-3').value(),
];

it('найдет относящуюся к продукту заявку', () => {
    const creditApplication = creditApplicationMock().withClaims(claims).value();
    const creditProduct = creditProductMock().withID('test-2').value();

    expect(getCreditProductMappedInfo({ creditApplication, creditProduct, banks })?.claim)
        .toEqual(claims[1]);
});

it('найдет относящийся к продукту банк', () => {
    const creditApplication = creditApplicationMock().value();
    const creditProduct = creditProductMock().withBankID(BankID.RAIFFEISEN).value();

    expect(getCreditProductMappedInfo({ creditApplication, creditProduct, banks })?.bank)
        .toEqual(banks[3]);
});

it('вернет верные параметры кредита', () => {
    const creditApplication = creditApplicationMock()
        .withRequirements({ amount: 500000, term: 48, fee: 100000 })
        .value();
    const creditProduct = creditProductMock()
        .withTermRange({ from: 48, to: 60 })
        .withAmountRange({ from: 500000, to: 1000000 })
        .withInterestRateRange({ from: 7.6, to: 11.3 })
        .withBankID(BankID.ALFABANK)
        .withID('test-2')
        .value();

    expect(getCreditProductMappedInfo({ creditApplication, creditProduct, banks }))
        .toMatchObject({
            term: 48,
            amount: 500000,
            overpayment: 85600,
            minRate: 7.6,
            monthlyPayment: 12200,
        });
});

it('вернет параметры кредита в разрешенных диапазонах кредитного продукта', () => {
    const creditApplication = creditApplicationMock()
        .withRequirements({ amount: 230000, term: 24, fee: 100000 })
        .value();
    const creditProduct = creditProductMock()
        .withTermRange({ from: 48, to: 60 })
        .withAmountRange({ from: 600000, to: 800000 })
        .withInterestRateRange({ from: 7.6, to: 11.3 })
        .withBankID(BankID.ALFABANK)
        .withID('test-2')
        .value();

    expect(getCreditProductMappedInfo({ creditApplication, creditProduct, banks }))
        .toMatchObject({ term: 48, amount: 600000 });
});

it('если у клейма есть отправленный снепшот, отдает ему предпочтение', () => {
    const claimWithSentSnapshot = creditApplicationClaimMock()
        .withProductID('test-4')
        .withSentSnapshot({
            created: '2021-06-23T15:00:30.154378Z',
            requirements: {
                max_amount: 555000,
                term_months: 36,
                initial_fee: 214000,
                geobase_ids: [ 213 ],
            },
            credit_product_properties: {
                interest_rate_range: {
                    from: 4.99,
                    to: 11.99,
                },
            },
        })
        .value();

    const creditApplication = creditApplicationMock()
        .withRequirements({ amount: 430000, term: 24, fee: 100000 })
        .withClaims([ ...claims, claimWithSentSnapshot ])
        .value();

    const creditProduct = creditProductMock()
        .withAmountRange({ from: 400000, to: 800000 })
        .withInterestRateRange({ from: 7.6, to: 11.3 })
        .withBankID(BankID.ALFABANK)
        .withID('test-4')
        .value();

    expect(getCreditProductMappedInfo({ creditApplication, creditProduct, banks }))
        .toMatchObject({ term: 36, amount: 555000, minRate: 4.99 });
});
