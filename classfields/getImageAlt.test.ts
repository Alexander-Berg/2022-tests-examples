import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import getImageAlt from './getImageAlt';

it('Должен подготовить alt для оффера', () => {
    expect(getImageAlt(offerMock, 0)).toEqual('2017 Ford EcoSport I, белый, 855000 рублей, вид 1');
});

it('Должен подготовить alt для оффера без данных о цвете', () => {
    const offerMockNoColor = { ...offerMock, color_hex: undefined } as unknown as Offer;
    expect(getImageAlt(offerMockNoColor)).toEqual('2017 Ford EcoSport I, 855000 рублей');
});

it('Не должен подготовить alt для оффера если не пришло данных', () => {
    const offerMockNoData = {} as unknown as Offer;
    expect(getImageAlt(offerMockNoData)).toBeUndefined();
});
