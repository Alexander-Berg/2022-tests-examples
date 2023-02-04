import {stub, SinonStub} from 'sinon';
import {Animator, AnimationHandler} from '../animator';

describe('Animator', () => {
    let renderLoopUpdate: SinonStub;
    let renderLoopRenderAddListener: SinonStub;
    let renderLoopRenderRemoveListener: SinonStub;
    let animator: Animator;
    let time: number;

    beforeEach(() => {
        time = 0;
        // There is no "performance" in Node environment.
        (global as any).performance = {now: () => time};
        renderLoopUpdate = stub();
        renderLoopRenderAddListener = stub();
        renderLoopRenderRemoveListener = stub();
        animator = new Animator({
            onRender: {
                addListener: renderLoopRenderAddListener,
                removeListener: renderLoopRenderRemoveListener
            },
            update: renderLoopUpdate,
            onBeforeRender: null as any,
            isActive: true
        });
    });

    afterEach(() => {
        delete (global as any).performance;
    });

    function advanceTime(dt: number): void {
        time += dt;
        renderLoopRenderAddListener.lastCall.args[0]();
    }

    it('subscribe to RenderLoop on the first "run" request', () => {
        animator.run({duration: 1, handle: stub()});
        expect(renderLoopRenderAddListener.callCount).toEqual(1);
        expect(renderLoopUpdate.callCount).toEqual(1);

        animator.run({duration: 1, handle: stub()});
        expect(renderLoopRenderAddListener.callCount).toEqual(1);
        expect(renderLoopUpdate.callCount).toEqual(1);
    });

    it('unsubscribe from RenderLoop on the last "cancel" request', () => {
        const handler1: AnimationHandler = {duration: 1, handle: stub()};
        const handler2: AnimationHandler = {duration: 1, handle: stub()};
        animator.run(handler1);
        animator.run(handler2);

        animator.cancel(handler1);
        expect(renderLoopRenderRemoveListener.callCount).toEqual(0);

        animator.cancel(handler2);
        expect(renderLoopRenderRemoveListener.callCount).toEqual(1);
    });

    it('unsubscribe from RenderLoop in destructor', () => {
        const handler1: AnimationHandler = {duration: 1, handle: stub()};
        const handler2: AnimationHandler = {duration: 1, handle: stub()};
        animator.run(handler1);
        animator.run(handler2);

        animator.destructor();

        expect(renderLoopRenderRemoveListener.callCount).toEqual(1);
    });

    it('throw an error on duplicate handler in "run" request', () => {
        const handler: AnimationHandler = {duration: 1, handle: stub()};
        animator.run(handler);

        expect(() => animator.run(handler)).toThrow('animation is already running');
    });

    it('ignore duplicate handler in "cancel" request', () => {
        const handler: AnimationHandler = {duration: 1, handle: stub()};
        animator.run(handler);
        animator.cancel(handler);

        expect(() => animator.cancel(handler)).not.toThrow();
    });

    it('provide "progress" status to handlers', () => {
        const handle1 = stub();
        const handle2 = stub();

        animator.run({duration: 10, handle: handle1});
        advanceTime(2);
        advanceTime(3);

        animator.run({duration: 8, handle: handle2});
        advanceTime(1);
        advanceTime(2);

        expect(handle1.args).toEqual([
            [0.2], [0.5], [0.6], [0.8]
        ]);
        expect(handle2.args).toEqual([
            [0.125], [0.375]
        ]);
    });

    it('do not invoke handle when its duration is over', () => {
        const handle = stub();
        animator.run({duration: 5, handle});

        advanceTime(3);
        advanceTime(1);
        advanceTime(2);
        advanceTime(1);
        advanceTime(2);

        expect(handle.args).toEqual([
            [0.6], [0.8], [1]
        ]);
    });
});
