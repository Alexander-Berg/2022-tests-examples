const getClientsListWithOffersCount = require('./getClientsListWithOffersCount').default;

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const createDeps = require('autoru-frontend/mocks/createDeps');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const clientListFixtures = require('auto-core/server/resources/publicApiCabinet/methods/getClientsList.nock.fixtures');
const getUserOffersCategoryCount = require('auto-core/server/resources/publicApiUserOffers/methods/getUserOffersCategoryCount.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

const getClientsListWithOffersCountWithDeps = (customerRole) => de.func({
    block: (args) => {
        const { blocks, ids } = createDeps({ customerRole }, args);

        return de.pipe({
            block: [
                blocks.customerRole,
                getClientsListWithOffersCount(ids),
            ],
        });
    },
});

it('должен обогащать список клиентов каунтерами', () => {
    publicApi
        .post('/1.0/cabinet/company/dealers')
        .reply(200, clientListFixtures.only_ids());

    publicApi
        .get('/1.0/user/offers/all/count')
        .query(true)
        .times(3)
        .reply(200, getUserOffersCategoryCount.only_count());

    return de.run(getClientsListWithOffersCountWithDeps('company'), { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен обогащать список клиентов каунтерами, если customerRole == agency', () => {
    publicApi
        .post('/1.0/cabinet/agency/dealers')
        .reply(200, clientListFixtures.for_agency());

    publicApi
        .get('/1.0/user/offers/all/count')
        .query(true)
        .times(1)
        .reply(200, getUserOffersCategoryCount.only_count_empty());

    return de.run(getClientsListWithOffersCountWithDeps('agency'), { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});
