const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const ERROR_ID = require('auto-core/server/descript/error-id').default;

const block = require('./checkSeoUrl');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

const fordFocusResponse = {
    body_type: [
        'HATCHBACK_3_DOORS',
        'WAGON_5_DOORS',
        'CABRIO',
        'SEDAN',
        'COUPE',
        'HATCHBACK_5_DOORS',
    ],
    engine_type: [
        'DIESEL',
        'GASOLINE',
        'ELECTRO',
    ],
    gear_type: [
        'FORWARD_CONTROL',
    ],
    transmission: [
        'VARIATOR',
        'AUTOMATIC',
        'MECHANICAL',
        'ROBOT',
    ],
};

const skodaResponse = {
    body_type: [
        'LIFTBACK',
        'COMPACTVAN',
        'HATCHBACK_3_DOORS',
        'WAGON_5_DOORS',
        'PICKUP_ONE',
        'WAGON_3_DOORS',
        'SEDAN',
        'ALLROAD_5_DOORS',
        'COUPE',
        'HATCHBACK_5_DOORS',
    ],
    engine_type: [
        'ATMO',
        'DIESEL',
        'TURBO',
        'GASOLINE',
        'HYBRID',
        'ELECTRO',
        'LPG',
    ],
    gear_type: [
        'ALL_WHEEL_DRIVE',
        'REAR_DRIVE',
        'FORWARD_CONTROL',
    ],
    transmission: [
        'AUTOMATIC',
        'MECHANICAL',
        'ROBOT',
    ],
};

const teslaResponse = {
    body_type: [
        'LIFTBACK',
        'SEDAN',
        'PICKUP_TWO',
        'TARGA',
        'ALLROAD_5_DOORS',
        'ROADSTER',
    ],
    engine_type: [
        'ELECTRO',
    ],
    gear_type: [
        'ALL_WHEEL_DRIVE',
        'REAR_DRIVE',
    ],
    transmission: [
        'AUTOMATIC',
    ],
};

it('200 при верных параметрах в ЧПУ', () => {
    req.url = '/moskva/cars/ford/focus/all/';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'FORD',
            model: 'FOCUS',
        })
        .reply(200, fordFocusResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
    } })
        .then((result) => {
            expect(result).toEqual(fordFocusResponse);
        });

});

it('200 при расширении кузова в ЧПУ (например компактвен до минивена)', () => {
    req.url = '/moskva/cars/ford/focus/all/';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'SKODA',
        })
        .reply(200, skodaResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'SKODA' } ],
        body_type_group: [
            'MINIVAN',
        ],
    } })
        .then((result) => {
            expect(result).toEqual(skodaResponse);
        });
});

it('200 для кузова LIFTBACK', () => {
    req.url = '/moskva/cars/skoda/all/body-liftback/';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'SKODA',
        })
        .reply(200, skodaResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'SKODA' } ],
        body_type_group: [
            'LIFTBACK',
        ],
    } }).then(result => {
        expect(result).toEqual(skodaResponse);
    });
});

it('200 для трансмиссии AUTOMATIC', () => {
    req.url = '/moskva/cars/tesla/all/transmission-automatic/';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'TESLA',
        })
        .reply(200, teslaResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'TESLA' } ],
        transmission: [ 'ROBOT', 'AUTOMATIC', 'VARIATOR', 'AUTO' ],
    } }).then(result => {
        expect(result).toEqual(teslaResponse);
    });
});

it('404 при неверных параметрах кузова в ЧПУ', () => {
    req.url = '/moskva/cars/ford/focus/all/body-allroad/';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'FORD',
            model: 'FOCUS',
        })
        .reply(200, fordFocusResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
        body_type_group: [
            'ALLROAD',
            'ALLROAD_3_DOORS',
            'ALLROAD_5_DOORS',
        ] } })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: ERROR_ID.SEF_PARAMS_NOT_VALID,
                        status_code: 404,
                    },
                });
            });
});

it('404 при неверных параметрах привода в ЧПУ', () => {
    req.url = '/moskva/cars/ford/focus/all/drive-4x4_wheel/';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'FORD',
            model: 'FOCUS',
        })
        .reply(200, fordFocusResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
        gear_type: [ 'ALL_WHEEL_DRIVE' ],
    } })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: ERROR_ID.SEF_PARAMS_NOT_VALID,
                        status_code: 404,
                    },
                });
            });
});

it('404 при неверных параметрах двигателя в ЧПУ', () => {
    req.url = '/moskva/cars/tesla/all/engine-benzin/ ';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'TESLA',
        })
        .reply(200, teslaResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'TESLA' } ],
        engine_group: [ 'GASOLINE' ],
    } })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: ERROR_ID.SEF_PARAMS_NOT_VALID,
                        status_code: 404,
                    },
                });
            });
});

it('404 при неверных параметрах трансмиссии в ЧПУ', () => {
    req.url = '/moskva/cars/tesla/all/transmission-mechanical/ ';

    publicApi
        .get('/1.0/reference/catalog/cars/available-variants')
        .query({
            mark: 'TESLA',
        })
        .reply(200, teslaResponse);

    return de.run(block, { context, params: {
        category: 'cars',
        section: 'all',
        catalog_filter: [ { mark: 'TESLA' } ],
        transmission: [ 'MECHANICAL' ],
    } })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: ERROR_ID.SEF_PARAMS_NOT_VALID,
                        status_code: 404,
                    },
                });
            });
});
