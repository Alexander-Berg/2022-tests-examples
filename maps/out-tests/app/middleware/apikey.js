"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.apikeyMiddleware = void 0;
const Boom = require("@hapi/boom");
const express_async_1 = require("@yandex-int/express-async");
const request_1 = require("app/lib/request");
const logger_1 = require("app/lib/logger");
exports.apikeyMiddleware = express_async_1.asyncMiddleware(async (req, _res) => {
    const ip = req.headers['x-real-ip'] || req.ip;
    const { apikey } = req.query;
    const { referer: referrer } = req.headers;
    const apikeysIntHost = req.intHosts.apikeysInt;
    let apikeysData;
    try {
        apikeysData = await request_1.postRequest(`${apikeysIntHost}v1/check`, {
            ip, referrer,
            key: apikey
        }, {
            headers: { 'X-Ya-Service-Ticket': req.tvm.apikeysInt },
            timeout: 500
        });
    }
    catch (error) {
        logger_1.logger.error(`apikeys-int ${JSON.stringify({ ip, apikey, referrer, error })}`);
        return;
    }
    // Error which doesn't connected with apikey checking (our internal)
    if (apikeysData.statusCode !== 200) {
        logger_1.logger.info(`apikeys-int ${JSON.stringify({ status: apikeysData.statusCode, ip, apikey, referrer, body: apikeysData.body })}`);
        return;
    }
    if (apikeysData.body.ok) {
        return;
    }
    logger_1.logger.info(`apikeys-int ${JSON.stringify({ status: apikeysData.statusCode, ip, apikey, referrer, body: apikeysData.body })}`);
    throw Boom.forbidden();
});
