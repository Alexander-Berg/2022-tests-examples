import {expect} from 'chai';
import Camera, {CenterWrapMode} from '../../../src/vector_render_engine/camera';
import * as vec2 from '../../../src/vector_render_engine/math/vector2';
import {getVisibleRegion} from '../../../src/vector_render_engine/util/camera/visible_region';
import {getVisibleRegionBBox} from '../../../src/vector_render_engine/util/camera/visible_region_bbox';

function setMagicParamsToCamera(camera: Camera): void {
    camera.center.x = 0.2207;
    camera.center.y = -0.9688;
    camera.zoom = 2.1;
    camera.tilt = 0.6981;
    camera.azimuth = 0.8897;
}

describe('Camera', () => {
    let camera: Camera;

    beforeEach(() => camera = new Camera());

    describe('center', () => {
        it('should clamp coordinates if wrap mode is set to CLAMP_TO_EDGE', () => {
            camera.center.x = 2;
            expect(camera.center.x).to.be.eq(1);
            camera.center.y = -2;
            expect(camera.center.y).to.be.eq(-1);
        });

        it('should cycle coordinates if wrap mode is set to REPEAT', () => {
            const camera = new Camera({wrapModeX: CenterWrapMode.REPEAT});
            camera.center.x = 2;
            expect(camera.center.x).to.be.eq(0);
        });
    });

    describe('zoom', () => {
        it('should be clamped to 0 and 24 by default', () => {
            camera.zoom = -1;
            expect(camera.zoom).to.be.eq(0);
            camera.zoom = 25;
            expect(camera.zoom).to.be.eq(24);
        });

        it('should be clamped to minZoom and maxZoom if passed', () => {
            const camera = new Camera({minZoom: 5, maxZoom: 9});
            expect(camera.zoom).to.be.eq(5);
            camera.zoom = 1;
            expect(camera.zoom).to.be.eq(5);
            camera.zoom = 10;
            expect(camera.zoom).to.be.eq(9);
        });
    });

    describe('tilt', () => {
        it('should be limited to 0 if zoom is small', () => {
            camera.tilt = 1;
            expect(camera.tilt).to.be.eq(0);
        });

        it('should not limited if zoom is > 2', () => {
            camera.zoom = 3;
            camera.tilt = 0.5;
            expect(camera.tilt).to.be.eq(0.5);
        });

        it('should be set to 0 if zoom goes back to 0', () => {
            camera.zoom = 3;
            camera.tilt = 1;
            camera.zoom = 0;
            expect(camera.tilt).to.be.eq(0);
        });
    });

    describe('getVisibleRegion()', () => {
        it.skip('should compute visible region', () => {
            setMagicParamsToCamera(camera);
            expect(getVisibleRegion(camera).map((p) => vec2.copy(p))).to.be.deep.eq([
                {x: -2.1614788985619593, y: -0.9035292128347766},
                {x: -0.33649085144444246, y: 1.3482185444828996},
                {x: 0.4291897433243458, y: -0.9745125389158935},
                {x: 0.26946568156591955, y: -1.1715868695791927}
            ]);
        });
    });

    describe('getVisibleBBox()', () => {
        it('should return bbox that encloses visible region', () => {
            setMagicParamsToCamera(camera);
            const bbox = getVisibleRegionBBox(camera);
            expect(getVisibleRegion(camera).every((p) => vec2.pointIsInBBox(p, bbox))).to.be.true;
        });
    });
});
