import { mocked } from 'ts-jest/utils';

import { getSource } from '../../url';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { MessageConnectionEmitter } from '../message-connection-emitter';

const mockedGetSource = mocked(getSource);

jest.mock('../../url');

describe('message-connection-emitter', () => {
    beforeEach(() => {
        mockedGetSource.mockReset();
    });

    describe('MessageConnectionEmitter', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;
        let target: Window;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);
            target = <Window>{ closed: false };

            mockedGetSource.mockReturnValue(sourceUrl);
        });

        describe('#', () => {
            it('should set the target', () => {
                const emitter = new MessageConnectionEmitter(options, target);

                expect(emitter.target).toBe(target);
            });

            it('should throw an error if the target is invalid', () => {
                const actual = () => {
                    new MessageConnectionEmitter(options, null);
                };

                expect(actual).toThrow('Target is not set or closed.');
            });
        });

        describe('send', () => {
            it('should send a message', () => {
                const payload = Date.now();
                const message = new ConnectionMessage(payload, channel, sourceUrl);
                const postMessageMethod = jest.fn();

                target.postMessage = postMessageMethod;

                const emitter = new MessageConnectionEmitter(options, target);

                emitter.send(payload);

                expect(postMessageMethod).toHaveBeenCalledWith(String(message), options.targetOrigin);
            });

            it('should not send a message if the target is not connected', () => {
                const postMessageMethod = jest.fn();

                target.postMessage = postMessageMethod;

                const emitter = new MessageConnectionEmitter(options, target);

                Object.defineProperty(target, 'closed', {
                    value: true,
                });

                emitter.send(null);

                expect(postMessageMethod).not.toHaveBeenCalled();
            });
        });

        describe('destroy', () => {
            it('should reset target', () => {
                const emitter = new MessageConnectionEmitter(options, target);

                emitter.destroy();

                expect(emitter.target).toBeNull();
            });
        });
    });
});
