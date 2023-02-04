/**
 * @jest-environment node
 */

jest.mock('auto-core/lib/geobase-binding', () => {
    return {
        getRegionById: (rid) => {
            switch (rid) {
                case 161426: return { type: 15, capital_id: 117920 };
                case 98604: return { type: 10, capital_id: 10748 };
                case 1: return { type: 5, capital_id: 213 };
                default: return {};
            }
        },

        getParentsIds: (rid) => {
            switch (rid) {
                case 117920: return [ 117920, 161426, 98604, 1, 3, 225, 10001, 10000 ];
                default: return [];
            }
        },
    };
});

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const linkBuilderMiddleware = require('./linkBuilderMiddleware');
const linkBuilder = jest.fn();

const DATA = {
    data: [
        {
            params: {
                type: 'catalog',
                category: 'cars',
                mark: 'audi',
                model: 'a3',
                super_gen: 20785010,
                configuration_id: 20785541,
            },
        },
        {
            params: {
                type: 'listing',
                category: 'cars',
                section: 'all',
                mark: 'audi',
                model: 'a3',
                super_gen: 20785010,
                configuration_id: 20785541,
            },
        },
        {
            params: {
                type: 'listing',
                isMobile: true,
                category: 'cars',
                section: 'all',
                mark: 'audi',
                model: 'a3',
                super_gen: 20785010,
                configuration_id: 20785541,
            },
        },
        {
            params: {
                type: 'index',
            },
        },
    ],
};

const RESULT = [
    [
        'catalog',
        {
            category: 'cars',
            configuration_id: 20785541,
            mark: 'audi',
            model: 'a3',
            super_gen: 20785010,
        },
        {
            baseDomain: 'auto.ru',
            geo:
                {
                    geoAlias: '',
                    geoOverride: false,
                    geoSeoOverride: false,
                    gids: [],
                },
        },
        undefined,
    ],
    [
        'listing',
        {
            category: 'cars',
            section: 'all',
            mark: 'audi',
            model: 'a3',
            super_gen: 20785010,
            configuration_id: 20785541,
        },
        {
            baseDomain: 'auto.ru',
            geo:
                {
                    geoAlias: '',
                    geoOverride: false,
                    geoSeoOverride: false,
                    gids: [],
                },
        },
        undefined,
    ],
    [
        'listing',
        {
            category: 'cars',
            configuration_id: 20785541,
            isMobile: true,
            mark: 'audi',
            model: 'a3',
            section: 'all',
            super_gen: 20785010,
        },
        {
            baseDomain: 'auto.ru',
            geo:
                {
                    geoAlias: '',
                    geoOverride: false,
                    geoSeoOverride: false,
                    gids: [],
                },
        },
        undefined,
    ],
    [
        'index',
        { },
        {
            baseDomain: 'auto.ru',
            geo:
                {
                    geoAlias: '',
                    geoOverride: false,
                    geoSeoOverride: false,
                    gids: [],
                },
        },
        undefined,
    ],
];

let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    linkBuilder.mockReset();
});

it('передает правильные параметры в билдер', () => {
    return new Promise((done) => {
        const result = [];

        req.body = DATA;

        linkBuilder.mockImplementation((...args) => {
            result.push(args);
            return args;
        });

        res.send.mockImplementation(() => {
            expect(result).toEqual(RESULT);

            done();
        });

        linkBuilderMiddleware(linkBuilder)(req, res);
    });
});

it('возвращает данные в нужном формате', () => {
    return new Promise((done) => {
        req.body = DATA;

        const COUNT = 2500;

        req.body = {
            data: [],
        };

        const results = [];

        for (let i = 0; i < COUNT; i++) {
            req.body.data.push(DATA.data[0]);

            results.push({
                url: `url-${ i + 1 }`,
            });
        }

        let counter = 1;

        linkBuilder.mockImplementation(() => {
            return `url-${ counter++ }`;
        });

        res.send.mockImplementation((data) => {
            expect(data).toEqual({
                urls: results,
            });

            done();
        });

        linkBuilderMiddleware(linkBuilder)(req, res);
    });
});
