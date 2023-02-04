const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const breadcrumbsForDealer = require('./breadcrumbsForDealer');

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

const MARK_LEVEL_MULTIPLE_MARKS = {
    entities: [
        {
            id: 'AUDI',
            mark: { cyrillic_name: 'Ауди' },
            numeric_id: 3139,
            offers_count: 3,
        },
        {
            id: 'BMW',
            mark: { cyrillic_name: 'БМВ' },
            numeric_id: 3141,
            offers_count: 3,
        },
        {
            id: 'LADA',
            mark: { cyrillic_name: 'Лада' },
            numeric_id: 3162,
            offers_count: 0,
        },
    ],
    meta_level: 'MARK_LEVEL',
};

const MARK_LEVEL_ONE_MARK = {
    entities: [
        {
            id: 'AUDI',
            mark: { cyrillic_name: 'Ауди' },
            numeric_id: 3139,
            offers_count: 3,
        },
        {
            id: 'BMW',
            mark: { cyrillic_name: 'БМВ' },
            numeric_id: 3141,
            offers_count: 0,
        },
    ],
    meta_level: 'MARK_LEVEL',
};

const MARK_LEVEL_NO_MARKS = {
    entities: [
        {
            id: 'AUDI',
            mark: { cyrillic_name: 'Ауди' },
            numeric_id: 3139,
            offers_count: 0,
        },
        {
            id: 'BMW',
            mark: { cyrillic_name: 'БМВ' },
            numeric_id: 3141,
            offers_count: 0,
        },
    ],
    meta_level: 'MARK_LEVEL',
};

const MODEL_LEVEL = {
    entities: [
        {
            id: 'A4',
            model: { cyrillic_name: 'A4' },
            offers_count: 3,
        },
    ],
    meta_level: 'MODEL_LEVEL',
};

it('должен вернуть крошки для дилера с несколькими марками', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .reply(200, {
            breadcrumbs: [
                MARK_LEVEL_MULTIPLE_MARKS,
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
        .reply(400);

    return de.run(breadcrumbsForDealer, {
        context,
    })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть крошки для дилера если указан каталог фильтр но нет офферов ни по одной марке', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query((query) => query.bc_lookup)
        .reply(200, {
            breadcrumbs: [
                MARK_LEVEL_NO_MARKS,
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
        .times(2)
        .reply(400);

    return de.run(breadcrumbsForDealer, {
        context,
        params: { catalog_filter: [ { mark: 'AUDI' } ] },
    })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть крошки для дилера с одной маркой', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query((query) => !query.bc_lookup)
        .reply(200, {
            breadcrumbs: [
                MARK_LEVEL_ONE_MARK,
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query((query) => query.bc_lookup === 'AUDI')
        .reply(200, {
            breadcrumbs: [
                MARK_LEVEL_ONE_MARK,
                MODEL_LEVEL,
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
        .times(2)
        .reply(400);

    return de.run(breadcrumbsForDealer, {
        context,
    })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть крошки для дилера с одной маркой, если указан catalog_filter', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs')
        .query((query) => query.bc_lookup === 'AUDI')
        .reply(200, {
            breadcrumbs: [
                MARK_LEVEL_ONE_MARK,
                MODEL_LEVEL,
            ],
            status: 'SUCCESS',
        });

    publicApi
        .get('/1.0/search/cars/mark-model-filters?category=cars&context=listing')
        .reply(400);

    return de.run(breadcrumbsForDealer, {
        context,
        params: { catalog_filter: [ { mark: 'AUDI' } ] },
    })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});
