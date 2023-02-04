import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import offer from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import getCreditCalculatorConditions from './getCreditCalculatorConditions';

it('вернет верные данные, если нет оффера', () => {
    const creditProduct = creditProductMock().value();
    expect(getCreditCalculatorConditions({ creditProduct }))
        .toMatchSnapshot();
});

it('вернет верные данные, если есть оффер', () => {
    const creditProduct = creditProductMock().value();
    expect(getCreditCalculatorConditions({ creditProduct, offer }))
        .toMatchSnapshot();
});

it('вернет null, если нет кредитного продукта', () => {
    expect(getCreditCalculatorConditions({ offer })).toBeNull();
});

it('поменяет максимальную цену для тачки дешевле минимальной суммы кредита', () => {
    const creditProduct = creditProductMock()
        .withAmountRange({
            from: 300000,
            to: 500000,
        })
        .withMinInitialFeeRate(0)
        .value();

    const offerMock = cloneOfferWithHelpers(offer)
        .withPrice(128000)
        .value();

    expect(getCreditCalculatorConditions({ creditProduct, offer: offerMock })).toMatchSnapshot();
});
