import * as assert from 'assert';
import {allRefsResolver} from '../../../server/processors/graphics-processor';
import GraphicsScheme, {ParamProp} from '../../../server/processors/types/graphics-scheme';

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

describe('graphics-preprocessor', () => {
    describe('allRefResolver', () => {
        it('should not fail on empty graphics', () => {
            const obj = {
                a: 1,
                b: 2,
                c: {$ref: '/parameters/hello/'},
                d: {$ref: '/parameters/colors/labelBackground'}
            };
            assert.deepEqual(
                allRefsResolver((obj as unknown) as Record<string, ParamProp>, ({} as unknown) as GraphicsScheme),
                {a: 1, b: 2, c: 'unknown', d: 'unknown'}
            );
        });

        it('should not fail on empty object', () => {
            assert.deepEqual(allRefsResolver({}, (graphics as unknown) as GraphicsScheme), {});
        });

        it('should not change object without refs', () => {
            const obj = {a: 1, b: 2, c: '3'};
            assert.deepEqual(allRefsResolver(obj, (graphics as unknown) as GraphicsScheme), obj);
        });

        it('should change object with one ref', () => {
            const obj = {a: 1, b: 2, c: '3', d: {$ref: '/parameters/colors/labelBackground'}};
            assert.deepEqual(
                allRefsResolver((obj as unknown) as Record<string, ParamProp>, (graphics as unknown) as GraphicsScheme),
                {a: 1, b: 2, c: '3', d: 'fffff3'}
            );
        });

        it('should not replace array fields', () => {
            const obj = {a: 1, b: 2, c: [4, 6], d: {$ref: '/parameters/colors/labelBackground'}};
            assert.deepEqual(
                allRefsResolver((obj as unknown) as Record<string, ParamProp>, (graphics as unknown) as GraphicsScheme),
                {a: 1, b: 2, c: [4, 6], d: 'fffff3'}
            );
        });

        it('should fallback object with unexistent ref', () => {
            const obj = {
                a: 1,
                b: 2,
                c: {$ref: '/parameters/hello/'},
                d: {$ref: '/parameters/colors/labelBackground'}
            };
            assert.deepEqual(
                allRefsResolver((obj as unknown) as Record<string, ParamProp>, (graphics as unknown) as GraphicsScheme),
                {a: 1, b: 2, c: 'unknown', d: 'fffff3'}
            );
        });

        it('should work on real example', () => {
            const realObject = {
                id: 'textStroke',
                z: 0,
                color: {
                    $ref: '/parameters/colors/labelBackground'
                },
                strokeWidth: '#{strokeWidth}',
                strokeColor: {
                    $ref: '/parameters/colors/labelStroke'
                },
                strokeLineJoin: 'round'
            };

            assert.deepEqual(
                allRefsResolver(
                    (realObject as unknown) as Record<string, ParamProp>,
                    (graphics as unknown) as GraphicsScheme
                ),
                {
                    id: 'textStroke',
                    z: 0,
                    color: 'fffff3',
                    strokeWidth: '#{strokeWidth}',
                    strokeColor: 'ffffffe6',
                    strokeLineJoin: 'round'
                }
            );
        });
    });
});
