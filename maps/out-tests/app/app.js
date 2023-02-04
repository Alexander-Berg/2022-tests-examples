"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.app = void 0;
const express = require("express");
const assert = require("assert");
const Boom = require("@hapi/boom");
const logger_1 = require("app/lib/logger");
const tvm_1 = require("app/middleware/tvm");
const apikey_1 = require("app/middleware/apikey");
const parser_1 = require("app/middleware/parser");
const zora_1 = require("app/middleware/zora");
const hosts_1 = require("app/middleware/hosts");
const config_1 = require("app/config");
exports.app = express()
    .disable('x-powered-by')
    .get('/ping', (_req, res) => res.end())
    .get([
    '/1.2/geoxml.xml',
    '/services/geoxml/1.2/geoxml.xml'
], [
    tvm_1.tvmMiddleware({
        daemonBaseUrl: config_1.config.tvmUrl,
        token: process.env.TVMTOOL_LOCAL_AUTHTOKEN
    }),
    hosts_1.hostsMiddleware,
    apikey_1.apikeyMiddleware,
    zora_1.zoraMiddleware,
    parser_1.parserMiddleware
]);
exports.app
    .use((req, res, next) => next(Boom.notFound('Endpoint not found')))
    .use((err, req, res, next) => {
    if (Boom.isBoom(err)) {
        sendError(res, err);
    }
    else {
        logger_1.logger.error(`app, url=${req.query.url}, ${err.stack || err}`);
        sendError(res, Boom.internal());
    }
});
if (!module.parent) {
    const port = getCustomPort() || 8080;
    exports.app.listen(port, () => {
        logger_1.logger.info(`Application started on port ${port}`);
    });
}
function sendError(res, err) {
    res.status(err.output.statusCode).json({ message: err.output.payload });
}
function getCustomPort() {
    if (process.env.MAPS_NODEJS_PORT === undefined) {
        return;
    }
    const port = parseInt(process.env.MAPS_NODEJS_PORT, 10);
    assert(!isNaN(port), 'Environment variable MAPS_NODEJS_PORT must be an integer');
    return port;
}
