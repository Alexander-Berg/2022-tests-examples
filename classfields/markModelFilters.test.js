const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const markModelFilters = require('./markModelFilters');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

[
    'catalog_filter',
    'currency',
    'mark_model_nameplate',
    'page',
    'page_size',
    'pinned_offer_id',
    'sort',
    'top_days',
].forEach((param) => {
    it(`должен исключить параметр ${ param } из запроса`, () => {
        publicApi
            .get('/1.0/search/cars/mark-model-filters')
            .query({
                category: 'cars',
                context: 'listing',
            })
            .reply(200, {
                mark_entries: [ 'response' ],
            });

        const params = {
            category: 'cars',
            [ param ]: 'value',
        };

        return de.run(markModelFilters, { context, params })
            .then((result) => {
                expect(result).toEqual([ 'response' ]);
            });
    });
});

describe('cache', () => {
    // https://st.yandex-team.ru/VS-1246#61b36f97ffe6936f44c70063

    let cache;
    let markModelFiltersWithCache;
    beforeEach(() => {
        cache = {
            get: jest.fn(() => ({})),
            set: jest.fn(() => {}),
        };

        markModelFiltersWithCache = markModelFilters({
            options: {
                cache,
            },
        });
    });

    it('должен построить правильный ключ кеша с only_nds', async() => {
        const params = {
            catalog_filter: [ { mark: 'SUBARU', model: 'LEGACY' }, { mark: 'SUBARU', model: 'WRX' } ],
            only_nds: true,
            section: 'all',
            category: 'cars',
            rid: 1,
        };

        await de.run(markModelFiltersWithCache, { context, params });

        expect(cache.get).toHaveBeenCalledTimes(1);
        expect(cache.get.mock.calls[0][0]).toMatchObject({
            key: 'descript3-publicApiLeprosarium:///1.0/search/cars/mark-model-filters?category=cars&context=listing&only_nds=true&rid=1&state_group=ALL',
        });
    });

    it('должен построить правильный ключ кеша без only_nds', async() => {
        const params = {
            catalog_filter: [ { mark: 'SUBARU', model: 'LEGACY' }, { mark: 'SUBARU', model: 'WRX' } ],
            section: 'all',
            category: 'cars',
            rid: 1,
        };

        await de.run(markModelFiltersWithCache, { context, params });

        expect(cache.get).toHaveBeenCalledTimes(1);
        expect(cache.get.mock.calls[0][0]).toMatchObject({
            key: 'descript3-publicApiLeprosarium:///1.0/search/cars/mark-model-filters?category=cars&context=listing&rid=1&state_group=ALL',
        });
    });
});
