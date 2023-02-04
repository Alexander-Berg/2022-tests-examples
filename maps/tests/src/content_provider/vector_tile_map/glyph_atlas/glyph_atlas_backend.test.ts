import {expect} from 'chai';
import {GlyphAtlasBackend} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/glyph_atlas_backend';
import {Font}from '../../../../../src/vector_render_engine/util/font';
import {GlyphRangeIndex} from '../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/util/glyph_range';
import {computeSpanningBBox2} from '../../../../../src/vector_render_engine/math/vector2';
import {generateFont} from './util/font_stub';

const ATLAS_SIZE = 1000;

describe('GlyphAtlasBackend', () => {
    const FONT1: Font = generateFont('font#1', 2, 2);
    const FONT2: Font = generateFont('font#2', 5, 5);
    const FONT3: Font = generateFont('font#3', 10, 10);
    const FONT_BIG: Font = generateFont('font#big', 100, 100);
    const UNKNOWN_FONT_ID = 'font#unknown';
    const STORAGE = new Map([
        [FONT1.id, FONT1],
        [FONT2.id, FONT2],
        [FONT3.id, FONT3],
        [FONT_BIG.id, FONT_BIG]
    ]);
    let atlas: GlyphAtlasBackend;

    beforeEach(() => {
        atlas = new GlyphAtlasBackend(1, ATLAS_SIZE, ATLAS_SIZE);
    });

    it('should allocate glyphs', () => {
        const ranges1 = new GlyphRangeIndex([
            [FONT1.id, 0],
            [FONT2.id, 0]
        ]);
        const ranges2 = new GlyphRangeIndex([
            [FONT2.id, 128]
        ]);

        expect(atlas.allocate(ranges1, STORAGE)).to.be.true;
        expect(atlas.allocate(ranges2, STORAGE)).to.be.true;
        expect(atlas.allocatedRanges.containsAll(ranges1)).to.be.true;
        expect(atlas.allocatedRanges.containsAll(ranges2)).to.be.true;
        expect(atlas.allocatedGlyphs.size).to.be.equal(2);
        expect(atlas.allocatedGlyphs.get(FONT1.id)!.length).to.be.equal(128);
        expect(atlas.allocatedGlyphs.get(FONT2.id)!.length).to.be.equal(256);
    });

    it('should allocate glyph content with margins', () => {
        const ranges1 = new GlyphRangeIndex([[FONT1.id, 0]]);

        atlas.allocate(ranges1, STORAGE);

        expect(atlas.data.slice(0, 10)).to.be.deep.equal(new Uint8Array([0, 0, 0, 0, 1, 1, 1, 1, 2, 2]));

        const GLYPH_ID = 60;
        const location = atlas.allocatedGlyphs.get(FONT1.id)![GLYPH_ID];

        for (let i = location.minX; i < location.maxX; ++i) {
            for (let j = location.minY; j < location.maxY; ++j) {
                // this is how bitmap of a glyph is generated: it just filled with id
                expect(atlas.data[j * atlas.width + i]).to.be.equal(GLYPH_ID);
            }
        }
        expect(atlas.data[location.maxY * atlas.width + location.maxX]).not.to.be.equal(GLYPH_ID);
    });

    it('should not change state if it failed to allocate a range', () => {
        const ranges1 = new GlyphRangeIndex([[FONT1.id, 0]]);
        const ranges2 = new GlyphRangeIndex([[FONT2.id, 0]]);
        const rangesBig = new GlyphRangeIndex([[FONT3.id, 0], [FONT_BIG.id, 0]]);

        expect(atlas.allocate(ranges1, STORAGE)).to.be.true;
        expect(atlas.allocate(rangesBig, STORAGE)).to.be.false;
        expect(atlas.allocate(ranges2, STORAGE)).to.be.true;
    });

    it('should throw if there is a missing glyph description', () => {
        const ranges = new GlyphRangeIndex([
            [FONT1.id, 0],
            [UNKNOWN_FONT_ID, 0]
        ]);

        expect(atlas.allocate.bind(atlas, ranges, STORAGE)).to.throw();
    });

    it('should flush updated rectangle', () => {
        atlas.allocate(new GlyphRangeIndex([[FONT1.id, 0]]), STORAGE);
        const bboxOfFont1 = computeSpanningBBox2(atlas.allocatedGlyphs.get(FONT1.id)!)!;
        const update1 = atlas.flushUpdates()!;
        expect(update1.offset).to.be.deep.equal({
            x: alignLeft(bboxOfFont1.minX, 4),
            y: bboxOfFont1.minY
        });
        expect(update1.size).to.be.deep.equal({
            width: alignRight(bboxOfFont1.maxX, 4) - alignLeft(bboxOfFont1.minX, 4),
            height: bboxOfFont1.maxY - bboxOfFont1.minY
        });

        atlas.allocate(new GlyphRangeIndex([[FONT2.id, 0]]), STORAGE);
        const bboxOfFont2 = computeSpanningBBox2(atlas.allocatedGlyphs.get(FONT2.id)!)!;
        const update2 = atlas.flushUpdates()!;
        expect(update2.offset).to.be.deep.equal({
            x: alignLeft(bboxOfFont2.minX, 4),
            y: bboxOfFont2.minY
        });
        expect(update2.size).to.be.deep.equal({
            width: alignRight(bboxOfFont2.maxX, 4) - alignLeft(bboxOfFont2.minX, 4),
            height: bboxOfFont2.maxY - bboxOfFont2.minY
        });

    });

});

function alignLeft(x: number, alignment: number): number {
    return Math.floor(x / alignment) * alignment;
}

function alignRight(x: number, alignment: number): number {
    return Math.ceil(x / alignment) * alignment;
}
