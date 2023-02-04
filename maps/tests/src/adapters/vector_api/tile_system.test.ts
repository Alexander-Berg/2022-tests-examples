import {assert, expect} from 'chai';
import {areFuzzyEqual} from '../../../../src/vector_render_engine/math/vector2';
import TileCoordinateSystem, {isParent} from '../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/tile_system';

const W = 2; // world size;

describe('tile coordinates system', () => {
    describe('coordinates conversion', () => {
        it('should properly calculate tile size in world coordinates', () => {
            expect(new TileCoordinateSystem(-3).getTileSize()).to.equal(W);
            expect(new TileCoordinateSystem(0).getTileSize()).to.equal(W);
            expect(new TileCoordinateSystem(0.5).getTileSize()).to.equal(W / 2);
            expect(new TileCoordinateSystem(1).getTileSize()).to.equal(W / 2);
            expect(new TileCoordinateSystem(4).getTileSize()).to.equal(W / 16);
        });

        it('should convert from world to tile coordinates', () => {
            assert(areFuzzyEqual(new TileCoordinateSystem(0).toTileCoordinates({x: 0, y: 0}), {x: 0.5, y: 0.5}));
            assert(areFuzzyEqual(new TileCoordinateSystem(1).toTileCoordinates({x: 0, y: 0}), {x: 1, y: 1}));
            assert(areFuzzyEqual(new TileCoordinateSystem(2).toTileCoordinates({x: 1, y: 1}), {x: 4, y: 0}));
            assert(areFuzzyEqual(new TileCoordinateSystem(2).toTileCoordinates({x: -1, y: 1}), {x: 0, y: 0}));
            assert(areFuzzyEqual(new TileCoordinateSystem(3).toTileCoordinates({x: 0.5, y: 0}), {x: 6, y: 4}));
            assert(areFuzzyEqual(
                new TileCoordinateSystem(3).toTileCoordinates({x: -3 / 8, y: -3 / 8}),
                {x: 2.5, y: 5.5})
            );
        });
    });

    it('should correctly identify relationships between two tiles', () => {
        expect(isParent({x: 0, y: 0, zoom: 0}, {x: 0, y: 0, zoom: 0})).to.be.false;
        expect(isParent({x: 0, y: 0, zoom: 0}, {x: 0, y: 0, zoom: 1})).to.be.true;
        expect(isParent({x: 0, y: 0, zoom: 0}, {x: 1, y: 1, zoom: 100})).to.be.true;

        expect(isParent({x: 1, y: 1, zoom: 1}, {x: 2, y: 2, zoom: 2})).to.be.true;
        expect(isParent({x: 1, y: 1, zoom: 1}, {x: 2, y: 3, zoom: 2})).to.be.true;
        expect(isParent({x: 1, y: 1, zoom: 1}, {x: 1, y: 2, zoom: 2})).to.be.false;
        expect(isParent({x: 1, y: 1, zoom: 1}, {x: 4, y: 2, zoom: 2})).to.be.false;
        expect(isParent({x: 1, y: 1, zoom: 1}, {x: 2, y: 1, zoom: 2})).to.be.false;
        expect(isParent({x: 1, y: 1, zoom: 1}, {x: 2, y: 4, zoom: 2})).to.be.false;

        expect(isParent({x: 2, y: 0, zoom: 2}, {x: 4, y: 0, zoom: 3})).to.be.true;

        expect(isParent({x: 2, y: 2, zoom: 2}, {x: 1, y: 1, zoom: 1})).to.be.false;
    });

});
