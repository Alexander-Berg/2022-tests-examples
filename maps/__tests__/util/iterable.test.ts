import {allOfIterable, filterIterable, findInIterable, mapIterable, oneOfIterable, reduceIterable, zipIterables} from '../../util/iterable';

describe('util/iterable', () => {
    function* zeroToFive(): IterableIterator<number> {
        yield* (new Array(6)).keys();
    }

    describe('findInIterable', () => {
        it('should return an item that satisfies a predicate if there\'s one', () => {
            expect(findInIterable(zeroToFive(), (n) => !!(n % 2))).toEqual(1);
        });

        it('should return `undefined` otherwise', () => {
            expect(findInIterable(zeroToFive(), () => false)).toBeUndefined();
        });
    });

    describe('filterIterable', () => {
        it('should filter items', () => {
            expect(Array.from(filterIterable(zeroToFive(), (n) => !(n % 2))))
                .toBeDeepCloseTo([0, 2, 4]);
        });
    });

    describe('mapIterable', () => {
        it('should map items', () => {
            expect(Array.from(mapIterable(zeroToFive(), (n) => 2 * n)))
                .toBeDeepCloseTo([0, 2, 4, 6, 8, 10]);
        });
    });

    describe('reduceIterable', () => {
        it('should reduce items', () => {
            expect(reduceIterable(zeroToFive(), (t, n) => t + n, 0)).toEqual(15);
        });
    });

    describe('oneOfIterable', () => {
        it('should return `true` if there\'s an item that satisfies a predicate', () => {
            expect(oneOfIterable(zeroToFive(), (n) => n == 2)).toBeTruthy();
        });

        it('should return `false` otherwise', () => {
            expect(oneOfIterable(zeroToFive(), (n) => n == -1)).toBeFalsy();
        });
    });

    describe('allOfIterable', () => {
        it('should return `true` if all items satisfy a predicate', () => {
            expect(allOfIterable(zeroToFive(), Number.isFinite)).toBeTruthy();
        });

        it('should return `false` otherwise', () => {
            expect(allOfIterable(zeroToFive(), (n) => n != 3)).toBeFalsy();
        });
    });

    describe('zipIterables', () => {
        it('should zip collections', () => {
            expect(Array.from(
                zipIterables(zeroToFive(), zeroToFive(), (n1, n2) => ([n1, n2]))
            ))
                .toBeDeepCloseTo([[0, 0], [1, 1], [2, 2], [3, 3], [4, 4], [5, 5]]);
        });

        it('should return collection of the same length as the shortest given', () => {
            function* failAfterFive(): IterableIterator<number> {
                yield* zeroToFive();
                yield 6;
                throw new Error('Should not reach this');
            }

            expect(Array.from(
                zipIterables(zeroToFive(), failAfterFive(), (n1, n2) => ([n1, n2]))
            ))
                .toBeDeepCloseTo([[0, 0], [1, 1], [2, 2], [3, 3], [4, 4], [5, 5]]);
        });
    });
});
