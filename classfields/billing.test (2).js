/**
 * @jest-environment node
 */
const _ = require('lodash');

const billing = require('./billing');

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const userFixtures = require('auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures');
const getSubscriptionFixtures = require('auto-core/server/resources/publicApiBilling/methods/getSubscription.nock.fixtures');
const getBillingSchedulesFixtures = require('auto-core/server/resources/publicApiBilling/methods/getBillingSchedules.nock.fixtures');
const getSubscriptionProductPricesFixtures = require('auto-core/server/resources/publicApiBilling/methods/getSubscriptionProductPrices.nock.fixtures');
const initPaymentMock = require('auto-core/server/resources/publicApiBilling/methods/initPayment.mock').default;

let context;
let req;
let res;
let params;

const defaultParams = {
    category: 'cars',
    from: 'offers-history-block_button',
    offerId: '1085952280-709fbce9',
    product: encodeURIComponent(JSON.stringify([ { name: 'turbo_package', count: 1 } ])),
    purchaseCount: '1',
    section: 'used',
    returnUrl: 'https%3A%2F%2Fauto.ru',
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
    params = _.cloneDeep(defaultParams);
});

it('для сервиса отчётов запросить статус текущих подписок пользователя', () => {
    setUpResponses();

    params.product = encodeURIComponent(JSON.stringify([ { name: 'offers-history-reports', count: 1 } ]));

    return de.run(billing, { context, params })
        .then((result) => {
            expect(result.reportsSubscription.counter).toBe('17');
        });
});

it('для сервиса отчётов запросить действующие пакеты', () => {
    setUpResponses();

    params.product = encodeURIComponent(JSON.stringify([ { name: 'offers-history-reports', count: 1 } ]));

    return de.run(billing, { context, params })
        .then((result) => {
            expect(result.reportsBundles).toHaveLength(2);
        });
});

it('для сервиса отчётов подложит в первый пакет номер инициализированной транзакции', () => {
    setUpResponses();

    params.product = encodeURIComponent(JSON.stringify([ { name: 'offers-history-reports', count: 1 } ]));

    return de.run(billing, { context, params })
        .then((result) => {
            expect(result.reportsBundles[0].ticketId).toBe('TEST_TICKET_ID');
        });
});

describe('не будет запрашивать статус текущих подписок пользователя и список действующих пакетов', () => {
    it('если сервис не отчёты по вину', () => {
        const newContext = {
            ...context,
            req: {
                ...context.req,
                isMobileDomain: false,
            },
        };
        setUpResponses();

        return de.run(billing, { context: newContext, params })
            .then((result) => {
                expect(result.reportsBundles.error.id).toBe('BLOCK_GUARDED');
                expect(result.reportsSubscription.error.id).toBe('BLOCK_GUARDED');
            });
    });
});

it('для "поднятия в поиске" запросить текущие расписания пользователя', () => {
    setUpResponses();

    params.product = encodeURIComponent(JSON.stringify([ { name: 'all_sale_fresh', count: 1 } ]));
    params.offer_id = '1085952280-709fbce9';

    return de.run(billing, { context, params })
        .then((result) => {
            expect(result.boostSchedules.schedule_type).toBe('ONCE_AT_TIME');
        });
});

it('не будет запрашивать текущие расписания пользователя если сервис не "поднятие в поиске"', () => {
    setUpResponses();

    return de.run(billing, { context, params })
        .then((result) => {
            expect(result.boostSchedules.error.id).toBe('BLOCK_GUARDED');
        });
});

function setUpResponses() {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());
    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.user_auth());
    publicApi
        .post('/1.0/billing/autoru/payment/init')
        .reply(200, initPaymentMock.value());
    publicApi
        .get('/1.0/billing/subscriptions/offers-history-reports')
        .query({
            domain: 'autoru',
        })
        .reply(200, getSubscriptionFixtures.offer_history_reports());
    publicApi
        .get('/1.0/billing/schedules')
        .query({
            offer_id: '1085952280-709fbce9',
            product: 'boost',
        })
        .reply(200, getBillingSchedulesFixtures.auto_boost());
    publicApi
        .get('/1.0/billing/subscriptions/offers-history-reports/prices')
        .query({
            domain: 'autoru',
            offerId: '1085952280-709fbce9',
        })
        .reply(200, getSubscriptionProductPricesFixtures.offer_history_reports());
}
