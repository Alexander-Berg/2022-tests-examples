const { readFileSync } = require('fs');
const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const method = require('./getOfferCard');

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

describe('preparer', () => {
    // eslint-disable-next-line jest/no-disabled-tests
    it.skip('должен обработать protobuf и прогнать данные через preparer (is_owner=false)', async() => {
        const proto = readFileSync(require.resolve('auto-core/proto/__messages__/auto.api.OfferResponse.bin'));
        publicApi
            .get('/1.0/offer/cars/123-abc')
            .reply(200, proto, {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.OfferResponse',
            });

        await expect(
            de.run(method, {
                context,
                params: { sale_id: '123', sale_hash: 'abc' },
            }),
        ).resolves.toMatchSnapshot();
    });

    // eslint-disable-next-line jest/no-disabled-tests
    it.skip('должен обработать protobuf и прогнать данные через preparer (is_owner=true)', async() => {
        const proto = readFileSync(require.resolve('auto-core/proto/__messages__/auto.api.OfferResponse.owner.bin'));
        publicApi
            .get('/1.0/offer/cars/123-abc')
            .reply(200, proto, {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.OfferResponse',
            });

        await expect(
            de.run(method, {
                context,
                params: { sale_id: '123', sale_hash: 'abc' },
            }),
        ).resolves.toMatchSnapshot();
    });
});
