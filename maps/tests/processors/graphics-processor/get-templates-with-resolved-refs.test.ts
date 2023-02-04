import * as assert from 'assert';
import {getTemplatesWithResolvedRefs} from '../../../server/processors/graphics-processor';
import GraphicsScheme, {RawTemplate} from '../../../server/processors/types/graphics-scheme';

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

const template1 = {
    id: 'transferSimplified',
    type: 'Template',
    layers: [
        {
            type: 'Path',
            id: 'border',
            opacity: 1,
            z: 2,
            strokeWidth: 2,
            strokeColor: {
                $ref: '/parameters/colors/transferStroke'
            },
            fillColor: {
                $ref: '/parameters/colors/transferFill'
            },
            path: '#{path}'
        }
    ]
};

const template2 = {
    id: 'transfer',
    type: 'Template',
    layers: [
        {
            type: 'Path',
            id: 'border',
            opacity: 1,
            z: 2,
            strokeWidth: 2,
            strokeColor: {
                $ref: '!parameters/colors___ss/stttransferStroke'
            },
            fillColor: {
                $ref: '/parameters/colors/transferFill'
            },
            path: '#{path}'
        },
        {
            type: 'Path',
            id: 'connection',
            opacity: 1,
            z: 3,
            strokeWidth: 2,
            strokeColor: {
                $ref: '/parameters/colors/transferConnectionStroke'
            },
            path: '#{connectionPath}'
        }
    ]
};

describe('graphics-preprocessor', () => {
    describe('getTemplatesWithResolvedRefs', () => {
        it('should work with empty templates array', () => {
            assert.deepEqual(getTemplatesWithResolvedRefs([], (graphics as unknown) as GraphicsScheme), []);
        });

        it('should not fail with empty layers', () => {
            const expected = [
                {
                    id: 'transferSimplified',
                    type: 'Template',
                    layers: [],
                    presets: undefined
                }
            ];

            assert.deepEqual(
                getTemplatesWithResolvedRefs([{...template1, layers: []}], ({} as unknown) as GraphicsScheme),
                expected
            );
        });

        it('should not fail with empty graphics', () => {
            const expected = [
                {
                    id: 'transferSimplified',
                    type: 'Template',
                    layers: [
                        {
                            type: 'Path',
                            id: 'border',
                            opacity: 1,
                            z: 2,
                            strokeWidth: 2,
                            strokeColor: 'unknown',
                            fillColor: 'unknown',
                            path: '#{path}'
                        }
                    ],
                    presets: undefined
                }
            ];

            assert.deepEqual(
                getTemplatesWithResolvedRefs(
                    [(template1 as unknown) as RawTemplate],
                    ({} as unknown) as GraphicsScheme
                ),
                expected
            );
        });

        it('should work on simple case', () => {
            const expected = [
                {
                    id: 'transferSimplified',
                    type: 'Template',
                    layers: [
                        {
                            type: 'Path',
                            id: 'border',
                            opacity: 1,
                            z: 2,
                            strokeWidth: 2,
                            strokeColor: '000000',
                            fillColor: 'ffffff',
                            path: '#{path}'
                        }
                    ],
                    presets: undefined
                }
            ];

            assert.deepEqual(
                getTemplatesWithResolvedRefs(
                    [(template1 as unknown) as RawTemplate],
                    (graphics as unknown) as GraphicsScheme
                ),
                expected
            );
        });

        it('should work for multiple templates', () => {
            const expected = [
                {
                    id: 'transferSimplified',
                    type: 'Template',
                    layers: [
                        {
                            type: 'Path',
                            id: 'border',
                            opacity: 1,
                            z: 2,
                            strokeWidth: 2,
                            strokeColor: '000000',
                            fillColor: 'ffffff',
                            path: '#{path}'
                        }
                    ],
                    presets: undefined
                },
                {
                    id: 'transfer',
                    type: 'Template',
                    layers: [
                        {
                            type: 'Path',
                            id: 'border',
                            opacity: 1,
                            z: 2,
                            strokeWidth: 2,
                            strokeColor: 'unknown',
                            fillColor: 'ffffff',
                            path: '#{path}'
                        },
                        {
                            type: 'Path',
                            id: 'connection',
                            opacity: 1,
                            z: 3,
                            strokeWidth: 2,
                            strokeColor: 'aaaaaa',
                            path: '#{connectionPath}'
                        }
                    ],
                    presets: undefined
                }
            ];

            assert.deepEqual(
                getTemplatesWithResolvedRefs(
                    [(template1 as unknown) as RawTemplate, (template2 as unknown) as RawTemplate],
                    (graphics as unknown) as GraphicsScheme
                ),
                expected
            );
        });
    });
});
