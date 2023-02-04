import getTermValuesInMonths from './getTermValuesInMonths';

describe('getTermValuesInMonths возвращает верный массив', () => {
    it('если from и to переводятся в целое количество лет', () => {
        expect(getTermValuesInMonths({ from: 12, to: 60 }))
            .toEqual([ 12, 24, 36, 48, 60 ]);
    });

    it('если to переводится в целое количество лет, а from - нет', () => {
        expect(getTermValuesInMonths({ from: 17, to: 60 }))
            .toEqual([ 17, 24, 36, 48, 60 ]);
    });

    it('если from переводится в целое количество лет, а to - нет', () => {
        expect(getTermValuesInMonths({ from: 24, to: 77 }))
            .toEqual([ 24, 36, 48, 60, 72, 77 ]);
    });

    it('если from и to не переводятся в целое количество лет', () => {
        expect(getTermValuesInMonths({ from: 31, to: 74 }))
            .toEqual([ 31, 36, 48, 60, 72, 74 ]);
    });
});
