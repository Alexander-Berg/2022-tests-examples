import {expect} from 'chai';
import {
    GLYPH_ID_RANGE_LENGTH_CHANGE,
    GlyphRangeIndex
} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/util/glyph_range';

describe('GlyphRangeIndexAtlasAllocator', () => {
    it('should add proper ranges for glyphs', () => {
        const FONT_ID = 'font#1';
        const ranges = new GlyphRangeIndex();

        ranges.addRangesForGlyphs(
            FONT_ID,
            0,
            10,
            100,
            150,
            1050,
            GLYPH_ID_RANGE_LENGTH_CHANGE + 100,
            GLYPH_ID_RANGE_LENGTH_CHANGE + 101
        );
        const storedRanges = [...[...ranges][0]];

        expect(storedRanges[0]).to.be.equal(FONT_ID);
        expect([...storedRanges[1]]).to.be.deep.equal([
            [0, 128],
            [128, 256],
            [1024, 1152],
            [GLYPH_ID_RANGE_LENGTH_CHANGE + 100, GLYPH_ID_RANGE_LENGTH_CHANGE + 100 + 1],
            [GLYPH_ID_RANGE_LENGTH_CHANGE + 101, GLYPH_ID_RANGE_LENGTH_CHANGE + 101 + 1]
        ]);
    });

    it('should tell the truth about its content', () => {
        const FONT_ID1 = 'font#1';
        const FONT_ID2 = 'font#2';
        const ranges = new GlyphRangeIndex();

        ranges.addRangesForGlyphs(FONT_ID1, 0, 260);
        ranges.addRangesForGlyphs(FONT_ID2, 130);

        expect(ranges.contains(FONT_ID1, 0)).to.be.true;
        expect(ranges.contains(FONT_ID1, 128)).to.be.false;
        expect(ranges.contains(FONT_ID1, 256)).to.be.true;
        expect(ranges.contains(FONT_ID1, 384)).to.be.false;
        expect(ranges.contains(FONT_ID2, 0)).to.be.false;
        expect(ranges.contains(FONT_ID2, 128)).to.be.true;
        expect(ranges.contains(FONT_ID2, 256)).to.be.false;

        const subRanges1 = new GlyphRangeIndex();
        subRanges1.addRangesForGlyphs(FONT_ID1, 0);
        subRanges1.addRangesForGlyphs(FONT_ID2, 140);
        expect(ranges.containsAll(subRanges1)).to.be.true;

        const subRanges2 = new GlyphRangeIndex();
        subRanges2.addRangesForGlyphs(FONT_ID1, 0, 1500);
        expect(ranges.containsAll(subRanges2)).to.be.false;
    });

});
