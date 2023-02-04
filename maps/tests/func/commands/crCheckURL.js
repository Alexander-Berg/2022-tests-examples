const url = require('url');

/**
 * Команда для проверки URL
 *
 * @name browser.crCheckURL
 * @param {Object|String} actualUrl - Cравниваемый URL
 * @param {Object|String} expectedUrl - Ожидаемый URL
 * @param {String} [message=Ошибочный адрес] - Кастомное базовое сообщение об ошибке,
 *   которое будет дополнено информацией о конкретной ошибке
 * @param {Object} [params] - Параметры для URL
 */

module.exports = function (actualUrl, expectedUrl, message, params) {
    const actual = getOrParseUrl(actualUrl);
    const expected = getOrParseUrl(expectedUrl);
    const errorMessage = (part) => `${message}, неправильный '${part}'`;

    /**
     *
     * @param {String} someUrl
     * @returns {*}
     */
    function getOrParseUrl(someUrl) {
        if (someUrl instanceof url.Url) {
            return someUrl;
        }
        if (typeof someUrl === 'string') {
            return url.parse(someUrl, true);
        }
        if (someUrl && someUrl.queryValidator && typeof someUrl.url === 'string') {
            return url.parse(someUrl.url, true);
        }

        return someUrl;
    }

    if (typeof message !== 'string') {
        params = message;
        message = 'Ошибочный адрес';
    }

    params = params ? Object.assign({}, params) : {};

    if (!params.skipProtocol) {
        assert.equal(actual.protocol, expected.protocol, errorMessage('protocol'));
    }

    if (!params.skipHostname) {
        assert.equal(actual.hostname, expected.hostname, errorMessage('hostname'));
    }

    if (!params.skipPathname) {
        assert.equal(actual.pathname, expected.pathname, errorMessage('pathname'));
    }

    if (!params.skipQuery) {
        if (expectedUrl.queryValidator instanceof Function) {
            assert(expectedUrl.queryValidator(actual.query), errorMessage('query'));
        } else {
            assert.deepEqual(actual.query, expected.query, errorMessage('query'));
        }
    }

    if (!params.skipHash) {
        assert.equal(actual.hash, expected.hash, errorMessage('hash'));
    }
};
