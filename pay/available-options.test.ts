import { getOptionByValue, createOptionIterator } from './available-options';

const OPTIONS_LIST = [
    { value: 1, items: [1] },
    { value: 2, items: [2] },
    { value: 3, items: [3] },
];

describe('available-options', function () {
    describe('getOptionByValue', function () {
        it('should return exist value', () => {
            expect(getOptionByValue(OPTIONS_LIST, 1, [])).toStrictEqual([1]);
            expect(getOptionByValue(OPTIONS_LIST, 2, [])).toStrictEqual([2]);
            expect(getOptionByValue(OPTIONS_LIST, 3, [])).toStrictEqual([3]);
        });

        it('should return default value', () => {
            expect(getOptionByValue(OPTIONS_LIST, 5, [])).toStrictEqual([]);
            expect(getOptionByValue(OPTIONS_LIST, 5, [-1])).toStrictEqual([-1]);
        });
    });

    describe('createOptionIterator', function () {
        it('should return next value', () => {
            const getNext = createOptionIterator(OPTIONS_LIST);

            expect(getNext()).toStrictEqual([1]);
            expect(getNext()).toStrictEqual([2]);
            expect(getNext()).toStrictEqual([3]);
            expect(getNext()).toStrictEqual(null);
            expect(getNext()).toStrictEqual(null);
        });

        it('should return freeze on last value', () => {
            const getNext = createOptionIterator(OPTIONS_LIST, { freezeOnLast: true });

            expect(getNext()).toStrictEqual([1]);
            expect(getNext()).toStrictEqual([2]);
            expect(getNext()).toStrictEqual([3]);
            expect(getNext()).toStrictEqual([3]);
        });
    });
});
