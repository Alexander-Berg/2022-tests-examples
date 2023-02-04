const _ = require('lodash');
const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const fixturesGetGarageCard = require('auto-core/server/resources/publicApiGarage/methods/getCard.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const method = require('./getRichGarageCard');

let goodResponseWithoutEmptyFields;
let context;
let req;
let res;
let params;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });

    params = { card_id: 123 };
    goodResponseWithoutEmptyFields = fixturesGetGarageCard.response200();
    goodResponseWithoutEmptyFields.card = _.omitBy(goodResponseWithoutEmptyFields.card, _.isEmpty);
});

it('должен отдать карточку без отзывов, если они не ответили', async() => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .reply(200, fixturesGetGarageCard.response200());

    await expect(
        de.run(method, { context, params }),
    ).resolves.toMatchObject(goodResponseWithoutEmptyFields);
});

it('должен пробросить ошибку', async() => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .reply(200, fixturesGetGarageCard.response200Deleted());

    await expect(
        de.run(method, { context, params }),
    ).resolves.toEqual({ error: 'CARD_NOT_FOUND', status: 'ERROR' });
});

it('должен отдать ошибку, если карточка 500ит', async() => {
    publicApi
        .get(`/1.0/garage/user/card/123`)
        .times(2)
        .reply(500, fixturesGetGarageCard.response500());

    await expect(
        de.run(method, { context, params }),
    ).rejects.toMatchObject({
        error: {
            id: 'REQUIRED_BLOCK_FAILED',
            path: '.garageCard',
        },
    });
});

describe('отзывы', () => {
    beforeEach(() => {
        publicApi
            .get(`/1.0/garage/user/card/123`)
            .reply(200, fixturesGetGarageCard.response200());
    });

    it('должен добавить отзывы, если они есть', async() => {
        publicApi
            .get('/1.0/reviews/auto/cars/counter?mark=HYUNDAI&model=ELANTRA&super_gen=3483538')
            .reply(200, { count: 142, status: 'SUCCESS' });

        publicApi
            .get('/1.0/reviews/auto/features/CARS?subject=auto&mark=HYUNDAI&model=ELANTRA&super_gen=3483538')
            .reply(200, { positive: [], negative: [], controversy: [], status: 'SUCCESS' });

        const expected = {
            ..._.cloneDeep(goodResponseWithoutEmptyFields),
        };
        expected.card.richReviews = {
            features: { positive: [], negative: [], controversy: [] },
            summary: { averageRating: 0, totalCount: 142, reviews: [] },
            resourceParams: { category: 'cars', mark: 'HYUNDAI', model: 'ELANTRA', super_gen: '3483538' },
        };

        await expect(
            de.run(method, { context, params }),
        ).resolves.toMatchObject(expected);
    });

    it('не должен добавить отзывы, если их нет', async() => {
        publicApi
            .get('/1.0/reviews/auto/cars/counter?mark=HYUNDAI&model=ELANTRA&super_gen=3483538')
            .reply(200, { count: 0, status: 'SUCCESS' });

        publicApi
            .get('/1.0/reviews/auto/features/CARS?subject=auto&mark=HYUNDAI&model=ELANTRA&super_gen=3483538')
            .reply(200, { positive: [], negative: [], controversy: [], status: 'SUCCESS' });

        await expect(
            de.run(method, { context, params }),
        ).resolves.toMatchObject(goodResponseWithoutEmptyFields);
    });
});
