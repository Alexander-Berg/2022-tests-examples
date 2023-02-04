import type { Request, Response } from 'express';

import MockDate from 'mockdate';
import publicApi from 'app/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import * as fixtures from 'app/server/resources/publicApi/methods/getCarfaxOfferReportRaw.fixtures';
import createHttpReq from 'mocks/createHttpReq';
import createHttpRes from 'mocks/createHttpRes';
import { formatXML } from 'mocks/utils';

import { androidMakeXmlForSearch } from 'app/server/routes/makeXml/android/makeXmlForSearch';

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

it('должен вернуть xml для не купленного отчета на экране поиска', async() => {
    publicApi
        .get('/1.0/carfax/offer/cars/12345-abc/raw')
        .reply(200, fixtures.response200Free());

    return androidMakeXmlForSearch({ offer_id: '12345-abc' }, { req, res }, { responseType: 'JSON' })
        .then((result) => {
            result.body.report.layout = formatXML(result.body.report.layout);
            expect(result).toMatchSnapshot();
        });
});
