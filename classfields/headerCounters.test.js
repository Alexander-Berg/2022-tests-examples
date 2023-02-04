/**
 * @jest-environment node
 */
jest.mock('auto-core/lib/core/isMobileApp');

const headerCounters = require('./headerCounters');

const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const isMobileApp = require('auto-core/lib/core/isMobileApp');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

let context;
let req;
let res;
let params;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

describe('не будет запрашивать блоки', () => {
    it('оба блока если это мобилка', async() => {
        isMobileApp.mockReturnValue(true);

        await expect(
            de.run(headerCounters, { context, params }),
        ).rejects.toMatchObject({
            error: { id: 'BLOCK_GUARDED' },
        });
    });

    it('блок favorites если я анон', () => {
        isMobileApp.mockReturnValue(false);

        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/compare/cars')
            .reply(200, { catalog_card_ids: [ 'foo_bar' ] });

        return de.run(headerCounters, { context, params })
            .then((result) => {
                expect(result.favorites.error.id).toBe('BLOCK_GUARDED');
                expect(result.compare.catalog_card_ids).toEqual([ 'foo_bar' ]);
            });
    });
});

it('запросит блоки если я не анон в десктопе', () => {
    isMobileApp.mockReturnValue(false);

    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .get('/1.0/user/favorites/cars/count')
        .reply(200, { count: 42 });

    publicApi
        .get('/1.0/user/favorites/moto/count')
        .reply(200, { count: 43 });

    publicApi
        .get('/1.0/user/favorites/trucks/count')
        .reply(200, { count: 44 });

    publicApi
        .get('/1.0/user/compare/cars')
        .reply(200, { catalog_card_ids: [ 'foo_bar' ] });

    return de.run(headerCounters, { context, params })
        .then((result) => {
            expect(result.favorites.count.all).toBe(129);
            expect(result.compare.catalog_card_ids).toEqual([ 'foo_bar' ]);
        });
});
