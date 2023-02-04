import {expect} from 'chai';
import 'tests/chai-extensions';
import * as boundsUtils from 'src/v2/bounds';
import * as projection from 'src/v2/wgs84-mercator';

function toPixels(coords: number[][]) {
    return coords.map((point) => projection.toPixels(point));
}

describe('bounds', () => {
    describe('toPixelBounds()/fromPixelBounds()', () => {
        it('should return same bounds when project/unproject', () => {
            const originBounds = [
                [35.453125, 41.138245846317766],
                [86.07812499999999, 64.60372145191228]
            ];

            const pixelBounds = boundsUtils.toPixelBounds(originBounds);
            const resultBounds = boundsUtils.fromPixelBounds(pixelBounds);

            expect(resultBounds).to.roughlyEqualBounds(originBounds);
        });
    });

    describe('pixelBoundsFromPoints()', () => {
        it('should compute bbox by given points', () => {
            const actual = boundsUtils.pixelBoundsFromPoints(toPixels([
                [79.11828613281249, 58.41272001458921],
                [98.1026611328125, 67.72816617554786],
                [127.6339111328125, 58.596723550320284],
                [103.02453613281251, 46.721576214323626],
                [140.2901611328125, 70.0093161117863],
                [130.79797363281247, 29.047377116763784],
                [99.86047363281249, 63.051480485377446],
                [92.47766113281247, 46.96280619735023],
                [118.8448486328125, 51.99619577811837]
            ]));

            const expected = boundsUtils.toPixelBounds([
                [79.11828613281249, 29.047377116763784],
                [140.2901611328125, 70.0093161117863]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });

        it('should compute bbox which cross 180th meridian', () => {
            const actual = boundsUtils.pixelBoundsFromPoints(toPixels([
                [165.6026611328125, 60.2970250865535],
                [179.8409423828125, 64.90397410616073],
                [-149.3973388671875, 65.85694695094064]
            ]));

            const expected = boundsUtils.toPixelBounds([
                [165.6026611328125, 60.2970250865535],
                [-149.3973388671875, 65.85694695094064]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });

        it('should return empty bbox for empty array of points', () => {
            const actual = boundsUtils.pixelBoundsFromPoints([]);

            const expected = [
                [0, 0],
                [0, 0]
            ];

            expect(actual).to.roughlyEqualBounds(expected);
        });

        it('should compute bbox for array with same points', () => {
            const actual = boundsUtils.pixelBoundsFromPoints(toPixels([
                [32.9783869385119, 31.087167102047204],
                [32.9783869385119, 31.087167102047204],
                [32.9783869385119, 31.087167102047204]
            ]));

            const expected = boundsUtils.toPixelBounds([
                [32.978386938511881, 31.087167102047204],
                [32.978386938511881, 31.087167102047204]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });
    });

    describe('pixelBoundsFromBounds()', () => {
        it('should return bbox covering all given bboxes', () => {
            const actual = boundsUtils.pixelBoundsFromBounds([
                boundsUtils.toPixelBounds([
                    [37.562500000000014, 54.728873883442496],
                    [49.515625, 60.73033544341544]
                ]),
                boundsUtils.toPixelBounds([
                    [98.03124999999996, 35.607890837936296],
                    [107.17187500000001, 41.66763873604122]
                ]),
                boundsUtils.toPixelBounds([
                    [60.765625000000014, 22.037050808076],
                    [69.20312499999999, 28.4276653110395]
                ])
            ]);

            const expected = boundsUtils.toPixelBounds([
                [37.562500000000014, 22.037050808076],
                [107.17187500000001, 60.73033544341544]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });

        it('should return bbox which cross 180th meridian', () => {
            const actual = boundsUtils.pixelBoundsFromBounds([
                boundsUtils.toPixelBounds([
                    [150.0625, 53.90677810487574],
                    [160.609375, 58.596723550320306]
                ]),
                boundsUtils.toPixelBounds([
                    [-141.03125, 69.52258728196277],
                    [-130.484375, 73.320407451055]
                ]),
                boundsUtils.toPixelBounds([
                    [-152.28125, 22.69103331638806],
                    [-129.078125, 31.488371130275752]
                ])
            ]);

            const expected = boundsUtils.toPixelBounds([
                [150.0625, 22.69103331638806],
                [-129.078125, 73.320407451055]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });

        it('should return bbox covering two cross bboxes', () => {
            const actual = boundsUtils.pixelBoundsFromBounds([
                boundsUtils.toPixelBounds([
                    [27.01562500000003, 25.912474654986703],
                    [74.125, 56.71267308549162]
                ]),
                boundsUtils.toPixelBounds([
                    [57.95312500000002, 44.752441540500875],
                    [94.515625, 66.63657884436869]
                ])
            ]);

            const expected = boundsUtils.toPixelBounds([
                [27.01562500000003, 25.912474654986703],
                [94.515625, 66.63657884436869]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });

        it('should return external bbox when one bbox inside another', () => {
            const externalBounds = boundsUtils.toPixelBounds([
                [48.10937500000001, 32.68553116191057],
                [128.265625, 65.7848673253621]
            ]);

            const actual = boundsUtils.pixelBoundsFromBounds([
                externalBounds,
                boundsUtils.toPixelBounds([
                    [72.71875, 47.20294659392695],
                    [80.453125, 50.00000000000525]
                ])
            ]);

            expect(actual).to.roughlyEqualBounds(externalBounds);
        });

        it('should return bbox which includes all points of given bboxes', () => {
            const actual = boundsUtils.pixelBoundsFromBounds([
                boundsUtils.toPixelBounds([
                    [-170, 0],
                    [170, 10]
                ]),
                // Use two same bounds for skip optimization with one parameter.
                boundsUtils.toPixelBounds([
                    [-170, 0],
                    [170, 10]
                ])
            ]);

            const expected = boundsUtils.toPixelBounds([
                [-170, 0],
                [170, 10]
            ]);

            expect(actual).to.roughlyEqualBounds(expected);
        });
    });

    describe('tileBoundsFromPixelBounds()', () => {
        it('should return tile bbox for pixel bbox', () => {
            const actual = boundsUtils.tileBoundsFromPixelBounds([
                [10, 10],
                [300, 510]
            ]);
            expect(actual).to.deep.equal([[0, 0], [1, 1]]);
        });

        it('should handle case when pixel bbox hits to the tile boundary', () => {
            const actual = boundsUtils.tileBoundsFromPixelBounds([
                [0, 10],
                [300, 512]
            ]);
            expect(actual).to.deep.equal([[0, 0], [1, 1]]);
        });
    });

    describe('pixelBoundsFromTileBounds()', () => {
        it('should return pixel bbox for tile bbox', () => {
            const actual = boundsUtils.pixelBoundsFromTileBounds([
                [0, 0],
                [1, 1]
            ]);
            expect(actual).to.deep.equal([[0, 0], [512, 512]]);
        });

        it('should return correct pixel bbox for single tile', () => {
            const actual = boundsUtils.pixelBoundsFromTileBounds([
                [0, 0],
                [0, 0]
            ]);
            expect(actual).to.deep.equal([[0, 0], [256, 256]]);
        });
    });
});
