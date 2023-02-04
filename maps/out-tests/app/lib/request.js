"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.postRequest = exports.getRequest = void 0;
const util = require("util");
const requestModule = require("request");
const qs = require("qs");
const logger_1 = require("app/lib/logger");
const requestGet = util.promisify(requestModule.get);
const requestPost = util.promisify(requestModule.post);
exports.getRequest = async (host, query, options = {}) => {
    const queryString = qs.stringify(query);
    const url = `${host}?${queryString}`;
    try {
        const res = await requestGet(url, Object.assign({ encoding: 'utf-8' }, options));
        if (res.statusCode !== 200) {
            logger_1.logger.error(`url=${url}, status=${res.statusCode}, response=${JSON.stringify(res.body)}`);
        }
        return res;
    }
    catch (err) {
        logger_1.logger.error(`url=${url}, status=${err.statusCode}, response=${JSON.stringify(err.body)}`);
        throw err;
    }
};
exports.postRequest = async (host, body, options = {}) => {
    try {
        const res = await requestPost(Object.assign({ url: host, body, json: true, encoding: 'utf-8' }, options));
        if (res.statusCode !== 200) {
            logger_1.logger.error(`url=${host}, status=${res.statusCode}, response=${JSON.stringify(res.body)}`);
        }
        return res;
    }
    catch (err) {
        logger_1.logger.error(`url=${host}, status=${err.statusCode}, response=${JSON.stringify(err.body)}`);
        throw err;
    }
};
