import * as assert from 'assert';
import translationsProcessor from '../../../server/processors/translations-processor';
import Translations, {Language} from '../../../server/processors/types/translations';
import SchemeData from '../../../server/processors/types/scheme-data';

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

const data = {
    stations: [{name: '$localized(generated/sr57356355-name)'}, {name: 'testtest)'}],
    lines: [
        {service: {attributes: {name: '$localized(generated/sr39074436-name)'}}},
        {service: {attributes: {name: '$localized(generated/sr985sadsad92-name)'}}}
    ],
    stops: [{id: 'someStop'}],
    graphics: {}
};

describe('translations-processor', () => {
    describe('translationsProcessor', () => {
        it('should work in real case', () => {
            assert.deepEqual(
                translationsProcessor((data as unknown) as SchemeData, (translations as unknown) as Translations, 'ru'),
                {
                    stations: [{name: 'Красная линия'}, {name: 'testtest)'}],
                    lines: [
                        {service: {attributes: {name: 'Скоростной трамвай'}}},
                        {service: {attributes: {name: '$localized(generated/sr985sadsad92-name)'}}}
                    ],
                    stops: [{id: 'someStop'}],
                    graphics: {}
                }
            );
        });
    });
});
