/*eslint prefer-arrow-callback: "off"*/

const url = require('url');
const firstOrDefault = require('../utils/firstOrDefault.js').firstOrDefault;

/**
 * @name browser.crCheckLink
 *
 * Команда для базовых проверок ссылки
 *
 * 1. Атрибут href ссылки не пуст и не состоит из пробельных символов
 * 2. Ссылка открывается с правильным атрибутом 'target'
 * 3. По ссылке можно кликнуть
 *
 * @param {String} selector
 * @param {String} [message=Параметры ссылки содержат неверные значения] - кастомное базовое сообщение об ошибке,
 *   которое будет дополнено информацией о конкретной ошибке
 * @param {Object} [params]
 * @param {String} [params.target=_blank|_self] - открывать ссылку в новой вкладке/оставаться в текущей вкладке
 *
 * @returns {Promise.<Url>} - разобранный URL
 */
module.exports = function (selector, message, params) {
    let originalHref;

    /**
     * Отменяет открытие в новом окне, проверяет кликабельность
     *
     * @param {String} selector
     * @param {String} message
     */
    function checkClickability(selector, message) {
        return this
            .execute(function (selector) {
                let triggered = false;

                window.$(selector).one('click touchend', function () {
                    if (triggered) {
                        return;
                    }
                    triggered = true;
                    return false;
                });
            }, selector)
            .click(selector)
            .catch((e) => {
                if (e.message.includes('element not visible')) {
                    assert.isNotOk('error', message.notVisible + `\n\n${e.message}`);
                }

                if (e.message.includes('Element is not clickable at point')) {
                    assert.isNotOk('error', message.notClickable + `\n\n${e.message}`);
                }
                assert.isNotOk('error', e);
            });
    }

    if (typeof message !== 'string') {
        params = message;
        message = 'Параметры ссылки содержат неверные значения';
    }

    params = params || {};
    params.target = params.target === '' ? '' : params.target || '_blank';

    const errorMessage = (details) => `${message}: ${details}`;

    return this
        .getAttribute(selector, 'href')
        .then((href) => {
            originalHref = firstOrDefault(href).trim();
        })
        .getAttribute(selector, 'target')
        .then((target) => {
            assert.strictEqual(firstOrDefault(target), params.target, errorMessage('неверный "target"'));
        })
        .then(() => checkClickability.call(this, selector, {
            notClickable: errorMessage('ссылка некликабельна'),
            notVisible: errorMessage('ссылка спрятана')
        }))
        .then(() => url.parse(originalHref, true));
};
