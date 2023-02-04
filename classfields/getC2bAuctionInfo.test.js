/**
 * @jest-environment node
 */
const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./getC2bAuctionInfo');

const OFFER_ID = '123-321';

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    publicApi
        .get(`/1.0/user/draft/cars/${ OFFER_ID }/carp_auction_application_info`)
        .query(() => true)
        .reply(200, {
            isCarPrice: true,
        });

    publicApi
        .get(`/1.0/user/draft/cars/${ OFFER_ID }/c2b_application_info`)
        .query(() => true)
        .reply(200, {
            isCarPrice: false,
        });
});

it('должен вернуть результат из ручки c2b_application_info', () => {
    return de.run(block, {
        context,
        params: {
            offer_id: OFFER_ID,
            parent_category: 'cars',
        },
    }).then((result) => {
        expect(result).toEqual({ isCarPrice: false });
    });
});

it('должен вернуть результат из ручки carp_auction_application_info если есть эксп AUTORUFRONT-20805_carprice_banner', () => {
    context.req.experimentsData.has = exp => exp === 'AUTORUFRONT-20805_carprice_banner';
    return de.run(block, {
        context,
        params: {
            offer_id: OFFER_ID,
            parent_category: 'cars',
        },
    }).then((result) => {
        expect(result).toEqual({ isCarPrice: true });
    });
});
