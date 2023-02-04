const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./getSearchTagDictionary');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let blockWithCache;
let cache;
let context;
let req;
let res;
beforeEach(() => {
    cache = new de.Cache();
    blockWithCache = block({
        options: { cache },
    });
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('хороший ответ', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/reference/catalog/tags/v1')
            .reply(200, {
                dictionary_v1: {
                    values: [
                        { code: 'big-trunk', name: 'Большой багажник' },
                    ],
                },
                status: 'SUCCESS',
            });
    });

    it('должен закешировать запрос с правильным ключом', () => {
        return de.run(blockWithCache, { context, params: { category: 'cars' } })
            .then(() => {
                expect(
                    cache.get({
                        key: 'descript3-publicApiCatalog:///1.0/reference/catalog/tags/v1?',
                    }),
                ).toBeDefined();
            });
    });
});

describe('плохой ответ', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/reference/catalog/tags/v1')
            .times(2)
            .reply(500);
    });

    it('не должен закешировать запрос', () => {
        context.cache = new de.Cache();

        return de.run(blockWithCache, { context, params: { category: 'cars' } })
            .then(() => {
                expect(Object.keys(cache._cache)).toHaveLength(0);
            });
    });

    it('должен вернуть fallback', () => {
        return de.run(blockWithCache, { context, params: { category: 'cars' } })
            .then((result) => {
                expect(result).toEqual([]);
            });
    });
});
