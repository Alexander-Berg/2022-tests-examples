import {expect} from 'chai';
import {stub, SinonStub} from 'sinon';
import {BinaryHttpRequest} from '../../src/vector_render_engine/util/http';
import {LoadManager} from '../../src/vector_render_engine/util/load_manager';

describe('LoadManager', () => {
    let loadManager: LoadManager;
    let httpSendStub: SinonStub;
    let httpCancelStub: SinonStub;

    // When *send* request is finished promises returned from *load* are resolved before queue is checked.
    //   send().then(() => resolve_promises()).finally(() => check_queue())
    // Timeout allows to wait for the moment when queue is check (tasks are executed after microtasks).
    function waitForQueueCheck(): Promise<void> {
        return new Promise((resolve) => setTimeout(resolve, 0));
    }

    beforeEach(() => {
        loadManager = new LoadManager();
        httpSendStub = stub(BinaryHttpRequest.prototype, 'send');
        httpCancelStub = stub(BinaryHttpRequest.prototype, 'cancel');
    });

    afterEach(() => {
        httpSendStub.restore();
        httpCancelStub.restore();
    });

    it('should make request', async () => {
        const buffer = new Uint8Array([1, 2, 3]);
        httpSendStub.resolves(buffer);

        const res = await loadManager.load('some url', 0);

        expect(res).to.equal(buffer);
    });

    it('should make a single request for the same resource', async () => {
        const buffer = new Uint8Array([1, 2, 3]);
        httpSendStub.resolves(buffer);
        const url = 'some url';

        const [r1, r2, r3] = await Promise.all([
            loadManager.load(url, 0),
            loadManager.load(url, 0),
            loadManager.load(url, 0)
        ]);

        expect(httpSendStub.callCount).to.equal(1);
        expect(r1).to.equal(buffer);
        expect(r2).to.equal(buffer);
        expect(r3).to.equal(buffer);
    });

    it('should make requests with same priority simultaneously', async () => {
        const buffer1 = new Uint8Array([1, 2, 3]);
        const buffer2 = new Uint8Array([2, 3, 4]);
        const buffer3 = new Uint8Array([3, 4, 5]);
        httpSendStub.onCall(0).resolves(buffer1);
        httpSendStub.onCall(1).resolves(buffer2);
        httpSendStub.onCall(2).resolves(buffer3);

        const [r1, r2, r3] = await Promise.all([
            loadManager.load('some url 1', 3),
            loadManager.load('some url 2', 3),
            loadManager.load('some url 3', 3)
        ]);

        expect(httpSendStub.callCount).to.equal(3);
        expect(r1).to.equal(buffer1);
        expect(r2).to.equal(buffer2);
        expect(r3).to.equal(buffer3);
    });

    it('should delay requests with lower priority and execute them later', async () => {
        const buffer1 = new Uint8Array([1, 2, 3]);
        const buffer2 = new Uint8Array([2, 3, 4]);
        const buffer3 = new Uint8Array([3, 4, 5]);
        let resolve1!: Function;
        let resolve2!: Function;

        httpSendStub.onCall(0).returns(new Promise((resolve) => {
            resolve1 = resolve;
        }));
        httpSendStub.onCall(1).returns(new Promise((resolve) => {
            resolve2 = resolve;
        }));
        httpSendStub.onCall(2).resolves(buffer3);

        // p1 -> p3 (7) -> p2 (5)
        const p1 = loadManager.load('some url 1', 8);
        const p2 = loadManager.load('some url 2', 5);
        const p3 = loadManager.load('some url 3', 7);

        expect(httpSendStub.callCount).to.equal(1);

        resolve1(buffer1);
        expect(await p1).to.equal(buffer1);

        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(2);

        resolve2(buffer2);
        expect(await p3).to.equal(buffer2);

        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(3);

        expect(await p2).to.equal(buffer3);
    });

    it('should cache delayed requests if they are for the same resource', async () => {
        const buffer1 = new Uint8Array([1, 2, 3]);
        const buffer2 = new Uint8Array([2, 3, 4]);
        const buffer3 = new Uint8Array([3, 4, 5]);
        let resolve1!: Function;
        let resolve2!: Function;

        httpSendStub.onCall(0).returns(new Promise((resolve) => {
            resolve1 = resolve;
        }));
        httpSendStub.onCall(1).returns(new Promise((resolve) => {
            resolve2 = resolve;
        }));
        httpSendStub.onCall(2).resolves(buffer3);

        // p1 -> p2, p4, p5 (7) -> p3 (5)
        // p4 and p5 are joined to p2
        const p1 = loadManager.load('some url 1', 8);
        const p2 = loadManager.load('some url 2', 7);
        const p3 = loadManager.load('some url 3', 5);
        const p4 = loadManager.load('some url 2', 3);
        const p5 = loadManager.load('some url 2', 4);

        resolve1(buffer1);
        expect(await p1).to.equal(buffer1);

        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(2);

        resolve2(buffer2);
        expect(await p2).to.equal(buffer2);
        expect(await p4).to.equal(buffer2);
        expect(await p5).to.equal(buffer2);

        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(3);

        expect(await p3).to.equal(buffer3);
    });

    it('should rearrange queue of delayed requests on same resource with higher priority', async () => {
        const buffer1 = new Uint8Array([1, 2, 3]);
        const buffer2 = new Uint8Array([2, 3, 4]);
        const buffer3 = new Uint8Array([3, 4, 5]);
        let resolve1!: Function;
        let resolve2!: Function;

        httpSendStub.onCall(0).returns(new Promise((resolve) => {
            resolve1 = resolve;
        }));
        httpSendStub.onCall(1).returns(new Promise((resolve) => {
            resolve2 = resolve;
        }));
        httpSendStub.onCall(2).resolves(buffer3);

        // p1 -> p2, p4, p5 (7) -> p3 (5)
        // p4 changes p1 -> p3 (5) -> p2 (2) to p1 -> p2, p4 (7) -> p3 (5)
        const p1 = loadManager.load('some url 1', 8);
        const p2 = loadManager.load('some url 2', 2);
        const p3 = loadManager.load('some url 3', 5);
        const p4 = loadManager.load('some url 2', 7);
        const p5 = loadManager.load('some url 2', 4);

        resolve1(buffer1);
        expect(await p1).to.equal(buffer1);

        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(2);

        resolve2(buffer2);
        expect(await p2).to.equal(buffer2);
        expect(await p4).to.equal(buffer2);
        expect(await p5).to.equal(buffer2);

        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(3);

        expect(await p3).to.equal(buffer3);
    });

    it('should cancel request', () => {
        httpSendStub.resolves(new Uint8Array([]));
        const p = loadManager.load('some url', 0);

        loadManager.cancel(p);

        expect(httpSendStub.callCount).to.equal(1);
        expect(httpCancelStub.callCount).to.equal(1);
    });

    it('should not cancel request if any callers are still waiting for it', async () => {
        const buffer = new Uint8Array([1, 2, 3]);
        httpSendStub.resolves(buffer);
        const url = 'some url';
        const p1 = loadManager.load(url, 0);
        const p2 = loadManager.load(url, 0);
        const p3 = loadManager.load(url, 0);
        let isP1Fulfilled = false;
        p2.then(() => { isP1Fulfilled = true; });

        loadManager.cancel(p2);

        expect(await p1).to.equal(buffer);
        expect(await p3).to.equal(buffer);
        expect(httpSendStub.callCount).to.equal(1);
        expect(httpCancelStub.callCount).to.equal(0);
        expect(isP1Fulfilled).to.be.false;
    });

    it('should cancel request if all callers are removed', () => {
        httpSendStub.resolves(new Uint8Array([]));
        const url = 'some url';
        const p1 = loadManager.load(url, 0);
        const p2 = loadManager.load(url, 0);
        const p3 = loadManager.load(url, 0);

        loadManager.cancel(p1);
        loadManager.cancel(p2);
        loadManager.cancel(p3);

        expect(httpSendStub.callCount).to.equal(1);
        expect(httpCancelStub.callCount).to.equal(1);
    });

    it('should remove pending request if it is canceled', async () => {
        const buffer = new Uint8Array([1, 2, 3]);
        httpSendStub.resolves(buffer);
        const p1 = loadManager.load('some url 1', 5);
        const p2 = loadManager.load('some url 2', 4);

        loadManager.cancel(p2);

        expect(await p1).to.equal(buffer);
        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(1);
        expect(httpCancelStub.callCount).to.equal(0);
    });

    it('should not remove pending request if any callers are still waiting for it', async () => {
        const buffer1 = new Uint8Array([1, 2, 3]);
        const buffer2 = new Uint8Array([2, 3, 4]);
        httpSendStub.onCall(0).resolves(buffer1);
        httpSendStub.onCall(1).resolves(buffer2);

        const p1 = loadManager.load('some url 1', 8);
        const p2 = loadManager.load('some url 2', 5);
        const p3 = loadManager.load('some url 2', 3);
        let isP1Fulfilled = false;
        p2.then(() => { isP1Fulfilled = true; });

        loadManager.cancel(p2);

        expect(await p1).to.equal(buffer1);
        expect(await p3).to.equal(buffer2);
        expect(isP1Fulfilled).to.be.false;
    });

    it('should remove pending request is all callers are removed', async () => {
        const buffer1 = new Uint8Array([1, 2, 3]);
        const buffer2 = new Uint8Array([2, 3, 4]);

        httpSendStub.onCall(0).resolves(buffer1);
        httpSendStub.onCall(1).resolves(buffer2);

        const p1 = loadManager.load('some url 1', 8);
        const p2 = loadManager.load('some url 2', 5);
        const p3 = loadManager.load('some url 2', 3);

        loadManager.cancel(p2);
        loadManager.cancel(p3);

        expect(await p1).to.equal(buffer1);
        await waitForQueueCheck();
        expect(httpSendStub.callCount).to.equal(1);
        expect(httpCancelStub.callCount).to.equal(0);
    });
});
