import * as assert from 'assert';
import {getLinksToTransferHubs} from '../../../server/processors/rule-data-processor';

describe('get-transfer-hubs-to-links', () => {
    it('should work on a simple case', () => {
        assert.deepEqual(
            getLinksToTransferHubs([{transferHubId: 'th04083532', linkId: 'lk20986205'}]),
            {lk20986205: 'th04083532'}
        );
    });

    it('should work for a multiple items case', () => {
        assert.deepEqual(
            getLinksToTransferHubs([
                {linkId: 'lk25748500', transferHubId: 'th04083532'},
                {linkId: 'lk10728636', transferHubId: 'th97258517'}
            ]),
            {lk25748500: 'th04083532', lk10728636: 'th97258517'}
        );
    });
});
