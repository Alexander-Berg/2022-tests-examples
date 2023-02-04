import { fetchGet, fetchPost, FetchError } from '../index';

describe('Concurrent fetches', () => {
    it('Previous request fails with "abort"', async () => {
        expect.assertions(1);

        const url = 'http://test/test/test1';

        const fetch1 = fetchGet(url, { test: 'test' });
        fetchGet(url, { test: 'test' });

        await expect(fetch1).rejects.toThrow(new FetchError('abort', url, 0));
    });

    it('Last request succeeds', async () => {
        expect.assertions(1);

        const url = 'http://test/test/test2';

        return expect(fetchGet(url, { test: 'test' }, true)).resolves.toMatchObject({
            url: `${url}?test=test`,
            status: 200
        });
    });

    it('Do not abort POST requests if manually set not to do it', async () => {
        expect.assertions(1);

        const url = 'http://test/test/test3';

        const fetch1 = fetchPost(url, { test: 'test' }, null);

        await fetchPost(url, { test: 'test' }, null, false);

        return expect(fetch1).resolves.toMatchObject({
            url,
            status: 200
        });
    });

    it('Do not abort GET requests if manually set not to do it', async () => {
        expect.assertions(2);

        const url = 'http://test/test/test4';

        const fetch1 = fetchGet(url, { test: 'test' }, true);

        await fetchGet(url, { test: 'test' }, false);

        return fetch1.then(response => {
            expect(response.url.indexOf(`${url}?test=test`) > -1).toBeTruthy();
            expect(response.status).toEqual(200);
        });
    });
});
