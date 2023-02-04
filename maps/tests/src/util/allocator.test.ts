import {expect} from 'chai';
import {ArenaAllocator, FreeListAllocator} from '../../../src/vector_render_engine/util/allocator';

describe('allocator', () => {
    describe('arena allocator', () => {
        let allocator: ArenaAllocator;

        beforeEach(() => {
            allocator = new ArenaAllocator(100);
        });

        it('should allocate sequential data', () => {
            expect(allocator.allocate(10)).to.be.equal(0);
            expect(allocator.allocate(10)).to.be.equal(10);
            expect(allocator.allocate(10)).to.be.equal(20);
            expect(allocator.allocate(10)).to.be.equal(30);
        });

        it('should deallocate', () => {
            const offset1 = allocator.allocate(10);
            const offset2 = allocator.allocate(10);
            const offset3 = allocator.allocate(10);

            allocator.deallocate(offset1);
            allocator.deallocate(offset2);

            expect(allocator.isEmpty).to.be.false;

            allocator.deallocate(offset3);

            expect(allocator.isEmpty).to.be.true;
        });

        it('should not overflow', () => {
            allocator.allocate(50);
            allocator.allocate(30);

            expect(allocator.allocate(30)).to.be.eq(-1);
        });

        it('should not overflow even after freeing up', () => {
            const offset1 = allocator.allocate(50);
            const offset2 = allocator.allocate(30);

            expect(allocator.allocate(30)).to.be.eq(-1);

            allocator.deallocate(offset1);
            allocator.deallocate(offset2);

            expect(allocator.allocate(30)).to.be.eq(-1);
        });
    });

    describe('free list allocator', () => {
        let allocator: FreeListAllocator;

        beforeEach(() => {
            allocator = new FreeListAllocator(100);
        });

        it('should allocate sequential data', () => {
            expect(allocator.allocate(10)).to.be.equal(0);
            expect(allocator.allocate(10)).to.be.equal(10);
            expect(allocator.allocate(10)).to.be.equal(20);
            expect(allocator.allocate(10)).to.be.equal(30);
        });

        it('should reuse freed ranges', () => {
            const offsets = [
                allocator.allocate(10),
                allocator.allocate(10),
                allocator.allocate(10)
            ];

            allocator.deallocate(offsets[1]);
            expect(allocator.allocate(10)).to.be.equal(10);

            allocator.deallocate(offsets[1]);
            expect(allocator.allocate(11)).to.be.equal(30);
        });

        it('should union freed ranges', () => {
            const offsets = [
                allocator.allocate(25),
                allocator.allocate(25),
                allocator.allocate(25),
                allocator.allocate(25)
            ];

            expect(allocator.maxAllocableSize).to.be.equal(0);

            allocator.deallocate(offsets[1]);
            expect(allocator.maxAllocableSize).to.be.equal(25);

            allocator.deallocate(offsets[2]);
            expect(allocator.maxAllocableSize).to.be.equal(50);
        });

        it('should resize', () => {
            allocator.allocate(100);
            allocator.extend(150);
            expect(allocator.allocate(50)).to.be.equal(100);
        });

        it('should allow allocation after resize', () => {
            allocator.allocate(25);
            expect(allocator.allocate(125)).to.be.equal(-1);

            allocator.extend(150);
            expect(allocator.allocate(125)).to.be.equal(25);
        });

    });
});
