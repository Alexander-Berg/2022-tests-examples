import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import {expect} from 'chai';
import {stub, SinonStub} from 'sinon';
import {promiseFinally} from '../../../src/vector_render_engine/util/promise';

chai.use(sinonChai);

const nativeFinally = Promise.prototype.finally;

describe('util/promise', () => {
    describe('promiseFinally', () => {
        let onFinallyStub: SinonStub;

        beforeEach(() => {
            onFinallyStub = stub();
            delete (Promise.prototype as any).finally;
        });

        afterEach(() => {
            Promise.prototype.finally = nativeFinally;
        });

        it('should execute after resolved', async () => {
            await promiseFinally(Promise.resolve(), onFinallyStub);
            expect(onFinallyStub).to.have.been.calledOnce;
        });

        it('should execute after reject', async () => {
            try {
                await promiseFinally(Promise.reject(), onFinallyStub);
            } catch (err) {}
            expect(onFinallyStub).to.have.been.calledOnce;
        });

        it('should resolve with incoming value', async () => {
            const value = await promiseFinally(Promise.resolve(42), () => 13);
            expect(value).to.equal(42);
        });

        it('should reject with incoming error', async () => {
            const error = new Error();
            const promise = promiseFinally(Promise.reject(error), () => 13);
            await expectToEventuallyThrow(promise, error);
        });

        it('should reject if error raised in callback', async () => {
            const error = new Error();
            const promise = promiseFinally(Promise.resolve(), () => {
                return Promise.reject(error);
            });
            await expectToEventuallyThrow(promise, error);
        });

        it('should resolve after promise returned from callback resolved', async () => {
            let resolved = false;
            const promise = promiseFinally(Promise.resolve(42), () => timeout(10));
            promise.then((value) => {
                expect(value).to.equal(42);
                resolved = true;
            });
            await timeout(0);
            expect(resolved).to.be.false;
            await promise;
            expect(resolved).to.be.true;
        });

        it('should reject after promise returned from callback rejected', async () => {
            let rejected = false;
            const error = new Error();
            const promise = promiseFinally(Promise.reject(error), () => timeout(10));
            promise.catch((actualError) => {
                expect(actualError).to.equal(error);
                rejected = true;
            });
            await timeout(0);
            expect(rejected).to.be.false;
            try {
                await promise;
            } catch (err) {}
            expect(rejected).to.be.true;
        });
    });
});

async function expectToEventuallyThrow(promise: Promise<any>, expectedError?: Error): Promise<void> {
    let thrown = false;
    try {
        await promise;
    } catch (actualError) {
        if (expectedError) {
            expect(actualError).to.equal(expectedError);
        }
        thrown = true;
    }
    expect(thrown, 'Promise did not throw').to.be.true;
}

function timeout(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
}
