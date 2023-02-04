import * as request from 'request';
import {app} from '../../app/app';
import {createInfrastructure, InfraHandler, API_KEYS_SUCCESS_RESPONSE} from '../utils';
import * as nock from 'nock';
import * as Boom from '@hapi/boom';
import {ymapsmlXml, ymapsmlResponse} from '../fixtures/ymapsml';
import {kmlResponse, kmlXml} from '../fixtures/kml';
import {gpxResponse, gpxXml} from '../fixtures/gpx';
import {FILE_SIZE_LIMIT} from '../../app/middleware/zora';
import {IResponse, IRequestHeaders, curlRequest} from '../../app/lib/curl';

jest.mock('../../app/lib/curl');

const mockCurlResponse = (
    body: string,
    statusCode = 200,
    inspectHeaders?: (headers: IRequestHeaders) => void
): void => {
    (curlRequest as jest.Mock)
        .mockImplementation(async (url: string, headers: IRequestHeaders): Promise<IResponse> => {
            inspectHeaders && inspectHeaders(headers);
            return {statusCode, body: Buffer.from(body, 'utf-8'), headers: {}};
        });
};

const VALID_FILE_URL = 'https://yandex.ru/data.xml';

describe('app', () => {
    let handler: InfraHandler;
    beforeAll(async () => handler = await createInfrastructure(app));
    afterAll(async () => await handler.release());

    beforeEach(() => {
        nock.cleanAll();
        // Curl response is ymapsmlXml by default
        mockCurlResponse(ymapsmlXml);
    });

    it('handle /ping request', (done) => {
        request(`${handler.url}/ping`).on('response', (response) => {
            expect(response.statusCode).toEqual(200);
            done();
        });
    });

    it('handle gpx format', (done) => {
        handler.mockApiKeysToSuccess();
        mockCurlResponse(gpxXml);
        request(handler.getUrlForFile(VALID_FILE_URL).toString(), (_, response, body) => {
            expect(response.statusCode).toEqual(200);
            expect(JSON.parse(body)).toEqual(gpxResponse);
            done();
        });
    });

    it('handle kml format', (done) => {
        handler.mockApiKeysToSuccess();
        mockCurlResponse(kmlXml);
        request(handler.getUrlForFile(VALID_FILE_URL).toString(), (_, response, body) => {
            expect(response.statusCode).toEqual(200);
            expect(JSON.parse(body)).toEqual(kmlResponse);
            done();
        });
    });

    it('handle ymapsml format', (done) => {
        handler.mockApiKeysToSuccess();
        mockCurlResponse(ymapsmlXml);
        request(handler.getUrlForFile(VALID_FILE_URL).toString(), (_, response, body) => {
            expect(response.statusCode).toEqual(200);
            expect(JSON.parse(body)).toEqual(ymapsmlResponse);
            done();
        });
    });

    it('send tvm ticket to apikeys', (done) => {
        let ticket: string;
        nock(handler.intHosts.apikeysInt)
            .matchHeader('X-Ya-Service-Ticket', (val) => {
                ticket = val;
                return true;
            })
            .post('/v1/check', () => true)
            .reply(200, API_KEYS_SUCCESS_RESPONSE);

        request(handler.getUrlForFile(VALID_FILE_URL).toString(), () => {
            expect(ticket).not.toEqual(undefined);
            done();
        });
    });

    it('send tvm ticket to zora', (done) => {
        handler.mockApiKeysToSuccess();
        mockCurlResponse(ymapsmlXml, 200, (headers) => {
            expect(headers.proxyHeaders['X-Ya-Service-Ticket']).not.toEqual(undefined);
            done();
        });
        request(handler.getUrlForFile(VALID_FILE_URL).toString());
    });

    it('return 403 if apikey is invalid', (done) => {
        nock(handler.intHosts.apikeysInt)
            .post('/v1/check', ({key}) => key === '123')
            .reply(200, {ok: false, statusCode: 403, message: 'Invalid key'});

        const url = handler.getUrlForFile(VALID_FILE_URL);
        url.searchParams.set('apikey', '123');
        request(url.toString()).on('response', (response) => {
            expect(response.statusCode).toEqual(403);
            done();
        });
    });

    it('return 400 if url is invalid', (done) => {
        handler.mockApiKeysToSuccess();
        request(handler.getUrlForFile('invalidUrl').toString(), (_, response) => {
            expect(response.statusCode).toEqual(400);
            done();
        });
    });

    it('return 400 if connect was failed', (done) => {
        handler.mockApiKeysToSuccess();
        (curlRequest as jest.Mock)
            .mockImplementation(async () => {
                throw Boom.badRequest();
            });
        request(handler.getUrlForFile('https://invalid_url.com/some').toString(), (_, response) => {
            expect(response.statusCode).toEqual(400);
            done();
        });
    });

    it('return 408 if request ended in timeout', (done) => {
        handler.mockApiKeysToSuccess();
        (curlRequest as jest.Mock)
            .mockImplementation(async () => {
                throw Boom.clientTimeout('couldn\'t load file');
            });
        request(handler.getUrlForFile('https://timeout.com/some').toString(), (_, response) => {
            expect(response.statusCode).toEqual(408);
            done();
        });
    });

    it('return 400 if zora statusCode != 200', (done) => {
        handler.mockApiKeysToSuccess();
        mockCurlResponse(ymapsmlXml, 500);
        request(handler.getUrlForFile(VALID_FILE_URL).toString(), (_, response) => {
            expect(response.statusCode).toEqual(400);
            done();
        });
    });

    it('return 400 if url is not provided', (done) => {
        handler.mockApiKeysToSuccess();
        const url = handler.getUrlForFile('');
        url.searchParams.delete('url');
        request(url.toString(), (_, response) => {
            expect(response.statusCode).toEqual(400);
            done();
        });
    });

    it('return 413 if file is too large', (done) => {
        const bigFile = new Array(FILE_SIZE_LIMIT + 1).fill(0).join('');
        handler.mockApiKeysToSuccess();
        mockCurlResponse(bigFile);
        request(handler.getUrlForFile(VALID_FILE_URL).toString(), (_, response) => {
            expect(response.statusCode).toEqual(413);
            done();
        });
    });

    it('return 500 if request was failed', (done) => {
        handler.mockApiKeysToSuccess();
        (curlRequest as jest.Mock)
            .mockImplementation(async () => {
                throw new Error('Some error, curlCode=23, connectCode=500');
            });
        request(handler.getUrlForFile(VALID_FILE_URL).toString(), (_, response) => {
            expect(response.statusCode).toEqual(500);
            done();
        });
    });
});
