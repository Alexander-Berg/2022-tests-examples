const de = require('descript');

const method = require('./searchHistory');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');
const searchHistoryFixtures = require('./searchHistory.nock.fixtires');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен пересортировать список по убыванию по дате создания', () => {
    publicApi.get('/1.0/search/history').reply(200, searchHistoryFixtures.listWithBadOrder());

    return de.run(method, { context }).then(result => {
        const list = result.saved_searches.map((item) => item.creation_timestamp);
        expect(list).toEqual([
            '2020-09-21T17:11:49.121Z',
            '2020-09-21T17:11:38.289Z',
            '2020-09-21T17:11:35.477Z',
            '2020-09-21T09:04:44.099Z',
            '2020-09-20T02:29:43.597Z',
            '2020-09-20T02:26:41.744Z',
            '2020-09-20T02:18:29.692Z',
            '2020-09-20T02:18:23.982Z',
            '2020-09-19T09:22:01.048Z',
            '2020-09-19T06:23:30.333Z',
            '2020-09-19T06:23:26.114Z',
            '2020-09-17T17:28:28.540Z',
        ]);
    });
});
