const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const historyByVin = require('./proauto-report');

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

it('должен выбросить ошибку 404 если нет vin_or_license_plate или offer_id', () => {
    const params = { history_entity_id: 'JF1GT7LL5JG008055' };

    return de.run(historyByVin, { context, params }).then(
        () => Promise.reject('ENEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NOT_FOUND',
                    status_code: 404,
                },
            });
        },
    );
});
