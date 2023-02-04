import config from '../config';
import Server from '../Server';
import FetchApi from '../Fetch/__stubs__/FetchApi';
import Dispatcher from '../../../Dispatcher';

describe('Basic tests of Server', () => {
    it('Calls handler correctly on getItem success', (done) => {
        const dispatcher = new Dispatcher();
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.ADD_ITEM, () => {
            done();
        });
        const server = new Server(config, dispatcher, new FetchApi(config));
        const addItemData = {
            qty: 1,
            old_qty: 0,
            service_id: 1,
            service_order_id: 1,
            payload: {}
        };

        server.addItem(addItemData);
    });


    it('Calls handler correctly on getList success', (done) => {
        const dispatcher = new Dispatcher();
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.GET_LIST, () => {
            done();
        });
        const server = new Server(config, dispatcher, new FetchApi(config));

        server.getList([1, 2]);
    });

    it('Invokes error message on error', (done) => {
        const dispatcher = new Dispatcher();
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.ADD_ITEM, () => {
            // invoke error to imitate server error
            throw new Error();
        });
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.ERROR, () => {
            done();
        });
        const server = new Server(config, dispatcher, new FetchApi(config));

        server.addItem({});
    });

    it('Starts reconnection interval on error', async () => {
        const config = {
            retry: {
                duration: 5,
                period: 1
            }
        };

        const getListHandlerMock = jest.fn();
        const dispatcher = new Dispatcher();
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.ADD_ITEM, () => {
            // invoke error to imitate server error
            throw new Error();
        });
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.GET_LIST, () => {
            getListHandlerMock();
        });
        const server = new Server(config, dispatcher, new FetchApi(config));

        await server.addItem({});

        await new Promise(resolve => setTimeout(() => resolve(), (config.retry.period + 1) * 1000));

        await expect(getListHandlerMock).toHaveBeenCalledTimes(1);
    });

    it('Does not start reconnection interval on error if period set to 0', async () => {
        const config = {
            retry: {
                duration: 0,
                period: 1
            }
        };

        const getListHandlerMock = jest.fn();
        const dispatcher = new Dispatcher();
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.ADD_ITEM, () => {
            // invoke error to imitate server error
            throw new Error();
        });
        dispatcher.subscribe(Dispatcher.EVENT.SERVER.GET_LIST, () => {
            getListHandlerMock();
        });
        const server = new Server(config, dispatcher, new FetchApi(config));

        await server.addItem({});

        await new Promise(resolve => setTimeout(() => resolve(), (config.retry.period + 1) * 1000));

        await expect(getListHandlerMock).not.toHaveBeenCalled();
    });
});
