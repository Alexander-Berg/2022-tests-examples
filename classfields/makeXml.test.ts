import type { Request, Response } from 'express';

import de from 'descript';
import { makeXmlMiddleware } from './makeXml';

import createHttpReq from 'mocks/createHttpReq';
import createHttpRes from 'mocks/createHttpRes';

import publicApi from 'app/server/resources/baseHttpBlockPublicApi.nock.fixtures';
import { getPaidReport } from 'mocks/vinReport/paidReport';

let req: Request<any>;
let res: Response;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
});

it('должен вернуть 200, если метод отработал', async() => {
    publicApi
        .get('/1.0/carfax/offer/cars/123-abc/raw')
        .reply(200, getPaidReport());

    req.params = { platform: 'ios', method: 'makeXmlForOffer' };
    req.query = { offer_id: '123-abc' };
    makeXmlMiddleware(req, res);
    await new Promise((resolve) => setTimeout(resolve, 200));

    expect(res.status).toHaveBeenCalledTimes(1);
    expect(res.status).toHaveBeenCalledWith(200);

    expect(res.setHeader).toHaveBeenCalledTimes(2);
    expect(res.setHeader).toHaveBeenCalledWith('content-type', 'application/json; charset=utf-8');
    expect(res.setHeader).toHaveBeenCalledWith('x-proto-name', 'auto.api.ReportLayoutResponse');

    expect(res.send).toHaveBeenCalledTimes(1);
});

describe('обработка ошибок', () => {
    it('должен вернуть 404 на неизвестный метод', async() => {
        req.params = { platform: 'ios', method: 'fooBar' };
        req.query = { offer_id: '123-abc' };
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 200));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(404);
    });

    it('должен вернуть 404, если метод вернул HTTP_404', async() => {
        publicApi
            .get('/1.0/carfax/offer/cars/123-abc/raw')
            .reply(404);

        req.params = { platform: 'ios', method: 'makeXmlForOffer' };
        req.query = { offer_id: '123-abc' };
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 200));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(404);
        expect(res.setHeader).toHaveBeenCalledWith('content-type', 'application/json');
    });

    it('должен вернуть 406, если запрашивают protobuf у метода без поддержки такого ответа', async() => {
        req.params = { platform: 'android', method: 'makeXmlForOffer' };
        req.query = { offer_id: '123-abc' };
        req.headers = {
            accept: 'application/protobuf',
        };
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 200));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(406);

        expect(res.end).toHaveBeenCalledWith('protobuf is not supported');
    });

    it('должен вернуть 500, если метод вернул HTTP_500', async() => {
        publicApi
            .get('/1.0/carfax/offer/cars/123-abc/raw')
            .times(2)
            .reply(500);

        req.params = { platform: 'ios', method: 'makeXmlForOffer' };
        req.query = { offer_id: '123-abc' };
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 500));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(500);
        expect(res.setHeader).toHaveBeenCalledWith('content-type', 'application/json');
    });

    it('должен вернуть 504, если метод вернул REQUEST_TIMEOUT', async() => {
        publicApi
            .get('/1.0/carfax/offer/cars/123-abc/raw')
            .times(2)
            .delayConnection(2500)
            .reply(504);

        req.params = { platform: 'ios', method: 'makeXmlForOffer' };
        req.query = { offer_id: '123-abc' };
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 5000));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(504);
        expect(res.end).toHaveBeenCalledWith(de.ERROR_ID.REQUEST_TIMEOUT);
    });

    it('должен вернуть 520 в случае js-ошибки (1)', async() => {
        req.params = { platform: 'test', method: 'methodWithJsError' };
        req.query = {};
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 500));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(520);
    });

    it('должен вернуть 400, если метод вернул BLOCK_GUARDED', async() => {
        req.params = { platform: 'ios', method: 'makeXmlForOffer' };
        req.query = {};
        makeXmlMiddleware(req, res);
        await new Promise((resolve) => setTimeout(resolve, 200));

        expect(res.status).toHaveBeenCalledTimes(1);
        expect(res.status).toHaveBeenCalledWith(400);

        expect(res.end).toHaveBeenCalledWith('"offer_id" is required');
    });
});
