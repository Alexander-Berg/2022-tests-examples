"use strict";
// ⚠️⚠️⚠️ IMPORTANT ⚠️⚠️⚠️
// Everything in this file is made to support backward compatibility when rewriting from xscript 🤦🤦🤦
Object.defineProperty(exports, "__esModule", { value: true });
exports.parserMiddleware = void 0;
const util = require("util");
const Boom = require("@hapi/boom");
const express_async_1 = require("@yandex-int/express-async");
const xml2js_1 = require("xml2js");
const unescapeDecode = require("unescape");
const sanitizer = require("yandex-sanitizer");
const xmljs = require("xml-js");
const kml_1 = require("app/parsers/kml");
const gpx_1 = require("app/parsers/gpx");
const ymapsml_1 = require("app/parsers/ymapsml");
const logger_1 = require("app/lib/logger");
const xml2json = util.promisify(xml2js_1.parseString);
unescapeDecode.extras = Object.assign(Object.assign({}, unescapeDecode.extras), { '&lt;': '<', '&#60;': '<', '&gt;': '>', '&#62;': '>', '&quot;': '"', '&#34;': '"', '&apos;': '\'', '&#39;': '\'' });
const parse = (data) => {
    if (data.kml) {
        return kml_1.kmlParser(data);
    }
    if (data.gpx) {
        return gpx_1.gpxParser(data);
    }
    if (data.ymaps) {
        return ymapsml_1.ymapsmlParser(data);
    }
    return null;
};
const parseUrl = (url) => {
    let parts = url.split('/');
    if (parts[parts.length - 1].split('.').length > 1) {
        parts = parts.slice(0, parts.length - 1);
    }
    return `${parts.join('/')}\\/`;
};
const cleanHtml = (content) => {
    const defaultAllowedTags = sanitizer.getDefaultAllowedTags();
    defaultAllowedTags.add('img');
    return sanitizer.sanitizeHtml(unescapeDecode(content), {
        allowedAttributes: {
            a: ['href', 'target'],
            img: ['src'],
            font: ['color'],
            div: ['style']
        },
        allowedTags: Array.from(defaultAllowedTags)
    });
};
const format = (content, baseUrl) => {
    // Backward compatibility
    // Xscript parser could detect html without CDATA and cover its
    return content
        .replace(/href=\\?"(?!http|https)(.+?)\\?"/gmi, `href="${parseUrl(baseUrl)}$1"`)
        .replace(/<repr:href>(.*)<\/repr:href>/gmi, (...args) => {
        if (args[1].startsWith('//')) {
            const path = args[1].split('/').slice(3).join('/');
            return `<repr:href>${parseUrl(baseUrl)}${path}</repr:href>`;
        }
        return `<repr:href>${args[1]}</repr:href>`;
    })
        .replace(/<gml:description>((<!\[CDATA\[)?((.|\s)*?)(\]\]>)?)<\/gml:description>/gmi, (...args) => `<gml:description><![CDATA[${cleanHtml(args[3])}]]><\/gml:description>`)
        .replace(/<text>((<!\[CDATA\[)?((.|\s)*?)(\]\]>)?)<\/text>/gmi, (...args) => `<text><![CDATA[${cleanHtml(args[3])}]]><\/text>`)
        .replace(/<repr:text>\s*((<!\[CDATA\[)?\s*((.|\s)*?)\s*(\]\]>)?)\s*<\/repr:text>/gmi, (...args) => `<repr:text><![CDATA[${cleanHtml(args[3])}]]><\/repr:text>`);
};
const valueNormalize = (value) => {
    if (typeof value === 'string') {
        return value;
    }
    return value;
};
exports.parserMiddleware = express_async_1.asyncMiddleware(async (req, res) => {
    const { url } = req.query;
    // TODO заменить модуль парсинга xml2json как здесь
    // https://github.yandex-team.ru/maps/coverage-service/blob/master/src/v2/parse-meta-response.ts
    try {
        // "xmljs" using like validator
        // @see https://st.yandex-team.ru/GEOMONITORINGS-9211#5e18da2dc2320b5a18c086cd
        xmljs.xml2json(req.file);
        const rawData = await xml2json(format(req.file, url), {
            explicitArray: false,
            mergeAttrs: true,
            attrNameProcessors: [xml2js_1.processors.stripPrefix],
            tagNameProcessors: [xml2js_1.processors.stripPrefix],
            attrValueProcessors: [valueNormalize]
        });
        const parsedData = parse(rawData);
        if (!parsedData) {
            // Invalid can be recieved from xml parser or format parser
            throw Boom.badData('unknown format');
        }
        const data = { response: parsedData };
        res.jsonp(data);
    }
    catch (err) {
        if (Boom.isBoom(err)) {
            throw err;
        }
        logger_1.logger.error(`parser, url=${url}, message=${err.message}`);
        throw Boom.badRequest();
    }
});
