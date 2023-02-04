import {Font} from '../../../../../../src/vector_render_engine/util/font';

/**
 * Generates stubby font with given number of equal glyphs, bitmap of a glyph is filled with its id mod 256.
 */
export function generateFont(
    id: string,
    glyphWidth: number,
    glyphHeight: number,
    startGlyphId: number = 0,
    numberOfGlyphs: number = 1024,
    xheight: number = 10,
    margin: number = 1
): Font {
    return {
        id,
        xheight,
        margin,
        glyphs: new Array(numberOfGlyphs).fill(0).map((_zero, index) => ({
            id: startGlyphId + index,
            width: glyphWidth,
            height: glyphHeight,
            bearingX: 0,
            bearingY: 0,
            advance: 0,
            bitmap: new Uint8Array((glyphWidth + 2 * margin) * (glyphHeight + 2 * margin)).fill(startGlyphId + index)
        }))
    }
}
