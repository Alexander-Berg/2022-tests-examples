const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');

const getRichVinReport = require('./getRichVinReportUnsafe');

const publicApi = require('auto-core/server/resources/baseHttpBlockPublicApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');
const getCarfaxReportRawFixtures = require('auto-core/server/resources/publicApiCarfax/methods/getCarfaxReportRaw.fixtures');
const getCarfaxOfferReportRawFixtures = require('auto-core/server/resources/publicApiCarfax/methods/getCarfaxOfferReportRaw.fixtures');

const { nbsp } = require('auto-core/react/lib/html-entities');

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('должен вернуть 404, если не передали vin_or_license_plate или offerID', () => {
    const params = {};

    return de.run(getRichVinReport, { context, params }).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (result) => {
            expect(result).toMatchObject({
                error: {
                    id: 'NOT_FOUND',
                },
            });
        });
});

it('должен вернуть отчёт по параметру vin_or_license_plate, если он ответил', () => {
    publicApi
        .get('/1.0/carfax/report/raw')
        .query({
            vin_or_license_plate: 'SALWA2FK7HA135034',
        })
        .reply(200, getCarfaxReportRawFixtures.report());

    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
    };

    return de.run(getRichVinReport, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                fetched: true,
                report: {
                    header: {
                        title: 'Volkswagen Passat, 2012',
                    },
                },
            });
        });
});

it('должен собрать оглавление отчета из блоков', () => {
    publicApi
        .get('/1.0/carfax/report/raw')
        .query({
            vin_or_license_plate: 'SALWA2FK7HA135034',
        })
        .reply(200, getCarfaxReportRawFixtures.report());

    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
    };

    return de.run(getRichVinReport, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                report: {
                    content: {
                        items: [
                            {
                                type: 'pts_info',
                                key: 'Данные из ПТС',
                                status: 'OK',
                                value: 'Найдены характеристики',
                            },
                            {
                                type: 'autoru_offers',
                                key: 'Размещения на Авто.ру',
                                status: 'OK',
                                record_count: 3,
                                value: `3${ nbsp }объявления`,
                            },
                        ],
                    },
                },
            });
        });
});

it('должен вернуть отчёт по параметру offerID, если он ответил', () => {
    publicApi
        .get('/1.0/carfax/offer/cars/1092094796-1a8ac86c/raw')
        .reply(200, getCarfaxOfferReportRawFixtures.report());

    const params = {
        offerID: '1092094796-1a8ac86c',
    };

    return de.run(getRichVinReport, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                fetched: true,
                report: {
                    header: {
                        title: 'Mercedes-Benz GLA, 2014',
                    },
                },
            });
        });
});

it('должен вернуть ошибку VIN_CODE_NOT_FOUND, если не найден отчет', () => {
    publicApi
        .get('/1.0/carfax/report/raw')
        .query({
            vin_or_license_plate: 'SALWA2FK7HA135034',
        })
        .reply(404, getCarfaxReportRawFixtures.http404());

    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
    };

    return de.run(getRichVinReport, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                error: 'VIN_CODE_NOT_FOUND',
            });
        });
});

it('должен вернуть ошибку IN_PROGRESS, если отчет в процесс создания', () => {
    publicApi
        .get('/1.0/carfax/report/raw')
        .query({
            vin_or_license_plate: 'SALWA2FK7HA135034',
        })
        .times(3)
        .reply(202, getCarfaxReportRawFixtures.http202());

    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
    };

    return de.run(getRichVinReport, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                error: 'IN_PROGRESS',
            });
        });
}, 16000);

it('должен вернуть ошибку UNKNOWN_ERROR, если бекенд 500ит', () => {
    publicApi
        .get('/1.0/carfax/report/raw')
        .query({
            vin_or_license_plate: 'SALWA2FK7HA135034',
        })
        .times(2)
        .reply(500, getCarfaxReportRawFixtures.http500());

    const params = {
        vin_or_license_plate: 'SALWA2FK7HA135034',
    };

    return de.run(getRichVinReport, { context, params })
        .then((result) => {
            expect(result).toMatchObject({
                error: 'UNKNOWN_ERROR',
            });
        });
}, 11000);
