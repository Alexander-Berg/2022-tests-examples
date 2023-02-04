const de = require('descript');
const mockdate = require('mockdate');
const nock = require('nock');
const { DAY, HOUR } = require('auto-core/lib/consts');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const getStats = require('./getStats');

let context;
let req;
let res;
beforeEach(() => {
    mockdate.set('2020-06-18');
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

afterEach(() => {
    mockdate.reset();
});

it('должен отправить запрос с from="сегодня", to="сегодня", если разместили сегодня', () => {
    const params = {
        category: 'trucks',
        offerId: '123-abc',
        creationDate: String(Date.now()),
    };

    publicApi
        .get(`/1.0/user/offers/trucks/123-abc/stats?from=2020-06-18&to=2020-06-18`)
        .reply(200, { status: 'SUCCESS' });

    return de.run(getStats, {
        context,
        params,
    })
        .then((result) => {
            expect(nock.isDone()).toEqual(true);
            expect(result).toEqual({ status: 'SUCCESS' });
        });
});

it('должен отправить запрос с from="вчера", to="сегодня", если разместили вчера но прошло меньше 24 часов', () => {
    mockdate.set('2020-06-18T10:00:00Z');

    const params = {
        category: 'trucks',
        offerId: '123-abc',
        creationDate: String(Date.now() - 15 * HOUR),
    };

    publicApi
        .get(`/1.0/user/offers/trucks/123-abc/stats?from=2020-06-17&to=2020-06-18`)
        .reply(200, { status: 'SUCCESS' });

    return de.run(getStats, {
        context,
        params,
    })
        .then((result) => {
            expect(nock.isDone()).toEqual(true);
            expect(result).toEqual({ status: 'SUCCESS' });
        });
});

it('должен отправить запрос с from="10 дней назад", to="сегодня", если разместили 10 дней назад', () => {
    const params = {
        category: 'trucks',
        offerId: '123-abc',
        creationDate: String(Date.now() - 10 * DAY),
    };

    publicApi
        .get(`/1.0/user/offers/trucks/123-abc/stats?from=2020-06-08&to=2020-06-18`)
        .reply(200, { status: 'SUCCESS' });

    return de.run(getStats, {
        context,
        params,
    })
        .then((result) => {
            expect(nock.isDone()).toEqual(true);
            expect(result).toEqual({ status: 'SUCCESS' });
        });
});

it('должен отправить запрос с from="25 дней назад", to="сегодня", если разместили 25 дней назад', () => {
    const params = {
        category: 'trucks',
        offerId: '123-abc',
        creationDate: String(Date.now() - 25 * DAY),
    };

    publicApi
        .get(`/1.0/user/offers/trucks/123-abc/stats?from=2020-05-24&to=2020-06-18`)
        .reply(200, { status: 'SUCCESS' });

    return de.run(getStats, {
        context,
        params,
    })
        .then((result) => {
            expect(nock.isDone()).toEqual(true);
            expect(result).toEqual({ status: 'SUCCESS' });
        });
});

it('должен отправить запрос с from="26 дней назад", to="сегодня", если разместили 27 дней назад', () => {
    const params = {
        category: 'trucks',
        offerId: '123-abc',
        creationDate: String(Date.now() - 27 * DAY),
    };

    publicApi
        .get(`/1.0/user/offers/trucks/123-abc/stats?from=2020-05-23&to=2020-06-18`)
        .reply(200, { status: 'SUCCESS' });

    return de.run(getStats, {
        context,
        params,
    })
        .then((result) => {
            expect(nock.isDone()).toEqual(true);
            expect(result).toEqual({ status: 'SUCCESS' });
        });
});
