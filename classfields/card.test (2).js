const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const _ = require('lodash');

const card = require('./card');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');

const offerMock = require('autoru-frontend/mockData/responses/offer-new-group.mock.json');

function successResponseForCardGroupComplectations() {
    publicApi
        .get('/1.0/reference/catalog/cars/techparam')
        .query(true)
        .reply(200, {});

    publicApi
        .get('/1.0/search/cars')
        .query((query) => _.isEqual(query.group_by, [ 'TECHPARAM', 'COMPLECTATION' ]))
        .reply(200, {
            offers: [ offerMock ],
        });
}

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

describe('редирект для объявления для экспертов', () => {
    let params;
    let offer;
    beforeEach(() => {
        offer = {
            offer: {
                additional_info: {
                    autoru_pro: true,
                },
                car_info: {
                    mark_info: { code: 'MARK_CODE' },
                    model_info: { code: 'MODEL_CODE' },
                    complectation: { id: '_complectation_id_' },
                    tech_param: { id: '_tech_param_id_' },
                },
                category: 'CARS',
                id: '200-nomodel',
                section: 'used',
                status: 'ACTIVE',
            },
        };

        params = {
            category: 'cars',
            section: 'used',
            mark: 'mark_code',
            model: 'model_code',
            sale_id: '123',
            sale_hash: 'abc',
        };

        successResponseForCardGroupComplectations();
    });

    it('должен ответить 302, если карточка вернула 403', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(403, {
                error: 'AUTORU_EXPERT_PROHIBITED',
                status: 'ERROR',
                detailed_error: 'autoru_expert section is prohibited',
            });

        return de.run(card, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'OFFER_AUTORU_EXPERT_PROHIBITED',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/promo/expert/',
                        status_code: 302,
                    },
                });
            });
    });

    it('должен ответить 200, если карточка вернула 200', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(200, offer);

        return de.run(card, { context, params })
            .then((result) => {
                expect(result).toMatchObject({
                    card: {
                        id: '200',
                    },
                });
            });
    });
});

describe('редирект для забаненного объявления', () => {
    let params;
    let offer;
    beforeEach(() => {
        offer = {
            offer: {
                additional_info: {},
                car_info: {
                    mark_info: { code: 'MARK_CODE' },
                    model_info: { code: 'MODEL_CODE' },
                    complectation: { id: '_complectation_id_' },
                    tech_param: { id: '_tech_param_id_' },
                },
                category: 'CARS',
                id: '200-nomodel',
                section: 'used',
                status: 'BANNED',
            },
        };

        params = {
            category: 'cars',
            section: 'used',
            mark: 'mark_code',
            model: 'model_code',
            sale_id: '123',
            sale_hash: 'abc',
        };

        successResponseForCardGroupComplectations();
    });

    it('должен ответить 301, если это объявление забанено и я не владелец и не модератор', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(200, offer);

        return de.run(card, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'OFFER_BANNED',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/mark_code/model_code/used/',
                        status_code: 301,
                    },
                });
            });
    });

    it('не должен ответить 301, если это объявление забанено и модератор', () => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.moderator_autoru());

        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(200, offer);

        return de.run(card, { context, params })
            .then((result) => {
                expect(result).toMatchObject({
                    card: {
                        id: '200',
                    },
                    session: {
                        isModerator: true,
                    },
                });
            });
    });

    it('не должен ответить 301, если это объявление забанено и я владелец', () => {
        offer.offer.additional_info.is_owner = true;

        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(200, offer);

        return de.run(card, { context, params })
            .then((result) => {
                expect(result).toMatchObject({
                    card: {
                        additional_info: {
                            is_owner: true,
                        },
                    },
                });
            });
    });

    it('не должен ответить 301, если это есть флаг can_view', () => {
        offer.offer.additional_info.can_view = true;

        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(200, offer);

        return de.run(card, { context, params })
            .then((result) => {
                expect(result).toMatchObject({
                    card: {
                        additional_info: {
                            can_view: true,
                        },
                    },
                });
            });
    });
});

describe('ответ 404', () => {
    it('должен ответить 404, если карточка ответила 404', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc?rid=213')
            .reply(404);

        const params = {
            category: 'cars',
            section: 'used',
            mark: 'mark_code',
            model: 'model_code',
            sale_id: '123',
            sale_hash: 'abc',
        };

        return de.run(card, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        id: 'OFFER_NOT_FOUND',
                        status_code: 404,
                    },
                });
            });
    });

});

describe('редирект старой карточки нового авто', () => {
    it('должен ответить 301, если карточка нового авто', () => {
        req.router.route.getName.mockReturnValue('card');

        successResponseForCardGroupComplectations();

        publicApi
            .get('/1.0/offer/cars/123-new?rid=213')
            .reply(200, {
                offer: {
                    additional_info: {},
                    car_info: {
                        mark_info: { code: 'MARK_CODE' },
                        model_info: { code: 'MODEL_CODE' },
                        complectation: { id: '_complectation_id_' },
                        tech_param: { id: '_tech_param_id_' },
                    },
                    category: 'CARS',
                    id: '123-new',
                    section: 'NEW',
                    status: 'ACTIVE',
                },
            });

        const params = {
            category: 'cars',
            section: 'new',
            mark: 'mark_code',
            model: 'model_code',
            sale_id: '123',
            sale_hash: 'new',
        };

        return de.run(card, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'OFFER_USED_TO_NEW',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/new/group/mark_code/model_code/_tech_param_id_/_complectation_id_/123-new/',
                        status_code: 301,
                    },
                });
            });
    });
});

describe('редирект с неправильной марки и модели на валидный url офера', () => {
    it('должен ответить 302, если марка и модель карточки не совпадает с маркой и моделью параметров запроса в секции new', () => {
        req.router.route.getName.mockReturnValue('card');

        successResponseForCardGroupComplectations();

        publicApi
            .get('/1.0/offer/cars/123-new?rid=213')
            .reply(200, {
                offer: {
                    additional_info: {},
                    car_info: {
                        mark_info: { code: 'MARK_CODE' },
                        model_info: { code: 'MODEL_CODE' },
                        complectation: { id: '_complectation_id_' },
                        tech_param: { id: '_tech_param_id_' },
                    },
                    category: 'CARS',
                    id: '123-new',
                    section: 'new',
                    status: 'ACTIVE',
                },
            });

        const params = {
            category: 'cars',
            section: 'new',
            mark: 'invalid_mark_code',
            model: 'invalid_model_code',
            sale_id: '123',
            sale_hash: 'new',
        };

        return de.run(card, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'OFFER_INVALID_MARK_MODEL',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/new/group/mark_code/model_code/_tech_param_id_/_complectation_id_/123-new/',
                        status_code: 302,
                    },
                });
            });
    });

    it('должен ответить 302, если марка и модель карточки не совпадает с маркой и моделью параметров запроса в секции used', () => {
        req.router.route.getName.mockReturnValue('card');

        successResponseForCardGroupComplectations();

        publicApi
            .get('/1.0/offer/cars/123-new?rid=213')
            .reply(200, {
                offer: {
                    additional_info: {},
                    car_info: {
                        mark_info: { code: 'MARK_CODE' },
                        model_info: { code: 'MODEL_CODE' },
                        complectation: { id: '_complectation_id_' },
                        tech_param: { id: '_tech_param_id_' },
                    },
                    category: 'CARS',
                    id: '123-new',
                    section: 'used',
                    status: 'ACTIVE',
                },
            });

        const params = {
            category: 'cars',
            section: 'new',
            mark: 'invalid_mark_code',
            model: 'invalid_model_code',
            sale_id: '123',
            sale_hash: 'new',
        };

        return de.run(card, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'OFFER_INVALID_MARK_MODEL',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/cars/used/sale/mark_code/model_code/123-new/',
                        status_code: 302,
                    },
                });
            });
    });
});

describe('publicUserInfo', () => {
    beforeEach(() => {
        successResponseForCardGroupComplectations();

        publicApi
            .get('/1.0/user/123-321_abc/info')
            .reply(200, {
                status: 'SUCCESS',
                offers_stats_by_category: {
                    CARS: {},
                    MOTO: {},
                    TRUCKS: {},
                },
            });
    });

    it('сделает запрос, если пришел оффер с encrypted_user_id', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc')
            .query(true)
            .reply(200, {
                offer: {
                    additional_info: { other_offers_show_info: { encrypted_user_id: '123-321_abc' } },
                    car_info: {
                        mark_info: { code: 'MARK_CODE' },
                        model_info: { code: 'MODEL_CODE' },
                        complectation: { id: '_complectation_id_' },
                        tech_param: { id: '_tech_param_id_' },
                    },
                    category: 'CARS',
                    id: '123-abc',
                    section: 'used',
                    status: 'ACTIVE',
                },
            });

        const params = {
            category: 'cars',
            section: 'used',
            mark: 'mark_code',
            model: 'model_code',
            sale_id: '123',
            sale_hash: 'abc',
        };

        return de.run(card, { context, params }).then(
            (result) => {
                expect(result.publicUserInfo).toMatchObject({ status: 'SUCCESS' });
            });
    });

    it('сработает guard на запрос, если пришел оффер без encrypted_user_id', () => {
        publicApi
            .get('/1.0/offer/cars/123-abc')
            .query(true)
            .reply(200, {
                offer: {
                    additional_info: { other_offers_show_info: { } },
                    car_info: {
                        mark_info: { code: 'MARK_CODE' },
                        model_info: { code: 'MODEL_CODE' },
                        complectation: { id: '_complectation_id_' },
                        tech_param: { id: '_tech_param_id_' },
                    },
                    category: 'CARS',
                    id: '123-abc',
                    section: 'used',
                    status: 'ACTIVE',
                },
            });

        const params = {
            category: 'cars',
            section: 'used',
            mark: 'mark_code',
            model: 'model_code',
            sale_id: '123',
            sale_hash: 'abc',
        };

        return de.run(card, { context, params }).then(
            (result) => {
                expect(result.publicUserInfo).toMatchObject({ error: { id: 'BLOCK_GUARDED' } });
            });
    });
});
