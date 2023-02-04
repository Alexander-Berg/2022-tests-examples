const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const adSellerTarget = require('./adSellerTarget');

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

it('должен обработать и вернуть пустой результат для неавторизованного пользователя', () => {
    publicApi.get('/1.0/targeting/cars/ad-seller-target').reply(401, {
        error: 'NO_AUTH',
        status: 'ERROR',
        detailed_error: 'Expected private user. But AnonymousUser. Provide valid session_id',
    });

    return de.run(adSellerTarget, {
        context,
        params: { client_id: 1 },
    }).then(result => {
        expect(result).toEqual({
            count: 0,
            status: 'SUCCESS',
        });
    });
});

it('должен вернуть offer для авторизованного пользователя с активных объявлением', () => {
    publicApi.get('/1.0/targeting/cars/ad-seller-target').reply(200, require('./adSellerTarget.auth.mock'));

    return de.run(adSellerTarget, {
        context,
        params: { client_id: 1 },
    }).then(result => {
        expect(result).toMatchObject({
            count: 1,
            offer: {
                vehicle_info: {
                    mark_info: {
                        code: 'BMW',
                    },
                },
            },
            status: 'SUCCESS',
        });
    });
});
