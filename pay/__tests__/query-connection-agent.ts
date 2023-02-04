import { mocked } from 'ts-jest/utils';

import { redirectTo, getSource } from '../../url';
import { ConnectionMessage } from '../connection-message';
import { ConnectionOptions } from '../connection-options';
import { setDataToUrl } from '../helpers';
import { QueryConnectionAgent } from '../query-connection-agent';

const mockedRedirectTo = mocked(redirectTo);
const mockedGetSource = mocked(getSource);

jest.mock('../../url');

describe('query-connection-agent', () => {
    beforeEach(() => {
        mockedRedirectTo.mockReset();
        mockedGetSource.mockReset();
    });

    describe('QueryConnectionAgent', () => {
        const targetUrl = 'http://target.yp.local.com/path/a/b/c';
        const sourceUrl = 'http://source.yp.local.com/path/a/b/c';
        const channel = 'some-channel';
        let options: ConnectionOptions;

        beforeEach(() => {
            options = new ConnectionOptions(targetUrl, channel);

            mockedGetSource.mockReturnValue(sourceUrl);
        });

        describe('#', () => {
            it('should immediately send the payload', () => {
                const payload = String(Date.now());
                const message = new ConnectionMessage(payload, channel, sourceUrl);
                const url = setDataToUrl(targetUrl, String(message));

                new QueryConnectionAgent(options, payload);

                expect(mockedRedirectTo).toHaveBeenCalledWith(url);
            });
        });
    });
});
