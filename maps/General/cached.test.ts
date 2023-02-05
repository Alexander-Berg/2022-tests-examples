import cached, {PromiseFn} from './cached';

/* eslint-disable jest/valid-expect */

describe('cached', () => {
    const TIMEOUT = 50;
    const invalidateCache = (): Promise<void> => new Promise((resolve) => setTimeout(resolve, TIMEOUT * 1.1));
    const id = (input?: string, error?: boolean): Promise<string | undefined> =>
        error ? Promise.reject(input) : Promise.resolve(input);
    let cachedInstance: PromiseFn<[string?, boolean?], string | undefined>;

    beforeEach(() => {
        cachedInstance = cached<[string?, boolean?], string | undefined>(id, {maxAge: TIMEOUT});
    });

    it('should return promise result', () => {
        expect.assertions(1);
        expect(cachedInstance('first')).resolves.toEqual('first');
    });

    it('should return same results on both times invoked', () => {
        expect.assertions(2);
        expect(cachedInstance('first')).resolves.toEqual('first');
        expect(cachedInstance('second')).resolves.toEqual('first');
    });

    it('should return same error on both times invoked', () => {
        expect.assertions(2);
        expect(cachedInstance('error', true)).rejects.toEqual('error');
        expect(cachedInstance('dont care')).rejects.toEqual('error');
    });

    it('should use first successful value', () => {
        expect.assertions(1);
        return cachedInstance('error', true)
            .catch(id)
            .then(() => expect(cachedInstance('second')).resolves.toEqual('second'));
    });

    describe('never return stale cache if staleWhileRevalidate === 0', () => {
        beforeEach(() => {
            cachedInstance = cached(id, {maxAge: TIMEOUT, staleWhileRevalidate: 0});
        });

        it('should not use cache after refresh', () => {
            expect.assertions(1);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => expect(cachedInstance('second')).resolves.toEqual('second'));
        });

        it('should use cache after refresh (+ simultaneous requests)', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('second')).resolves.toEqual('second'),
                        expect(cachedInstance('third')).resolves.toEqual('second')
                    ])
                );
        });

        it('should return last truthy value after error', () => {
            expect.assertions(1);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('error', true).catch(id))
                .then(() => expect(cachedInstance('second')).resolves.toEqual('first'));
        });

        it('should refresh value after error and invalidation', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('error', true).catch(id))
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('second')).resolves.toEqual('second'),
                        expect(cachedInstance('third')).resolves.toEqual('second')
                    ])
                );
        });

        it('should use new result after several refreshes', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('second'))
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('third')).resolves.toEqual('third'),
                        expect(cachedInstance('fourth')).resolves.toEqual('third')
                    ])
                );
        });

        it('should use new error after several refreshes', () => {
            expect.assertions(3);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('second'))
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('error', true)).rejects.toEqual('error'),
                        expect(cachedInstance('anything')).rejects.toEqual('error'),
                        expect(cachedInstance('otherthing')).rejects.toEqual('error')
                    ])
                );
        });
    });

    describe('return stale cache before cache update', () => {
        it('should return old result', () => {
            expect.assertions(1);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => expect(cachedInstance('second')).resolves.toEqual('first'));
        });

        it('should use cached result after refresh', () => {
            expect.assertions(1);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => {
                    cachedInstance('second');
                    return expect(cachedInstance('third')).resolves.toEqual('first');
                });
        });

        it('should use cached result in the same time as refresh with error', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('error', true)).resolves.toEqual('first'),
                        expect(cachedInstance('third')).resolves.toEqual('first')
                    ])
                );
        });

        it('should return last successful result after refresh with error', () => {
            expect.assertions(1);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('error', true).catch(id))
                .then(() => expect(cachedInstance('second')).resolves.toEqual('first'));
        });

        it('should return last successful result after refresh with error and invalidation', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('error', true).catch(id))
                .then(invalidateCache)
                .then(() => expect(cachedInstance('second')).resolves.toEqual('first'))
                .then(invalidateCache)
                .then(() => expect(cachedInstance('third')).resolves.toEqual('second'));
        });

        it('should use new result after several refreshes', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('second'))
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('third')).resolves.toEqual('second'),
                        expect(cachedInstance('fourth')).resolves.toEqual('second')
                    ])
                );
        });

        it('should return last successful result after several refreshes with error', () => {
            expect.assertions(2);
            return cachedInstance('first')
                .then(invalidateCache)
                .then(() => cachedInstance('error', true).catch(id))
                .then(invalidateCache)
                .then(() =>
                    Promise.all([
                        expect(cachedInstance('third')).resolves.toEqual('first'),
                        expect(cachedInstance('fourth')).resolves.toEqual('first')
                    ])
                );
        });
    });

    describe('shouldSkipCache is on', () => {
        beforeEach(() => {
            cachedInstance = cached(id, {maxAge: TIMEOUT, shouldSkipCache: (arg) => arg === 'skip'});
        });

        it('should not skip cache on falsy arg', async () => {
            expect(await cachedInstance('first')).toEqual('first');
            expect(await cachedInstance('second')).toEqual('first');
        });

        it('should skip cache on truthy arg', async () => {
            expect(await cachedInstance('first')).toEqual('first');
            expect(await cachedInstance('skip')).toEqual('skip');
            expect(await cachedInstance('third')).toEqual('first');
        });

        it('should not cache skipped arg', async () => {
            expect(await cachedInstance('skip')).toEqual('skip');
            expect(await cachedInstance('second')).toEqual('second');
        });
    });

    describe('shouldResetCache is on', () => {
        beforeEach(() => {
            cachedInstance = cached(id, {maxAge: TIMEOUT, shouldResetCache: (arg) => arg === 'new value'});
        });

        it('should not reset cache on falsy arg', async () => {
            expect(await cachedInstance('first')).toEqual('first');
            expect(await cachedInstance('second')).toEqual('first');
        });

        it('should reset cache on truthy arg', async () => {
            expect(await cachedInstance('first')).toEqual('first');
            expect(await cachedInstance('new value')).toEqual('new value');
            expect(await cachedInstance('third')).toEqual('new value');
        });
    });

    describe('getKey is specified', () => {
        beforeEach(() => {
            cachedInstance = cached(id, {
                maxAge: TIMEOUT,
                staleWhileRevalidate: 0,
                getKey: (arg) => arg!.split('_')[0]
            });
        });

        it('should return different values', async () => {
            expect(await cachedInstance('key1_first')).toEqual('key1_first');
            expect(await cachedInstance('key1_second')).toEqual('key1_first');
            expect(await cachedInstance('key2_first')).toEqual('key2_first');
            expect(await cachedInstance('key2_second')).toEqual('key2_first');

            await invalidateCache();

            expect(await cachedInstance('key1_third')).toEqual('key1_third');
            expect(await cachedInstance('key2_third')).toEqual('key2_third');
        });
    });

    describe('staleWhileRevalidate is specified', () => {
        it('serves stale content after invalidation but before staleWhileRevalidate', async () => {
            cachedInstance = cached(id, {
                maxAge: TIMEOUT,
                staleWhileRevalidate: 2 * TIMEOUT
            });

            expect(await cachedInstance('first')).toEqual('first');

            await invalidateCache();

            expect(await cachedInstance('second')).toEqual('first');
            expect(await cachedInstance()).toEqual('second');
        });

        it('serves fresh content after invalidation and after staleWhileRevalidate', async () => {
            cachedInstance = cached(id, {
                maxAge: TIMEOUT,
                staleWhileRevalidate: 0.05 * TIMEOUT
            });

            expect(await cachedInstance('first')).toEqual('first');

            await invalidateCache();

            expect(await cachedInstance('second')).toEqual('second');
        });
    });
});
