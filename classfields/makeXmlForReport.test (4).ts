import type { Request, Response } from 'express';

import MockDate from 'mockdate';
import publicApi from 'app/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import * as fixtures from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import createHttpReq from 'mocks/createHttpReq';
import createHttpRes from 'mocks/createHttpRes';

import { iosMakeXmlForReport } from './makeXmlForReport';
import nock from 'nock';
import { formatXML } from 'mocks/utils';
import { getListing } from 'mocks/reviews/reviewsListing';
import { toCamel } from 'snake-camel';

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

it('должен вернуть карточку без отзывов', async() => {
    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=В573СН76')
        .reply(200, fixtures.response200Paid());

    publicApi
        .get('/1.0/reviews/auto/cars/counter?mark=VOLVO&model=S60&super_gen=2309989')
        .times(2)
        .reply(500);

    publicApi
        .get('/1.0/reviews/auto/cars/rating?mark=VOLVO&model=S60&super_gen=2309989')
        .times(2)
        .reply(500);

    return iosMakeXmlForReport({ vin_or_license_plate: 'В573СН76' }, { req, res }, { responseType: 'JSON' })
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});

it('должен вернуть карточку c отзывами', async() => {
    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=В573СН76')
        .reply(200, fixtures.response200Paid());

    publicApi
        .get('/1.0/reviews/auto/cars/counter?mark=VOLVO&model=S60&super_gen=2309989')
        .reply(200, { count: 200 });

    publicApi
        .get('/1.0/reviews/auto/cars/rating?mark=VOLVO&model=S60&super_gen=2309989')
        .reply(200, { ratings: [ { value: 4.6, name: 'total' } ] });

    publicApi
        .get('/1.0/reviews/auto/listing?category=CARS&mark=VOLVO&model=S60&page=1&page_size=10&photo=true&sort=relevance-desc&super_gen=2309989')
        .reply(200, toCamel(getListing()));

    return iosMakeXmlForReport({ vin_or_license_plate: 'В573СН76' }, { req, res }, { responseType: 'JSON' })
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});

it('должен прокинуть decrement_quota d запрос', async() => {
    publicApi
        .get('/1.0/carfax/report/raw?vin_or_license_plate=В573СН76&decrement_quota=true')
        .reply(200, fixtures.response200Paid());

    return iosMakeXmlForReport({ vin_or_license_plate: 'В573СН76', decrement_quota: 'true' }, { req, res }, { responseType: 'JSON' })
        .then(() => {
            expect(nock.isDone()).toEqual(true);
        });
});
