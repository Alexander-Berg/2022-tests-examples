import { IMicrodataCoreStore } from 'realty-core/view/react/common/components/Microdata/types';

export const MOCK_SEARCH = {
    search: {
        offers: {
            pager: {
                totalItems: 3881,
                minPriceTotalItems: 8000000,
                maxPriceTotalItems: 8000000,
            },
            entities: [
                {
                    price: {
                        value: 8000000,
                    },
                },
            ],
        },
    },
    seoTexts: {
        h1: 'Купить 3-комнатную квартиру в Москве и МО',
        description:
            'Более 3 881 объявления по продаже 3-комнатных квартир по цене' +
            ' от 4 700 555 ₽. Карты цен, инфраструктуры, +транспортной доступности и ' +
            'качества воздуха в Москве и МО помогут выбрать и купить трехкомнатную квартиру на Яндекс.Недвижимости.',
    },
} as IMicrodataCoreStore;
