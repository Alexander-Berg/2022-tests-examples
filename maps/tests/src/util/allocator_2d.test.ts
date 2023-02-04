import {expect} from 'chai';
import {ShelfAllocator2D} from '../../../src/vector_render_engine/util/allocator_2d';

describe('Allocator 2D', () => {
    describe('shelf allocator', () => {
        let allocator: ShelfAllocator2D;

        beforeEach(() => {
            allocator = new ShelfAllocator2D(150, 150);
        });

        it('should allocate one bbox', () => {
            const bbox1 = allocator.allocate({width: 10, height: 10});
            expect(bbox1).to.be.deep.equal({minX: 0, maxX: 10, minY: 0, maxY: 10});
        });

        it('should allocate into a single shelf', () => {
            const bbox1 = allocator.allocate({width: 10, height: 10});
            const bbox2 = allocator.allocate({width: 10, height: 9});
            const bbox3 = allocator.allocate({width: 10, height: 10});

            expect(bbox1).to.be.deep.equal({minX: 0, maxX: 10, minY: 0, maxY: 10});
            expect(bbox2).to.be.deep.equal({minX: 10, maxX: 20, minY: 0, maxY: 9});
            expect(bbox3).to.be.deep.equal({minX: 20, maxX: 30, minY: 0, maxY: 10});
        });

        it('should allocate a new shelf if the height doesnt fit existing', () => {
            const bbox1 = allocator.allocate({width: 10, height: 10});
            const bbox2 = allocator.allocate({width: 10, height: 11});

            expect(bbox1).to.be.deep.equal({minX: 0, maxX: 10, minY: 0, maxY: 10});
            expect(bbox2).to.be.deep.equal({minX: 0, maxX: 10, minY: 10, maxY: 21});
        });

        it('should start a shelf if the previous one if full', () => {
            // create and fill 10 height shelf
            allocator.allocate({width: 100, height: 10});
            allocator.allocate({width: 45, height: 10});

            const bbox1 = allocator.allocate({width: 10, height: 10});
            expect(bbox1).to.be.deep.equal({minX: 0, maxX: 10, minY: 10, maxY: 20});
        });

        it('should allocate find a shelf with minimum height diff', () => {
            allocator.allocate({width: 100, height: 10}); // offset: 0
            allocator.allocate({width: 100, height: 20}); // offset: 10
            allocator.allocate({width: 100, height: 30}); // offset: 30
            allocator.allocate({width: 100, height: 15}); // offset: 60

            const bbox1 = allocator.allocate({width: 10, height: 17});
            const bbox2 = allocator.allocate({width: 10, height: 14});

            expect(bbox1).to.be.deep.equal({minX: 100, maxX: 110, minY: 10, maxY: 27});
            expect(bbox2).to.be.deep.equal({minX: 100, maxX: 110, minY: 60, maxY: 74});
        });

        it('should deallocate', () => {
            const bbox1 = allocator.allocate({width: 10, height: 10})!;
            const bbox2 = allocator.allocate({width: 10, height: 10})!;
            const bbox3 = allocator.allocate({width: 10, height: 10})!;

            expect(bbox1).to.be.deep.equal({minX: 0, maxX: 10, minY: 0, maxY: 10});

            allocator.deallocate(bbox1);
            allocator.deallocate(bbox2);

            const bbox4 = allocator.allocate({width: 15, height: 10});
            expect(bbox4).to.be.deep.equal({minX: 0, maxX: 15, minY: 0, maxY: 10});

            allocator.deallocate(bbox3);

            const bbox5 = allocator.allocate({width: 15, height: 10});
            expect(bbox5).to.be.deep.equal({minX: 15, maxX: 30, minY: 0, maxY: 10});
        });

        it('should return null if there is no room', () => {
            const bbox1 = allocator.allocate({width: 10, height: 151});

            expect(bbox1).to.be.deep.equal(null);
        });

        it('should return null if there is no room in width as well', () => {
            const bbox1 = allocator.allocate({width: 151, height: 10});

            expect(bbox1).to.be.deep.equal(null);
        });

        it('should resize', () => {
            allocator.allocate({width: 50, height: 50});
            expect(allocator.allocate({width: 150, height: 150})).to.be.deep.equal(null);

            allocator.resize(200, 200);
            expect(allocator.allocate({width: 150, height: 150}))
                .to.be.deep.equal({minX: 0, maxX: 150, minY: 50, maxY: 200});
            expect(allocator.allocate({width: 125, height: 25}))
                .to.be.deep.equal({minX: 50, maxX: 175, minY: 0, maxY: 25});
        });
    });
});
