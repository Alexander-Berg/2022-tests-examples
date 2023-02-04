import * as assert from 'assert';
import {getTranslationForName} from '../../../server/processors/translations-processor';
import Translations, {Language} from '../../../server/processors/types/translations';

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

describe('translations-preprocessor', () => {
    describe('getTranslationForName', () => {
        it('should work in simple valid case', () => {
            assert.deepEqual(
                getTranslationForName(
                    '$localized(generated/sr57356355-name)',
                    (translations as unknown) as Translations,
                    'ru'
                ),
                'Красная линия'
            );
        });

        it('should fallback for en for non-existent keyset', () => {
            assert.deepEqual(
                getTranslationForName(
                    '$localized(generated/sr57356355-name)',
                    (translations as unknown) as Translations,
                    'tr'
                ),
                'Red line'
            );
        });

        it('should return original name if no pattern match', () => {
            assert.deepEqual(
                getTranslationForName(
                    'lokkalized(generated/sr57356355-name)',
                    (translations as unknown) as Translations,
                    'ru'
                ),
                'lokkalized(generated/sr57356355-name)'
            );
        });
    });
});
