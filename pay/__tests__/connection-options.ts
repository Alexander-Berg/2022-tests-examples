import { mocked } from 'ts-jest/utils';

import { getSource } from '../../url';
import { ConnectionOptions } from '../connection-options';
import { ANY_ORIGIN, ANY_CHANNEL, DEFAULT_URL } from '../constants';

const mockedGetSource = mocked(getSource);

jest.mock('../../url');

describe('connection-options', () => {
    beforeEach(() => {
        mockedGetSource.mockReset();
    });

    describe('ConnectionOptions', () => {
        describe('#', () => {
            it('should set props', () => {
                const targetUrl = 'http://target.yp.local.com/path/a/b/c';
                const channel = 'some-channel';

                const options = new ConnectionOptions(targetUrl, channel);

                expect(options.targetUrl).toBe(targetUrl);
                expect(options.channel).toBe(channel);
            });

            it('should use default values', () => {
                const options = new ConnectionOptions();

                expect(options.targetUrl).toBe(DEFAULT_URL);
                expect(options.channel).toBe(ANY_CHANNEL);
            });
        });

        describe('targetOrigin', () => {
            it('should return the origin of the target url', () => {
                const origin = 'http://target.yp.local.com';
                const targetUrl = `${origin}/path/a/b/c`;

                const options = new ConnectionOptions(targetUrl);

                expect(options.targetOrigin).toBe(origin);
            });

            it('should return the any origin constant if the target url does not contain an origin', () => {
                const options = new ConnectionOptions();

                expect(options.targetOrigin).toBe(ANY_ORIGIN);
            });
        });

        describe('sourceUrl', () => {
            it('should return the source url', () => {
                const sourceUrl = 'http://source.yp.local.com/path/a/b/c';

                mockedGetSource.mockReturnValue(sourceUrl);

                const options = new ConnectionOptions();

                expect(options.sourceUrl).toBe(sourceUrl);
            });
        });

        describe('sourceOrigin', () => {
            it('should return the origin of the source url', () => {
                const origin = 'http://source.yp.local.com';
                const sourceUrl = `${origin}/path/a/b/c`;

                mockedGetSource.mockReturnValue(sourceUrl);

                const options = new ConnectionOptions();

                expect(options.sourceOrigin).toBe(origin);
            });

            it('should return the any origin constant if the source url does not contain an origin', () => {
                mockedGetSource.mockReturnValue('');

                const options = new ConnectionOptions();

                expect(options.sourceOrigin).toBe(ANY_ORIGIN);
            });
        });

        describe('isInitialized', () => {
            it('should return a truthy value if the target url is initialized', () => {
                const targetUrl = 'http://target.yp.local.com/path/a/b/c';

                const options = new ConnectionOptions(targetUrl);

                expect(options.isInitialized).toBeTruthy();
            });

            it('should return a truthy value if the target url is equals to the default value', () => {
                const options = new ConnectionOptions();

                expect(options.isInitialized).toBeFalsy();
            });
        });
    });
});
