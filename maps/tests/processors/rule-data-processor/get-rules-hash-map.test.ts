import * as assert from 'assert';
import {getDataToGraphics} from '../../../server/processors/rule-data-processor';
import {RulesItem} from '../../../server/processors/types/rules';

describe('get-rules-hash-map', () => {
    it('should not fail on empty rules array', () => {
        assert.deepEqual(getDataToGraphics(([] as unknown) as RulesItem[]), {});
    });

    // for now it just ignores such graphics
    it('should work for data w/out objectId', () => {
        assert.deepEqual(getDataToGraphics(([{data: [], graphicsId: 'b3fe2c28'}] as unknown) as RulesItem[]), {});
    });

    it('should work for a simple case', () => {
        assert.deepEqual(
            getDataToGraphics(([
                {data: [{id: 'st92336396', type: 'Station'}], graphicsId: 'b3fe2c28'}
            ] as unknown) as RulesItem[]),
            {st92336396: ['b3fe2c28']}
        );
    });

    it('should work for a multiple items case', () => {
        assert.deepEqual(
            getDataToGraphics(([
                {data: [], graphicsId: 'b3fe2c28'},
                {data: [{id: 'st92336396', type: 'Station'}], graphicsId: 'b3fe2c28'}
            ] as unknown) as RulesItem[]),
            {st92336396: ['b3fe2c28']}
        );
    });
});
