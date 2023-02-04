/**
 * @jest-environment node
 */
const index = require('./index');

const de = require('descript');
const MockDate = require('mockdate');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const createDeps = require('autoru-frontend/mocks/createDeps');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');

let context;
let req;
let res;
let controller;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    MockDate.set('2018-11-05');

    const deps = {
        customerRole: CUSTOMER_ROLES.client,
    };

    controller = de.func({
        block: (args) => {
            const { blocks, ids } = createDeps(deps, args);

            return de.object({
                block: {
                    ...blocks,
                    index: index(ids),
                },
            });
        },
    });
});

afterEach(() => {
    MockDate.reset();
});

it('должен запрашивать корректные дефолтные даты для статистики офферов [-61 день, сегодня]', () => {
    const successfulResult = { days: 30 };

    publicApi
        .get('/1.0/dealer/offers-daily-stats')
        .query({
            from_date: '2018-09-05',
            to_date: '2018-11-05',
        })
        .reply(200, successfulResult);

    return de.run(controller, { context })
        .then((result) => {
            expect(result.index.offerDailyStats).toEqual(successfulResult.days);
        });
});

it('должен передавать правильный интервал для статистики офферов, который в 2 раза больше переданного в параметрах страницы', () => {
    const successfulResult = { days: 30 };

    publicApi
        .get('/1.0/dealer/offers-daily-stats')
        .query({
            from_date: '2018-09-04',
            to_date: '2018-09-15',
        })
        .reply(200, successfulResult);

    const params = {
        from: '2018-09-10',
        to: '2018-09-15',
    };

    return de.run(controller, { context, params })
        .then((result) => {
            expect(result.index.offerDailyStats).toEqual(successfulResult.days);
        });
});

it('должен редиректить на тот же роут с безопасным интервалом для статистики списаний, если он был превышен', async() => {
    const params = {
        from: '2018-03-10',
        to: '2018-09-20',
        additional_param: 'foo',
    };

    await expect(
        de.run(controller, { context, params }),
    ).rejects.toMatchObject({
        error: {
            code: 'CABINET_UNSAFE_DATE_LIMITS',
            id: 'REDIRECTED',
            location: '/?from=2018-06-20&to=2018-09-20&additional_param=foo',
            status_code: 302,
        },
    });
});
