"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.hostsMiddleware = void 0;
const express_async_1 = require("@yandex-int/express-async");
const maps_host_configs_1 = require("@yandex-int/maps-host-configs");
const env_1 = require("app/lib/env");
const config_1 = require("app/config");
const intHostConfigLoader = maps_host_configs_1.createIntHostConfigLoader({
    env: env_1.env,
    basePath: config_1.config['inthosts.configPath']
});
exports.hostsMiddleware = express_async_1.asyncMiddleware(async (req) => {
    try {
        const intHosts = await intHostConfigLoader.get({ overrides: req.query.host_config });
        req.intHosts = intHosts;
    }
    catch (err) {
        throw new Error(`Failed to load inthosts: ${err}`);
    }
});
