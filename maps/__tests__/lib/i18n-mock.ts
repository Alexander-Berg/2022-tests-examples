import * as path from 'path';
import * as I18nKeyests from '../../tools/lib/i18n-keysets';

const DEFAULT_LANG = 'ru';
const keysets = I18nKeyests.getKeyset(DEFAULT_LANG, path.resolve(__dirname, '../../out/i18n/'));

function i18n(keyset: any, key: string, options: any): string {
    const value = keysets[keyset][key];
    return Object.prototype.toString.call(value) === 'function' ?
        value(options) : value;
}

module.exports = i18n;
