import {strict as assert} from 'assert';

import {spbProvider} from './providers/spb';

describe('spb', function () {
    this.timeout(5000);

    it('/payment/init', async () => {
        const result = await spbProvider.initPayment(15);
        assert.ok(result.sum > 0);
    });

    it('/payment/finish', async () => {
        const paymentId = `${Date.now()}`;
        const {transactionId, sum} = await spbProvider.initPayment(15);
        const finishResult = await spbProvider.finishPayment(transactionId, paymentId, sum);
        assert.strictEqual(finishResult.end.getTime() - finishResult.start.getTime(), 15 * 60 * 1000);

        // should throw error when we try to finish the same transaction
        await assert.rejects(async () => {
            return spbProvider.finishPayment(transactionId, paymentId, sum);
        }, {message: 'Error: code=TerminalTransactionSuccessfullyClosedError'});
    });

    it('/payment/init + /payment/finish (extend parking)', async () => {
        const {transactionId, sum} = await spbProvider.initPayment(15);
        const finishResult = await spbProvider.finishPayment(transactionId, `${Date.now() + 1}`, sum);
        assert.strictEqual(finishResult.end.getTime() - finishResult.start.getTime(), 15 * 60 * 1000);
    });

    it('/objects?types=zone', async () => {
        const zones = await spbProvider.getZonesNumbers();

        assert.ok(zones.length > 0);
    });
});
