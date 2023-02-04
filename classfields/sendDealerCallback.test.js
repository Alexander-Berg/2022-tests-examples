const de = require('descript');
const nock = require('nock');
const createContext = require('auto-core/server/descript/createContext');

const mockGetBunkerDict = jest.fn(() => {
    return {};
});

jest.mock('auto-core/lib/util/getBunkerDict', () => mockGetBunkerDict);

const sendDealerCallback = require('./sendDealerCallback');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен отправить запрос с нужными параметрами', () => {
    const params = {
        category: 'cars',
        offerIdHash: '23456',
        phone: '+78762223424',
        name: 'my name',
        booking_call: 'booking_call',
        provider: 'CALLKEEPER',
    };

    publicApi
        .post(
            `/1.0/offer/${ params.category }/${ params.offerIdHash }/register-callback`,
            {
                phone: params.phone,
                booking_call: params.booking_call,
                provider: 'CALLKEEPER',
                payload: {},
            },
        )
        .reply(200, {
            status: 'SUCCESS',
        });

    return de.run(sendDealerCallback, {
        context,
        params,
    })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});

it('при наличии utm-меток и данных uaas добавит их в тело запроса', () => {
    const params = {
        category: 'cars',
        offerIdHash: '23456',
        phone: '+78762223424',
        booking_call: 'booking_call',
    };

    req.experimentsData.uaas = {
        expboxes: [ '1', '2' ],
    };

    req.cookies.salon_phone_utms = 'utm_medium=20&utm_source=foo&utm_campaign=&utm_content=';

    publicApi
        .post(
            `/1.0/offer/${ params.category }/${ params.offerIdHash }/register-callback`,
            {
                phone: params.phone,
                booking_call: params.booking_call,
                payload: {
                    expboxes: JSON.stringify([ '1', '2' ]),
                    utm_medium: '20',
                    utm_source: 'foo',
                },
            },
        )
        .reply(200, {
            status: 'SUCCESS',
        });

    return de.run(sendDealerCallback, {
        context,
        params,
    })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});
