/*eslint no-console: "off"*/

const url = require('url');

/**
 * @name browser.wdtOpen
 * @param {String} search
 * @param {Object} [options]
 * @returns {Browser}
 */
module.exports = function (search, options = {}) {
    const req = {};
    const filename = options.filename || 'base';
    const id = options.id || '#map';
    const hn = options.isEnterprise ? 'enterprise.api-maps.tst.c.maps.yandex.ru' : 'api-maps.tst.c.maps.yandex.ru';

    req.protocol = options.protocol || 'https';
    req.hostname = options.hostname || hn;
    req.pathname = options.pathname || 'services/constructor/1.0/js';
    req.search = search || '';

    const stand = 'https://jsapi.luisa-s.alexa.maps.dev.yandex.net/api/widget-autotests/' + filename + '.html';
    const link = url.format(req);
    const selector = options.selector || (options.isIframe ? 'iframe' : 'script');

    return this
        .url(stand)
        .execute(function cb(id, link, selector) {
            const map = document.querySelector(id);
            const script = map.querySelector(selector);

            script.setAttribute('src', link);

            return true;
        }, id, link, selector)
        .catch((err) => {
            throw new Error(err);
        })
        .setMeta('widget', link);
};
