import { mocked } from 'ts-jest/utils';

import { redirectTo, getSource } from '../../url';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { setDataToUrl } from '../helpers';
import { QueryConnectionEmitter } from '../query-connection-emitter';

const mockedRedirectTo = mocked(redirectTo);
const mockedGetSource = mocked(getSource);

jest.mock('../../url');

describe('query-connection-emitter', () => {
    beforeEach(() => {
        mockedRedirectTo.mockReset();
        mockedGetSource.mockReset();
    });

    describe('QueryConnectionEmitter', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);

            mockedGetSource.mockReturnValue(sourceUrl);
        });

        describe('send', () => {
            it('should be redirected with payload', () => {
                const payload = String(Date.now());
                const message = new ConnectionMessage(payload, channel, sourceUrl);
                const url = setDataToUrl(targetUrl, String(message));

                const emitter = new QueryConnectionEmitter(options);

                emitter.send(payload);

                expect(mockedRedirectTo).toHaveBeenCalledWith(url);
            });

            it('should be redirected with payload without sourceUrl', () => {
                const payload = String(Date.now());
                const message = new ConnectionMessage(payload, channel);
                const url = setDataToUrl(targetUrl, String(message));

                const emitter = new QueryConnectionEmitter(options, { noEmitSourceUrl: true });

                emitter.send(payload);

                expect(mockedRedirectTo).toHaveBeenCalledWith(url);
            });

            it('should be redirected once', () => {
                const emitter = new QueryConnectionEmitter(options);

                emitter.send(null);
                emitter.send(null);

                expect(mockedRedirectTo).toHaveBeenCalledTimes(1);
            });
        });

        describe('destroy', () => {
            it('should disable sending', () => {
                const emitter = new QueryConnectionEmitter(options);

                emitter.destroy();
                emitter.send(null);

                expect(mockedRedirectTo).not.toHaveBeenCalled();
            });
        });

        describe('isConnected', () => {
            it('should return a falsy value', () => {
                const emitter = new QueryConnectionEmitter(options);

                expect(emitter.isConnected).toBeFalsy();
            });
        });
    });
});
