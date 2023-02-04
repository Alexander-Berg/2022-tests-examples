import getModelsItems from './getModelsItems';

const models = [
    {
        count: 33,
        reviews_count: 42,
        cyrillic_name: 'Аэростар',
        id: 'AEROSTAR',
        itemFilterParams: {
            model: 'AEROSTAR',
        },
        name: 'Aerostar',
        nameplates: [],
        popular: false,
        year_from: 1986,
        year_to: 1997,
    },
    {
        count: 0,
        reviews_count: 0,
        cyrillic_name: 'Эспайр',
        id: 'ASPIRE',
        itemFilterParams: {
            model: 'ASPIRE',
        },
        name: 'Aspire',
        nameplates: [],
        popular: false,
        year_from: 1993,
        year_to: 1997,
    },
    {
        count: 15,
        reviews_count: 0,
        cyrillic_name: 'Б-МАКС',
        id: 'B_MAX',
        itemFilterParams: {
            model: 'B_MAX',
        },
        name: 'B-MAX',
        nameplates: [],
        popular: false,
        year_from: 2012,
        year_to: 2018,
    },
];

describe('function getModelsItems.all()', () => {
    it('вернет список из всех марок если нет условий фильтрации', () => {
        const result = getModelsItems.all(models, undefined, undefined);
        expect(result).toHaveLength(models.length);
    });

    it('вернет список из марок с непустыми каунтерами если переданы условия фильтрации', () => {
        const result = getModelsItems.all(models, undefined, undefined, { withEmpty: false, counterField: 'reviews_count' });
        expect(result).toHaveLength(1);
    });

    it('вернет список из всех марок если не переданы флаг withEmpty', () => {
        const result = getModelsItems.all(models, undefined, undefined, { counterField: 'reviews_count' });
        expect(result).toHaveLength(models.length);
    });
});
