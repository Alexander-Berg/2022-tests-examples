import getMarksItems from './getMarksItems';

const marks = [
    {
        id: 'ALFA_ROMEO',
        count: 10,
        reviews_count: 0,
        cyrillic_name: 'Альфа Ромео',
        popular: false,
        name: 'Alfa Romeo',
        numeric_id: 3137,
        itemFilterParams: {
            mark: 'ALFA_ROMEO',
        },
        'big-logo': 'image',
        logo: 'image',
        year_from: 1927,
        year_to: 2020,
    },
    {
        id: 'ALPINE',
        count: 0,
        reviews_count: 42,
        cyrillic_name: 'Альпин',
        popular: false,
        name: 'Alpine',
        numeric_id: 7001151,
        itemFilterParams: {
            mark: 'ALPINE',
        },
        'big-logo': 'image',
        logo: 'image',
        year_from: 1961,
        year_to: 2020,
    },
    {
        id: 'ASTON_MARTIN',
        count: 1,
        reviews_count: 0,
        cyrillic_name: 'Астон Мартин',
        popular: false,
        name: 'Aston Martin',
        numeric_id: 3138,
        itemFilterParams: {
            mark: 'ASTON_MARTIN',
        },
        'big-logo': 'image',
        logo: 'image',
        year_from: 1963,
        year_to: 2020,
    },
];

describe('function getMarksItems.all()', () => {
    it('вернет список из всех марок если нет условий фильтрации', () => {
        const result = getMarksItems.all(marks, undefined, undefined);
        expect(result).toHaveLength(marks.length);
    });

    it('вернет список из марок с непустыми каунтерами если переданы условия фильтрации', () => {
        const result = getMarksItems.all(marks, undefined, undefined, { withEmpty: false, counterField: 'reviews_count' });
        expect(result).toHaveLength(1);
    });

    it('вернет список из всех марок если не переданы флаг withEmpty', () => {
        const result = getMarksItems.all(marks, undefined, undefined, { counterField: 'reviews_count' });
        expect(result).toHaveLength(marks.length);
    });
});
