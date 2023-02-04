import {URL, URLSearchParams} from 'url';
import * as got from 'got';
import {expect} from 'chai';
import {app} from 'app/app';
import * as sr from 'app/schemas/search/v1/response';
import {TestServer} from 'tests/utils/test-server';
import {TvmDaemon} from 'tests/utils/tvm-daemon';
import {nockUtils} from 'tests/utils/nock-utils';
import {config} from 'tests/config';

describe('search/v1', () => {
    let testServer: TestServer;
    let tvmDaemon: TvmDaemon;

    before(async () => {
        [testServer, tvmDaemon] = await Promise.all([
            TestServer.start(app),
            TvmDaemon.start()
        ]);

        await nockUtils.beforeAll(__filename);
    });

    after(async () => {
        await Promise.all([
            testServer.stop(),
            tvmDaemon.stop()
        ]);

        await nockUtils.afterAll(__filename);
    });

    // found is optional by documentation
    // however, some clients will break if found is removed
    it('should have found in response', async () => {
        const searchParams = new URLSearchParams({
            text: 'cafe',
            lang: 'ru_RU',
            apikey: config.apikey
        });

        const url = new URL('search/v1/', testServer.url);
        url.search = searchParams.toString();
        const localResult: got.Response<Object> | undefined = await got.default(url, {responseType: 'json'});
        const response = localResult.body as sr.FeatureCollection;
        const found = response!.properties!.ResponseMetaData!.SearchResponse!.found;
        expect(found).greaterThan(0);
    });

    it('should respond with application/javascript for jsonp', async () => {
        const searchParams = new URLSearchParams({
            text: 'cafe',
            lang: 'ru_RU',
            apikey: config.apikey,
            callback: 'callback'
        });

        const url = new URL('search/v1/', testServer.url);
        url.search = searchParams.toString();
        const localResult: got.Response<string> | undefined = await got.default(url);
        expect(localResult.headers['content-type']).contains('text/javascript');
    });
});
