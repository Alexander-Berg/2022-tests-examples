const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const block = require('./getVinReportFull');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

let context;
let req;
let res;

const HISTORY_ENTITY_ID = 'SOME_ID';

const OFFER_VIN_REPORT_RESULT = {
    report: 'Yay! getOfferVinReportRaw',
    status: 'SUCCESS',
};

const ORDER_VIN_REPORT_RESULT = {
    error: 'HTTP_403',
    errorBody: 'Report is empty',
};

const CARFAX_VIN_REPORT_RESULT = {
    report: 'Yay! getCarfaxReportRaw',
    status: 'SUCCESS',
};

beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен сходить в getOfferVinReportRaw, если есть offerID', () => {
    publicApi
        .get(`/1.0/carfax/offer/cars/${ HISTORY_ENTITY_ID }/raw`)
        .reply(200, OFFER_VIN_REPORT_RESULT);

    return de.run(block, { context, params: { offerID: HISTORY_ENTITY_ID } })
        .then((result) => {
            expect(result).toEqual(OFFER_VIN_REPORT_RESULT);
        });
});

it('должен сходить в getOrderVinReportRaw, если есть order_id', () => {
    publicApi
        .get(`/1.0/carfax/orders/result?order_id=${ HISTORY_ENTITY_ID }`)
        .reply(200, ORDER_VIN_REPORT_RESULT);

    return de.run(block, { context, params: { order_id: HISTORY_ENTITY_ID } })
        .then((result) => {
            expect(result).toEqual(ORDER_VIN_REPORT_RESULT);
        });
});

it('должен сходить в getCarfaxReportRaw, если есть vin_or_license_plate', () => {
    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=SOME_ID')
        .reply(200, CARFAX_VIN_REPORT_RESULT);

    return de.run(block, { context, params: { vin_or_license_plate: HISTORY_ENTITY_ID } })
        .then((result) => {
            expect(result).toEqual(CARFAX_VIN_REPORT_RESULT);
        });
});
