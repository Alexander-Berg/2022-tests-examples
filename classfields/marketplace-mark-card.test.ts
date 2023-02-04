import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import type { TDescriptContext } from 'auto-core/server/descript/createContext';
import createContext from 'auto-core/server/descript/createContext';

import type { THttpRequest, THttpResponse } from 'auto-core/http';

import controller from './marketplace-mark-card';

let context: TDescriptContext;
let req: THttpRequest;
let res: THttpResponse;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен средиректить на карточку модели, если в листинге только одна тачка', () => {
    const params = {
        category: 'cars',
        section: 'new',
        catalog_filter: [ {
            mark: 'renault',
        } ],
    };

    publicApi
        .get('/1.0/search/cars/cross-links-count')
        .query(() => true)
        .reply(200, {});

    publicApi
        .get('/1.0/search/cars')
        .query(() => true)
        .reply(200, {
            offers: [ {
                id: 'abc-def',
                car_info: {
                    mark_info: {
                        code: 'RENAULT',
                    },
                    model_info: {
                        code: 'ARKANA',
                    },
                    super_gen: {
                        id: '123',
                    },
                    configuration: {
                        id: '345',
                    },
                },
            } ],

        });

    return de.run(controller, { params, context }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            return expect(result).toMatchObject({
                error: {
                    code: 'MARKETPLACE_MARK_TO_MODEL_CARD',
                    id: 'REDIRECTED',
                    location: 'https://autoru_frontend.base_domain/cars/new/',
                    status_code: 302,
                },
            });
        },
    );
});
