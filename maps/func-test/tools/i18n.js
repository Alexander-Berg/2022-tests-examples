const i18nPath = '../../src/shared/i18n/',
    keysets = {},
    { DEFAULT_LANG } = require('./constants');

/**
 * @name i18n
 * @param {String} keysetName
 * @param {String} keyName
 * @param {String} [lang]
 */
module.exports = function i18n(keysetName, keyName, lang = DEFAULT_LANG) {
    if(typeof keysets[keysetName] === 'undefined') {
        const keyset = require(`${i18nPath}keysets/${keysetName}/${lang}.js`);
        keysets[keysetName] = keyset[keysetName];
    }
    const keyText = keysets[keysetName][keyName];
    if(!keyText) {
        throw new Error(`Key "${keyName}" in "${keysetName}" is undefined`);
    }
    return keyText;
};
