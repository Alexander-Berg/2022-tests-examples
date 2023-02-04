"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.config = void 0;
const env_1 = require("app/lib/env");
const production = {
    intHostsPath: '/usr/share/yandex/maps/inthosts/1.1/',
    'zora.timeout': 8000,
    'logger.level': 'info',
    'logger.colorize': false,
    'inthosts.configPath': '/usr/share/yandex/maps/inthosts/1.1/'
};
const testing = Object.assign(Object.assign({}, production), { 'logger.level': 'silly' });
const development = Object.assign(Object.assign({}, testing), { intHostsPath: '/usr/local/share/yandex/maps/inthosts/1.1/', tvmUrl: 'http://localhost:8001/', 'logger.colorize': true, 'inthosts.configPath': 'sandbox-resources/inthosts/1.1/' });
const stress = Object.assign({}, testing);
const configs = { production, testing, stress, development };
const config = configs[env_1.env];
exports.config = config;
