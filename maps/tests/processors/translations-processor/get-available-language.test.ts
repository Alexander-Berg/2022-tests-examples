import {getAvailableLanguage} from '../../../server/processors/translations-processor';
import Translations, {Language} from '../../../server/processors/types/translations';
import assert from 'assert';

const translations = {
    translations: ['en', 'ru'] as Language[],
    default: 'en' as Language,
    keysets: {
        general: {
            name: {
                en: 'Dubai',
                ru: 'Дубай'
            }
        },
        generated: {
            'sr57356355-name': {
                en: 'Red line',
                ru: 'Красная линия'
            },
            'sr93888592-name': {
                en: 'Green line',
                ru: 'Зелёная линия'
            },
            'sr39074436-name': {
                en: 'Dubai Tram',
                ru: 'Скоростной трамвай'
            }
        }
    }
};

const translationsNoEn = {
    translations: ['tr', 'ru'] as Language[],
    default: 'tr' as Language,
    keysets: {}
};

const translationsNoTranslations = {
    translations: [] as Language[],
    default: 'tr' as Language,
    keysets: {}
};

describe('translations-preprocessor', () => {
    describe('getAvailableLanguage', () => {
        it('should work in simple valid case', () => {
            assert.deepEqual(getAvailableLanguage((translations as unknown) as Translations, 'ru'), 'ru');
        });

        it('should fallback for en for non-existent language in keyset', () => {
            assert.deepEqual(getAvailableLanguage((translations as unknown) as Translations, 'tr'), 'en');
        });

        it('should fallback for first key in keyset if no en and no language in keyset', () => {
            assert.deepEqual(getAvailableLanguage((translationsNoEn as unknown) as Translations, 'uz'), 'ru');
        });

        it('should return null for empty keyset', () => {
            assert.deepEqual(getAvailableLanguage((translationsNoTranslations as unknown) as Translations, 'kk'), null);
        });
    });
});
