jest.mock('auto-core/lib/util/getBunkerDict', () => {
    return (path) => {
        if (path === 'common/af-search-app') {
            return {
                recommended_exp_pp: 0,
                recommended_exp: 100,
                groups_amount: 10,
            };
        }
    };
});

const de = require('descript');
const nock = require('nock');

const getPresetByYandexuid = require('./getPresetByYandexuid');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const offerMock = require('autoru-frontend/mockData/responses/offer.mock.json');

let context;
let req;
let res;
let badRequest;
let goodRequest;
let params;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    goodRequest = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        reqheaders: {
            'x-yandexuid': 'good_12',
        },
    })
        .get('/1.0/personalization/get-recommended-offers')
        .reply(200, {
            offers: [
                { ...offerMock, id: '1' },
                { ...offerMock, id: '2' },
                { ...offerMock, id: '3' },
                { ...offerMock, id: '4' },
                { ...offerMock, id: '5' },
            ],
        });

    badRequest = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        reqheaders: {
            'x-yandexuid': 'bad_12',
        },
    })
        .get('/1.0/personalization/get-recommended-offers')
        .reply(200, {
            offers: [],
        });

    params = { preset: 'web', yandexuid: 'good_12' };
});

it('должен отдать объявления для preset=web, если их >= 1', () => {
    return de.run(getPresetByYandexuid, { context, params }).then(
        (result) => {
            expect(result.offers).toMatchObject([
                { link_params: { sale_id: '1' } },
                { link_params: { sale_id: '2' } },
                { link_params: { sale_id: '3' } },
                { link_params: { sale_id: '4' } },
                { link_params: { sale_id: '5' } },
            ]);
            expect(goodRequest.isDone()).toEqual(true);
            expect(badRequest.isDone()).toEqual(false);
        });
});

it('не должен отдать объявления для preset=web, если их < 1', () => {
    params.yandexuid = 'bad_12';
    return de.run(getPresetByYandexuid, { context, params }).then(
        (result) => {
            expect(result).toEqual([]);
            expect(goodRequest.isDone()).toEqual(false);
            expect(badRequest.isDone()).toEqual(true);
        });
});

it('не должен запросить объявления, если нет yandexuid', () => {
    delete params.yandexuid;
    return de.run(getPresetByYandexuid, { context, params }).then(
        (result) => {
            expect(result).toEqual([]);
            expect(goodRequest.isDone()).toEqual(false);
            expect(badRequest.isDone()).toEqual(false);
        });
});

it('должен вернуть пустой результат в случае таймаута', () => {
    nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
        reqheaders: {
            'x-yandexuid': 'timeout_12',
        },
    })
        .get('/1.0/personalization/get-recommended-offers')
        .delay(300)
        .reply(200, {
            offers: [
                { ...offerMock, id: '1' },
                { ...offerMock, id: '2' },
                { ...offerMock, id: '3' },
                { ...offerMock, id: '4' },
                { ...offerMock, id: '5' },
            ],
        });

    params.yandexuid = 'timeout_12';
    return de.run(getPresetByYandexuid, { context, params }).then(
        (result) => {
            expect(result).toEqual([]);
        });
});
