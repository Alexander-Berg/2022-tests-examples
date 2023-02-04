import {expect} from 'chai';
import {
    swap,
    reverse,
    rotate,
    copy,
    zip,
    compare,
    insertionSort,
    mergeSort,
    spliceElement
} from '../../../src/vector_render_engine/util/array';

interface Item {
    key: number;
}

function objectComparator(a: Item, b: Item): number {
    return a.key - b.key;
}

function numericComparator(a: number, b: number): number {
    return a - b;
}

describe('array', () => {
    let a: number[];

    beforeEach(() => {
        a = [0, 1, 2, 3, 4, 5];
    });

    describe('swap', () => {
        it('should swap elements', () => {
            swap(a, 3, 5);
            expect(a).to.be.deep.eq([0, 1, 2, 5, 4, 3]);
        });
    });

    describe('reverse', () => {
        it('should reverse an array', () => {
            reverse(a);
            expect(a).to.be.deep.eq([5, 4, 3, 2, 1, 0]);
        });

        it('should reverse subarray', () => {
            reverse(a, 2, 5);
            expect(a).to.be.deep.eq([0, 1, 4, 3, 2, 5]);
        });
    });

    describe('rotate', () => {
        it('should rotate whole array in positive direction', () => {
            rotate(a, 1);
            expect(a).to.be.deep.eq([5, 0, 1, 2, 3, 4]);
            rotate(a, 3);
            expect(a).to.be.deep.eq([2, 3, 4, 5, 0, 1]);
        });

        it('should rotate subarray in positive direction', () => {
            rotate(a, 1, 2);
            expect(a).to.be.deep.eq([0, 1, 5, 2, 3, 4]);
        });
    });

    describe('copy', () => {
        it('should copy array', () => {
            const b = new Array(a.length);
            copy(a, b);
            expect(b).to.be.deep.eq(a);
        });

        it('should copy subarray', () => {
            const b = new Array(a.length);
            b.fill(0);
            copy(a, b, 3, 5, 3);
            expect(b).to.be.deep.eq([0, 0, 0, 3, 4, 0]);
        });
    });

    describe('zip', () => {
        it('should zip arrays', () => {
            expect(zip(a, a, (n1, n2) => ([n1, n2])))
                .to.be.deep.eq([[0, 0], [1, 1], [2, 2], [3, 3], [4, 4], [5, 5]]);
        });
    });

    describe('compare', () => {
        it('should compare arrays', () => {
            expect(compare(numericComparator, [], [])).to.be.eq(0);
            expect(compare(numericComparator, [1, 2, 3], [1, 2, 3])).to.be.eq(0);
            expect(compare(numericComparator, [1, 2, 3], [1, 2, 3, 4])).to.be.lt(0);
            expect(compare(numericComparator, [1, 2, 3], [2, 1, 3])).to.be.lt(0);
            expect(compare(numericComparator, [100], [1, 2, 3])).to.be.gt(0);
        });
    });

    describe('insertionSort', () => {
        const N = 32;
        const a = new Array(N);
        const aCopy = new Array(N);

        beforeEach(() => {
            for (let i = 0; i < N; ++i) {
                a[i] = aCopy[i] = Math.random();
            }
        });

        it('should sort an array', () => {
            insertionSort(a, numericComparator);

            for (let i = 1; i < N; ++i) {
                expect(a[i - 1] <= a[i]).to.be.true;
            }
        });

        it('should sort a range in an array', () => {
            insertionSort(a, numericComparator, 8, 24);

            for (let i = 0; i < 8; ++i) {
                expect(a[i]).to.be.eq(aCopy[i]);
            }

            for (let i = 9; i < 24; ++i) {
                expect(a[i - 1] <= a[i]).to.be.true;
            }

            for (let i = 24; i < N; ++i) {
                expect(a[i]).to.be.eq(aCopy[i]);
            }
        });
    });

    describe('mergeSort', () => {
        const N = 123;
        const a = new Array(N);

        beforeEach(() => {
            for (let i = 0; i < N; ++i) {
                a[i] = {
                    // To thoroughly test stability of the merge sort, use
                    // only a handful of values to ensure there're plenty of
                    // repeats.
                    value: 64 * Math.random() | 0,
                    idx: i
                };
            }
        });

        it('should stably sort an array', () => {
            mergeSort(a, (a, b) => a.value - b.value);

            for (let i = 1; i < N; ++i) {
                expect(
                    // Either values are distinct...
                    a[i - 1].value < a[i].value ||
                    // ...or they're in the same order as generated.
                    (a[i - 1].value === a[i].value && a[i - 1].idx < a[i].idx)
                ).to.be.true;
            }
        });

        it('should stably sort a range in an array', () => {
            const START = 17;
            const END = 73;

            mergeSort(a, (a, b) => a.value - b.value, START, END);

            for (let i = 0; i < START; ++i) {
                expect(a[i].idx).to.be.eq(i);
            }

            for (let i = START + 1; i < END; ++i) {
                expect(
                    // Either values are distinct...
                    a[i - 1].value < a[i].value ||
                    // ...or they're in the same order as generated.
                    (a[i - 1].value === a[i].value && a[i - 1].idx < a[i].idx)
                ).to.be.true;
            }

            for (let i = END; i < N; ++i) {
                expect(a[i].idx).to.be.eq(i);
            }
        });
    });

    describe('spliceElement', () => {
        it('should remove element', () => {
            const a = [1, 2, 3, 4, 5];

            expect(spliceElement(a, 3)).to.equal(true);
            expect(a).to.deep.equal([1, 2, 4, 5]);

            expect(spliceElement(a, 1)).to.equal(true);
            expect(a).to.deep.equal([2, 4, 5]);

            expect(spliceElement(a, 5)).to.equal(true);
            expect(a).to.deep.equal([2, 4]);
        });

        it('should not remove element', () => {
            const a = [1, 2, 3];

            expect(spliceElement(a, 4)).to.equal(false);
            expect(a).to.deep.equal([1, 2, 3]);
        });
    });
});
