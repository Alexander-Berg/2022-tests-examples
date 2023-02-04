import { getSearchProductData } from '../../getSearchProductData';

import { MOCK_SEARCH } from './mock';

it('Должен вернуть разметку для @type Product, когда есть все данные', () => {
    const result = {
        '@context': 'https://schema.org',
        '@type': 'Product',
        description:
            'Более 3 881 объявления по продаже 3-комнатных квартир по цене от 4 700 555 ₽. ' +
            'Карты цен, инфраструктуры, +транспортной доступности и качества воздуха в Москве и МО помогут ' +
            'выбрать и купить трехкомнатную квартиру на Яндекс.Недвижимости.',
        name: 'Купить 3-комнатную квартиру в Москве и МО',
        offers: {
            '@type': 'AggregateOffer',
            priceCurrency: 'RUR',
        },
    };

    expect(getSearchProductData(MOCK_SEARCH)).toMatchObject(result);
});

it('Не должен вернуть разметку, когда нет офферов', () => {
    const state = {
        ...MOCK_SEARCH,
        search: undefined,
    };

    expect(getSearchProductData(state)).toBeNull();
});
