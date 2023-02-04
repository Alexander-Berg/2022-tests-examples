const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const block = require('./getPreset');

let context;
let req;
let res;
let params;
beforeEach(() => {
    req = createHttpReq();
    req.gradius.get.mockReturnValue(200);

    params = {
        acceleration_to: '3',
        section: 'all',
    };

    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть объявы из запроса без радиуса, если их хватает', () => {
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.rid === '213' && !query.geo_radius && query.acceleration_to === '3' && query.state_group === 'ALL';
        })
        .reply(200, {
            offers: Array(40)
                .fill(null)
                .map((item, index) => ({ additional_info: {}, id: `${ index }inRegion-rid213` })),
            pagination: {
                page: 1,
                page_size: 40,
                total_offers_count: 50,
            },
        });

    req.geoIds = [ 213 ];

    return de.run(block, { context, params }).then(result => {
        expect(result).toHaveLength(40);
        expect(result[0]).toMatchObject({ id: '0inRegion', hash: 'rid213' });
    });
});

it('должен вернуть объявы из запроса без радиуса и из запроса с радиусом, если в первом запросе не хватило объявлений', () => {
    // не хватает выдачи в регионе(2) без радиуса
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.rid === '2' && !query.geo_radius && query.acceleration_to === '3' && query.state_group === 'ALL';
        })
        .reply(200, {
            offers: Array(20)
                .fill(null)
                .map((item, index) => ({ additional_info: {}, id: `${ index }inRegion-rid2` })),
            pagination: {
                total_offers_count: 20,
            },
        });

    // хватает выдачи в регионе(2) с радиусом
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.rid === '2' && query.geo_radius && query.acceleration_to === '3' && query.state_group === 'ALL';
        })
        .reply(200, {
            offers: Array(20)
                .fill(null)
                .map((item, index) => ({ additional_info: {}, id: `${ index }inRegionWithRadius-rid2` })),
            pagination: {
                total_offers_count: 50,
            },
        });

    req.geoIds = [ 2 ];

    return de.run(block, { context, params }).then(result => {
        expect(result).toHaveLength(40);
        expect(result[0]).toMatchObject({ id: '0inRegion', hash: 'rid2' });
        expect(result.slice(-1)[0]).toMatchObject({ id: '19inRegionWithRadius', hash: 'rid2' });
    });
});

it('должен вернуть объявы из россии, если в первом и втором запросах не хватило объявлений', () => {
    // не хватает выдачи в регионе(10900) без радиуса
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.rid === '10900' && !query.geo_radius && query.acceleration_to === '3' && query.state_group === 'ALL';
        })
        .reply(200, {
            offers: Array(10)
                .fill(null)
                .map((item, index) => ({ additional_info: {}, id: `${ index }inRegion-rid10900` })),
            pagination: {
                total_offers_count: 10,
            },
        });

    // не хватает выдачи в регионе(10900) с радиусом
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.rid === '10900' && query.geo_radius && query.acceleration_to === '3' && query.state_group === 'ALL';
        })
        .reply(200, {
            offers: Array(10)
                .fill(null)
                .map((item, index) => ({ additional_info: {}, id: `${ index }inRegionWithRadius-rid10900` })),
            pagination: {
                total_offers_count: 10,
            },
        });

    // выдача по России
    publicApi
        .get('/1.0/search/cars')
        .query(query => {
            return query.rid === '225' && query.acceleration_to === '3' && query.state_group === 'ALL';
        })
        .reply(200, {
            offers: Array(10)
                .fill(null)
                .map((item, index) => ({ additional_info: {}, id: `${ index }inRussia-rid225` })),
            pagination: {
                total_offers_count: 10,
            },
        });

    req.geoIds = [ 10900 ];

    return de.run(block, { context, params }).then(result => {
        expect(result).toHaveLength(30);
        expect(result[0]).toMatchObject({ id: '0inRegion', hash: 'rid10900' });
        expect(result[10]).toMatchObject({ id: '0inRegionWithRadius', hash: 'rid10900' });
        expect(result[20]).toMatchObject({ id: '0inRussia', hash: 'rid225' });
    });
});
