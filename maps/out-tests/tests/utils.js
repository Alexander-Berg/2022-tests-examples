"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.createInfrastructure = exports.API_KEYS_SUCCESS_RESPONSE = void 0;
const http = require("http");
const tvm_daemon_1 = require("./tvm-daemon");
const maps_host_configs_1 = require("@yandex-int/maps-host-configs");
const env_1 = require("app/lib/env");
const config_1 = require("app/config");
const nock = require("nock");
const intHostConfigLoader = maps_host_configs_1.createIntHostConfigLoader({
    env: env_1.env,
    basePath: config_1.config['inthosts.configPath']
});
exports.API_KEYS_SUCCESS_RESPONSE = { ok: true, tariff: 'apimaps_free' };
async function createInfrastructure(app) {
    const server = http.createServer(app);
    const tvmDaemon = await tvm_daemon_1.TvmDaemon.start();
    const url = await new Promise((resolve) => {
        server.listen(() => resolve(`http://127.0.0.1:${server.address().port}`));
    });
    const intHosts = await intHostConfigLoader.get();
    const getUrlForFile = (fileUrl) => new URL(`${url}/services/geoxml/1.2/geoxml.xml?url=${fileUrl}`);
    const mockApiKeysToSuccess = () => {
        nock(intHosts.apikeysInt)
            .post('/v1/check')
            .reply(200, exports.API_KEYS_SUCCESS_RESPONSE);
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
exports.createInfrastructure = createInfrastructure;
