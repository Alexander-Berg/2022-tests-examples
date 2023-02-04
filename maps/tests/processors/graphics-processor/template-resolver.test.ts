import * as assert from 'assert';
import {templateResolver} from '../../../server/processors/graphics-processor';
import {RawTemplate, Line, Template} from '../../../server/processors/types/graphics-scheme';

const defaultParams = {
    linkOutterStrokeWidth: 12,
    path: 'M 760, 1950 L 760, 2000',
    linkClosedDashArray: [4, 4],
    color: 'ffc61a',
    x1: 760,
    x2: 760,
    y1: 2000,
    y2: 2050,
    linkClosedOverlayStrokeWidth: 4,
    linkHollowOverlayLineOpacity: 0,
    linkStrokeWidth: 8
};

const template1: RawTemplate = {
    id: 'linkPath',
    type: 'Template',
    layers: [
        {
            id: 'backLine',
            type: 'Path',
            opacity: 1,
            strokeLineJoin: 'round',
            strokeLineCap: 'butt',
            strokeWidth: '#{linkOutterStrokeWidth}',
            strokeColor: 'ffffff',
            path: '#{path}',
            z: 0
        }
    ]
};

const template2: RawTemplate = {
    id: 'linkLine',
    type: 'Template',
    layers: [
        ({
            id: 'backLine',
            type: 'Line',
            opacity: 1,
            strokeLineCap: 'butt',
            strokeWidth: '#{linkOutterStrokeWidth}',
            strokeColor: 'ffffff',
            y2: '#{y2}',
            x2: '#{x2}',
            y1: '#{y1}',
            x1: '#{x1}',
            z: 0
        } as unknown) as Line,
        ({
            id: 'frontLine',
            type: 'Line',
            opacity: 1,
            strokeLineCap: 'butt',
            strokeWidth: '#{linkStrokeWidth}',
            strokeColor: '#{color}',
            y2: '#{y2}',
            x2: '#{x2}',
            y1: '#{y1}',
            x1: '#{x1}',
            z: 1
        } as unknown) as Line,
        ({
            id: 'hollowOverlayLine',
            type: 'Line',
            opacity: '#{linkHollowOverlayLineOpacity}',
            strokeLineCap: 'butt',
            strokeWidth: '#{linkClosedOverlayStrokeWidth}',
            strokeColor: 'ffffff',
            y2: '#{y2}',
            x2: '#{x2}',
            y1: '#{y1}',
            x1: '#{x1}',
            z: 2
        } as unknown) as Line
    ]
};

describe('graphics-preprocessor', () => {
    describe('allRefResolver', () => {
        it('should not fail on empty layers', () => {
            assert.deepEqual(
                templateResolver({...template1, layers: []}, defaultParams),
                {...template1, layers: [], presets: undefined}
            );
        });

        it('should not fail on empty $params', () => {
            const expected = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        type: 'Path',
                        id: 'backLine',
                        opacity: 1,
                        strokeLinejoin: 'round',
                        strokeLinecap: 'butt',
                        strokeWidth: 'unknown',
                        stroke: '#ffffff',
                        d: 'unknown'
                    }
                ],
                presets: undefined
            };
            assert.deepEqual(templateResolver(template1, {}), expected);
        });

        it('should not fail on missing parameter field', () => {
            const params = {linkOutterStrokeWidth: 12};
            const obj: RawTemplate = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        id: 'backLine',
                        type: 'Path',
                        opacity: 1,
                        z: 1,
                        path: '#{path}'
                    }
                ]
            };

            const expected: Template = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        id: 'backLine',
                        type: 'Path',
                        opacity: 1,
                        d: 'unknown'
                    }
                ],
                presets: undefined
            };

            assert.deepEqual(templateResolver(obj, params), expected);
        });

        it('should not fail on "unknown" parameter field', () => {
            const params = {linkOutterStrokeWidth: 12, path: 'unknown'};
            const obj: RawTemplate = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        id: 'backLine',
                        type: 'Path',
                        opacity: 1,
                        z: 1,
                        path: '#{path}'
                    }
                ]
            };

            const expected: Template = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        id: 'backLine',
                        type: 'Path',
                        opacity: 1,
                        d: 'unknown'
                    }
                ],
                presets: undefined
            };

            assert.deepEqual(templateResolver(obj, params), expected);
        });

        it('should work with object w/o #{}-s', () => {
            const obj: RawTemplate = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        id: 'backLine',
                        type: 'Path',
                        opacity: 1,
                        z: 1,
                        path: '123321123321'
                    }
                ]
            };

            assert.deepEqual(
                templateResolver(obj, defaultParams),
                {
                    id: 'linkPath',
                    type: 'Template',
                    layers: [
                        {
                            id: 'backLine',
                            type: 'Path',
                            opacity: 1,
                            d: '123321123321'
                        }
                    ],
                    presets: undefined
                }
            );
        });

        it('should work on a single layer case', () => {
            const expected = {
                id: 'linkPath',
                type: 'Template',
                layers: [
                    {
                        type: 'Path',
                        id: 'backLine',
                        opacity: 1,
                        strokeLinejoin: 'round',
                        strokeLinecap: 'butt',
                        strokeWidth: 12,
                        stroke: '#ffffff',
                        d: 'M 760, 1950 L 760, 2000'
                    }
                ],
                presets: undefined
            };

            assert.deepEqual(templateResolver(template1, defaultParams), expected);
        });

        it('should work on a real case', () => {
            const expected = {
                id: 'linkLine',
                type: 'Template',
                layers: [
                    {
                        type: 'Line',
                        id: 'backLine',
                        opacity: 1,
                        strokeLinecap: 'butt',
                        strokeWidth: 12,
                        stroke: '#ffffff',
                        y2: 2050,
                        x2: 760,
                        y1: 2000,
                        x1: 760
                    },
                    {
                        type: 'Line',
                        id: 'frontLine',
                        opacity: 1,
                        strokeLinecap: 'butt',
                        strokeWidth: 8,
                        stroke: '#ffc61a',
                        y2: 2050,
                        x2: 760,
                        y1: 2000,
                        x1: 760
                    },
                    {
                        type: 'Line',
                        id: 'hollowOverlayLine',
                        opacity: 0,
                        strokeLinecap: 'butt',
                        strokeWidth: 4,
                        stroke: '#ffffff',
                        y2: 2050,
                        x2: 760,
                        y1: 2000,
                        x1: 760
                    }
                ],
                presets: undefined
            };

            assert.deepEqual(templateResolver(template2, defaultParams), expected);
        });
    });
});
