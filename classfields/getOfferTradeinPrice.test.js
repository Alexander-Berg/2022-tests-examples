const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./getOfferTradeinPrice');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('отдает цену трейдина', () => {
    publicApi
        .post('/1.0/stats/predict')
        .reply(200, {
            prices: {
                tradein: {
                    from: 552000,
                    to: 613000,
                    currency: 'RUR',
                },
            },
        });

    const params = { offer: mockOffer };

    return de.run(block, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                currentOfferTradeinPrice: 613000,
            });
        });
});
