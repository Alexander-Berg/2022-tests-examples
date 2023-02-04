jest.mock('auto-core/lib/core/isCabinetApp');

const nock = require('nock');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const { EXP_FLAGS_KEY, TEST_ID_KEY } = require('auto-core/data/experimentCookieNames');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const isCabinetApp = require('auto-core/lib/core/isCabinetApp');
const GET_EXPERIMENTS_MOCK_VALUE = {
    expflags: [ {
        HANDLER: 'AUTO_RU',
        CONTEXT: { AUTO_RU: { flags: 'expflags' } },

    }, {
        HANDLER: 'AUTO_RU',
        CONTEXT: { AUTO_RU: { SEARCHER: { abtexp: 'searcherExpflags' } } },
    },
    {
        HANDLER: 'AUTORU_CABINET',
        CONTEXT: { MAIN: { SEARCHER: { abtexp: 'searcherExpflags' } } },
    },
    {
        HANDLER: 'AUTORU_CABINET',
        CONTEXT: {
            MAIN: {
                AUTORU_CABINET: {
                    'AUTORUFRONT-21145-a': true,
                    'AUTORUFRONT-21145-b': true,
                },
            },
        },
    },
    ],
    expboxes: [],
};

jest.mock('auto-core/server/resources/uaas/methods/getExperiments', () => GET_EXPERIMENTS_MOCK_VALUE);

const experiments = require('./experiments');

let next;
let req;
let res;
beforeEach(() => {
    isCabinetApp.mockReturnValue(false);
    req = createHttpReq();
    res = createHttpRes();
    next = jest.fn();
});

it('пропускает получание данных из пилота и uaas, если в куке exp_flags что-то лежит', () => {
    return new Promise((done) => {
        req.cookies.autoruuid = 'autoruuid';
        req.cookies[EXP_FLAGS_KEY] = '["without_exp"]';

        const next = jest.fn(() => {
            expect(req.experimentsData.skipGettingExperiments).toBe(true);
            expect(req.experimentsData.uaas).toBeUndefined();

            done();
        });

        experiments(req, res, next);
    });
});

it('запускает получание данных из пилота и uaas, если в куке exp_flags ничего нет', () => {
    return new Promise((done) => {
        req.cookies.autoruuid = 'autoruuid';

        const next = jest.fn(() => {
            expect(req.experimentsData.skipGettingExperiments).toBe(false);
            expect(req.experimentsData.uaas).toMatchObject(GET_EXPERIMENTS_MOCK_VALUE);
            expect(req.experimentsData.experiments).toEqual({
                expflags: true,
                searcherExpflags: true,
            });

            done();
        });

        experiments(req, res, next);
    });
});

it('должен получить данные для приложения cabinet', () => {
    return new Promise((done) => {
        isCabinetApp.mockReturnValue(true);
        req.cookies.autoruuid = 'autoruuid';

        const next = jest.fn(() => {
            expect(req.experimentsData.experiments).toEqual({
                'AUTORUFRONT-21145-a': true,
                'AUTORUFRONT-21145-b': true,
                searcherExpflags: true,
            });

            done();
        });

        experiments(req, res, next);
    });
});

it('поддерживаем передачу exp_flags в качестве массива в query_params', () => {
    return new Promise((done) => {
        req.query = {
            exp_flags: [ 'exp1', 'exp2' ],
        };

        const next = jest.fn(() => {
            expect(req.experimentsData.skipGettingExperiments).toBe(true);
            expect(req.experimentsData.experiments).toMatchSnapshot();

            done();
        });

        experiments(req, res, next);
    });
});

it('передаем эксперименты в параметр exp_flags в query_params через запятую', () => {
    return new Promise((done) => {
        req.query = {
            exp_flags: 'exp1,exp2',
        };

        const next = jest.fn(() => {
            expect(req.experimentsData.skipGettingExperiments).toBe(true);
            expect(req.experimentsData.experiments).toMatchSnapshot();

            done();
        });

        experiments(req, res, next);
    });
});

it('проставит куку test-id из GET-параметров', () => {
    req.query = {
        [TEST_ID_KEY]: '123',
    };

    experiments(req, res, next);
    expect(res.cookie).toHaveBeenCalledWith('test-id', '["123"]');
});

it('передаем несколько значений в test-id в query_params через нижнее подчеркивание', () => {
    req.query = {
        [TEST_ID_KEY]: '123_231_444',
    };

    experiments(req, res, next);
    expect(res.cookie).toHaveBeenCalledWith('test-id', '["123","231","444"]');
});

it('если нет куки autoruuid, то должен сходить в /session, получить там куки и потом в uaas', () => {
    return new Promise((resolve, reject) => {
        publicApi.get('/1.0/session/').reply(200, sessionFixtures.user_auth());
        delete req.cookies.autoruuid;

        experiments(req, res, () => {
            try {
                expect(nock.isDone()).toEqual(true);

                expect(req.experimentsData.uaas).toMatchObject(GET_EXPERIMENTS_MOCK_VALUE);
                expect(req.experimentsData.experiments).toEqual({
                    expflags: true,
                    searcherExpflags: true,
                });
                expect(req.cookies.autoruuid).toEqual('TEST_DEVICE_ID');

                resolve();
            } catch (e) {
                reject(e);
            }
        });
    });
});
