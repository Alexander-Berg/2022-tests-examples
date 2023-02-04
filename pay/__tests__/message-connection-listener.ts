import { mocked } from 'ts-jest/utils';

import { wait } from '../../promise/wait';
import { getSource } from '../../url';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { ANY_ORIGIN } from '../constants';
import { MessageConnectionListener } from '../message-connection-listener';

const mockedGetSource = mocked(getSource);

jest.mock('../../url');

describe('message-connection-listener', () => {
    beforeEach(() => {
        mockedGetSource.mockReset();
    });

    describe('MessageConnectionListener', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);

            mockedGetSource.mockReturnValue(sourceUrl);
        });

        describe('#', () => {
            it('should catch the message', async () => {
                const payload = Date.now();
                const message = new ConnectionMessage(payload, ANY_ORIGIN, '');
                const handler = jest.fn();

                const listener = new MessageConnectionListener(options);

                listener.on(handler);
                window.postMessage(String(message), ANY_ORIGIN);
                await wait(0);

                expect(handler).toHaveBeenCalledWith(payload, message);
            });

            it('should process the message', async () => {
                const message = new ConnectionMessage(null, 'abc');
                const handler = jest.fn();

                const listener = new MessageConnectionListener(options);

                listener.on(handler);
                window.postMessage(String(message), ANY_ORIGIN);
                await wait(0);

                expect(handler).not.toHaveBeenCalled();
            });
        });

        describe('destroy', () => {
            it('should unsubscribe from message events', async () => {
                const message = new ConnectionMessage(null);
                const handler = jest.fn();

                const listener = new MessageConnectionListener(options);

                listener.destroy();
                listener.on(handler);
                window.postMessage(String(message), ANY_ORIGIN);
                await wait(0);

                expect(handler).not.toHaveBeenCalled();
            });
        });
    });
});
