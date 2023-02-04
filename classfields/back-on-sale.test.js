const de = require('descript');
const mockdate = require('mockdate');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const postComebackFixtures = require('auto-core/server/resources/publicApiComeback/methods/postComeback.fixtures');

const baseController = require('../baseController.mock');
const backOnSale = require('./back-on-sale');

let context;
let controller;
let req;
let res;
beforeEach(() => {
    // 2019-12-31T21:00:00.000Z / 2019-12-31T00:00:00.000+03:00
    mockdate.set(1577826000000);
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    controller = baseController(backOnSale);
});

it('должен сформировать листинга за 30 дней, если не переданы даты', () => {
    publicApi
        .post('/1.0/comeback', {
            // 2019-12-02T00:00:00.000+03:00 - 2020-01-02T00:00:00.000+03:00 (+1 день добавляется внутри ручки специально)
            filter: { creation_date_from: 1575234000000, creation_date_to: 1577912400000, page_size: 10 },
            sorting: 'CREATION_DATE',
            pagination: { page: 1, page_size: 10 },
        })
        .reply(200, postComebackFixtures.emptyResponse('1575234000000', '1577912400000'));

    return de.run(controller, { context })
        .then((result) => {
            expect(result.page.listing).toEqual({
                offers: [],
                search_parameters: {
                    creation_date_from: '2019-12-02',
                    creation_date_to: '2020-01-01',
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

it('должен передать даты в таймзоне MSK, если даты пришли в запрос', () => {
    publicApi
        .post('/1.0/comeback', {
            // 2020-01-01T00:00:00.000+03:00 - 2020-01-16T00:00:00.000+03:00 (+1 день добавляется внутри ручки специально)
            filter: { creation_date_from: 1577826000000, creation_date_to: 1579122000000, page_size: 10 },
            sorting: 'CREATION_DATE',
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

    return de.run(controller, args)
        .then((result) => {
            expect(result.page.listing).toEqual({
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
