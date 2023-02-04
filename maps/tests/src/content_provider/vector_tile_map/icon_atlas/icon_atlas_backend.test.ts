import {expect} from 'chai';
import {
    IconAtlasBackend
} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/icon_atlas_backend';
import {IconAtlasRegion} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/icon_atlas/util/icon_atlas_region';

describe('IconAtlasBackend', () => {
    let atlas: IconAtlasBackend;

    beforeEach(() => {
        atlas = new IconAtlasBackend(1, 30, 20);
    });

    function generateTestData(width: number, height: number): Uint8Array {
        const count = width * height;
        return new Uint8Array(4 * count).fill(count & 0xFF);
    }

    function extractRegionPart(
        region: IconAtlasRegion,
        {x, y, width, height}: {x: number, y: number, width: number, height: number},
    ): Uint8Array {
        const src = new Uint32Array(region.data.buffer);
        const dst = new Uint32Array(width * height);
        const fullWidth = region.size.width;
        for (let j = 0; j < height; ++j) {
            for (let i = 0; i < width; ++i) {
                dst[j * width + i] = src[(y + j) * fullWidth + x + i];
            }
        }
        return new Uint8Array(dst.buffer);
    }

    it('should allocate icon', () => {
        const testData = generateTestData(10, 8);

        const ret = atlas.allocate({width: 10, height: 8}, testData);

        expect(ret).not.to.be.undefined;
        const region = atlas.flushUpdates()!;
        expect(region.offset).to.deep.equal({x: 0, y: 0});
        expect(region.size).to.deep.equal({width: 11, height: 9});
        expect(extractRegionPart(region, {x: 0, y: 0, width: 10, height: 8})).to.deep.equal(testData);
    });

    it('should not allocate icon if there is not enough space', () => {
        const ret = atlas.allocate({width: 40, height: 10}, generateTestData(40, 10));

        expect(ret).to.be.undefined;
    });

    it('should regard already occupied space during allocation', () => {
        atlas.allocate({width: 10, height: 8}, generateTestData(10, 8));
        atlas.flushUpdates();
        const testData = generateTestData(8, 6);

        const ret = atlas.allocate({width: 8, height: 6}, testData);

        expect(ret).not.to.be.undefined;
        const region = atlas.flushUpdates()!;
        expect(region.offset).to.deep.equal({x: 11, y: 0});
        expect(region.size).to.deep.equal({width: 9, height: 7});
        expect(extractRegionPart(region, {x: 0, y: 0, width: 8, height: 6})).to.deep.equal(testData);
    });

    it('should reset internal state', () => {
        atlas.allocate({width: 10, height: 8}, generateTestData(10, 8));
        atlas.reset();
        const testData = generateTestData(8, 6);

        const ret = atlas.allocate({width: 8, height: 6}, testData);

        expect(ret).not.to.be.undefined;
        const region = atlas.flushUpdates()!;
        expect(region.offset).to.deep.equal({x: 0, y: 0});
        expect(region.size).to.deep.equal({width: 9, height: 7});
        expect(extractRegionPart(region, {x: 0, y: 0, width: 8, height: 6})).to.deep.equal(testData);
    });

    it('should return *undefined* region initially', () => {
        expect(atlas.flushUpdates()).to.undefined;
    });

    it('should return *undefined* region after flushing', () => {
        atlas.allocate({width: 10, height: 9}, generateTestData(10, 9));
        atlas.flushUpdates();

        expect(atlas.flushUpdates()).to.undefined;
    });

    it('should return combined region of all allocated icons', () => {
        const testData1 = generateTestData(10, 8);
        const testData2 = generateTestData(4, 5);
        const testData3 = generateTestData(8, 9);
        atlas.allocate({width: 10, height: 8}, testData1);
        atlas.allocate({width: 4, height: 5}, testData2);
        atlas.allocate({width: 8, height: 9}, testData3);

        const region = atlas.flushUpdates()!;

        expect(region.offset).to.deep.equal({x: 0, y: 0});
        expect(region.size).to.deep.equal({width: 16, height: 19});
        expect(extractRegionPart(region, {x: 0, y: 0, width: 10, height: 8})).to.deep.equal(testData1);
        expect(extractRegionPart(region, {x: 11, y: 0, width: 4, height: 5})).to.deep.equal(testData2);
        expect(extractRegionPart(region, {x: 0, y: 9, width: 8, height: 9})).to.deep.equal(testData3);
    });

    it('should return icon location', () => {
        const loc1 = atlas.allocate({width: 10, height: 8}, generateTestData(10, 8));
        const loc2 = atlas.allocate({width: 4, height: 5}, generateTestData(4, 5));
        const loc3 = atlas.allocate({width: 8, height: 9}, generateTestData(8, 9));

        expect(loc1).to.deep.equal({minX: 0, minY: 0, maxX: 10, maxY: 8});
        expect(loc2).to.deep.equal({minX: 11, minY: 0, maxX: 15, maxY: 5});
        expect(loc3).to.deep.equal({minX: 0, minY: 9, maxX: 8, maxY: 18});
    });
});
