/**
 * @jest-environment node
 */
const calls = require('./calls');

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

const tariffsMock = [
    {
        category: 'CARS',
        type: 'CALLS',
        enabled: true,
        section: [
            'NEW',
        ],
        placement_prices_info: [
            {
                placement_price: 0,
            },
        ],
        service_prices: [
            {
                service: 'premium',
                offers_count: 0,
                price: 500,
                days: 1,
                prolongable: true,
            },
            {
                service: 'boost',
                offers_count: 0,
                price: 250,
                prolongable: false,
            },
            {
                service: 'badge',
                offers_count: 0,
                price: 20,
                badges_count: 0,
                days: 1,
                prolongable: true,
            },
        ],
        product: 'call',
        calls: {
            price: 5400,
            deposit_coefficient: 0,
        },
        priority_placement: 'ENABLED',
    },
];

const getCallsWithDeps = (tariffs) => de.func({
    block: (args) => {
        const { blocks, ids } = createDeps({ tariffs }, args);

        return de.pipe({
            block: [
                blocks.tariffs,
                calls(ids),
            ],
        });
    },
});

it('должен вызывать редирект на страницу настроек, если не подключен коллтрекинг и нет звонкового тарифа', async() => {
    setCallsMocks();

    const clientId = '20101';
    context.req.router.params = { client_id: clientId };

    await expect(
        de.run(getCallsWithDeps([]), { context }),
    ).rejects.toMatchObject({
        error: {
            id: 'REDIRECTED',
            code: 'CABINET_TO_CALLTRACKING_SETTINGS',
            location: '/calls/settings/?client_id=' + clientId,
            status_code: 302,
        },
    });
});

it('должен сгвардить запрос callsTotalStatsMultiposting, если у клиента выключен мультипостинг', () => {
    setCallsMocks2({
        multipostingEnabled: false,
        calltrackingClassifiedsEnabled: true,
    });

    const clientId = '20101';
    context.req.router.params = { client_id: clientId };

    return de.run(getCallsWithDeps(tariffsMock), { context })
        .then(
            (result) => {
                expect(result.callsTotalStatsMultiposting.error.id).toBe('BLOCK_GUARDED');
            },
        );
});

it('должен сгвардить запрос callsTotalStatsMultiposting, если у клиента включен мультипостинг, но выключена настройка "Подменные номера на другие сайты"',
    () => {
        setCallsMocks2({
            multipostingEnabled: true,
            calltrackingClassifiedsEnabled: false,
        });

        const clientId = '20101';
        context.req.router.params = { client_id: clientId };

        return de.run(getCallsWithDeps(tariffsMock), { context })
            .then(
                (result) => {
                    expect(result.callsTotalStatsMultiposting.error.id).toBe('BLOCK_GUARDED');
                },
            );
    });

function setCallsMocks() {
    publicApi
        .post('/1.0/calltracking')
        .reply(200, { calls: [], pagination: {}, request: {} });

    publicApi
        .post('/1.0/calltracking/aggregated')
        .reply(200, {});

    publicApi
        .get('/1.0/dealer/tariff')
        .reply(200, { tariffs: [] });

    publicApi
        .get('/1.0/calltracking/settings')
        .reply(200, { settings: { calltracking_enabled: false } });
}

function setCallsMocks2({
    calltrackingClassifiedsEnabled,
    multipostingEnabled,
}) {
    publicApi
        .get('/1.0/dealer/info')
        .reply(200, { multiposting_enabled: multipostingEnabled });

    publicApi
        .post('/1.0/calltracking')
        .reply(200, { calls: [], pagination: {}, request: {} });

    publicApi
        .post('/1.0/calltracking/aggregated')
        .reply(200, {});

    publicApi
        .get('/1.0/calltracking/settings')
        .reply(200, { settings: { calltracking_classifieds_enabled: calltrackingClassifiedsEnabled } });
}
