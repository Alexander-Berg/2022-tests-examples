import { mocked } from 'ts-jest/utils';

import { getSource } from '../../url';
import { ConnectionListener } from '../connection-listener';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { ANY_ORIGIN, ANY_CHANNEL, DEFAULT_URL } from '../constants';

const mockedGetSource = mocked(getSource);

jest.mock('../../url');

class SomeConnectionListener extends ConnectionListener<any> {
    publicProcessMessage(message: ConnectionMessage<any>): void {
        this.processMessage(message);
    }

    destroy(): void {}
}

describe('connection-listener', () => {
    beforeEach(() => {
        mockedGetSource.mockReset();
    });

    describe('ConnectionListener', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);

            mockedGetSource.mockReturnValue(sourceUrl);
        });

        describe('processMessage', () => {
            it('should emit if the message origin is equal to the target origin', () => {
                const message = new ConnectionMessage(null, ANY_CHANNEL, targetUrl);
                const handler = jest.fn();

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalled();
            });

            it('should emit if the message origin is equal the any origin constant', () => {
                const message = new ConnectionMessage(null, ANY_CHANNEL, ANY_ORIGIN);
                const handler = jest.fn();

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalled();
            });

            it('should emit if the target origin is equal to the any origin constant', () => {
                const message = new ConnectionMessage(null, ANY_CHANNEL, targetUrl);
                const handler = jest.fn();

                options.targetUrl = DEFAULT_URL;

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalled();
            });

            it('should emit if the message channel is equal to the current channel', () => {
                const message = new ConnectionMessage(null, channel);
                const handler = jest.fn();

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalled();
            });

            it('should emit if the message channel is equal to any channel constant', () => {
                const message = new ConnectionMessage(null, ANY_CHANNEL, DEFAULT_URL);
                const handler = jest.fn();

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalled();
            });

            it('should emit if the current channel is eual to any channel constant', () => {
                const message = new ConnectionMessage(null, channel, DEFAULT_URL);
                const handler = jest.fn();

                options.channel = ANY_CHANNEL;

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalled();
            });

            it('should throw error if the message origin is not equal to the target origin', () => {
                const otherUrl = 'http://other.yp.local.com/path/a/b/c';
                const message = new ConnectionMessage(null, ANY_CHANNEL, otherUrl);

                const listener = new SomeConnectionListener(options);
                const actual = () => listener.publicProcessMessage(message);

                expect(actual).toThrow(
                    'Failed to process a message with a different origin\n' +
                        ` message origin — ${message.origin}\n` +
                        ` target origin — ${options.targetOrigin}`,
                );
            });

            it('should throw error if the message channel is not equal to the current channel', () => {
                const otherChannel = 'other-channel';
                const message = new ConnectionMessage(null, otherChannel, DEFAULT_URL);

                const listener = new SomeConnectionListener(options);
                const actual = () => listener.publicProcessMessage(message);

                expect(actual).toThrow(
                    'Failed to process a message with a different channel\n' +
                        ` message channel — ${message.channel}\n` +
                        ` current channel — ${options.targetOrigin}`,
                );
            });

            it('should emit a message and payload', () => {
                const payload = Date.now();
                const message = new ConnectionMessage(payload, ANY_CHANNEL, DEFAULT_URL);
                const handler = jest.fn();

                const listener = new SomeConnectionListener(options);

                listener.on(handler);
                listener.publicProcessMessage(message);

                expect(handler).toHaveBeenCalledWith(payload, message);
            });
        });
    });
});
