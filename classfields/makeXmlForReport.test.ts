import type { Request, Response } from 'express';

import MockDate from 'mockdate';
import publicApi from 'app/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import * as fixtures from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import createHttpReq from 'mocks/createHttpReq';
import createHttpRes from 'mocks/createHttpRes';
import { formatXML } from 'mocks/utils';

import { androidMakeXmlForReport } from '../makeXmlForReport';

let req: Request<any>;
let res: Response;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();

    MockDate.set('2021-04-30');
    jest.spyOn(Math, 'random').mockReturnValue(0);
});

afterEach(() => {
    MockDate.reset();
});

it('должен вернуть набор вьюх для VIN Z8T4DNFUCDM014995', async() => {
    req.headers['x-android-app-version'] = '9.8.0';

    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=Z8T4DNFUCDM014995')
        .reply(200, fixtures.response200ForVin('Z8T4DNFUCDM014995'));

    return androidMakeXmlForReport(
        { vin_or_license_plate: 'Z8T4DNFUCDM014995' },
        { req, res }, { responseType: 'JSON' },
    )
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть набор вьюх для VIN Z94K241BAKR092916', async() => {
    req.headers['x-android-app-version'] = '9.8.0';

    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=Z94K241BAKR092916')
        .reply(200, fixtures.response200ForVin('Z94K241BAKR092916'));

    return androidMakeXmlForReport(
        { vin_or_license_plate: 'Z94K241BAKR092916' },
        { req, res }, { responseType: 'JSON' },
    )
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть набор вьюх для VIN XTA21154094778989', async() => {
    req.headers['x-android-app-version'] = '9.8.0';

    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=XTA21154094778989')
        .reply(200, fixtures.response200ForVin('XTA21154094778989'));

    return androidMakeXmlForReport(
        { vin_or_license_plate: 'XTA21154094778989' },
        { req, res }, { responseType: 'JSON' },
    )
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});
