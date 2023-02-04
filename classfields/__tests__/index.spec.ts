import { Test, TestingModule } from '@nestjs/testing';

import { getFixtures } from '../../../tests/get-fixtures';
import { TextToLinksReplacerModule, TextToLinksReplacer } from '../index';
import { LinksDictionaryItem } from '../types';

import { fixtures } from './index.fixtures';

const linksDictionary: LinksDictionaryItem[] = [
    {
        strings: ['Ford Focus', 'Форд Фокус', 'Focus', 'Фокус'],
        link: 'https://auto.ru/moskva/cars/ford/focus/all/',
    },
    {
        strings: ['Kia Rio', 'Киа Рио', 'Rio', 'Рио'],
        link: 'https://auto.ru/moskva/cars/kia/rio/all/',
    },
    {
        strings: ['Toyota Camry', 'Тойота Камри', 'Camry', 'Камри'],
        link: 'https://auto.ru/moskva/cars/toyota/camry/all/',
    },
];

describe('TextToLinksReplacer', () => {
    let testingModule: TestingModule;
    let replacer: TextToLinksReplacer;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [TextToLinksReplacerModule],
        }).compile();

        replacer = await testingModule.resolve(TextToLinksReplacer);
        replacer.linksDictionary = linksDictionary;
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('Метод replaceManyBlocks', () => {
        describe('Корректно заменяет текст на ссылки', () => {
            it('В нескольких разных блоках', () => {
                const { blocks } = getFixtures(fixtures);
                const { result, newLinksNum } = replacer.replaceManyBlocks(blocks, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
                expect(newLinksNum).toEqual(4);
            });
        });
    });
});
