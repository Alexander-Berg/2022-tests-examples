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

const formAddReview = require('./form-add-review');

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

describe('редирект /add/<offerId>/ -> /edit/<reviewId>/', () => {
    beforeEach(() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.no_auth());
    });

    it('должен сделать редирект, если ручка ответила', () => {
        publicApi
            .get('/1.0/reviews/auto/cars/offer/123-abc')
            .reply(200, {
                review: { id: '_review-id_' },
            });

        return de.run(formAddReview, {
            context,
            params: { parent_category: 'cars', offerId: '123-abc' },
        })
            .then(
                () => Promise.reject('UNEXPECTED_RESOLVE'),
                (error) => {
                    expect(nock.isDone()).toEqual(true);

                    expect(error).toMatchObject({
                        error: {
                            code: 'FORM_ADD_TO_EDIT',
                            id: 'REDIRECTED',
                            location: '/cars/reviews/edit/_review-id_',
                            status_code: 302,
                        },
                    });
                },
            );
    });

    it('не должен сделать редирект, если ручка ответила', () => {
        publicApi
            .get('/1.0/reviews/auto/cars/offer/123-abc')
            .reply(404);

        return de.run(formAddReview, {
            context,
            params: { parent_category: 'cars', offerId: '123-abc' },
        })
            .then(() => {
                expect(nock.isDone()).toEqual(true);
            });
    });
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

    it('должен попробовать восстановить черновик', () => {
        // нет черновика
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(404, { error: 'REVIEW_NOT_FOUND', status: 'ERROR', detailed_error: 'REVIEW_NOT_FOUND' });

        publicApi
            .get('/1.0/search/cars/breadcrumbs?rid=225')
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

        return de.run(formAddReview, {
            context,
            params: { parent_category: 'cars' },
        })
            .then(() => {
                expect(nock.isDone()).toEqual(true);
            });
    });

    it('должен средиректить на форму редактирования, если восстановил черновик', () => {
        // есть черновик
        publicApi
            .get('/1.0/reviews/auto/anon-draft')
            .reply(200, {
                review: {
                    id: '_review-id_',
                    item: {
                        auto: {
                            category: 'CARS',
                        },
                    },
                },
            });

        return de.run(formAddReview, {
            context,
            params: { parent_category: 'cars' },
        }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(nock.isDone()).toEqual(true);

                expect(result).toMatchObject({
                    error: {
                        code: 'FORM_ADD_TO_EDIT',
                        id: 'REDIRECTED',
                        location: '/cars/reviews/edit/_review-id_',
                        status_code: 302,
                    },
                });
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

    it('не должен попробовать восстановить черновик анонима', () => {
        return de.run(formAddReview, {
            context,
            params: { parent_category: 'cars' },
        }).then((result) => {
            expect(result).toMatchObject({
                _reviewDraft: {
                    error: { id: 'BLOCK_GUARDED' },
                },
            });
        });
    });
});
