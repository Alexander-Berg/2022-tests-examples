/**
 * Команда для быстрого изменения значения value
 *
 * Используется для установки длинного текста в инпут,
 * так как нативная делает это очень долго
 *
 * @name browser.crSetValue
 * @param {String} selector
 * @param {String} value
 */
module.exports = function (selector, value) {
    return this
        .execute((sel, value) => {
            const el = document.querySelectorAll(sel)[0];
            el.value = value;
            return true;
        }, selector, value);
};
