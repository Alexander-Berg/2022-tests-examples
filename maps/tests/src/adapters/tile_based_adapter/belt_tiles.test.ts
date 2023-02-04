import {expect} from 'chai';
import {TileItem} from '../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/tile_system';
import {calculateBeltTiles} from '../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/belt_tiles';

function tilesComparator(a: TileItem, b: TileItem): number {
    return a.x - b.x || a.y - b.y;
}

describe('belt tiles calculation', () => {
    const zoom = 3;

    it('should return nothing for 0-sized belt', () => {
        expect([...calculateBeltTiles([
            {x: 2, y: 2, zoom},
            {x: 2, y: 3, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 3, zoom}
        ], 0, false, false)]).to.be.deep.equal([]);
    });

    it('should calculate belt tiles for single tile', () => {
        expect([...calculateBeltTiles([
            {x: 3, y: 3, zoom}
        ], 1, false, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 2, y: 2, zoom},
            {x: 2, y: 3, zoom},
            {x: 2, y: 4, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 4, zoom},
            {x: 4, y: 2, zoom},
            {x: 4, y: 3, zoom},
            {x: 4, y: 4, zoom}
        ].sort(tilesComparator));
    });

    it('should calculate belt tiles for a simple square visible region', () => {
        expect([...calculateBeltTiles([
            {x: 3, y: 3, zoom},
            {x: 3, y: 4, zoom},
            {x: 4, y: 3, zoom},
            {x: 4, y: 4, zoom}
        ], 1, false, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 2, y: 2, zoom},
            {x: 2, y: 3, zoom},
            {x: 2, y: 4, zoom},
            {x: 2, y: 5, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 5, zoom},
            {x: 4, y: 2, zoom},
            {x: 4, y: 5, zoom},
            {x: 5, y: 2, zoom},
            {x: 5, y: 3, zoom},
            {x: 5, y: 4, zoom},
            {x: 5, y: 5, zoom}
        ].sort(tilesComparator));
    });

    it('should calculate belt tiles for a rotated trapezoid visible region 1', () => {
        expect([...calculateBeltTiles([
            {x: 2, y: 5, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 3, zoom},
            {x: 3, y: 4, zoom},
            {x: 3, y: 5, zoom},
            {x: 4, y: 3, zoom},
            {x: 4, y: 4, zoom},
            {x: 4, y: 5, zoom},
            {x: 4, y: 6, zoom},
            {x: 5, y: 4, zoom},
            {x: 5, y: 5, zoom}
        ], 1, false, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 1, y: 4, zoom},
            {x: 1, y: 5, zoom},
            {x: 1, y: 6, zoom},
            {x: 2, y: 1, zoom},
            {x: 2, y: 2, zoom},
            {x: 2, y: 3, zoom},
            {x: 2, y: 4, zoom},
            {x: 2, y: 6, zoom},
            {x: 3, y: 1, zoom},
            {x: 3, y: 6, zoom},
            {x: 3, y: 7, zoom},
            {x: 4, y: 1, zoom},
            {x: 4, y: 2, zoom},
            {x: 4, y: 7, zoom},
            {x: 5, y: 2, zoom},
            {x: 5, y: 3, zoom},
            {x: 5, y: 6, zoom},
            {x: 5, y: 7, zoom},
            {x: 6, y: 3, zoom},
            {x: 6, y: 4, zoom},
            {x: 6, y: 5, zoom},
            {x: 6, y: 6, zoom}
        ].sort(tilesComparator));
    });

    it('should calculate belt tiles for a rotated trapezoid visible region 2', () => {
        expect([...calculateBeltTiles([
            {x: 2, y: 4, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 3, zoom},
            {x: 3, y: 4, zoom},
            {x: 4, y: 3, zoom},
            {x: 4, y: 4, zoom}
        ], 2, false, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 0, y: 2, zoom},
            {x: 0, y: 3, zoom},
            {x: 0, y: 4, zoom},
            {x: 0, y: 5, zoom},
            {x: 0, y: 6, zoom},
            {x: 1, y: 0, zoom},
            {x: 1, y: 1, zoom},
            {x: 1, y: 2, zoom},
            {x: 1, y: 3, zoom},
            {x: 1, y: 4, zoom},
            {x: 1, y: 5, zoom},
            {x: 1, y: 6, zoom},
            {x: 2, y: 0, zoom},
            {x: 2, y: 1, zoom},
            {x: 2, y: 2, zoom},
            {x: 2, y: 3, zoom},
            {x: 2, y: 5, zoom},
            {x: 2, y: 6, zoom},
            {x: 3, y: 0, zoom},
            {x: 3, y: 1, zoom},
            {x: 3, y: 5, zoom},
            {x: 3, y: 6, zoom},
            {x: 4, y: 0, zoom},
            {x: 4, y: 1, zoom},
            {x: 4, y: 2, zoom},
            {x: 4, y: 5, zoom},
            {x: 4, y: 6, zoom},
            {x: 5, y: 0, zoom},
            {x: 5, y: 1, zoom},
            {x: 5, y: 2, zoom},
            {x: 5, y: 3, zoom},
            {x: 5, y: 4, zoom},
            {x: 5, y: 5, zoom},
            {x: 5, y: 6, zoom},
            {x: 6, y: 1, zoom},
            {x: 6, y: 2, zoom},
            {x: 6, y: 3, zoom},
            {x: 6, y: 4, zoom},
            {x: 6, y: 5, zoom},
            {x: 6, y: 6, zoom}
        ].sort(tilesComparator));
    });

    it('should not return belt tiles outside of the range for current zoom', () => {
        expect([...calculateBeltTiles([
            {x: 1, y: 1, zoom}
        ], 2, false, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 0, y: 0, zoom},
            {x: 0, y: 1, zoom},
            {x: 0, y: 2, zoom},
            {x: 0, y: 3, zoom},
            {x: 1, y: 0, zoom},
            {x: 1, y: 2, zoom},
            {x: 1, y: 3, zoom},
            {x: 2, y: 0, zoom},
            {x: 2, y: 1, zoom},
            {x: 2, y: 2, zoom},
            {x: 2, y: 3, zoom},
            {x: 3, y: 0, zoom},
            {x: 3, y: 1, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 3, zoom}
        ].sort(tilesComparator));
    });

    it('should not return belt tiles outside of the range for current zoom', () => {
        expect([...calculateBeltTiles([
            {x: 2, y: 1, zoom},
            {x: 3, y: 0, zoom},
            {x: 3, y: 1, zoom},
            {x: 4, y: 0, zoom},
            {x: 5, y: 0, zoom},
            {x: 4, y: 7, zoom}
        ], 1, false, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 1, y: 0, zoom},
            {x: 1, y: 1, zoom},
            {x: 1, y: 2, zoom},
            {x: 2, y: 0, zoom},
            {x: 2, y: 2, zoom},
            {x: 3, y: 2, zoom},
            {x: 3, y: 6, zoom},
            {x: 3, y: 7, zoom},
            {x: 4, y: 1, zoom},
            {x: 4, y: 2, zoom},
            {x: 4, y: 6, zoom},
            {x: 5, y: 1, zoom},
            {x: 5, y: 6, zoom},
            {x: 5, y: 7, zoom},
            {x: 6, y: 0, zoom},
            {x: 6, y: 1, zoom}
        ].sort(tilesComparator));
    });

    it('should return tiles cycled in x direction only', () => {
        expect([...calculateBeltTiles([
            {x: 7, y: 7, zoom}
        ], 1, true, false)].sort(tilesComparator)).to.be.deep.equal([
            {x: 0, y: 6, zoom},
            {x: 0, y: 7, zoom},
            {x: 6, y: 6, zoom},
            {x: 6, y: 7, zoom},
            {x: 7, y: 6, zoom}
        ].sort(tilesComparator));
    });

    it('should return tiles cycled in y direction only', () => {
        expect([...calculateBeltTiles([
            {x: 7, y: 7, zoom}
        ], 1, false, true)].sort(tilesComparator)).to.be.deep.equal([
            {x: 6, y: 0, zoom},
            {x: 6, y: 6, zoom},
            {x: 6, y: 7, zoom},
            {x: 7, y: 0, zoom},
            {x: 7, y: 6, zoom}
        ].sort(tilesComparator));
    });

    it('should return tiles cycled in both coordinates', () => {
        expect([...calculateBeltTiles([
            {x: 7, y: 7, zoom}
        ], 1, true, true)].sort(tilesComparator)).to.be.deep.equal([
            {x: 0, y: 0, zoom},
            {x: 0, y: 6, zoom},
            {x: 0, y: 7, zoom},
            {x: 6, y: 0, zoom},
            {x: 6, y: 6, zoom},
            {x: 6, y: 7, zoom},
            {x: 7, y: 0, zoom},
            {x: 7, y: 6, zoom}
        ].sort(tilesComparator));
    });

    it('should return tiles cycled in both coordinates', () => {
        expect([...calculateBeltTiles([
            {x: 0, y: 0, zoom}
        ], 1, true, true)].sort(tilesComparator)).to.be.deep.equal([
            {x: 0, y: 1, zoom},
            {x: 0, y: 7, zoom},
            {x: 1, y: 0, zoom},
            {x: 1, y: 1, zoom},
            {x: 1, y: 7, zoom},
            {x: 7, y: 0, zoom},
            {x: 7, y: 1, zoom},
            {x: 7, y: 7, zoom}
        ].sort(tilesComparator));
    });

});
