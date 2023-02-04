const { readFileSync } = require('fs');
const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const method = require('./search');

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
    it.skip('должен обработать protobuf и прогнать данные через preparer', async() => {
        const proto = readFileSync(require.resolve('auto-core/proto/__messages__/auto.api.OfferListingResponse.bin'));
        publicApi
            // eslint-disable-next-line max-len
            .get('/1.0/search/cars?catalog_filter=mark%3DMERCEDES&category=cars&context=listing&page_size=37&page=1&sort=fresh_relevance_1-desc&state_group=USED')
            .reply(200, proto, {
                'content-type': 'application/protobuf',
                'x-proto-name': 'auto.api.OfferListingResponse',
            });

        await expect(
            de.run(method, {
                context,
                params: { catalog_filter: 'mark=MERCEDES', state_group: 'USED' },
            }),
        ).resolves.toMatchSnapshot();
    });
});
