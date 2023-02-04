import comparableOffersMock from 'autoru-frontend/mockData/compare/offersBase';

import offerCarsMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { OfferCompareData } from 'auto-core/types/proto/auto/api/compare_model';

import getRowsWithUniqueValues from './getRowsWithUniqueValues';

let itemsMock: Array<OfferCompareData>;
beforeEach(() => {
    itemsMock = comparableOffersMock.map((compareOffer) => ({
        ...compareOffer,
        summary: offerCarsMock as unknown as OfferCompareData['summary'],
    }));
});

it('должен собрать информацию о строках с разными значениями для построения диффа', () => {
    const result = getRowsWithUniqueValues(itemsMock, 'specifications', itemsMock[0].specifications[0], 0);

    expect(result).toEqual({
        accidents: true,
        color: true,
        condition: false,
        custom_cleared: false,
        exchange: false,
        legal_purity: true,
        mileage: true,
        owners_count: true,
        pts: true,
        seller_type: true,
        state: true,
    });
});
