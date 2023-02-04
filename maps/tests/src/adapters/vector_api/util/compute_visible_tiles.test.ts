import Camera, {CenterWrapMode} from '../../../../../src/vector_render_engine/camera';
import {expect} from 'chai';
import {TileItem} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/tile_system';
import {computeVisibleTiles} from '../../../../../src/vector_render_engine/content_provider/vector_tile_map/util/tiles/visible_tile';
import {getVisibleRegion} from '../../../../../src/vector_render_engine/util/camera/visible_region';
import {getVisibleRegionBBox} from '../../../../../src/vector_render_engine/util/camera/visible_region_bbox';

describe('adapters/vector_api/util/compute_visible_tiles', () => {
    let camera: Camera;
    let tiles: TileItem[];

    beforeEach(() => {
        camera = new Camera({wrapModeX: CenterWrapMode.REPEAT});

        camera.center.x = 0.2207;
        camera.center.y = -0.9688;
        camera.zoom = 2.1;
        camera.tilt = 0.6981;
        camera.azimuth = 0.8897;

        tiles = [...computeVisibleTiles(
            getVisibleRegion(camera),
            getVisibleRegionBBox(camera),
            camera.options.wrapModeX,
            camera.options.wrapModeY,
            camera.zoom
        )];
    });

    it('should always return (0, 0, 0) for z=0', () => {
        const camera = new Camera();
        expect([...computeVisibleTiles(
            getVisibleRegion(camera),
            getVisibleRegionBBox(camera),
            camera.options.wrapModeX,
            camera.options.wrapModeY,
            camera.zoom
        )]).to.be.deep.eq([{
            x: 0,
            y: 0,
            zoom: 0
        }]);
    });

    it('should always return unique tile items', () => {
        expect(
            tiles.every(
                (tile) => tile === tiles.find(
                    ({x, y, zoom}) => x === tile.x && y === tile.y && zoom === tile.zoom
                )
            )
        ).to.be.true;
    });

    it('should always return tiles within the world on the same zoom', () => {
        const {zoom} = tiles[0];
        const tileCount = 1 << zoom;
        expect(
            tiles.every(
                (tile) => tile.zoom === zoom &&
                    0 <= tile.x && tile.x < tileCount &&
                    0 <= tile.y && tile.y < tileCount
            )
        ).to.be.true;
    });
});
