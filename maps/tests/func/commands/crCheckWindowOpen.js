const url = require('url');
const firstOrDefault = require('../utils/firstOrDefault').firstOrDefault;

/**
 * Команда для базовых проверок ссылок/кнопок, открывающихся в новом окне javascript'ом
 *
 * 1. Сработал window.open
 * 2. По ссылке/кнопке можно кликнуть
 * 3. В window.open передаётся url, который не пуст и не состоит из пробельных символов
 * 4. Ссылка/кнопка открывается с правильным значением 'target'
 *
 * @name browser.crCheckWindowOpen
 * @param {String} selector
 * @param {String} [message=Параметры ссылки содержат неверные значения] - кастомное базовое сообщение об ошибке,
 *   которое будет дополнено информацией о конкретной ошибке
 * @param {Object} [params]
 * @param {String|RegExp} [params.target=_blank|_self|RegExp] - с каким значением 'target' открывается новое окно.
 *
 * @returns {Promise.<Url>} - разобранный URL
 */
module.exports = function (selector, message, params) {
    const errorMessage = (details) => `${message}: ${details}`;

    /**
     * Подменяет оригинальную функцию window.open и запоминает параметры вызова
     *
     * @returns {{calledWith: calledWith, intercept: intercept}}
     */
    function replace() {
        let originalWindowOpen;
        let calledWith;
        return {
            calledWith: function () {
                return calledWith;
            },
            intercept: function () {
                calledWith = undefined;
                originalWindowOpen = this.open;
                this.open = function (url, target) {
                    calledWith = {url: url.trim(), target: target};
                    this.open = originalWindowOpen;
                    return null;
                };
            }
        };
    }

    if (typeof message !== 'string') {
        params = message;
        message = 'Параметры ссылки содержат неверные значения';
    }

    params = params || {};
    params.target = params.target === '' ? '' : (params.target || '_blank');
    return this
        .execute(replace().intercept)
        .crShouldBeVisible(selector)
        .click(selector)
        .execute(replace().calledWith)
        .then((calledWith) => {
            calledWith = calledWith.value;
            const href = firstOrDefault(calledWith.url);
            assert(href.length > 0, errorMessage('"href" – пустая строка или пробельные символы'));

            if (!calledWith.target) {
                assert.isUndefined(calledWith.target, errorMessage('неверный "target"'));
            } else if (typeof params.target === 'string') {
                assert.strictEqual(firstOrDefault(calledWith.target),
                    params.target, errorMessage('неверный "target"'));
            } else {
                assert.match(firstOrDefault(calledWith.target),
                    params.target, errorMessage('неверный "target"'));
            }

            return url.parse(href, true);
        });
};
