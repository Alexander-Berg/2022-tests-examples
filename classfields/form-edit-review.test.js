jest.mock('auto-core/lib/luster-bunker', () => {
    return {
        getNode(path) {
            if (path === '/auto_ru/common/vas') {
                return {};
            }
        },
    };
});

const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const sessionFixtures = require('auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures');
const userFixtures = require('auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures');
const getAnonDraftFixtures = require('auto-core/server/resources/publicApiReviews/methods/getAnonDraft.fixtures');
const getUserReviewFixtures = require('auto-core/server/resources/publicApiReviews/methods/getUserReview.fixtures');

const formEditReview = require('./form-edit-review');

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

describe('нет авторизации', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.no_auth());
    });

    it('должен восстановить черновик и запросить данные для него', () => {
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(200, getAnonDraftFixtures.response200());

        publicApi
            .get('/1.0/reference/catalog/cars/suggest?mark=AUDI&model=A4')
            .reply(200, {
                car_suggest: {
                    body_types: [],
                },
            });

        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI%23A4&rid=225')
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

        return de.run(formEditReview, {
            context,
            params: { parent_category: 'cars', review_id: '123' },
        })
            .then((result) => {
                expect(nock.isDone()).toEqual(true);

                expect(result).toMatchObject({
                    breadcrumbsPublicApi: [
                        { entities: [ { id: 'AUDI' } ], meta_level: 'MARK_LEVEL' },
                    ],
                    carsTechOptions: { body_type: [] },
                    user: { auth: false },
                    review: { review: { id: '123' } },
                });
            });
    });

    it('должен сделать редирект на правильный review_id, если открыт не свой черновик, но есть другой черновик', async() => {
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(200, getAnonDraftFixtures.response200());

        publicApi
            .get('/1.0/reference/catalog/cars/suggest?mark=AUDI&model=A4')
            .reply(200, {
                car_suggest: {
                    body_types: [],
                },
            });

        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI%23A4&rid=225')
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

        await expect(
            de.run(formEditReview, {
                context,
                params: { parent_category: 'trucks', review_id: '0987654321' },
            }),
        ).rejects.toMatchObject({
            error: {
                code: 'REVIEW_ANON_DRAFT_TO_SELF_REVIEW_ID',
                id: 'REDIRECTED',
                location: 'https://autoru_frontend.base_domain/trucks/reviews/edit/123/',
                status_code: 302,
            },
        });
    });

    it('должен сделать редирект на добавление отзыва, если запрашивается не свой черновик и нет своего черновика', async() => {
        publicApi
            .get('/1.0/user/reviews/123')
            .reply(401, getUserReviewFixtures.response401());

        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(404, getAnonDraftFixtures.response404());

        await expect(
            de.run(formEditReview, {
                context,
                params: { parent_category: 'trucks', review_id: '123' },
            }),
        ).rejects.toMatchObject({
            error: {
                code: 'REVIEW_ANON_DRAFT_TO_ADD',
                id: 'REDIRECTED',
                location: 'https://autoru_frontend.base_domain/trucks/reviews/add/',
                status_code: 302,
            },
        });
    });
});

describe('есть авторизация', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.user_auth());
    });

    it('должен восстановить черновик и запросить данные для него', () => {
        publicApi
            .get('/1.0/user/reviews/123')
            .reply(200, getUserReviewFixtures.response200());

        publicApi
            .get('/1.0/reference/catalog/cars/suggest?mark=AUDI&model=A4')
            .reply(200, {
                car_suggest: {
                    body_types: [],
                },
            });

        publicApi
            .get('/1.0/search/cars/breadcrumbs?bc_lookup=AUDI%23A4&rid=225')
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

        return de.run(formEditReview, {
            context,
            params: { parent_category: 'cars', review_id: '123' },
        })
            .then((result) => {
                expect(nock.isDone()).toEqual(true);

                expect(result).toMatchObject({
                    breadcrumbsPublicApi: [
                        { entities: [ { id: 'AUDI' } ], meta_level: 'MARK_LEVEL' },
                    ],
                    carsTechOptions: { body_type: [] },
                    user: { auth: true },
                    review: {
                        review: {
                            id: '123',
                            reviewer: {
                                name: 'Битумощебнераспределитель',
                                unconfirmed_email: 'natix@yandex-team.ru',
                                phones: [
                                    {
                                        phone: '71234567890',
                                        added: '2018-02-22T17:21:27Z',
                                        phone_formatted: '+7 123 456-78-90',
                                    },
                                ],
                            },
                        },
                    },
                });
            });
    });

    it('должен отдать 404 запрашивается не свой черновик', async() => {
        publicApi
            .get('/1.0/user/reviews/123')
            .reply(404, getUserReviewFixtures.response404());

        await expect(
            de.run(formEditReview, {
                context,
                params: { parent_category: 'cars', review_id: '123' },
            }),
        ).rejects.toMatchObject({
            error: {
                id: 'REVIEW_NOT_FOUND',
                status_code: 404,
            },
        });
    });
});
