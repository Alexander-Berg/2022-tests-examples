import {expect} from 'chai';
import * as sinon from 'sinon';
import {delay} from 'prex';
import * as promiseUtils from 'src/lib/promise-utils';
import {expectRejection} from 'tests/assertions';

describe('promiseUtils', () => {
    describe('timeout()', async () => {
        describe('when timeout not exceeded', () => {
            it('should fulfill promise', async () => {
                const promise = Promise.resolve(1);
                expect(await promiseUtils.timeout(promise, 100)).to.equal(1);
            });

            it('should not call onTimeout() callback', async () => {
                const onTimeout = sinon.spy();
                const promise = Promise.resolve(1);
                await promiseUtils.timeout(promise, 100, onTimeout);
                expect(onTimeout.called).to.be.false;
            });
        });

        describe('when timeout exceeded', () => {
            it('should reject promise with TimeoutError', async () => {
                const promise = delay(100);
                await expectRejection(promiseUtils.timeout(promise, 20), (err) => {
                    expect(err).to.be.instanceof(promiseUtils.TimeoutError);
                });
            });

            it('should reject promise with error thrown from onTimeout() callback', async () => {
                const origPromise = delay(100);
                const resPromise = promiseUtils.timeout(origPromise, 20, () => {
                    throw new Error('foobar');
                });
                await expectRejection(resPromise, (err) => {
                    expect(err).to.be.instanceof(Error);
                    expect(err.message).to.equal('foobar');
                });
            });

            it('should fulfill promise with onTimeout() result', async () => {
                const origPromise = new Promise<number>((resolve) => {
                    setTimeout(() => resolve(1), 100);
                });
                const resPromise = promiseUtils.timeout(origPromise, 20, () => 2);
                expect(await resPromise).to.equal(2);
            });

            it('should fulfill promise with onTimeout() promise', async () => {
                const origPromise = new Promise<number>((resolve) => {
                    setTimeout(() => resolve(1), 100);
                });
                const resPromise = promiseUtils.timeout(origPromise, 20, () => Promise.resolve(2));
                expect(await resPromise).to.equal(2);
            });
        });
    });
});
