import { SimpleEmitter } from '../simple-emitter';

describe('simple-emitter', () => {
    describe('SimpleEmitter', () => {
        describe('on', () => {
            it('should subscribe to multiple listeners', () => {
                const emitter = new SimpleEmitter();
                const listenerA = jest.fn();
                const listenerB = jest.fn();
                const listenerC = jest.fn();

                emitter.on(listenerA);
                emitter.on(listenerB);
                emitter.on(listenerC);
                emitter.emit();

                expect(listenerA).toHaveBeenCalled();
                expect(listenerB).toHaveBeenCalled();
                expect(listenerC).toHaveBeenCalled();
            });

            it('should unsubscribe the listener with the callback', () => {
                const emitter = new SimpleEmitter();
                const listener = jest.fn();

                const unsubscribeCallback = emitter.on(listener);

                unsubscribeCallback();
                emitter.emit();

                expect(listener).not.toHaveBeenCalled();
            });
        });

        describe('off', () => {
            it('should unsubscribe the listener', () => {
                const emitter = new SimpleEmitter();
                const listener = jest.fn();

                emitter.on(listener);
                emitter.off(listener);
                emitter.emit();

                expect(listener).not.toHaveBeenCalled();
            });

            it('should do nothing when unsubscribing from an unknown listener', () => {
                const emitter = new SimpleEmitter();
                const listenerA = jest.fn();
                const listenerB = jest.fn();

                emitter.on(listenerA);
                emitter.off(listenerB);
                emitter.emit();

                expect(listenerA).toHaveBeenCalled();
                expect(listenerB).not.toHaveBeenCalled();
            });
        });

        describe('emit', () => {
            it('should call the listeners with the received data', () => {
                const emitter = new SimpleEmitter();
                const listener = jest.fn();
                const data = Symbol();

                emitter.on(listener);
                emitter.emit(data);

                expect(listener).toHaveBeenCalledWith(data);
            });
        });

        describe('destroy', () => {
            it('should unsubscribe all listeners to destroy', () => {
                const emitter = new SimpleEmitter();
                const listenerA = jest.fn();
                const listenerB = jest.fn();
                const listenerC = jest.fn();

                emitter.on(listenerA);
                emitter.on(listenerB);
                emitter.on(listenerC);
                emitter.destroy();

                expect(listenerA).not.toHaveBeenCalled();
                expect(listenerB).not.toHaveBeenCalled();
                expect(listenerC).not.toHaveBeenCalled();
            });
        });
    });
});
