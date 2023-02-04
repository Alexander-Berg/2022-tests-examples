const de = require('descript');

jest.mock('auto-core/server/blocks/ads.js', () => {
    const de = require('descript');
    return de.func({
        block: ({ params }) => {
            return { params };
        },
    });
});

const createContext = require('auto-core/server/descript/createContext');

const adsForCard = require('./adsForCard');

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

    req.geoIdsInfo = [
        {
            linguistics: { preposition: 'в', prepositional: 'Москве' },
        },
    ];
});

describe('проверка обработки ответа крошек', () => {
    let breadcrumbsResponse;
    beforeEach(() => {
        breadcrumbsResponse = {
            breadcrumbs: [
                {
                    entities: [
                        {
                            id: 'A4',
                            model: { cyrillic_name: 'А4' },
                            name: 'A4name',
                        },
                    ],
                    meta_level: 'MODEL_LEVEL',
                    mark: {
                        id: 'AUDI',
                        name: 'Audi',
                    },
                },
                {
                    entities: [
                        {
                            id: 'AUDI',
                            mark: { cyrillic_name: 'Ауди' },
                            name: 'AudiName',
                        },
                    ],
                    meta_level: 'MARK_LEVEL',
                },
            ],
        };
    });

    it('должен сделать запрос для cars с данными из крошек', () => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs')
            .query({
                bc_lookup: 'AUDI#A4',
            })
            .reply(200, breadcrumbsResponse);

        const params = {
            category: 'cars',
            mark: 'audi',
            model: 'a4',
        };

        return de.run(adsForCard, { context, params })
            .then((result) => {
                expect(result).toEqual({
                    params: {
                        button: {
                            text: 'купить автомобиль AudiName A4name в Москве',
                        },
                        gallery: {
                            text: 'купить автомобиль AudiName A4name в Москве',
                            'page-no': 2,
                        },
                        top: {
                            text: 'купить автомобиль AudiName A4name в Москве',
                        },

                        category: 'cars',
                        mark: 'audi',
                        model: 'a4',
                    },
                });
            });
    });

    it('должен сделать запрос для moto с данными из крошек', () => {
        publicApi
            .get('/1.0/search/moto/breadcrumbs')
            .query({
                bc_lookup: 'ATV#AUDI#A4',
            })
            .reply(200, breadcrumbsResponse);

        const params = {
            category: 'moto',
            moto_category: 'atv',
            mark: 'audi',
            model: 'a4',
        };

        return de.run(adsForCard, { context, params })
            .then((result) => {
                expect(result).toEqual({
                    params: {
                        button: {
                            text: 'купить мотовездеходы AudiName A4name в Москве',
                        },
                        gallery: {
                            text: 'купить мотовездеходы AudiName A4name в Москве',
                            'page-no': 2,
                        },
                        top: {
                            text: 'купить мотовездеходы AudiName A4name в Москве',
                        },

                        category: 'moto',
                        moto_category: 'atv',
                        mark: 'audi',
                        model: 'a4',
                    },
                });
            });
    });

    it('должен сделать запрос для trucks с данными из крошек', () => {
        publicApi
            .get('/1.0/search/trucks/breadcrumbs')
            .query({
                bc_lookup: 'ARTIC#AUDI#A4',
            })
            .reply(200, breadcrumbsResponse);

        const params = {
            category: 'trucks',
            trucks_category: 'artic',
            mark: 'audi',
            model: 'a4',
        };

        return de.run(adsForCard, { context, params })
            .then((result) => {
                expect(result).toEqual({
                    params: {
                        button: {
                            text: 'купить тягачи AudiName A4name в Москве',
                        },
                        gallery: {
                            text: 'купить тягачи AudiName A4name в Москве',
                            'page-no': 2,
                        },
                        top: {
                            text: 'купить тягачи AudiName A4name в Москве',
                        },

                        category: 'trucks',
                        trucks_category: 'artic',
                        mark: 'audi',
                        model: 'a4',
                    },
                });
            });
    });
});

describe('обработка неответа крошек', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI%23A4&category=cars')
            .times(2)
            .reply(500);
    });

    it('должен вернуть пустой объект, если крошки не ответили', () => {
        const params = {
            category: 'cars',
            mark: 'audi',
            model: 'a4',
        };

        return de.run(adsForCard, { context, params })
            .then((result) => {
                expect(result).toEqual({});
            });
    });
});
