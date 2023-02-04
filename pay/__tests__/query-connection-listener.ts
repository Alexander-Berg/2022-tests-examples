import { mocked } from 'ts-jest/utils';

import { wait } from '../../promise/wait';
import { getSource } from '../../url';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { setDataToUrl } from '../helpers';
import { QueryConnectionListener } from '../query-connection-listener';

const mockedGetSource = mocked(getSource);

jest.mock('../../url');

describe('query-connection-listener', () => {
    beforeEach(() => {
        mockedGetSource.mockReset();
    });

    describe('QueryConnectionListener', () => {
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
                const message = new ConnectionMessage(payload);
                const url = setDataToUrl(sourceUrl, String(message));
                const handler = jest.fn();

                mockedGetSource.mockReturnValue(url);

                const listener = new QueryConnectionListener(options);

                listener.on(handler);
                await wait(0);

                expect(handler).toHaveBeenCalledWith(payload, message);
            });

            it('should process the message', async () => {
                const message = new ConnectionMessage(null, 'abc');
                const url = setDataToUrl(sourceUrl, String(message));
                const handler = jest.fn();

                mockedGetSource.mockReturnValue(url);

                const listener = new QueryConnectionListener(options);

                listener.on(handler);
                await wait(0);

                expect(handler).not.toHaveBeenCalled();
            });
        });

        describe('destroy', () => {
            it('should cancel message processing', async () => {
                const message = new ConnectionMessage(null);
                const url = setDataToUrl(sourceUrl, String(message));
                const handler = jest.fn();

                mockedGetSource.mockReturnValue(url);

                const listener = new QueryConnectionListener(options);

                listener.destroy();
                listener.on(handler);
                await wait(0);

                expect(handler).not.toHaveBeenCalled();
            });
        });
    });
});
