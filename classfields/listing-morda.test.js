jest.mock('auto-core/ydb/query/getSearchPreset', () => {
    return {
        'default': jest.fn(() => Promise.resolve([
            { data: JSON.stringify([]) },
        ])),
    };
});

jest.mock('auto-core/ydb/query/getRecommendedFromCache', () => {
    return {
        'default': jest.fn(() => Promise.resolve([])),
    };
});

jest.mock('auto-core/ydb/query/updateRecommendedYdbCache', () => {
    return {
        'default': jest.fn(() => Promise.resolve(undefined)),
    };
});

jest.mock('auto-core/lib/util/getBunkerDict', () => {
    return jest.fn(() => (
        {
            recommended_exp: 100,
            doubledeck: {
                max_offers_count: 20,
                ydb_offers_count: 30,
            },
            recommended: {
                max_offers_count: 15,
            },
        }));
});

const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const controller = require('./listing-morda');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const getSearchPreset = require('auto-core/ydb/query/getSearchPreset').default;

let context;
let req;
let res;
let desktopParams;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    desktopParams = {
        dpr: '1.25',
        from: 'home',
        lang: 'ru',
        mordazone: 'ru',
        reqid: '1604747576.1192.85615.138450',
        platform: 'desktop',
    };
});

describe('вычисление региона', () => {
    it('должен запросить данные для Саратова, если geoid=118085', () => {
        desktopParams.geoid = '118085'; // Зоринский (Саратовская область)

        return de.run(controller, { context, params: desktopParams }).then(
            () => {
                expect(getSearchPreset).toHaveBeenCalledTimes(1);
                expect(getSearchPreset).toHaveBeenCalledWith(
                    99844,
                    'web-desktop',
                    {
                        _request_id: 'jest-request-id',
                        experiments: [ undefined ],
                        geo_id: 99844,
                        geohelper: false,
                        maxOffersCount: 15,
                        platform: 'desktop',
                        preset: 'web-desktop',
                        query: 'getSearchPreset',
                        zen_new_touch: false,
                        theme: undefined,
                        timeout: 250,
                        yandexuid: undefined,
                    },
                    req,
                );
            });
    });

    it('должен запросить данные для Краснодара, если geoid=35', () => {
        desktopParams.geoid = '35';

        return de.run(controller, { context, params: desktopParams }).then(
            () => {
                expect(getSearchPreset).toHaveBeenCalledTimes(1);
                expect(getSearchPreset).toHaveBeenCalledWith(
                    35,
                    'web-desktop',
                    {
                        _request_id: 'jest-request-id',
                        experiments: [ undefined ],
                        geo_id: 35,
                        geohelper: false,
                        maxOffersCount: 15,
                        platform: 'desktop',
                        preset: 'web-desktop',
                        query: 'getSearchPreset',
                        zen_new_touch: false,
                        theme: undefined,
                        timeout: 250,
                        yandexuid: undefined,
                    },
                    req,
                );
            });
    });

});

describe('запрос рекомендованных', () => {
    let scope;
    beforeEach(() => {
        scope = nock(`http://${ process.env.PUBLICAPI_HOSTNAME }`, {
            reqheaders: { 'x-yandexuid': '123456' },
        })
            .get('/1.0/personalization/get-recommended-offers?max_offers_count=15&geo_id=99844')
            .reply(200, {});

        desktopParams.geoid = '118085';
    });

    it('должен сделать запрос, если есть yandexuid', () => {
        req.cookies.yandexuid = '123456';

        return de.run(controller, { context, params: desktopParams }).then(
            () => {
                expect(scope.isDone()).toBe(true);
            });
    });

    it('не должен сделать запрос, если нет yandexuid', () => {
        return de.run(controller, { context, params: desktopParams }).then(
            () => {
                expect(scope.isDone()).toBe(false);
            });
    });
});
