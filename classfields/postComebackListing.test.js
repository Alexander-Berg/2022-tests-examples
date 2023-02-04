const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const postComebackFixtures = require('auto-core/server/resources/publicApiComeback/methods/postComeback.fixtures');

const postComebackListing = require('./postComebackListing');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен передать даты в таймзоне MSK', () => {
    publicApi
        .post('/1.0/comeback', {
            // 2020-01-01T00:00:00.000+03:00 - 2020-01-16T00:00:00.000+03:00 (+1 день добавляется внутри ручки специально)
            filter: { creation_date_from: 1577826000000, creation_date_to: 1579122000000 },
            pagination: { page: 1, page_size: 10 },
        })
        .reply(200, postComebackFixtures.emptyResponse('1577826000000', '1579122000000'));

    const args = {
        context,
        params: {
            creation_date_from: '2020-01-01',
            creation_date_to: '2020-01-15',
        },
    };

    return de.run(postComebackListing, args)
        .then((result) => {
            expect(result).toEqual({
                offers: [],
                search_parameters: {
                    creation_date_to: '2020-01-15',
                    creation_date_from: '2020-01-01',
                    sorting: 'CREATION_DATE',
                },
                pagination: {
                    page: 1,
                    page_size: 10,
                    total_offers_count: 0,
                    total_page_count: 1,
                },
                vin_error: false,
                status: 'SUCCESS',
            });
        });
});
