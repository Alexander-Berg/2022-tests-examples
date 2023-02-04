/**
 * @jest-environment node
 */
const getNavigation = require('./getNavigation');

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const apiCabinet = require('auto-core/server/resources/apiCabinet/getResource.nock.fixtures');

const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');

const BASE_API_CABINET_QUERY = {
    access_key: 'autoru_frontend.api_cabinet.access_key',
    from_app_id: 'af-jest',
    autoruuid: '',
    session_id: '',
};

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вызывать только клиентский садйбар, если запрос сделан от роли клиента', () => {
    setSidebarMocks();

    const params = {
        customerRole: CUSTOMER_ROLES.client,
        client_id: 20101,
    };

    return de.run(getNavigation, { context, params })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вызывать только агентскую навигацию, если запрос сделан от роли агенства', () => {
    setSidebarMocks();

    const params = {
        customerRole: CUSTOMER_ROLES.agency,
    };

    return de.run(getNavigation, { context, params })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вызывать только агентскую навигацию, если запрос сделан от роли группы компаний', () => {
    setSidebarMocks();

    const params = {
        customerRole: CUSTOMER_ROLES.company,
    };

    return de.run(getNavigation, { context, params })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

describe('для запросов с client_id', () => {
    const roles = [
        CUSTOMER_ROLES.agency,
        CUSTOMER_ROLES.company,
        CUSTOMER_ROLES.manager,
    ];

    roles.forEach((role) => {
        it(`должен вызывать сайдбар агентского клиента, если запрос сделан от роли "${ role }"`, () => {
            setSidebarMocks();

            const params = {
                customerRole: role,
                client_id: 20101,
            };

            return de.run(getNavigation, { context, params })
                .then((result) => {
                    expect(result).toMatchSnapshot();
                });
        });
    });
});

function setSidebarMocks() {
    apiCabinet
        .get('/desktop/v1.0.0/sidebar/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, {});

    apiCabinet
        .get('/agency/v1.0.0/sidebar/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, {});

    apiCabinet
        .get(/\/(crm|agency)\/v1.0.0\/client\/sidebar\/get/)
        .query({
            ...BASE_API_CABINET_QUERY,
            client_id: '20101',
        })
        .reply(200, {});
}
