const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const mockOffer = require('autoru-frontend/mockData/responses/offer.mock').offer;

const block = require('./widget-banner');

let context;
let req;
let res;
let params;
beforeEach(() => {
    req = createHttpReq();
    req.gradius.get.mockReturnValue(200);

    params = {
        section: 'all',
        geo_id: 10900,
        mark_id: 'AUDI',
        model_id: 'A6',
        generation_id: '20246005',
    };

    res = createHttpRes();
    context = createContext({ req, res });
});
const geo = {
    geoAlias: 'rossiya',
    gids: [ '225' ],
};

it('должен вернуть объявления по марке, модели и поколению', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(() => true)
        .reply(200, {
            offers: Array(12)
                .fill(null)
                .map((item, index) => ({ ...mockOffer, id: `${ index }inRegion-rid10900` })),
            pagination: {
                total_offers_count: 12,
            },
        });

    return de.run(block, { context, params })
        .then(result => {
            expect(result.offers).toHaveLength(12);
            expect(result.showMoreParams).toEqual({
                mark: 'AUDI',
                model: 'A6',
                super_gen: '20246005',
                geo,
            });
        });
});

it('должен вернуть объявления по марке и модели, если по поколению была пустая выдача', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => query.catalog_filter === 'mark=AUDI,model=A6')
        .reply(200, {
            offers: Array(12)
                .fill(null)
                .map((item, index) => ({ ...mockOffer, id: `${ index }inRegion-rid10900` })),
            pagination: {
                total_offers_count: 12,
            },
        });

    return de.run(block, { context, params })
        .then(result => {
            expect(result.offers).toHaveLength(12);
            expect(result.showMoreParams).toEqual({
                mark: 'AUDI',
                model: 'A6',
                geo,
            });
        });
});

it('должен вернуть объявления по марке, если по модели была пустая выдача', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => query.catalog_filter === 'mark=AUDI')
        .reply(200, {
            offers: Array(12)
                .fill(null)
                .map((item, index) => ({ ...mockOffer, id: `${ index }inRegion-rid10900` })),
            pagination: {
                total_offers_count: 12,
            },
        });

    return de.run(block, { context, params })
        .then(result => {
            expect(result.offers).toHaveLength(12);
            expect(result.showMoreParams).toEqual({ mark: 'AUDI', geo });
        });
});

it('должен вернуть дефолтную выдачу, если ничего не нашлось по марке и fallback=true', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => !query.catalog_filter)
        .reply(200, {
            offers: Array(12)
                .fill(null)
                .map((item, index) => ({ ...mockOffer, id: `${ index }inRegion-rid10900` })),
            pagination: {
                total_offers_count: 12,
            },
        });

    return de.run(block, { context, params })
        .then(result => {
            expect(result.offers).toHaveLength(12);
            expect(result.showMoreParams).toEqual({ geo });
        });
});

it('должен вернуть пустую выдачу, если ничего не нашлось по марке и fallback=false', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => !query.catalog_filter)
        .reply(200, {
            offers: Array(12)
                .fill(null)
                .map((item, index) => ({ ...mockOffer, id: `${ index }inRegion-rid10900` })),
            pagination: {
                total_offers_count: 12,
            },
        });

    params.fallback = 'false';

    return de.run(block, { context, params })
        .then(result => {
            expect(result.offers).toHaveLength(0);
        });
});
