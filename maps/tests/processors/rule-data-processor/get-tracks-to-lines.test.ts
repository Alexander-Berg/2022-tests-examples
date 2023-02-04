import * as assert from 'assert';
import {getHashMap} from '../../../server/processors/rule-data-processor';
import {LinesToTrackItem} from '../../../server/processors/types/data';

const getTracksToLines = (arr: LinesToTrackItem[]): Record<string, string[]> => getHashMap(arr, 'trackId', 'lineId');

describe('get-tracks-to-lines', () => {
    it('should work on a simple case', () => {
        assert.deepEqual(
            getTracksToLines([{trackId: 'lk25748500', lineId: 'sr70375344'}]),
            {lk25748500: ['sr70375344']}
        );
    });

    it('should work for a multiple values', () => {
        assert.deepEqual(
            getTracksToLines([
                {trackId: 'lk25748500', lineId: 'sr70375344'},
                {trackId: 'lk25748500', lineId: 'sr76383450'}
            ]),
        {lk25748500: ['sr70375344', 'sr76383450']}
        );
    });

    it('should work for a multiple items case', () => {
        assert.deepEqual(
            getTracksToLines([
                {trackId: 'lk25748500', lineId: 'sr70375344'},
                {trackId: 'lk10728636', lineId: 'sr76383450'}
            ]),
            {lk25748500: ['sr70375344'], lk10728636: ['sr76383450']}
        );
    });
});
