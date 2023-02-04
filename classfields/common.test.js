/**
 * @jest-environment node
 */
const common = require('./common');

const de = require('descript');
const MockDate = require('mockdate');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const createDeps = require('autoru-frontend/mocks/createDeps');

const session = require('auto-core/server/resources/publicApiAuth/methods/session');

const apiCabinet = require('auto-core/server/resources/apiCabinet/getResource.nock.fixtures');
const apiCabinetNext = require('auto-core/server/resources/apiCabinetNext/apiCabinetNext.nock.fixtures');
const salesman = require('auto-core/server/resources/salesman/getResource.nock.fixtures');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const promocoder = require('auto-core/server/resources/promocoder/promocoder.nock.fixtures');

const customerRoleFixtures = require('auto-core/server/resources/apiCabinet/methods/getCustomerRole.nock.fixtures');
const getDealerAccountFixtures = require('auto-core/server/resources/publicApiDealers/methods/getDealerAccount.nock.fixtures');
const getDealerCampaignsFixtures = require('auto-core/server/resources/publicApiDealers/methods/getDealerCampaigns.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const getPromoFeatures = require('auto-core/server/resources/promocoder/methods/getPromoFeatures.nock.fixtures');

const BASE_API_CABINET_QUERY = {
    access_key: 'autoru_frontend.api_cabinet.access_key',
    from_app_id: 'af-jest',
    autoruuid: 'TEST_DEVICE_ID',
    session_id: 'ID',
};

const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    MockDate.set('2018-01-05');
});

afterEach(() => {
    MockDate.reset();
});

const commonBlockWithDeps = de.func({
    block: (args) => {
        const { blocks, ids } = createDeps({
            agency: null,
            billingCampaignCall: null,
            billingCampaignCallCarsUsed: null,
            client: null,
            customerRole: CUSTOMER_ROLES.client,
            dealerAccount: null,
            userOffers: null,
        }, args);

        return de.pipe({
            block: [
                session,
                blocks.customerRole,
                common(ids),
            ],
        });
    },
});

it('должен вызывать правильный набор блоков для роли клиента', () => {
    setCommonClientMocks();

    apiCabinetNext
        .get('/api/1.x/client/TEST_CLIENT_ID/invoice/persons')
        .reply(200, {});

    return de.run(commonBlockWithDeps, { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вызывать ручку регистраций, если это новый клиент', () => {
    const response = [
        { allowed: true, confirmed: true, step: 'poi' },
    ];

    apiCabinet
        .get('/desktop/v1.0.0/clients/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, { result: { first_moderated: '1', status: 'new' } });

    apiCabinetNext
        .get('/api/1.x/client/TEST_CLIENT_ID/registration/steps')
        .reply(200, response);

    setCommonClientMocks();

    return de.run(commonBlockWithDeps, { context })
        .then((result) => {
            expect(result.registrationSteps).toEqual(response);
        });
});

it('не должен вызывать ручку списка плательщиков, если это агентский клиент', () => {
    apiCabinet
        .get('/desktop/v1.0.0/clients/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, { result: { agent_id: '1' } });

    const invoicePersons = apiCabinetNext
        .get('/api/1.x/client/TEST_CLIENT_ID/invoice/persons')
        .reply(200, {});

    setCommonClientMocks();

    return de.run(commonBlockWithDeps, { context })
        .then(() => {
            expect(invoicePersons.isDone()).toBe(false);
        });
});

function setCommonClientMocks() {
    context.req.headers['x-forwarded-host'] = 'cabinet.auto.ru';

    apiCabinet
        .get('/common/v1.0.0/customer/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, customerRoleFixtures.client());

    apiCabinet
        .get('/desktop/v1.0.0/clients/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, { result: { first_moderated: '1' } });

    apiCabinet
        .get('/desktop/v1.0.0/sidebar/get')
        .query(true)
        .reply(200, { result: 'clientSidebar' });

    apiCabinetNext
        .get('/api/1.x/access/client/TEST_CLIENT_ID')
        .times(2)
        .reply(200, {});

    apiCabinetNext
        .get('/api/1.x/client/TEST_CLIENT_ID/manager')
        .reply(200, {});

    publicApi
        .get('/1.0/dealer/account')
        .reply(200, getDealerAccountFixtures.with_balance());

    publicApi
        .get('/1.0/dealer/tariff')
        .query(true)
        .reply(200, { tariffs: [] });

    publicApi
        .get('/1.0/dealer/loyalty/report')
        .reply(200, {});

    publicApi
        .get('/1.0/session/')
        .times(3)
        .reply(200, sessionFixtures.client_auth());

    publicApi
        .get('/1.0/dealer/campaigns')
        .query({})
        .reply(200, getDealerCampaignsFixtures.only_cars_new());

    publicApi
        .get('/1.0/user/offers/all/mark-models')
        .query({})
        .reply(200, {});

    promocoder
        .get('/api/1.x/service/autoru/feature/user/autoru_client_TEST_CLIENT_ID')
        .reply(200, getPromoFeatures.common());

    salesman
        .get('/api/1.x/service/autoru/billing/campaign/call/client/TEST_CLIENT_ID')
        .reply(200, {});

    salesman
        .get('/api/1.x/service/autoru/billing/campaign/call:cars:used/client/TEST_CLIENT_ID')
        .reply(200, {});

    publicApi
        .get('/1.0/user/offers/all/count')
        .query({ category: 'all', status: 'active' })
        .reply(200, { count: 20 });

    publicApi
        .get('/1.0/user/offers/all/count')
        .query({ category: 'all', multiposting_status: 'active' })
        .reply(200, { count: 20 });

    publicApi
        .get('/1.0/cabinet/settings')
        .reply(200, { settings: { sex: 'male' } });
}
