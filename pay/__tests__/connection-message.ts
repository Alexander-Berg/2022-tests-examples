import { ConnectionMessage } from '../connection-message';
import { ANY_ORIGIN, ANY_CHANNEL, DEFAULT_URL } from '../constants';

describe('connection-message', () => {
    describe('ConnectionMessage', () => {
        describe('#', () => {
            it('should set props', () => {
                const payload = Date.now();
                const channel = 'some-channel';
                const sourceUrl = 'http://some.yp.local.com/path/a/b/c';

                const message = new ConnectionMessage(payload, channel, sourceUrl);

                expect(message.payload).toBe(payload);
                expect(message.channel).toBe(channel);
                expect(message.sourceUrl).toBe(sourceUrl);
            });

            it('should use default values', () => {
                const message = new ConnectionMessage(null);

                expect(message.channel).toBe(ANY_CHANNEL);
                expect(message.sourceUrl).toBe(undefined);
            });
        });

        describe('origin', () => {
            it('should return the origin of the source url', () => {
                const origin = 'http://some.yp.local.com';
                const sourceUrl = `${origin}/path/a/b/c`;

                const message = new ConnectionMessage(null, ANY_CHANNEL, sourceUrl);

                expect(message.origin).toBe(origin);
            });

            it('should return the any origin constant if the source url does not contain an origin', () => {
                const message = new ConnectionMessage(null);

                expect(message.origin).toBe(ANY_ORIGIN);
            });
        });

        describe('toString', () => {
            it('should return the json string', () => {
                const payload = Date.now();
                const channel = 'some-channel';
                const sourceUrl = 'http://some.yp.local.com/path/a/b/c';
                const json = JSON.stringify({ payload, channel, sourceUrl });

                const message = new ConnectionMessage(payload, channel, sourceUrl);

                expect(String(message)).toBe(json);
            });
        });
    });
});
