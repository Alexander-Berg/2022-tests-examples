import {expect} from 'chai';
import {AtlasAllocator} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/util/atlas_allocator';

describe('AtlasAllocator', () => {
    it('should return position of allocated area in case of successful allocation', () => {
        const allocator = new AtlasAllocator(10, 10);
        const allocations = [
            allocator.allocate(2, 3),
            allocator.allocate(3, 3),
            allocator.allocate(4, 2),
            allocator.allocate(4, 3)
        ];

        for (let i = 0; i < allocations.length; ++i) {
            for (let j = i + 1; j < allocations.length; ++j) {
                const allocationA = allocations[i];
                const allocationB = allocations[j];

                expect(allocationA).not.to.be.undefined;
                expect(allocationB).not.to.be.undefined;

                // check intersections, "<=" comparison because of half-closed interval borders nature
                expect(
                    allocationA!.maxX <= allocationB!.minX ||
                    allocationB!.maxX <= allocationA!.minX ||
                    allocationA!.maxY <= allocationB!.minY ||
                    allocationB!.maxY <= allocationA!.minY
                ).to.be.true;
            }
        }
    });

    it('should return nothing in case of failed allocation', () => {
        const allocator = new AtlasAllocator(10, 10);
        const allocations1 = allocator.allocate(3, 3);
        const allocations2 = allocator.allocate(8, 8);

        expect(allocations1).not.to.be.undefined;
        expect(allocations2).to.be.undefined;
    });

    it('should restores to saved points', () => {
        const allocator = new AtlasAllocator(10, 10);

        allocator.allocate(3, 3);

        const restorePoint = allocator.createRestorePoint();

        expect(allocator.allocate(5, 5)).not.to.be.undefined;
        expect(allocator.allocate(6, 6)).to.be.undefined;

        allocator.resetToRestorePoint(restorePoint);
        expect(allocator.allocate(6, 6)).not.to.be.undefined;
    });
});
