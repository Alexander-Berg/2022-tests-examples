import * as assert from 'assert';
import {getSchemeObjectsWithResolvedRefsInParams} from '../../../server/processors/graphics-processor';
import GraphicsScheme, {SchemeObject} from '../../../server/processors/types/graphics-scheme';

const graphics = {
    parameters: {
        colors: {
            background: 'ffffff',
            label: '12161a',
            labelBackground: 'fffff3',
            labelClosed: '999999',
            labelStroke: 'ffffffe6',
            lineIconText: 'ffffff',
            linkStroke: 'ffffff',
            multiserviceStationBackground: 'ffffff',
            stationClosedFill: '333333',
            stationStroke: 'ffffff',
            stationInTransferStroke: 'ffffff',
            stationSuperSimplifiedFill: 'ffffff',
            stationWarningFill: '333333',
            transferConnectionStroke: 'aaaaaa',
            transferFill: 'ffffff',
            transferOvergroundConnection: '979797',
            transferStroke: '000000',
            transferSuperSimplifiedFill: 'ffffff',
            transferSuperSimplifiedStroke: '000000',
            'line-1': 'da2128',
            'line-2': '48b85e',
            'line-3': '8e479b',
            'line-4': 'ffc61a'
        }
    }
};

const object1 = {
    id: '695fb574',
    type: 'Object',
    x: 0,
    y: 0,
    z: 1018,
    content: {
        $ref: '/common/templates/[linkLine]',
        $params: {
            color: {
                $ref: '/parameters/colors/line-4'
            },
            x1: 760,
            y1: 1850,
            x2: 760,
            y2: 1900
        }
    }
};

const object2 = {
    id: 'e67510e2',
    type: 'Object',
    x: 0,
    y: 0,
    z: 1016,
    content: {
        $ref: '/common/templates/[linkPath]',
        $params: {
            color: {
                $ref: '/parameters/colors/line-1'
            },
            linkStrokeWidth: 8,
            path: 'M 2054.32, 1598.93 A 621, 621 0 0 0 2097.94, 1488.17'
        }
    }
};

describe('graphics-preprocessor', () => {
    describe('getSchemeObjectsWithResolvedRefsInParams', () => {
        it('should not fail with an empty objects array', () => {
            assert.deepEqual(getSchemeObjectsWithResolvedRefsInParams([], (graphics as unknown) as GraphicsScheme), []);
        });

        it('should not fail with empty graphics', () => {
            const expected = [
                {
                    id: '695fb574',
                    type: 'Object',
                    x: 0,
                    y: 0,
                    z: 1018,
                    content: {
                        $ref: '/common/templates/[linkLine]',
                        $params: {
                            color: 'unknown',
                            x1: 760,
                            y1: 1850,
                            x2: 760,
                            y2: 1900,
                            x: 0,
                            y: 0
                        }
                    }
                }
            ];

            assert.deepEqual(
                getSchemeObjectsWithResolvedRefsInParams(
                    [(object1 as unknown) as SchemeObject],
                    ({} as unknown) as GraphicsScheme
                ),
                expected
            );
        });

        it('should work on a simple case', () => {
            const expected = [
                {
                    id: '695fb574',
                    type: 'Object',
                    x: 0,
                    y: 0,
                    z: 1018,
                    content: {
                        $ref: '/common/templates/[linkLine]',
                        $params: {
                            color: 'ffc61a',
                            x1: 760,
                            y1: 1850,
                            x2: 760,
                            y2: 1900,
                            x: 0,
                            y: 0
                        }
                    }
                }
            ];

            assert.deepEqual(
                getSchemeObjectsWithResolvedRefsInParams(
                    [(object1 as unknown) as SchemeObject],
                    (graphics as unknown) as GraphicsScheme
                ),
                expected
            );
        });

        it('should work for multiple objects', () => {
            const expected = [
                {
                    id: '695fb574',
                    type: 'Object',
                    x: 0,
                    y: 0,
                    z: 1018,
                    content: {
                        $ref: '/common/templates/[linkLine]',
                        $params: {
                            color: 'ffc61a',
                            x1: 760,
                            y1: 1850,
                            x2: 760,
                            y2: 1900,
                            x: 0,
                            y: 0
                        }
                    }
                },
                {
                    id: 'e67510e2',
                    type: 'Object',
                    x: 0,
                    y: 0,
                    z: 1016,
                    content: {
                        $ref: '/common/templates/[linkPath]',
                        $params: {
                            color: 'da2128',
                            linkStrokeWidth: 8,
                            path: 'M 2054.32, 1598.93 A 621, 621 0 0 0 2097.94, 1488.17',
                            x: 0,
                            y: 0
                        }
                    }
                }
            ];

            assert.deepEqual(
                getSchemeObjectsWithResolvedRefsInParams(
                    [(object1 as unknown) as SchemeObject, (object2 as unknown) as SchemeObject],
                    (graphics as unknown) as GraphicsScheme
                ),
                expected
            );
        });
    });
});
