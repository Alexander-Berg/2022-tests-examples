import { OfferPosition_OrderedPosition_Sort } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import getRelativeSearchPosition from './getRelativeSearchPosition';

it('если нет информации о позиции вернет undefined', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSearchPositions([])
        .value();

    const result = getRelativeSearchPosition(offer);
    expect(result).toBeUndefined();
});

it('если позиция меньше 1 вернет undefined', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSearchPositions([ {
            positions: [
                { position: -1, sort: OfferPosition_OrderedPosition_Sort.SIMPLE_RELEVANCE, total_count: 12 },
            ],
            total_count: 12,
        } ])
        .value();

    const result = getRelativeSearchPosition(offer);
    expect(result).toBeUndefined();
});

it('если общее число офферов меньше 1 вернет undefined', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSearchPositions([ {
            positions: [
                { position: 12, sort: OfferPosition_OrderedPosition_Sort.SIMPLE_RELEVANCE, total_count: 0 },
            ],
            total_count: 0,
        } ])
        .value();

    const result = getRelativeSearchPosition(offer);
    expect(result).toBeUndefined();
});

it('если в выдаче только 1 оффер вернет 0', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSearchPositions([ {
            positions: [
                { position: 12, sort: OfferPosition_OrderedPosition_Sort.SIMPLE_RELEVANCE, total_count: 1 },
            ],
            total_count: 1,
        } ])
        .value();

    const result = getRelativeSearchPosition(offer);
    expect(result).toBe(0);
});

it('правильно рассчитывает результат для не пустой выдачи', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withSearchPositions([ {
            positions: [
                { position: 12, sort: OfferPosition_OrderedPosition_Sort.SIMPLE_RELEVANCE, total_count: 101 },
            ],
            total_count: 1,
        } ])
        .value();

    const result = getRelativeSearchPosition(offer);
    expect(result).toBe(11);
});
