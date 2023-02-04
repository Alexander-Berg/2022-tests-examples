import Dispatcher from '../Dispatcher';

describe('Basic tests of Dispatcher', () => {
    it('Subscribes to new events', (done) => {
        const dispatcher = new Dispatcher();

        dispatcher.subscribe('eventName', () => {
            done();
        });
        dispatcher.dispatch('eventName');
    });

    it('Passes data correctly', () => {
        const dispatcher = new Dispatcher();
        const testData = { a: 1 };
        const fn = jest.fn();

        dispatcher.subscribe('eventName', fn);
        dispatcher.dispatch('eventName', testData);
        expect(fn).toBeCalledWith(testData);
    });

    it('Unsubscribes from events', () => {
        const dispatcher = new Dispatcher();
        const testData = { a: 1 };
        const fn = jest.fn();

        dispatcher.subscribe('eventName', fn);
        dispatcher.unsubscribe('eventName', fn);
        dispatcher.dispatch('eventName', testData);
        expect(fn).not.toBeCalled();
    });
});
