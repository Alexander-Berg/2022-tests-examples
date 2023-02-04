jest.mock('auto-core/lib/luster-bunker', () => {
    return {
        getNode() {
            return {};
        },
    };
});

import de from 'descript';
import nock from 'nock';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import sessionFixtures from 'auto-core/server/resources/publicApiAuth/methods/session.nock.fixtures';
import userFixtures from 'auto-core/server/resources/publicApiAuth/methods/user.nock.fixtures';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import controller from './form';

let context: TDescriptContext;
let req: THttpRequest;
let res: THttpResponse;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отдать пустую форму добавления, если нет черновика (/cars/used/add/)', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.no_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.no_auth());

    publicApi
        .get('/1.0/user/draft/cars')
        .reply(200, {
            offer: {
                status: 'DRAFT',
                id: '8838891971971507399-24c663ed',
            },
        });

    const params = {
        category: 'cars',
        form_type: 'add',
        section: 'used',
    };

    await expect(
        de.run(controller, { context, params }),
    ).resolves.toMatchObject({
        offerDraft: {
            offer: {
                status: 'DRAFT',
                id: '8838891971971507399',
                hash: '24c663ed',
                saleId: '8838891971971507399-24c663ed',
            },
        },
        session: {
            auth: false,
        },
    });

    expect(nock.isDone()).toBe(true);
});

it('должен создать черновик из объявления на форме редактирования (/cars/used/edit/123-abc/)', async() => {
    publicApi
        .get('/1.0/session/')
        .reply(200, sessionFixtures.user_auth());

    publicApi
        .get('/1.0/user/')
        .reply(200, userFixtures.user_auth());

    publicApi
        .post('/1.0/user/offers/cars/123-abc/edit')
        .reply(200, {
            status: 'SUCCESS',
            offer_id: '986-def',
        });

    publicApi
        .get('/1.0/user/draft/cars/986-def')
        .reply(200, {
            status: 'SUCCESS',
            offer: {
                id: '986-def',
                status: 'DRAFT',
            },
        });

    const params = {
        category: 'cars',
        form_type: 'edit',
        section: 'used',
        sale_id: '123',
        sale_hash: 'abc',
    };

    await expect(
        de.run(controller, { context, params }),
    ).resolves.toMatchObject({
        offerDraft: {
            offer: {
                status: 'DRAFT',
                id: '986',
                hash: 'def',
                saleId: '986-def',
            },
        },
        session: {
            auth: true,
        },
    });

    expect(nock.isDone()).toBe(true);
});

describe('страница с МММ в урле', () => {
    it('если драфт не заполнен сохранит в него марку и модель из урла', async() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.no_auth());

        publicApi
            .get('/1.0/reference/catalog/cars/suggest?mark=KIA&model=RIO&rid=225')
            .reply(200, {});

        publicApi
            .get('/1.0/user/draft/cars')
            .reply(200, {
                offer: {
                    status: 'DRAFT',
                    id: '8838891971971507399-24c663ed',
                },
                offer_id: '8838891971971507399-24c663ed',
            });

        publicApi
            .put('/1.0/user/draft/CARS/8838891971971507399-24c663ed')
            .reply(200, {
                offer: {
                    status: 'DRAFT',
                    id: '8838891971971507399-24c663ed',
                    car_info: {
                        mark_info: { name: 'Kia' },
                        model_info: { name: 'Rio' },
                    },
                    category: 'cars',
                },
                status: 'SUCCESS',
            });

        const params = {
            category: 'cars',
            form_type: 'add',
            section: 'used',
            mark: 'Kia',
            model: 'Rio',
        };

        await expect(
            de.run(controller, { context, params }),
        ).resolves.toMatchObject({
            offerDraft: {
                offer: {
                    vehicle_info: {
                        mark_info: { name: 'Kia' },
                        model_info: { name: 'Rio' },
                    },
                },
            },
        });
    });

    it('если ммм в урле не валидны, отдаст 404', async() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.no_auth());

        publicApi
            .get('/1.0/reference/catalog/cars/suggest?mark=KIA&model=RIO&rid=225')
            .reply(400, {
                status: 'ERROR',
                error: {
                    id: 'HTTP_400',
                },
            });

        publicApi
            .get('/1.0/user/draft/cars')
            .reply(200, {
                offer: {
                    status: 'DRAFT',
                    id: '8838891971971507399-24c663ed',
                },
                offer_id: '8838891971971507399-24c663ed',
            });

        publicApi
            .put('/1.0/user/draft/CARS/8838891971971507399-24c663ed')
            .reply(200, {
                offer: {
                    status: 'DRAFT',
                    id: '8838891971971507399-24c663ed',
                    car_info: {
                        mark_info: { name: 'Kia' },
                        model_info: { name: 'Rio' },
                    },
                    category: 'cars',
                },
                status: 'SUCCESS',
            });

        const params = {
            category: 'cars',
            form_type: 'add',
            section: 'used',
            mark: 'Kia',
            model: 'Rio',
        };

        await expect(
            de.run(controller, { context, params }),
        ).rejects.toMatchObject({
            error: {
                id: 'INCORRECT_URL_PARAMS: MARK OR MODEL',
                status_code: 404,
            },
        });
    });

    it('если драфт заполнен отдаст его как он есть', async() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.no_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.no_auth());

        publicApi
            .get('/1.0/reference/catalog/cars/suggest?mark=KIA&model=RIO&rid=225')
            .reply(200, {});

        publicApi
            .get('/1.0/user/draft/cars')
            .reply(200, {
                offer: {
                    status: 'DRAFT',
                    id: '8838891971971507399-24c663ed',
                    car_info: {
                        mark_info: { name: 'Ford' },
                    },
                    category: 'cars',
                },
                offer_id: '8838891971971507399-24c663ed',
            });

        publicApi
            .put('/1.0/user/draft/CARS/8838891971971507399-24c663ed')
            .reply(200, {
                offer: {
                    status: 'DRAFT',
                    id: '8838891971971507399-24c663ed',
                    car_info: {
                        mark_info: { name: 'Kia' },
                        model_info: { name: 'Rio' },
                    },
                    category: 'cars',
                },
                status: 'SUCCESS',
            });

        const params = {
            category: 'cars',
            form_type: 'add',
            section: 'used',
            mark: 'Kia',
            model: 'Rio',
        };

        await expect(
            de.run(controller, { context, params }),
        ).resolves.toMatchObject({
            offerDraft: {
                offer: {
                    vehicle_info: {
                        mark_info: { name: 'Ford' },
                    },
                },
            },
        });
    });
});

describe('редактирование черновика по draft_id', () => {
    it('должен запросить указанный черновик на форме редактирования (/cars/used/edit/draft/123-abc/)', async() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.user_auth());

        publicApi
            .get('/1.0/user/draft/cars/123-abc')
            .reply(200, {
                status: 'SUCCESS',
                offer: {
                    id: '123-abc',
                    status: 'DRAFT',
                },
            });

        const params = {
            category: 'cars',
            form_type: 'edit',
            section: 'used',
            draft_id: '123-abc',
        };

        await expect(
            de.run(controller, { context, params }),
        ).resolves.toMatchObject({
            offerDraft: {
                offer: {
                    status: 'DRAFT',
                    id: '123',
                    hash: 'abc',
                    saleId: '123-abc',
                },
            },
            session: {
                auth: true,
            },
        });

        expect(nock.isDone()).toBe(true);
    });

    it('должен вернуть 404, если черновик не найден (/cars/used/edit/draft/123-baddraft/)', async() => {
        publicApi
            .get('/1.0/session/')
            .reply(200, sessionFixtures.user_auth());

        publicApi
            .get('/1.0/user/')
            .reply(200, userFixtures.user_auth());

        publicApi
            .get('/1.0/user/draft/cars/123-baddraft')
            .reply(404, {
                error: 'DRAFT_NOT_FOUND',
                status: 'ERROR',
                detailed_error: 'DRAFT_NOT_FOUND',
            });

        const params = {
            category: 'cars',
            form_type: 'edit',
            section: 'used',
            draft_id: '123-baddraft',
        };

        await expect(
            de.run(controller, { context, params }),
        ).rejects.toEqual({
            error: { id: 'DRAFT_NOT_FOUND', status_code: 404 },
        });

        expect(nock.isDone()).toBe(true);
    });
});
