jest.mock('auto-core/lib/m-redirect', () => {
    return {
        shouldRedirect: jest.fn(),
    };
});

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const { shouldRedirect } = require('auto-core/lib/m-redirect');

const subscriptionDelete = require('./subscription-delete');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен сделать редирект на морду, если нет user_id', () => {
    publicApi.put('/1.0/user/favorites/all/subscriptions/200-ok/email').reply(200, {
        status: 'SUCCESS',
    });

    return de.run(subscriptionDelete, {
        context,
        params: { subscription_id: '200-ok' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'SUBSCRIPTIONS_DELETE_REJECT',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/',
                },
            });
        },
    );
});

it('должен сделать редирект на морду, если нет subscription_id', () => {
    return de.run(subscriptionDelete, {
        context,
        params: { user_id: 'user:12345' },
    }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    code: 'SUBSCRIPTIONS_DELETE_REJECT',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/',
                },
            });
        },
    );
});

describe('хороший ответ', () => {
    let params;
    beforeEach(() => {
        publicApi.put('/1.0/user/favorites/all/subscriptions/200-ok/email').reply(200, {
            status: 'SUCCESS',
        });

        params = {
            subscription_id: '200-ok',
            user_id: 'user:12345',
        };
    });

    it('должен сделать редирект на морду для десктопа', () => {
        shouldRedirect.mockReturnValue(false);
        return de.run(subscriptionDelete, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'SUBSCRIPTIONS_DELETE',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/like/searches/?show-searches=true&subs_delete_popup=true',
                    },
                });
            },
        );
    });

    it('должен сделать редирект на страницу поисков для мобилки', () => {
        shouldRedirect.mockReturnValue(true);
        return de.run(subscriptionDelete, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            (result) => {
                expect(result).toMatchObject({
                    error: {
                        code: 'SUBSCRIPTIONS_DELETE',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/like/searches/?show-searches=true&subs_delete_popup=true',
                    },
                });
            },
        );
    });
});

describe('плохой ответ', () => {
    let params;
    beforeEach(() => {
        publicApi.put('/1.0/user/favorites/all/subscriptions/404-notfound/email').reply(404, {
            error: 'SUBSCRIPTION_NOT_FOUND',
            status: 'ERROR',
        });

        params = {
            subscription_id: '404-notfound',
            user_id: 'user:12345',
        };
    });

    it('должен сделать редирект на листинг для десктопа', () => {
        shouldRedirect.mockReturnValue(false);
        return de.run(subscriptionDelete, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            result => {
                expect(result).toMatchObject({
                    error: {
                        code: 'SUBSCRIPTIONS_DELETE',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/like/searches/?subs_delete_error=SUBSCRIPTION_NOT_FOUND&subs_delete_popup=false',
                    },
                });
            },
        );
    });

    it('должен сделать редирект на страницу поисков для мобилки', () => {
        shouldRedirect.mockReturnValue(true);
        return de.run(subscriptionDelete, { context, params }).then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            result => {
                expect(result).toMatchObject({
                    error: {
                        code: 'SUBSCRIPTIONS_DELETE',
                        id: 'REDIRECTED',
                        location: 'https://autoru_frontend.base_domain/like/searches/?subs_delete_error=SUBSCRIPTION_NOT_FOUND&subs_delete_popup=false',
                    },
                });
            },
        );
    });
});
