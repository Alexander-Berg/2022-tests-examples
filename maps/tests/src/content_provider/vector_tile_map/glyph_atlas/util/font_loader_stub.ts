import {FontLoader} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/util/font_loader';
import {
    Font,
    UNKNOWN_FONT_MARGIN,
    UNKNOWN_FONT_XHEIGHT
} from '../../../../../../src/vector_render_engine/util/font';
import {generateFont} from './font_stub';
import {getGlyphRangeLength} from '../../../../../../src/vector_render_engine/content_provider/base_vector_content_provider/glyph_atlas/util/glyph_range';
import {LoadManager} from '../../../../../../src/vector_render_engine/util/load_manager';

export class FontLoaderStub extends FontLoader {
    constructor(
        loadManager: LoadManager,
        glyphWidth: number = 1,
        glyphHeight: number = 1,
        fontXheight: number = 1,
        fontMargin: number = 0
    ) {
        super(loadManager);

        this._glyphWidth = glyphWidth;
        this._glyphHeight = glyphHeight;
        this._fontXheight = fontXheight;
        this._fontMargin = fontMargin;
    }

    fillFont(url_: string, font: Font, glyphIdFrom: number): Promise<Font> {
        return new Promise((resolve) => {
            const subFont = generateFont(
                font.id,
                this._glyphWidth,
                this._glyphHeight,
                glyphIdFrom,
                getGlyphRangeLength(glyphIdFrom),
                this._fontXheight,
                this._fontMargin
            );
            if (font.xheight === UNKNOWN_FONT_XHEIGHT) {
                font.xheight = subFont.xheight;
            }
            if (font.margin === UNKNOWN_FONT_MARGIN) {
                font.margin = subFont.margin;
            }
            for (const glyph of subFont.glyphs) {
                font.glyphs[glyph.id] = glyph;
            }

            resolve(font);
        });
    }

    private readonly _glyphWidth: number;
    private readonly _glyphHeight: number;
    private readonly _fontXheight: number;
    private readonly _fontMargin: number;
}
