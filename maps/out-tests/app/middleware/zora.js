"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.zoraMiddleware = exports.FILE_SIZE_LIMIT = void 0;
const url_1 = require("url");
const Boom = require("@hapi/boom");
const express_async_1 = require("@yandex-int/express-async");
const legacy = require("legacy-encoding");
const jschardet = require("jschardet");
const config_1 = require("app/config");
const curl_1 = require("app/lib/curl");
const logger_1 = require("app/lib/logger");
// https://st.yandex-team.ru/MAPSHTTPAPI-1646
exports.FILE_SIZE_LIMIT = 1024 * 1024 * 10; // 10 megs;
exports.zoraMiddleware = express_async_1.asyncMiddleware(async (req, _res) => {
    const { url } = req.query;
    if (!url || typeof url !== 'string') {
        throw Boom.badRequest('"url" is required');
    }
    const zoraHost = req.intHosts.zoraGo;
    // @see https://wiki.yandex-team.ru/zora/gozora/instrukcija-polzovatelja/#specialnyezagolovki
    const headers = {
        mainHeaders: {
            'Accept-Charset': 'utf-8',
            'X-Ya-Follow-Redirects': true,
            'X-Ya-Ignore-Certs': true
        },
        proxyHeaders: {
            'X-Ya-Service-Ticket': req.tvm.zora,
        }
    };
    const urlForZora = url
        // Some url replacements which are necessary for zora proxy
        .replace(/\s/gi, '%20')
        .replace(/\$/gi, '\\$');
    try {
        new url_1.URL(urlForZora);
    }
    catch (err) {
        throw Boom.badRequest();
    }
    try {
        // hack for encodind russian symbols in query
        let fixedUrl = new url_1.URL(urlForZora).toString();
        let data = await curl_1.curlRequest(fixedUrl, headers, zoraHost, config_1.config['zora.timeout']);
        if (data.statusCode === 200) {
            if (data.body.length > exports.FILE_SIZE_LIMIT) {
                logger_1.logger.error(`zora, response size size exceeded by url=${urlForZora}`);
                throw Boom.entityTooLarge('size exceeded');
            }
            const encoding = jschardet.detect(data.body).encoding;
            if (!encoding) {
                req.file = data.body.toString();
                return;
            }
            req.file = legacy.decode(data.body, encoding.toLowerCase());
            return;
        }
        logger_1.logger.error(`zora, status=${data.statusCode}, errorCode=${data.headers['x-ya-gozora-error-code']}, url=${fixedUrl}`);
        throw Boom.badRequest();
    }
    catch (err) {
        if (!Boom.isBoom(err)) {
            logger_1.logger.error(`zora, message=${err.message}`);
        }
        throw err;
    }
});
