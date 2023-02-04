"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.curlRequest = void 0;
const node_libcurl_1 = require("node-libcurl");
const Boom = require("@hapi/boom");
const env_1 = require("app/lib/env");
// Curl.feature.NO_DATA_PARSING = true;
const normalizeResponseHeaders = (headersRow) => {
    const keysToLowerCase = (obj) => {
        return Object.keys(obj).reduce((res, key) => {
            res[key.toString().toLowerCase()] = obj[key];
            return res;
        }, {});
    };
    if (!Array.isArray(headersRow)) {
        return keysToLowerCase(headersRow);
    }
    return headersRow.reduce((res, el) => (Object.assign(Object.assign({}, res), keysToLowerCase(el))), {});
};
const normalizeRequestHeaders = (headers) => {
    return Object.keys(headers).map((key) => `${key}: ${headers[key]}`);
};
exports.curlRequest = (url, headers, proxy, timeout = 2000) => {
    const { mainHeaders, proxyHeaders } = headers;
    const curl = new node_libcurl_1.Curl();
    curl.enable(1);
    curl.setOpt(node_libcurl_1.Curl.option.URL, url);
    curl.setOpt(node_libcurl_1.Curl.option.HTTPHEADER, normalizeRequestHeaders(mainHeaders));
    curl.setOpt(node_libcurl_1.Curl.option.CUSTOMREQUEST, 'GET');
    curl.setOpt(node_libcurl_1.Curl.option.TIMEOUT_MS, timeout);
    // curl request should follow redirects
    // see https://node-libcurl-docs.netlify.app/interfaces/_lib_generated_curloption_.curloption.html#followlocation
    curl.setOpt(node_libcurl_1.Curl.option.FOLLOWLOCATION, true);
    curl.setOpt(node_libcurl_1.Curl.option.PROXY, proxy);
    curl.setOpt(node_libcurl_1.Curl.option.PROXYTYPE, 'CURLPROXY_HTTP');
    curl.setOpt(node_libcurl_1.Curl.option.PROXYHEADER, normalizeRequestHeaders(proxyHeaders));
    // we didn't succeed in certificate verification when going to gozora with curl + geoxml service rarely re-deployed,
    // certificate will be outdated + in gozora example they also use verify=false
    // https://a.yandex-team.ru/arc/trunk/arcadia/robot/zora/gozora/samples/python/gozora/gozora.py?rev=r7995772#L32
    // ==> SSL_VERIFYPEER = 0
    curl.setOpt(node_libcurl_1.Curl.option.SSL_VERIFYPEER, 0);
    if (env_1.env === 'development') {
        curl.setOpt(node_libcurl_1.Curl.option.VERBOSE, true);
    }
    return new Promise((resolve, reject) => {
        try {
            curl.on('end', (statusCode, body, headers) => {
                headers = normalizeResponseHeaders(headers);
                curl.close();
                resolve({ statusCode, body, headers });
            });
            curl.on('error', (error, errorCode) => {
                const connectCode = curl.getInfo('HTTP_CONNECTCODE');
                curl.close();
                if (errorCode === node_libcurl_1.CurlCode.CURLE_OPERATION_TIMEDOUT) {
                    reject(Boom.clientTimeout('couldn\'t load file'));
                }
                else if ([502, 504].includes(Number(connectCode))) {
                    // zora return 5xx in `connectCode` when CONNECT to url was failed
                    reject(Boom.badRequest());
                }
                else {
                    reject(new Error(`${error.message}, curlCode=${errorCode}, connectCode=${connectCode}`));
                }
            });
            curl.perform();
        }
        catch (err) {
            if (curl.isRunning) {
                curl.close();
            }
            reject(err);
        }
    });
};
