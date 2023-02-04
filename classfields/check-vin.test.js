jest.mock('auto-core/server/helpers/checkAccessToResource', () => {
    return () => {};
});

jest.mock('auto-core/lib/util/getBunkerDict', () => {
    return () => {};
});

const checkVin = require('./check-vin');

const de = require('descript');
const mockdate = require('mockdate');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const apiMoisha = require('auto-core/server/resources/apiMoisha/apiMoisha.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    mockdate.set('2020-10-03T00:01:00+03:00');
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вызвать правильный набор блоков', () => {
    apiMoisha
        .post('/api/1.x/service/autoru/price', {
            offer: { allowDefaults: true },
            context: { clientRegionId: null },
            product: 'vin-history',
            interval: { from: '2020-10-03T00:00:00.000+03:00', to: '2020-10-04T23:59:59.999+03:00' },
        })
        .reply(200, { response: 'true' });

    const controller = de.func({
        block: ({ generate_id: generateId }) => {
            const ids = {
                client: generateId(),
            };

            return de.object({
                block: {
                    client: de.object({
                        block: {},
                        options: {
                            id: ids.client,
                        },
                    }),
                    checkVin: checkVin(ids),
                },
            });
        },
    });

    return de.run(controller, { context })
        .then((result) => {
            expect(result.checkVin).toEqual({
                vinHistoryPrice: { response: 'true' },
                bunker: { vin_history: undefined },
            });
        });
});
