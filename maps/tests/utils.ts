import * as express from 'express';
import * as http from 'http';
import {TvmDaemon} from './tvm-daemon';
import {AddressInfo} from 'net';
import {createIntHostConfigLoader, Hosts} from '@yandex-int/maps-host-configs';
import {env} from 'app/lib/env';
import {config} from 'app/config';
import * as nock from 'nock';

export type InfraHandler = {
    url: string,
    intHosts: Hosts,
    getUrlForFile: (fileUrl: string) => URL,
    mockApiKeysToSuccess: () => void,
    release: () => Promise<void>
};

const intHostConfigLoader = createIntHostConfigLoader({
    env,
    basePath: config['inthosts.configPath']
});

export const API_KEYS_SUCCESS_RESPONSE = {ok: true, tariff: 'apimaps_free'};

export async function createInfrastructure(app: express.Application): Promise<InfraHandler> {
    const server = http.createServer(app);
    const tvmDaemon = await TvmDaemon.start();
    const url = await new Promise<string>((resolve) => {
        server.listen(() => resolve(`http://127.0.0.1:${(server.address() as AddressInfo).port}`));
    });
    const intHosts = await intHostConfigLoader.get();
    const getUrlForFile = (fileUrl: string) => new URL(`${url}/services/geoxml/1.2/geoxml.xml?url=${fileUrl}`);
    const mockApiKeysToSuccess = () => {
        nock(intHosts.apikeysInt)
            .post('/v1/check')
            .reply(200, API_KEYS_SUCCESS_RESPONSE);
    };

    nock.disableNetConnect();
    nock.enableNetConnect(/^(127\.0\.0\.1|localhost)/);

    return {
        url,
        intHosts,
        getUrlForFile,
        mockApiKeysToSuccess,
        release: async () => {
            await tvmDaemon.stop();
            await new Promise((resolve) => server.close(resolve));
            nock.cleanAll();
            nock.enableNetConnect();
        }
    };
}
