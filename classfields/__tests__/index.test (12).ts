import { ISitePlansRoom } from 'realty-core/types/sitePlans';

import { getTitle } from '../libs/getTitle';

describe('LastApartmentsPromo getTitle', () => {
    test.each([
        [1, 'Осталась всего 1\u00a0студия'],
        [2, 'Осталось всего 2\u00a0студии'],
        [10, 'Осталось всего 10\u00a0студий'],
    ])('Студии (%i шт.)', (offersCount, expected) => {
        expect(getTitle({ roomType: 'STUDIO', offersCount } as ISitePlansRoom)).toBe(expected);
    });

    test.each([
        [1, 'Осталась всего 1\u00a0однушка'],
        [2, 'Осталось всего 2\u00a0однушки'],
        [10, 'Осталось всего 10\u00a0однушек'],
    ])('Однушки (%i шт.)', (offersCount, expected) => {
        expect(getTitle({ roomType: '1', offersCount } as ISitePlansRoom)).toBe(expected);
    });

    test.each([
        [1, 'Осталась всего 1\u00a0двушка'],
        [2, 'Осталось всего 2\u00a0двушки'],
        [10, 'Осталось всего 10\u00a0двушек'],
    ])('Двушки (%i шт.)', (offersCount, expected) => {
        expect(getTitle({ roomType: '2', offersCount } as ISitePlansRoom)).toBe(expected);
    });

    test.each([
        [1, 'Осталась всего 1\u00a0трёшка'],
        [2, 'Осталось всего 2\u00a0трёшки'],
        [10, 'Осталось всего 10\u00a0трёшек'],
    ])('Трёшка (%i шт.)', (offersCount, expected) => {
        expect(getTitle({ roomType: '3', offersCount } as ISitePlansRoom)).toBe(expected);
    });

    test.each([
        [1, 'Осталась всего одна 4\u2011комн. квартира'],
        [2, 'Осталось всего две 4\u2011комн. квартиры'],
        [3, 'Осталось всего три 4\u2011комн. квартиры'],
        [4, 'Осталось всего четыре 4\u2011комн. квартиры'],
        [5, 'Осталось всего пять 4\u2011комн. квартир'],
        [6, 'Осталось всего шесть 4\u2011комн. квартир'],
        [7, 'Осталось всего семь 4\u2011комн. квартир'],
        [8, 'Осталось всего восемь 4\u2011комн. квартир'],
        [9, 'Осталось всего девять 4\u2011комн. квартир'],
        [10, 'Осталось всего десять 4\u2011комн. квартир'],
    ])('4+ (%i шт.)', (offersCount, expected) => {
        expect(getTitle({ roomType: 'PLUS_4', offersCount } as ISitePlansRoom)).toBe(expected);
    });
});
