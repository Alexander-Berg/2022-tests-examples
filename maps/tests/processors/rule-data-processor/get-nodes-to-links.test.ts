import * as assert from 'assert';
import {getHashMap} from '../../../server/processors/rule-data-processor';
import {LinkItem} from '../../../server/processors/types/data';

const getNodesToLinks = (arr: LinkItem[]): Record<string, string[]> => getHashMap(arr, 'fromNodeId', 'id');

describe('get-nodes-to-links', () => {
    it('should work on a simple case', () => {
        assert.deepEqual(
            getNodesToLinks(([{id: 'lk25748500', fromNodeId: 'nd96585445'}] as unknown) as LinkItem[]),
            {nd96585445: ['lk25748500']}
        );
    });

    it('should work for a multiple values', () => {
        assert.deepEqual(
            getNodesToLinks(([
                {id: 'lk25748500', fromNodeId: 'nd96585445'},
                {id: 'lk10728636', fromNodeId: 'nd96585445'}
            ] as unknown) as LinkItem[]),
            {nd96585445: ['lk25748500', 'lk10728636']}
        );
    });

    it('should work for a multiple items case', () => {
        assert.deepEqual(
            getNodesToLinks(([
                {id: 'lk25748500', fromNodeId: 'nd96585445'},
                {id: 'lk10728636', fromNodeId: 'nd26538282'}
            ] as unknown) as LinkItem[]),
            {nd96585445: ['lk25748500'], nd26538282: ['lk10728636']}
        );
    });
});
