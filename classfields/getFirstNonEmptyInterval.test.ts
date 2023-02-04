import getFirstNonEmptyInterval from './getFirstNonEmptyInterval';

it('возвращает правильный массив', () => {
    const array = [ undefined, undefined, 1, 2, 3, undefined, undefined, 4, 5, undefined ];
    const result = getFirstNonEmptyInterval(array);
    expect(result).toEqual([ 1, 2, 3 ]);
});
