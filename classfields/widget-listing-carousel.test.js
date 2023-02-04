/**
 * @jest-environment node
 */
const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./widget-listing-carousel');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отработать guard, если категория не cars', () => {
    publicApi
        .get('/1.0/search/trucks')
        .query(() => true)
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '456',
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            section: 'all',
            category: 'trucks',
        },
    }).then(({ generationCatalog }) => {
        expect(generationCatalog).toMatchObject({
            error: {
                id: 'BLOCK_GUARDED',
            },
        });
    });
});

it('должен отработать guard, если нет марки', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(() => true)
        .reply(200, {
            offers: [
                {
                    additional_info: {},
                    id: '456',
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(block, {
        context,
        params: {
            section: 'all',
            category: 'cars',
        },
    }).then(({ generationCatalog }) => {
        expect(generationCatalog).toMatchObject({
            error: {
                id: 'BLOCK_GUARDED',
            },
        });
    });
});
