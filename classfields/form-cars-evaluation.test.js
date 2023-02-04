jest.mock('auto-core/lib/luster-bunker', () => {
    return {
        getNode(path) {
            if (path === '/auto_ru/common/vas') {
                return {};
            }
        },
    };
});

jest.mock('auto-core/lib/geobase-binding', () => {
    return {
        getParentsIds: () => [ 1, 2, 3 ],
    };
});

const mockEvaluationShareGetFromYdb = jest.fn();
jest.mock('auto-core/server/blocks/evaluationShareGetFromYdb', () => {
    const de = require('descript');
    return de.func({
        block: mockEvaluationShareGetFromYdb,
    });
});

const de = require('descript');

const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const formCarsEvaluation = require('./form-cars-evaluation');

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
    req.regionByIp = { type: 1 };

    mockEvaluationShareGetFromYdb.mockReturnValue(null);
});

it('должен запросить крошки, если нет шарилки', () => {
    publicApi
        .get('/1.0/search/cars/breadcrumbs?state=NEW&state=USED&rid=225')
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                    ],
                    meta_level: 'MARK_LEVEL',
                    offers_count: 12345,
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(formCarsEvaluation, {
        context,
        params: { parent_category: 'cars' },
    })
        .then((result) => {
            expect(nock.isDone()).toEqual(true);

            expect(result).toMatchObject({
                evaluationShare: null,
            });
        });
});

it('должен запросить крошки из шарилки ydb, если она есть', () => {
    mockEvaluationShareGetFromYdb.mockReturnValue({
        value: JSON.stringify({ mark: 'AUDI', model: 'A4' }),
    });

    publicApi
        .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI%23A4&state=NEW&state=USED&rid=225')
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                    ],
                    meta_level: 'MARK_LEVEL',
                    offers_count: 12345,
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(formCarsEvaluation, {
        context,
        params: { key: 'N2FlNjAwNzAtYTc1My0xMWViLWEyYWQtNWYyMmU2MmE0Y2Ez', parent_category: 'cars' },
    })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});

it('должен запросить крошки из шарилки с учетом марок и моделей из трейд-ина', () => {
    const response = {
        mark: 'AUDI',
        model: 'A4',
        tradein: [
            {
                mark: 'HYUNDAI',
                models: [ { id: 'TUCSON' } ],
            },
            {
                mark: 'BMW',
                models: [ { id: '3ER' } ],
            },
        ],
    };
    mockEvaluationShareGetFromYdb.mockReturnValue({
        value: JSON.stringify(response),
    });

    publicApi
        .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI%23A4&bc_lookup=HYUNDAI&bc_lookup=BMW&state=NEW&state=USED&rid=225')
        .reply(200, {
            breadcrumbs: [
                {
                    entities: [
                        { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                    ],
                    meta_level: 'MARK_LEVEL',
                    offers_count: 12345,
                },
            ],
            status: 'SUCCESS',
        });

    return de.run(formCarsEvaluation, {
        context,
        params: { key: 'N2FlNjAwNzAtYTc1My0xMWViLWEyYWQtNWYyMmU2MmE0Y2Ez', parent_category: 'cars' },
    })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});

describe('urlFormData', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?state=NEW&state=USED&rid=225')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                        offers_count: 12345,
                    },
                ],
                status: 'SUCCESS',
            });
    });

    it('не должен вернуть address, если он не вычислился из req', () => {
        const params = { parent_category: 'cars' };

        return de.run(formCarsEvaluation, { context, params }).then(
            (result) => {
                expect(result.urlFormData).toEqual({});
            },
        );
    });

    it('должен вернуть address с первым городом из geoIdsInfo', () => {
        req.geoIdsInfo = [
            { type: 5 },
            { id: 213, name: 'Морква', type: 6 },
        ];
        const params = { parent_category: 'cars' };

        return de.run(formCarsEvaluation, { context, params }).then(
            (result) => {
                expect(result.urlFormData).toEqual({
                    address: {
                        value: {
                            cityName: 'Морква',
                            geo_id: 213,
                            geoParentsIds: [ 1, 2, 3 ],
                        },
                    },
                });
            },
        );
    });

    it('должен вернуть address с городом из regionByIp, если такого нет в geoIdsInfo', () => {
        req.geoIdsInfo = [
            { type: 5 },
            { id: 213, name: 'Морква', type: 4 },
        ];
        req.regionByIp = { id: 214, name: 'Морква из ip', type: 6 };
        const params = { parent_category: 'cars' };

        return de.run(formCarsEvaluation, { context, params }).then(
            (result) => {
                expect(result.urlFormData).toEqual({
                    address: {
                        value: {
                            cityName: 'Морква из ip',
                            geo_id: 214,
                            geoParentsIds: [ 1, 2, 3 ],
                        },
                    },
                });
            },
        );
    });
});

describe('evaluationTradeInConfig', () => {

    beforeEach(() => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?state=NEW&state=USED&rid=225')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                        offers_count: 12345,
                    },
                ],
                status: 'SUCCESS',
            });
        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI&state=NEW&state=USED&rid=225')
            .reply(200, {
                breadcrumbs: [
                    {
                        entities: [
                            { id: 'AUDI', mark: { cyrillic_name: 'Ауди' }, name: 'AudiName' },
                        ],
                        meta_level: 'MARK_LEVEL',
                        offers_count: 12345,
                    },
                ],
                status: 'SUCCESS',
            });
    });

    it('должен вернуть правильный конфиг, если эксперимент включен, ' +
        'фичи трейдина и причины продажи включены в бункере и регион пользователя' +
        'входит в список регионов из бункера', () => {
        const tradeinButtonConfig = {
            isFeatureEnabled: true,
            featureRegions: [ 1 ],
        };
        const reasonFormConfig = {
            isFeatureEnabled: true,
        };
        const lusterBunker = require('auto-core/lib/luster-bunker');
        lusterBunker.getNode = nodeName => nodeName === '/auto_ru/common/evaluation_form_reason' ? reasonFormConfig : tradeinButtonConfig;
        req.regionByIp = { id: 1, name: 'Морква', type: 6 };
        req.experimentsData.has = () => true;

        return de.run(formCarsEvaluation, { context }).then(
            (result) => {
                expect(result.evaluationTradeInConfig).toEqual({
                    isTradeinButtonFeatureEnabled: true,
                    featureRegions: [ 1 ],
                    isReasonFeatureEnabled: true,
                    shouldShowTradeinForm: true,
                    shouldShowTradeinMarkModels: true,
                });
            },
        );
    });

    it('должен вернуть правильный конфиг, если эксперимент включен, ' +
        'фичи трейдина и причины продажи включены и регион оценки входит в список регионов из бункера', () => {
        const tradeinButtonConfig = {
            isFeatureEnabled: true,
            featureRegions: [ 1 ],
        };
        const reasonFormConfig = {
            isFeatureEnabled: true,
        };

        mockEvaluationShareGetFromYdb.mockReturnValue({
            value: JSON.stringify({ mark: 'AUDI', address: { geo_id: 1 } }),
        });

        const lusterBunker = require('auto-core/lib/luster-bunker');
        lusterBunker.getNode = nodeName => nodeName === '/auto_ru/common/evaluation_form_reason' ? reasonFormConfig : tradeinButtonConfig;
        req.experimentsData.has = () => true;

        return de.run(formCarsEvaluation, {
            context,
            params: { key: 'N2FlNjAwNzAtYTc1My0xMWViLWEyYWQtNWYyMmU2MmE0Y2Ez', parent_category: 'cars' },
        }).then(
            (result) => {
                expect(result.evaluationTradeInConfig).toEqual({
                    isTradeinButtonFeatureEnabled: true,
                    featureRegions: [ 1 ],
                    isReasonFeatureEnabled: true,
                    shouldShowTradeinForm: true,
                    shouldShowTradeinMarkModels: true,
                });
            },
        );
    });

    it('должен вернуть правильный конфиг, если эксперимент - выключен', () => {
        const tradeinButtonConfig = {
            isFeatureEnabled: true,
            featureRegions: [ 1 ],
        };
        const reasonFormConfig = {
            isFeatureEnabled: true,
        };

        const lusterBunker = require('auto-core/lib/luster-bunker');
        lusterBunker.getNode = nodeName => nodeName === '/auto_ru/common/evaluation_form_reason' ? reasonFormConfig : tradeinButtonConfig;
        req.regionByIp = { id: 1, name: 'Морква', type: 6 };
        req.experimentsData.has = () => false;

        return de.run(formCarsEvaluation, { context }).then(
            (result) => {
                expect(result.evaluationTradeInConfig).toEqual({
                    isTradeinButtonFeatureEnabled: true,
                    featureRegions: [ 1 ],
                    isReasonFeatureEnabled: true,
                    shouldShowTradeinForm: true,
                    shouldShowTradeinMarkModels: false,
                });
            },
        );
    });

    it('должен вернуть правильный конфиг, если фичи трейдина и причины продажи выключены в бункере, эксп выключен',
        () => {
            const tradeinButtonConfig = {
                isFeatureEnabled: false,
                featureRegions: [ 1 ],
            };
            const reasonFormConfig = {
                isFeatureEnabled: false,
            };
            const lusterBunker = require('auto-core/lib/luster-bunker');
            lusterBunker.getNode = nodeName => nodeName === '/auto_ru/common/evaluation_form_reason' ? reasonFormConfig : tradeinButtonConfig;

            req.regionByIp = { id: 1, name: 'Морква', type: 6 };
            req.experimentsData.has = () => true;

            return de.run(formCarsEvaluation, { context }).then(
                (result) => {
                    expect(result.evaluationTradeInConfig).toEqual({
                        isTradeinButtonFeatureEnabled: false,
                        featureRegions: [ 1 ],
                        isReasonFeatureEnabled: false,
                        shouldShowTradeinForm: false,
                        shouldShowTradeinMarkModels: false,
                    });
                },
            );
        });

    it('должен вернуть правильный конфиг, ' +
        'если регион не входит в список регионов из бункера, эксп включен', () => {
        const tradeinButtonConfig = {
            isFeatureEnabled: true,
            featureRegions: [ 666 ],
        };
        const reasonFormConfig = {
            isFeatureEnabled: true,
        };
        const lusterBunker = require('auto-core/lib/luster-bunker');
        lusterBunker.getNode = nodeName => nodeName === '/auto_ru/common/evaluation_form_reason' ? reasonFormConfig : tradeinButtonConfig;

        req.regionByIp = { id: 1, name: 'Морква', type: 6 };
        req.experimentsData.has = () => true;

        return de.run(formCarsEvaluation, { context }).then(
            (result) => {
                expect(result.evaluationTradeInConfig).toEqual({
                    isTradeinButtonFeatureEnabled: true,
                    featureRegions: [ 666 ],
                    isReasonFeatureEnabled: true,
                    shouldShowTradeinForm: false,
                    shouldShowTradeinMarkModels: true,
                });
            },
        );
    });

    it('должен вернуть правильный конфиг, ' +
        'если регион не входит в список регионов из бункера, эксп выключен', () => {
        const tradeinButtonConfig = {
            isFeatureEnabled: true,
            featureRegions: [ 666 ],
        };
        const reasonFormConfig = {
            isFeatureEnabled: true,
        };
        const lusterBunker = require('auto-core/lib/luster-bunker');
        lusterBunker.getNode = nodeName => nodeName === '/auto_ru/common/evaluation_form_reason' ? reasonFormConfig : tradeinButtonConfig;

        req.regionByIp = { id: 1, name: 'Морква', type: 6 };

        return de.run(formCarsEvaluation, { context }).then(
            (result) => {
                expect(result.evaluationTradeInConfig).toEqual({
                    isTradeinButtonFeatureEnabled: true,
                    featureRegions: [ 666 ],
                    isReasonFeatureEnabled: true,
                    shouldShowTradeinForm: false,
                    shouldShowTradeinMarkModels: false,
                });
            },
        );
    });
});
