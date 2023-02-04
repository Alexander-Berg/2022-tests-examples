import * as assert from 'assert';
import {getHashMap} from '../../../server/processors/rule-data-processor';
import {LinksToTrackItem} from '../../../server/processors/types/data';

const getLinksToTracks = (arr: LinksToTrackItem[]): Record<string, string[]> => getHashMap(arr, 'linkId', 'trackId');

describe('get-links-to-tracks', () => {
    it('should work on a simple case', () => {
        assert.deepEqual(
            getLinksToTracks([{linkId: 'lk25748500', trackId: 'lk25748500'}]),
            {lk25748500: ['lk25748500']}
        );
    });

    it('should work for a multiple values', () => {
        assert.deepEqual(
            getLinksToTracks([
                {linkId: 'lk25748500', trackId: 'lk25748500'},
                {linkId: 'lk25748500', trackId: 'lk10728636'}
            ]),
            {lk25748500: ['lk25748500', 'lk10728636']}
        );
    });

    it('should work for a multiple items case', () => {
        assert.deepEqual(
            getLinksToTracks([
                {linkId: 'lk25748500', trackId: 'lk25748500'},
                {linkId: 'lk10728636', trackId: 'lk10728636'}
            ]),
            {lk25748500: ['lk25748500'], lk10728636: ['lk10728636']}
        );
    });
});
