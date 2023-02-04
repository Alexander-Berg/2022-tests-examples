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

it('должен вернуть пустой список вьюх если версия аппа меньше чем 9.8.0', async() => {
    req.headers['x-android-app-version'] = '9.6.1';

    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=В573СН76')
        .reply(200, fixtures.response200Free());

    return androidMakeXmlForReport({ vin_or_license_plate: 'В573СН76' }, { req, res }, { responseType: 'JSON' })
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть не пустой список вьюх если версия аппа равна 9.8.0', async() => {
    req.headers['x-android-app-version'] = '9.8.0';

    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=В573СН76')
        .reply(200, fixtures.response200Free());

    return androidMakeXmlForReport({ vin_or_license_plate: 'В573СН76' }, { req, res }, { responseType: 'JSON' })
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});
