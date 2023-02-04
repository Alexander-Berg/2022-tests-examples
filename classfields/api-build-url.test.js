const de = require('descript');
const controller = require('./api-build-url');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    req.geoIds = [ 213 ];
    req.geoParents = [];
    req.geoIdsInfo = [];
});

it('должен ответить 400 для неизвестного роута', () => {
    context.req.body = {
        route_name: 'listing',
    };

    return de.run(controller, { context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (error) => {
            expect(error).toMatchObject({
                error: {
                    id: 'UNKNOWN_ROUTE',
                    status_code: 400,
                },
            });
        },
    );
});

describe('генерация урлов', () => {
    const TESTS = [
        {
            body: {
                route_name: 'card',
                additional_info: {
                    sale_id: '123',
                    sale_hash: 'abc',
                },
                search_parameters: {
                    state_group: 'USED',
                    catalog_filter: [
                        { mark: 'AUDI', model: 'A4' },
                    ],
                },
            },
            result: {
                url: 'https://autoru_frontend.base_domain/cars/used/sale/audi/a4/123-abc/',
            },
        },
        {
            body: {
                route_name: 'card-group',
                search_parameters: {
                    cars_params: {
                        engine_group: 'ELECTRO',
                    },
                    state_group: 'NEW',
                    catalog_filter: [
                        { mark: 'AUDI', model: 'A4', generation: '123', configuration: '456', tech_param_id: '789' },
                    ],
                },
            },
            result: {
                // eslint-disable-next-line max-len
                url: 'https://autoru_frontend.base_domain/cars/new/group/audi/a4/123-456/?catalog_filter=tech_param_id%3D789%2Cmark%3DAUDI%2Cmodel%3DA4%2Cgeneration%3D123%2Cconfiguration%3D456&engine_group=ELECTRO',
            },
        },
        {
            body: {
                route_name: 'card-group',
                search_parameters: {
                    cars_params: {
                        complectation_id: [ '098' ],
                        engine_group: 'ELECTRO',
                    },
                    state_group: 'NEW',
                    catalog_filter: [
                        { mark: 'AUDI', model: 'A4', generation: '123', configuration: '456', tech_param_id: '789' },
                    ],
                },
            },
            result: {
                // eslint-disable-next-line max-len
                url: 'https://autoru_frontend.base_domain/cars/new/group/audi/a4/123-456/?catalog_filter=tech_param_id%3D789%2Cmark%3DAUDI%2Cmodel%3DA4%2Cgeneration%3D123%2Cconfiguration%3D456&engine_group=ELECTRO',
            },
        },
    ];

    TESTS.forEach((testCase) => {
        it(`должен сгенерировать ${ testCase.result.url } для ${ JSON.stringify(testCase.body) }`, () => {
            context.req.body = testCase.body;

            return de.run(controller, { context }).then(
                () => {
                    expect(res.send).toHaveBeenCalledWith(testCase.result);
                },
            );
        });
    });
});
