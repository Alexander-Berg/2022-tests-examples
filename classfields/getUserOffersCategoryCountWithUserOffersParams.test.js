const de = require('descript');
const block = require('./getUserOffersCategoryCountWithUserOffersParams');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const req = createHttpReq();
const res = createHttpRes();

const context = createContext({ req, res });
it('должен устанавливать заголовок x-dealer-id и возвращать счетчик объявлений', () => {
    publicApi.get('/1.0/user/offers/all/count?category=all&status=active')
        .matchHeader('x-dealer-id', val => val === 16453)
        .reply(200, { status: 'SUCCESS', count: 0 });

    return de.run(block, { context, params: { dealer_id: 16453 } })
        .then((result) => {
            expect(result).toEqual({ count: 0, status: 'SUCCESS' });
        });
});
