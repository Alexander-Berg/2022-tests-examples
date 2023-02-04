import de from 'descript';

import createHttpReq from 'autoru-frontend/mocks/createHttpReq';
import createHttpRes from 'autoru-frontend/mocks/createHttpRes';

import createContext from 'auto-core/server/descript/createContext';
import publicApi from 'auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures';

import type { THttpResponse, THttpRequest } from 'auto-core/http';

import getGroupListingWithRating from './getGroupListingWithRating';

let context: ReturnType<typeof createContext>;
let req: THttpRequest;
let res: THttpResponse;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен обогатить выдачу листинга общей оценкой по отзывам', () => {
    publicApi
        .get('/1.0/search/cars')
        .query({
            category: 'cars',
            context: 'listing',
            group_by: 'CONFIGURATION',
            only_official: true,
            page_size: 37,
            page: 1,
            sort: 'fresh_relevance_1-desc',
            state_group: 'NEW',
        })
        .reply(200, {
            offers: [
                { id: '1-1', vehicle_info: { mark_info: { code: 'RENAULT' }, model_info: { code: 'DUSTER' }, super_gen: { id: 1 } } },
                { id: '2-2', vehicle_info: { mark_info: { code: 'RENAULT' }, model_info: { code: 'SANDERO' }, super_gen: { id: 2 } } },
                { id: '3-3', vehicle_info: { mark_info: { code: 'RENAULT' }, model_info: { code: 'LOGAN' }, super_gen: { id: 3 } } },
                { id: '4-4', vehicle_info: { mark_info: { code: 'RENAULT' }, model_info: { code: 'ARKANA' }, super_gen: { id: 4 } } },
            ],
        });

    publicApi
        .get('/1.0/reviews/auto/cars/rating')
        .reply(200, {
            ratings: [ { name: 'total', value: 4.14 } ],
        });
    publicApi
        .get('/1.0/reviews/auto/cars/rating')
        .reply(200, {
            ratings: [ { name: 'total', value: 5.0 } ],
        });
    publicApi
        .get('/1.0/reviews/auto/cars/rating')
        .reply(200, {
            ratings: [],
        });
    publicApi
        .get('/1.0/reviews/auto/cars/rating')
        .reply(200, {
            ratings: [ { name: 'total', value: 4.5 } ],
        });

    return de.run(getGroupListingWithRating, {
        context,
    }).then(res => {
        expect(res.offers[0].total_rating).toBe(4.14);
        expect(res.offers[1].total_rating).toBe(5.0);
        expect(res.offers[2].total_rating).toBe(undefined);
        expect(res.offers[3].total_rating).toBe(4.5);
    });
});
