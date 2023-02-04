import {expect} from 'chai';

import BinaryHeap from '../../../src/vector_render_engine/util/binary_heap';

describe('util/heap', () => {
    let h: BinaryHeap<number>;
    let i: number;

    beforeEach(() => {
        h = new BinaryHeap<number>();

        for (let n = 32; n > 0; --n) {
            h.insert(Math.random());
        }

        i = Math.random();

        h.insert(i);
    });

    describe('pop', () => {
        it('should remove maximum number and return it', () => {
            const max = h.pop()!;

            for (const i of h) {
                expect(i).to.be.lte(max);
            }

            expect(h.size).to.be.eq(32);
        });
    });

    describe('peek', () => {
        it('should return maximum number w/o removing it', () => {
            const max = h.peek()!;

            for (const i of h) {
                expect(i).to.be.lte(max);
            }

            expect(h.size).to.be.eq(33);
        });
    });

    describe('remove', () => {
        it('should remove element', () => {
            h = new BinaryHeap<number>();
    
            for (let n = 32; n > 0; --n) {
                h.insert(n);
            }
    
            h.remove((item) => { return item === 4; });
            for (const i of h) {
                expect(i).not.to.be.eql(4);
            }
            expect(h.size).to.be.eq(31);
        });
    });
});
