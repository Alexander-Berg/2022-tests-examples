import { mocked } from 'ts-jest/utils';

import { getSource } from '../../url';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { getTop, openWindow, setDataToUrl } from '../helpers';
import { MessageConnectionAgent } from '../message-connection-agent';

const mockedGetSource = mocked(getSource);
const mockedGetTop = mocked(getTop);
const mockedOpenWindow = mocked(openWindow);

jest.mock('../../url');
jest.mock('../helpers');

describe('message-connection-agent', () => {
    beforeEach(() => {
        mockedGetSource.mockReset();
        mockedGetTop.mockReset();
        mockedOpenWindow.mockReset();
    });

    describe('MessageConnectionAgent', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;
        let target: Window;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);
            target = <Window>{ closed: false };

            mockedGetSource.mockReturnValue(sourceUrl);
            mockedGetTop.mockReturnValue(window);
            mockedOpenWindow.mockReturnValue(target);
        });

        describe('#', () => {
            it('should open a new window', () => {
                const payload = Date.now();
                const features = 'abcd';
                const message = new ConnectionMessage(payload, channel, sourceUrl);
                const url = setDataToUrl(options.targetUrl, String(message));

                new MessageConnectionAgent(options, payload, features);

                expect(mockedOpenWindow).toHaveBeenCalledWith(url, {
                    target: options.channel,
                    features,
                });
            });

            it('should set a new target', () => {
                const agent = new MessageConnectionAgent(options, null);

                expect(agent.target).toBe(target);
            });
        });

        describe('focus', () => {
            it('should try to call the focus method', () => {
                const focusMethod = jest.fn();

                target.focus = focusMethod;

                const agent = new MessageConnectionAgent(options, null);

                agent.focus();

                expect(focusMethod).toHaveBeenCalled();
            });

            it('should not try to call the focus method if the target is not connected', () => {
                const focusMethod = jest.fn();

                target.focus = focusMethod;

                const agent = new MessageConnectionAgent(options, null);

                Object.defineProperty(target, 'closed', {
                    value: true,
                });
                agent.focus();

                expect(focusMethod).not.toHaveBeenCalled();
            });
        });

        describe('close', () => {
            it('should try to call the close method', () => {
                const closeMethod = jest.fn();

                target.close = closeMethod;

                const agent = new MessageConnectionAgent(options, null);

                agent.close();

                expect(closeMethod).toHaveBeenCalled();
            });

            it('should not try to call the close method if the target is not connected', () => {
                const closeMethod = jest.fn();

                target.close = closeMethod;

                const agent = new MessageConnectionAgent(options, null);

                Object.defineProperty(target, 'closed', {
                    value: true,
                });
                agent.close();

                expect(closeMethod).not.toHaveBeenCalled();
            });
        });
    });
});
