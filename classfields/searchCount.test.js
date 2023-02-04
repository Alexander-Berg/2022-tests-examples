const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const searchCount = require('./searchCount');

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

// проверяем по принципу "работает один - работают все"
it(`должен исключить параметр "sort" из запроса`, () => {
    publicApi
        .get('/1.0/search/cars/count')
        .query({
            category: 'cars',
            context: 'listing',
        })
        .reply(200, {
            count: 100,
            pagination: {
                page: 1,
                page_size: 20,
                total_offers_count: 100,
                total_page_count: 10,
            },
            search_parameters: {},
        });

    const params = {
        sort: 'value',
    };

    return de.run(searchCount, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                pagination: {
                    total_offers_count: 100,
                },
                search_parameters: {},
            });
        });
});
