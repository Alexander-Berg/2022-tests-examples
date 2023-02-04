import * as http from 'http';
import * as got from 'got';
import {ACCEPTED_ENCODING} from '../../app/lib/proto';

import {app} from '../../app/app';
import {waitForPendingMocks} from './nock-utils';

const PORT = 8090;
const BASE_URL = `http://localhost:${PORT}/`;

export function prepareServer(): void {
    let server: http.Server;

    beforeAll(async () => {
        server = http.createServer(app);
        await new Promise((resolve) => server.listen(PORT, resolve));
    });

    afterAll(() => {
        server.close((err) => {
            if (err) {
                throw new Error(`Error on stopping server: ${String(err)}`);
            }
        });
    });
}

export const protoClient = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    prefixUrl: BASE_URL,
    headers: {
        'Content-Type': ACCEPTED_ENCODING
    },
    responseType: 'buffer'
});

export const jsonClient = got.default.extend({
    throwHttpErrors: false,
    retry: 0,
    timeout: 2000,
    prefixUrl: BASE_URL,
    headers: {
        'Content-Type': 'application/json'
    },
    responseType: 'json'
});

type RequestOptions = {
    expectedPendingMocks?: number;
};

export type RequestGenerator<Req, Res> = (data: Req, options?: RequestOptions) => Promise<Res>;

export function makeRequestGenerator<Req, Res, BodyType = string>(
    makeRequest: (data: Req) => got.CancelableRequest<got.Response<BodyType>>,
    parseResponseData: (response: got.Response<BodyType>) => Res
): RequestGenerator<Req, Res> {
    return async (data, options = {}) => {
        const result = await makeRequest(data);
        await waitForPendingMocks(options.expectedPendingMocks);
        return parseResponseData(result);
    };
}
