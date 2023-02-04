import { mocked } from 'ts-jest/utils';

import { wait } from '../../promise/wait';
import { replaceWith, getSource } from '../../url';
import { ConnectionManager } from '../connection-manager';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { ANY_ORIGIN, ANY_CHANNEL, DEFAULT_URL } from '../constants';
import { getTop, openWindow, setDataToUrl } from '../helpers';
import { MessageConnectionAgent } from '../message-connection-agent';
import { MessageConnectionEmitter } from '../message-connection-emitter';
import { MessageConnectionListener } from '../message-connection-listener';
import { QueryConnectionAgent } from '../query-connection-agent';
import { QueryConnectionEmitter } from '../query-connection-emitter';
import { QueryConnectionListener } from '../query-connection-listener';

const MockedMessageConnectionListener = mocked(MessageConnectionListener);
const MockedQueryConnectionListener = mocked(QueryConnectionListener);
const mockedReplaceWith = mocked(replaceWith);
const mockedGetSource = mocked(getSource);
const mockedGetTop = mocked(getTop);
const mockedOpenWindow = mocked(openWindow);

jest.mock('../message-connection-listener');
jest.mock('../query-connection-listener');
jest.mock('../../url');
jest.mock('../helpers');

const getInstance = <T>(instance: jest.MockInstance<T, any[]>, index: number): T => {
    return instance.mock.instances[index];
};

const getCall = <Y extends any[]>(instance: jest.MockInstance<any, Y>, index: number): Y => {
    return instance.mock.calls[index];
};

describe('connection-manager', () => {
    beforeEach(() => {
        MockedMessageConnectionListener.mockReset();
        MockedQueryConnectionListener.mockReset();
        mockedReplaceWith.mockReset();
        mockedGetSource.mockReset();
        mockedGetTop.mockReset();
        mockedOpenWindow.mockReset();
    });

    describe('ConnectionManager', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);

            mockedGetSource.mockReturnValue(sourceUrl);
        });

        describe('#', () => {
            it('should proxy the event from the message listener', () => {
                const payload = Date.now();
                const message = new ConnectionMessage(payload);
                const handler = jest.fn();

                const manager = new ConnectionManager(options);
                const listener = getInstance(MockedMessageConnectionListener, 0);
                const [managerHandler] = getCall(mocked(listener.on), 0);

                manager.on(handler);
                managerHandler(payload, message);

                expect(handler).toHaveBeenLastCalledWith(payload, message);
            });

            it('should proxy the event from the query listener', () => {
                const payload = Date.now();
                const message = new ConnectionMessage(payload);
                const handler = jest.fn();

                const manager = new ConnectionManager(options);
                const listener = getInstance(MockedQueryConnectionListener, 0);
                const [managerHandler] = getCall(mocked(listener.on), 0);

                manager.on(handler);
                managerHandler(payload, message);

                expect(handler).toHaveBeenLastCalledWith(payload, message);
            });

            it('should update the options with the message of the query listener', async () => {
                // TODO: Нужно отключить каналы в sdk
                //       вот тут https://github.yandex-team.ru/trust/yandex-pay/blob/v0.11.0/services/sdk/src/payment.ts#L79
                //       а после вернуть проверку тут
                // const otherChannel = 'other-channel';
                const otherChannel = '*';
                const otherUrl = 'http://other.yp.local.com/path/a/b/c';
                const message = new ConnectionMessage(null, otherChannel, otherUrl);

                options.channel = ANY_CHANNEL;
                options.targetUrl = DEFAULT_URL;

                new ConnectionManager(options);

                const listener = getInstance(MockedQueryConnectionListener, 0);
                const [managerHandler] = getCall(mocked(listener.on), 1);

                managerHandler(null, message);

                expect(options.channel).toBe(otherChannel);
                expect(options.targetUrl).toBe(otherUrl);
            });

            it('should update the options with the message of the query listener without sourceUrl', async () => {
                // TODO: Нужно отключить каналы в sdk
                //       вот тут https://github.yandex-team.ru/trust/yandex-pay/blob/v0.11.0/services/sdk/src/payment.ts#L79
                //       а после вернуть проверку тут
                // const otherChannel = 'other-channel';
                const otherChannel = '*';
                const message = new ConnectionMessage(null, otherChannel);

                options.channel = ANY_CHANNEL;
                options.targetUrl = DEFAULT_URL;

                new ConnectionManager(options);

                const listener = getInstance(MockedQueryConnectionListener, 0);
                const [managerHandler] = getCall(mocked(listener.on), 1);

                managerHandler(null, message);

                expect(options.channel).toBe(otherChannel);
                expect(options.targetUrl).toBe(DEFAULT_URL);
            });

            it('should clear the query if the options are initialized', async () => {
                const message = new ConnectionMessage(null);

                new ConnectionManager(options);

                const listener = getInstance(MockedQueryConnectionListener, 0);
                const [managerHandler] = getCall(mocked(listener.on), 1);

                managerHandler(null, message);

                expect(mockedReplaceWith).toHaveBeenCalledWith(sourceUrl);
            });
        });

        describe('connect', () => {
            it('should create the message emitter', () => {
                const manager = new ConnectionManager(options);
                const actualEmitter = manager.connect({ message: { target: window } });

                expect(actualEmitter).toBeInstanceOf(MessageConnectionEmitter);
            });

            it('should create the query emitter', () => {
                const manager = new ConnectionManager(options);
                const actualEmitter = manager.connect();

                expect(actualEmitter).toBeInstanceOf(QueryConnectionEmitter);
            });
        });

        describe('open', () => {
            it('should create the message agent', () => {
                mockedGetTop.mockReturnValue(window);
                mockedOpenWindow.mockReturnValue(window);

                const manager = new ConnectionManager(options);
                const actualAgent = manager.open(null);

                expect(actualAgent).toBeInstanceOf(MessageConnectionAgent);
            });

            it('should create the query agent', () => {
                const manager = new ConnectionManager(options);
                const actualAgent = manager.open(null);

                expect(actualAgent).toBeInstanceOf(QueryConnectionAgent);
            });
        });

        describe('destroy', () => {
            it('should destroy all listeners', () => {
                const manager = new ConnectionManager(options);
                const messageListener = getInstance(MockedQueryConnectionListener, 0);
                const queryListener = getInstance(MockedQueryConnectionListener, 0);

                manager.destroy();

                expect(messageListener.destroy).toHaveBeenCalled();
                expect(queryListener.destroy).toHaveBeenCalled();
            });
        });
    });
});
