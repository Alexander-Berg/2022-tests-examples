const { readFileSync } = require('fs');
const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const getUserOffers = require('./getUserOffers');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

import { loadAllMessages } from 'auto-core/proto/schema-registry';

let context;
let req;
let res;
beforeEach(async() => {
    await loadAllMessages();
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

// eslint-disable-next-line jest/no-disabled-tests
describe.skip('должен обработать protobuf и прогнать данные через preparer', () => {
    it('ответ без объявлений', async() => {
        const proto = readFileSync(require.resolve('auto-core/proto/__messages__/auto.api.OfferListingResponse.empty.bin'));
        publicApi
            .get('/1.0/user/offers/cars')
            .reply(200, proto, {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.OfferListingResponse',
            });

        await expect(
            de.run(getUserOffers, {
                context,
                params: { category: 'cars' },
            }),
        ).resolves.toMatchSnapshot();
    });

    it('ответ с объявлениями', async() => {
        const proto = readFileSync(require.resolve('auto-core/proto/__messages__/auto.api.OfferListingResponse.one_offer.bin'));
        publicApi
            .get('/1.0/user/offers/cars')
            .reply(200, proto, {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.OfferListingResponse',
            });

        await expect(
            de.run(getUserOffers, {
                context,
                params: { category: 'cars' },
            }),
        ).resolves.toMatchSnapshot();
    });
});
