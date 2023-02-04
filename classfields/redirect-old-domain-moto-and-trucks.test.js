const { baseDomain: BASE_DOMAIN } = require('auto-core/appConfig');

const middleware = require('./redirect-old-domain-moto-and-trucks');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const yaAuto = require('auto-core/server/resources/yaAuto/getResource.nock.fixtures');

let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    req.headers['x-forwarded-host'] = BASE_DOMAIN;
    req.method = 'GET';

    res = createHttpRes();

    yaAuto
        .get('/motoBreadcrumbs')
        .query((query) => {
            return query.moto_category === 'motorcycle' && !query.mark && !query.model;
        })
        .reply(200, {
            data: [
                [
                    {
                        data: [
                            { id: 'YAMAHA', 'mark-autoru-code': 'yamaha' },
                            { id: 'ZIP_MOTORS', 'mark-autoru-code': 'zip-motors' },
                        ],
                        meta: 'MARK_LEVEL',
                    },
                ],
            ],
            status: 'SUCCESS',
        });

    yaAuto
        .get('/motoBreadcrumbs')
        .query((query) => {
            return query.moto_category === 'motorcycle' && query.mark === 'YAMAHA' && !query.model;
        })
        .reply(200, {
            data: [
                [
                    {
                        data: [
                            { id: 'YAMAHA', 'mark-autoru-code': 'yamaha' },
                            { id: 'ZIP_MOTORS', 'mark-autoru-code': 'zip-motors' },
                        ],
                        meta: 'MARK_LEVEL',
                    },
                    {
                        data: [
                            { id: 'YZ_250', 'model-autoru-code': 'yz-250' },
                        ],
                        meta: 'MODEL_LEVEL',
                    },
                ],
            ],
            status: 'SUCCESS',
        });
});

it('не должен редиректить, если нет поддомена', () => {
    req.headers['x-forwarded-host'] = BASE_DOMAIN;
    req.url = '/';

    return new Promise((done) => {
        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('не должен редиректить, если поддомен неизвестен', () => {
    req.headers['x-forwarded-host'] = 'foo.' + BASE_DOMAIN;
    req.url = '/';

    return new Promise((done) => {
        middleware(req, res, (error) => {
            expect(error).toBeUndefined();
            done();
        });
    });
});

it('должен сделать редирект moto.auto.ru -> auto.ru/motorcycle/all/', () => {
    req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
    req.url = '/';

    return new Promise((done) => {
        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'DOMAIN_MOTO_TRUCKS',
                data: {
                    location: 'https://autoru_frontend.base_domain/motorcycle/all/',
                    status: 301,
                },
            });
            done();
        });
    });
});

describe('статически редиректы', () => {
    describe('trucks ->', function() {
        const TESTS = [
            {
                url: '/',
                location: 'https://autoru_frontend.base_domain/lcv/all/',
            },
            {
                url: '/light_trucks/',
                location: 'https://autoru_frontend.base_domain/lcv/all/',
            },
            {
                url: '/artic/',
                location: 'https://autoru_frontend.base_domain/artic/all/',
            },
            {
                url: '/drags/',
                location: 'https://autoru_frontend.base_domain/trailer/all/',
            },
            {
                url: '/trucks/',
                location: 'https://autoru_frontend.base_domain/truck/all/',
            },
            {
                url: '/bus/',
                location: 'https://autoru_frontend.base_domain/bus/all/',
            },
            {
                url: '/swap_body/',
                location: 'https://autoru_frontend.base_domain/trailer/all/',
            },
            {
                url: '/extsearch/light_trucks/used/',
                location: 'https://autoru_frontend.base_domain/lcv/used/',
            },
            {
                url: '/light_trucks/used/sale/6005264-5728e.html/',
                location: 'https://autoru_frontend.base_domain/lcv/used/sale/6005264-5728e/',
            },
            {
                url: '/mysearch/',
                location: 'https://auto.ru/my_search/',
            },
            {
                url: '/office/?op=subscribe',
                location: 'https://auto.ru/my_search/',
            },
            {
                url: '/office/',
                location: 'https://auto.ru/my/trucks/',
            },
            {
                url: '/bus/used/sale/5742935-050a.html/?category_id=34&section_id=1&nomobile',
                location: 'https://autoru_frontend.base_domain/bus/used/sale/5742935-050a/?nomobile=',
            },
            {
                url: '/extsearch/commercial/used/?qs=region_id%3D87%26country_id%3D1%26category_id%3D29%26section_id%3D1&mode=edit',
                location: 'https://autoru_frontend.base_domain/lcv/used/',
            },
        ];

        beforeEach(() => {
            req.headers['x-forwarded-host'] = `trucks.${ BASE_DOMAIN }`;
        });

        TESTS.forEach(function(testCase, i) {
            it(`test #${ i }: "trucks.auto.ru${ testCase.url }" -> "${ JSON.stringify(testCase.location) }" `, function() {
                req.url = testCase.url;

                return new Promise((done) => {
                    middleware(req, res, (error) => {
                        expect(error).toMatchObject({
                            code: 'DOMAIN_MOTO_TRUCKS',
                            data: {
                                location: testCase.location,
                                status: 301,
                            },
                        });
                        done();
                    });
                });
            });
        });
    });

    describe('moto ->', function() {
        const TESTS = [
            {
                url: '/',
                location: 'https://autoru_frontend.base_domain/motorcycle/all/',
            },
            {
                url: '/amphibious/',
                location: 'https://autoru_frontend.base_domain/atv/all/',
            },
            {
                url: '/atv/',
                location: 'https://autoru_frontend.base_domain/atv/all/',
            },
            {
                url: '/baggi/',
                location: 'https://autoru_frontend.base_domain/atv/all/',
            },
            {
                url: '/carting/',
                location: 'https://autoru_frontend.base_domain/motorcycle/all/',
            },
            {
                url: '/motorcycle/',
                location: 'https://autoru_frontend.base_domain/motorcycle/all/',
            },
            {
                url: '/scooters/',
                location: 'https://autoru_frontend.base_domain/scooters/all/',
            },
            {
                url: '/snowmobile/',
                location: 'https://autoru_frontend.base_domain/snowmobile/all/',
            },
        ];

        beforeEach(() => {
            req.headers['x-forwarded-host'] = `moto.${ BASE_DOMAIN }`;
        });

        TESTS.forEach(function(testCase, i) {
            it(`test #${ i }: "moto.auto.ru${ testCase.url }" -> "${ JSON.stringify(testCase.location) }" `, function() {
                req.url = testCase.url;

                return new Promise((done) => {
                    middleware(req, res, (error) => {
                        expect(error).toMatchObject({
                            code: 'DOMAIN_MOTO_TRUCKS',
                            data: {
                                location: testCase.location,
                                status: 301,
                            },
                        });
                        done();
                    });
                });
            });
        });
    });
});

it('должен сделать редирект moto.auto.ru/office/ -> auto.ru/my/moto/', () => {
    req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
    req.url = '/office/';

    return new Promise((done) => {
        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'DOMAIN_MOTO_TRUCKS',
                data: {
                    location: 'https://auto.ru/my/moto/',
                    status: 301,
                },
            });
            done();
        });
    });
});

it('должен сделать редирект trucks.auto.ru/office/ -> auto.ru/my/trucks/', () => {
    req.headers['x-forwarded-host'] = 'trucks.' + BASE_DOMAIN;
    req.url = '/office/';

    return new Promise((done) => {
        middleware(req, res, (error) => {
            expect(error).toMatchObject({
                code: 'DOMAIN_MOTO_TRUCKS',
                data: {
                    location: 'https://auto.ru/my/trucks/',
                    status: 301,
                },
            });
            done();
        });
    });
});

describe('динамические редиректы', () => {
    beforeEach(() => {
        yaAuto
            .get('/motoBreadcrumbs')
            .query((query) => {
                return query.moto_category === 'motorcycle' && !query.mark && !query.model;
            })
            .reply(200, {
                data: [
                    [
                        {
                            data: [
                                { id: 'YAMAHA', 'mark-autoru-code': 'yamaha' },
                                { id: 'ZIP_MOTORS', 'mark-autoru-code': 'zip-motors' },
                            ],
                            meta: 'MARK_LEVEL',
                        },
                    ],
                ],
                status: 'SUCCESS',
            });

        yaAuto
            .get('/motoBreadcrumbs')
            .query((query) => {
                return query.moto_category === 'motorcycle' && query.mark === 'YAMAHA' && !query.model;
            })
            .reply(200, {
                data: [
                    [
                        {
                            data: [
                                { id: 'YAMAHA', 'mark-autoru-code': 'yamaha' },
                                { id: 'ZIP_MOTORS', 'mark-autoru-code': 'zip-motors' },
                            ],
                            meta: 'MARK_LEVEL',
                        },
                        {
                            data: [
                                { id: 'YZ_250', 'model-autoru-code': 'yz-250' },
                            ],
                            meta: 'MODEL_LEVEL',
                        },
                    ],
                ],
                status: 'SUCCESS',
            });
    });

    it('должен сделать редирект moto.auto.ru/motorcycle/used/zip-motors/ -> auto.ru/motorcycle/zip_motors/used/ (есть такая марка)', () => {
        req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
        req.url = '/motorcycle/used/zip-motors/';

        return new Promise((done) => {
            middleware(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'DOMAIN_MOTO_TRUCKS',
                    data: {
                        location: 'https://autoru_frontend.base_domain/motorcycle/zip_motors/used/',
                        status: 301,
                    },
                });
                done();
            });
        });
    });

    it('должен сделать редирект moto.auto.ru/motorcycle/used/zip-motors1/ -> auto.ru/motorcycle/used/ (нет такой марки)', () => {
        req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
        req.url = '/motorcycle/used/zip-motors/';

        return new Promise((done) => {
            middleware(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'DOMAIN_MOTO_TRUCKS',
                    data: {
                        location: 'https://autoru_frontend.base_domain/motorcycle/zip_motors/used/',
                        status: 301,
                    },
                });
                done();
            });
        });
    });

    it('должен сделать редирект moto.auto.ru/motorcycle/used/yamaha/yz-250/ -> auto.ru/motorcycle/yamaha/yz_250/used/ (есть такая марка+модель)', () => {
        req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
        req.url = '/motorcycle/used/yamaha/yz-250/';

        return new Promise((done) => {
            middleware(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'DOMAIN_MOTO_TRUCKS',
                    data: {
                        location: 'https://autoru_frontend.base_domain/motorcycle/yamaha/yz_250/used/',
                        status: 301,
                    },
                });
                done();
            });
        });
    });

    it('должен сделать редирект moto.auto.ru/motorcycle/used/yamaha/yz-250-123/ -> auto.ru/motorcycle/yamaha/used/ (нет такой марка+модель)', () => {
        req.headers['x-forwarded-host'] = 'moto.' + BASE_DOMAIN;
        req.url = '/motorcycle/used/yamaha/yz-250-123/';

        return new Promise((done) => {
            middleware(req, res, (error) => {
                expect(error).toMatchObject({
                    code: 'DOMAIN_MOTO_TRUCKS',
                    data: {
                        location: 'https://autoru_frontend.base_domain/motorcycle/yamaha/used/',
                        status: 301,
                    },
                });
                done();
            });
        });
    });
});
