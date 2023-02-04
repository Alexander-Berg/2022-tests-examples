/**
 * @jest-environment node
 */
jest.mock('./pages/common', () => require('./mocks/common.mock'));
const baseController = require('./baseController');

const de = require('descript');

const session = require('auto-core/server/resources/publicApiAuth/methods/session');
const apiCabinet = require('auto-core/server/resources/apiCabinet/getResource.nock.fixtures');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const customerRoleFixtures = require('auto-core/server/resources/apiCabinet/methods/getCustomerRole.nock.fixtures');
const pageMock = require('./mocks/page.mock');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const BASE_API_CABINET_QUERY = {
    access_key: 'autoru_frontend.api_cabinet.access_key',
    from_app_id: 'af-jest',
    autoruuid: 'TEST_DEVICE_ID',
    session_id: 'ID',
};

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть правильный набор блоков', () => {
    const baseControllerWithDeps = de.func({
        block: () => {
            return de.pipe({
                block: [
                    session,
                    baseController('start', pageMock()),
                ],
            });
        },
    });

    setBaseControllerClientMocks();

    return de.run(baseControllerWithDeps, { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

function setBaseControllerClientMocks() {
    context.req.headers['x-forwarded-host'] = 'cabinet.auto.ru';

    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.client_auth());

    apiCabinet
        .get('/common/v1.0.0/customer/get')
        .query(BASE_API_CABINET_QUERY)
        .reply(200, customerRoleFixtures.client());
}
