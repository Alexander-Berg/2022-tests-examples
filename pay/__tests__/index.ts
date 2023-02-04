import {
    SearchParams,
    redirectTo,
    replaceWith,
    getSource,
    getSourceSearch,
    getOrigin,
    getUrlParts,
    getSearchString,
    getHash,
    getSearchParams,
    createSearchString,
    setSearchParams,
} from '../index';

describe('index', () => {
    describe('redirectTo', () => {
        it('should redirect to the received url', () => {
            const urlParts = getUrlParts(getSource());
            const sourceUrl = urlParts.poorUrl;
            const targetUrl = `${sourceUrl}#${Date.now()}`;

            redirectTo(targetUrl);

            expect(getSource()).toBe(targetUrl);
        });
    });

    describe('replaceWith', () => {
        it('should replace the current url', () => {
            const urlParts = getUrlParts(getSource());
            const sourceUrl = urlParts.poorUrl;
            const targetUrl = `${sourceUrl}#${Date.now()}`;

            replaceWith(targetUrl);

            expect(getSource()).toBe(targetUrl);
        });
    });

    describe('getSourceSearch', () => {
        it('should return the current search string', () => {
            const search = '?abc=123&def=456';
            const urlParts = getUrlParts(getSource());
            const sourceUrl = urlParts.poorUrl;
            const targetUrl = `${sourceUrl}${search}`;

            replaceWith(targetUrl);

            expect(getSourceSearch()).toBe(search);
        });
    });

    describe('getOrigin', () => {
        it('should cut the origin from the received url', () => {
            const origin = 'http://any.yp.local.com';
            const url = `${origin}/path/a/b/c`;

            const actualOrigin = getOrigin(url);

            expect(actualOrigin).toBe(origin);
        });
    });

    describe('getUrlParts', () => {
        it('should return the url parts', () => {
            const poorUrl = 'http://any.yp.local.com/path/a/b/c';
            const search = '?abc=123&def=456';
            const hash = '#abcdef';
            const url = `${poorUrl}${search}${hash}`;

            const actualUrlParts = getUrlParts(url);

            expect(actualUrlParts).toEqual({
                poorUrl,
                search,
                hash,
            });
        });

        it('should return the empty strings if an url is empty', () => {
            const actualUrlParts = getUrlParts('');

            expect(actualUrlParts).toEqual({
                poorUrl: '',
                search: '',
                hash: '',
            });
        });

        it('should set the property "poorUrl" to an empty string if there is not an url after a search', () => {
            const search = '?abc=123&def=456';
            const hash = '#abcdef';
            const url = `${search}${hash}`;

            const actualUrlParts = getUrlParts(url);

            expect(actualUrlParts).toEqual({
                poorUrl: '',
                search,
                hash,
            });
        });

        it('should set the property "search" to an empty string if it does not exist', () => {
            const poorUrl = 'http://any.yp.local.com/path/a/b/c';
            const hash = '#abcdef';
            const url = `${poorUrl}${hash}`;

            const actualUrlParts = getUrlParts(url);

            expect(actualUrlParts).toEqual({
                poorUrl,
                search: '',
                hash,
            });
        });

        it('should set the property "hash" to an empty string if it does not exist', () => {
            const poorUrl = 'http://any.yp.local.com/path/a/b/c';
            const search = '?abc=123&def=456';
            const url = `${poorUrl}${search}`;

            const actualUrlParts = getUrlParts(url);

            expect(actualUrlParts).toEqual({
                poorUrl,
                search,
                hash: '',
            });
        });
    });

    describe('getSearchString', () => {
        it('should retrurn the search string from the received url', () => {
            const searchString = 'abc=123&def=456';
            const url = `http://any.yp.local.com/path/a/b/c?${searchString}`;

            const actualSearchString = getSearchString(url);

            expect(actualSearchString).toBe(searchString);
        });

        it('should return an empty string if the received url does not contain a search string 1', () => {
            const url = 'http://any.yp.local.com/path/a/b/c';

            const actualSearchString = getSearchString(url);

            expect(actualSearchString).toBe('');
        });

        it('should return an empty string if the received url does not contain a search string 2', () => {
            const url = 'http://any.yp.local.com/path/a/b/c?';

            const actualSearchString = getSearchString(url);

            expect(actualSearchString).toBe('');
        });
    });

    describe('getHash', () => {
        it('should retrurn the hash from the received url', () => {
            const hash = 'abcdef';
            const url = `http://any.yp.local.com/path/a/b/c#${hash}`;

            const actualHash = getHash(url);

            expect(actualHash).toBe(hash);
        });

        it('should return an empty string if the received url does not contain a hash 1', () => {
            const url = 'http://any.yp.local.com/path/a/b/c';

            const actualHash = getHash(url);

            expect(actualHash).toBe('');
        });

        it('should return an empty string if the received url does not contain a hash 2', () => {
            const url = 'http://any.yp.local.com/path/a/b/c#';

            const actualHash = getHash(url);

            expect(actualHash).toBe('');
        });
    });

    describe('getSearchParams', () => {
        it('should return search params 1', () => {
            const url = 'http://any.yp.local.com/path/a/b/c?abc=123&def=456';

            const actualSearchParams = getSearchParams(url);

            expect(actualSearchParams).toEqual({
                abc: '123',
                def: '456',
            });
        });

        it('should return an empty object if the received url does not contain a search string', () => {
            const url = 'http://any.yp.local.com/path/a/b/c';

            const actualSearchParams = getSearchParams(url);

            expect(actualSearchParams).toEqual({});
        });
    });

    describe('createSearchString', () => {
        it('should return a search string', () => {
            const searchParams: SearchParams = { a: '1', b: '2', c: '3' };

            const actualSearchString = createSearchString(searchParams);

            expect(actualSearchString).toBe('a=1&b=2&c=3');
        });

        it('should return an empty string if there are not the search params', () => {
            const searchParams: SearchParams = {};

            const actualSearchString = createSearchString(searchParams);

            expect(actualSearchString).toBe('');
        });

        it('should reject the values "undefined"', () => {
            const searchParams: SearchParams = { a: '1', b: undefined, c: '3' };

            const actualSearchString = createSearchString(searchParams);

            expect(actualSearchString).toBe('a=1&c=3');
        });
    });

    describe('setSearchParams', () => {
        it('should return an url with the received parameters 1', () => {
            const searchParams: SearchParams = { b: '2', c: '3' };
            const urlWithoutSearchAndHash = 'http://any.yp.local.com/path/a/b/c';
            const url = `${urlWithoutSearchAndHash}?a=1&b=10`;

            const actualUrl = setSearchParams(url, searchParams);

            expect(actualUrl).toBe(`${urlWithoutSearchAndHash}?a=1&b=2&c=3`);
        });

        it('should return an url with the received parameters 2', () => {
            const searchParams: SearchParams = { b: '2', c: '3' };
            const urlWithoutSearchAndHash = 'http://any.yp.local.com/path/a/b/c';
            const url = `${urlWithoutSearchAndHash}?a=1&b=10#abcdef`;

            const actualUrl = setSearchParams(url, searchParams);

            expect(actualUrl).toBe(`${urlWithoutSearchAndHash}?a=1&b=2&c=3#abcdef`);
        });

        it('should return an url without changes if there are not the search params 1', () => {
            const url = 'http://any.yp.local.com/path/a/b/c?abc=123&def=456';

            const actualUrl = setSearchParams(url, {});

            expect(actualUrl).toBe(url);
        });

        it('should return an url without changes if there are not the search params 2', () => {
            const url = 'http://any.yp.local.com/path/a/b/c#abcdef';

            const actualUrl = setSearchParams(url, {});

            expect(actualUrl).toBe(url);
        });
    });
});
