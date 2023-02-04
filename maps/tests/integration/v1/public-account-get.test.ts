import * as http from 'http';
import * as path from 'path';
import * as fs from 'fs';
import * as nock from 'nock';
import {extend, Response} from 'got';
import {OpenApiValidator} from 'express-openapi-validate';
import {app} from 'app/app';
import {config} from 'app/config';
import {hostConfigLoader} from 'app/lib/host-loader';
import {startServer, stopServer} from 'tests/integration/test-server';
import {
    publicAccountResponseRaw,
    publicAccountResponse
} from 'tests/integration/v1/data/public-account';
import {
    WRONG_SRC_SERVICE_TICKET,
    CORRECT_SRC_SERVICE_TICKET
} from 'tests/integration/tvm-tickets';

const specDoc = JSON.parse(fs.readFileSync(path.resolve('./out/generated/spec.json'), 'utf-8'));
const validator = new OpenApiValidator(specDoc);

const data = {
    enabledPublicAccountPseudonym: 'Qse_a1_q',
    disabledPublicAccountPseudonym: 'Qse_a1_z'
};

const defaultHeaders = {
    'X-Ya-Service-Ticket': CORRECT_SRC_SERVICE_TICKET
};

const defaultQuery = {
    publicId: data.enabledPublicAccountPseudonym,
    origin: 'test',
    lang: 'ru'
};

const client = extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 1500,
    json: true,
    headers: defaultHeaders,
    query: defaultQuery
});

describe('GET /v1/public_account', () => {
    let server: http.Server;
    let url: string;
    let ugcSearch: string;

    beforeAll(async () => {
        const hosts = await hostConfigLoader.get();
        ugcSearch = hosts.ugcSearch;

        [server, url] = await startServer(app);
        nock.disableNetConnect();
        nock.enableNetConnect(/localhost/);
    });

    afterAll(async () => {
        await stopServer(server);
        nock.enableNetConnect();
    });

    beforeEach(() => {
        nock(ugcSearch)
            .get(`/${data.enabledPublicAccountPseudonym}`)
            .query({
                ...config.ugcPublicAccountParams,
                tld: config.ugcTld,
                json: 1
            })
            .reply(200, publicAccountResponseRaw);

        nock(ugcSearch)
            .get(`/${data.disabledPublicAccountPseudonym}`)
            .query({
                ...config.ugcPublicAccountParams,
                tld: config.ugcTld,
                json: 1
            })
            .reply(200, {});
    });

    afterEach(() => {
        nock.cleanAll();
    });

    describe('response validation', () => {
        it('should validate success response with enabled public account', async () => {
            const response = await client.get(`${url}/v1/public_account`);

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(publicAccountResponse.enable);
            validateResponseBySchema(response);
        });

        it('should validate success response with disabled public account', async () => {
            const response = await client.get(`${url}/v1/public_account`, {
                query: {
                    publicId: data.disabledPublicAccountPseudonym
                }
            });

            expect(response.statusCode).toEqual(200);
            expect(response.body).toEqual(publicAccountResponse.disable);
            validateResponseBySchema(response);
        });
    });

    describe('parameters checking', () => {
        it('should return 403 if "X-Ya-Service-Ticket" header is wrong', async () => {
            const response = await client.get(`${url}/v1/public_account`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': WRONG_SRC_SERVICE_TICKET
                }
            });
            expect(response.statusCode).toEqual(403);
            validateResponseBySchema(response);
        });

        it('should return 401 if "X-Ya-Service-Ticket" header is missed', async () => {
            const response = await client.get(`${url}/v1/public_account`, {
                headers: {
                    ...defaultHeaders,
                    'X-Ya-Service-Ticket': ''
                }
            });
            expect(response.statusCode).toEqual(401);
            validateResponseBySchema(response);
        });

        it('should return 400 if "publicId" parameter is missed', async () => {
            const response = await client.get(`${url}/v1/public_account`, {
                query: {
                    origin: 'test',
                    publicId: ''
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });

        it('should return 400 if "origin" parameter is missed', async () => {
            const response = await client.get(`${url}/v1/public_account`, {
                query: {
                    publicId: data.enabledPublicAccountPseudonym,
                    origin: ''
                }
            });
            expect(response.statusCode).toEqual(400);
            validateResponseBySchema(response);
        });
    });
});

function validateResponseBySchema(response: Response<any>) {
    const validateResponse = validator.validateResponse('get', '/v1/public_account');
    validateResponse(response);
}
