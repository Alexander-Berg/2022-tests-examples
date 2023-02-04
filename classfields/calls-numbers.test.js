/**
 * @jest-environment node
 */
const callsNumbers = require('./calls-numbers');

const de = require('descript');

const createContext = require('auto-core/server/descript/createContext');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const createDeps = require('autoru-frontend/mocks/createDeps');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

const callsNumbersWithDeps = de.func({
    block: (args) => {
        const { blocks, ids } = createDeps({ tariffs: [] }, args);

        return de.pipe({
            block: [
                blocks.tariffs,
                callsNumbers(ids),
            ],
        });
    },
});

it('должен вызвать правильный набор блоков', () => {
    setMocks();

    publicApi
        .get('/1.0/calltracking/settings')
        .reply(200, { settings: { calltracking_enabled: true } });

    return de.run(callsNumbersWithDeps, { context })
        .then((result) => {
            expect(result).toMatchSnapshot();
        });
});

it('должен вызывать редирект на страницу настроек, если не подключен коллтрекинг и нет звонкового тарифа', async() => {
    setMocks();

    publicApi
        .get('/1.0/calltracking/settings')
        .reply(200, { settings: { calltracking_enabled: false } });

    const clientId = '20101';
    context.req.router.params = { client_id: clientId };

    await expect(
        de.run(callsNumbersWithDeps, { context }),
    ).rejects.toMatchObject({
        error: {
            code: 'CABINET_TO_CALLTRACKING_SETTINGS',
            id: 'REDIRECTED',
            location: '/calls/settings/?client_id=20101',
            status_code: 302,
        },
    });
});

function setMocks() {
    publicApi
        .get('/1.0/dealer/phones/redirects')
        .query({ page: 1, page_size: 10, with_offers: true })
        .reply(200, { phones_with_offers: [], pagination: {} });

    publicApi
        .get('/1.0/dealer/phones/redirects')
        .query({ with_offers: true, confirmed_status: 'unconfirmed' })
        .reply(200, { phones_with_offers: [] });
}
