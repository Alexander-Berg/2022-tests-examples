import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { BankID, CreditProductType, Domain } from 'auto-core/types/TCreditBroker';

import getCreditProductParamsByOffer from './getCreditProductParamsByOffer';

// @todo перейти на общий мок
const baseCreditProduct = {
    id: 'test',
    bank_id: BankID.TINKOFF,
    product_type: CreditProductType.UNKNOWN_CREDIT_PRODUCT_TYPE,
    domain: Domain.DOMAIN_AUTO,
    priority: 3,
};

it('должен вернуть параметры, если дефолты входят в диапазон', () => {
    const creditProduct = {
        ...baseCreditProduct,
        term_months_range: { from: 12, to: 60 },
        interest_rate_range: { from: 0.079, to: 0.169 },
        min_initial_fee_rate: 0.1,
        amount_range: { from: 100000, to: 1500000 },
    };

    expect(getCreditProductParamsByOffer({ offer, creditProduct }))
        .toEqual({ amount: 684000, feeRate: 0.2, rate: 0.079, term: 60 });
});

it('должен вернуть параметры, если дефолт срока не входит в диапазон', () => {
    const creditProduct = {
        ...baseCreditProduct,
        term_months_range: { from: 24, to: 48 },
        interest_rate_range: { from: 0.129, to: 0.169 },
        min_initial_fee_rate: 0.1,
        amount_range: { from: 100000, to: 1500000 },
    };

    expect(getCreditProductParamsByOffer({ offer, creditProduct }))
        .toEqual({ amount: 684000, feeRate: 0.2, rate: 0.129, term: 48 });
});

it('должен вернуть параметры, если дефолт процента первоначального взноса меньше минимального', () => {
    const creditProduct = {
        ...baseCreditProduct,
        term_months_range: { from: 24, to: 72 },
        interest_rate_range: { from: 0.119, to: 0.169 },
        min_initial_fee_rate: 0.3,
        amount_range: { from: 100000, to: 1500000 },
    };

    expect(getCreditProductParamsByOffer({ offer, creditProduct }))
        .toEqual({ amount: 598500, feeRate: 0.3, rate: 0.119, term: 60 });
});

it('должен вернуть параметры, если стоимость авто больше максимальной суммы', () => {
    const creditProduct = {
        ...baseCreditProduct,
        term_months_range: { from: 24, to: 72 },
        interest_rate_range: { from: 0.089, to: 0.169 },
        min_initial_fee_rate: 0.1,
        amount_range: { from: 100000, to: 200000 },
    };

    expect(getCreditProductParamsByOffer({ offer, creditProduct }))
        .toEqual({ amount: 200000, feeRate: 0.2, rate: 0.089, term: 60 });
});

it('должен вернуть параметры, если стоимость авто меньше минимальной суммы', () => {
    const creditProduct = {
        ...baseCreditProduct,
        term_months_range: { from: 24, to: 72 },
        interest_rate_range: { from: 0.069, to: 0.169 },
        min_initial_fee_rate: 0.3,
        amount_range: { from: 1300000, to: 2000000 },
    };

    expect(getCreditProductParamsByOffer({ offer, creditProduct }))
        .toEqual({ amount: 1300000, feeRate: 0.3, rate: 0.069, term: 60 });
});
