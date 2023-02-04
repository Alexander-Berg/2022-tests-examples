import { Test, TestingModule } from '@nestjs/testing';

import { getFixtures } from '../../../tests/get-fixtures';
import { StringsReplacer } from '../strings.replacer';
import { LinksDictionaryItem } from '../types';
import { TextToLinksReplacerModule } from '../index';

import { fixtures } from './strings.replacer.fixtures';

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

describe('StringsReplacer', () => {
    let testingModule: TestingModule;
    let replacer: StringsReplacer;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [TextToLinksReplacerModule],
        }).compile();

        replacer = await testingModule.resolve(StringsReplacer);
        replacer.linksDictionary = linksDictionary;
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('Метод replace', () => {
        describe('Корректно возвращает текст неизменным', () => {
            it('При наличии совпадений текста, но с другим регистром', () => {
                const { text } = getFixtures(fixtures);
                const result = replacer.replace(text, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
            });
        });

        describe('Заменяет единственное вхождение текста на ссылку', () => {
            it('При отсутствии другого текста', () => {
                const { text } = getFixtures(fixtures);
                const result = replacer.replace(text, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
            });

            it('Корректно обрабатывая конец слова (не заменяя части слова)', () => {
                const { text } = getFixtures(fixtures);
                const result = replacer.replace(text, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
            });

            it('При отсутствии других ссылок в тексте', () => {
                const { text } = getFixtures(fixtures);
                const result = replacer.replace(text, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
            });

            it('При наличии ссылки в тексте, ссылка остается нетронутой', () => {
                const { text } = getFixtures(fixtures);
                const result = replacer.replace(text, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
            });
        });

        describe('Заменяет множественные вхождения текста на ссылки', () => {
            describe('При нулевом минимальном расстоянии между ссылками', () => {
                it('В простом тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10 });

                    expect(result).toMatchSnapshot();
                });

                it('При наличии html-тегов в тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10 });

                    expect(result).toMatchSnapshot();
                });

                it('При наличии html-сущностей в тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10 });

                    expect(result).toMatchSnapshot();
                });
            });

            describe('При ненулевом минимальном расстоянии между ссылками', () => {
                it('В простом тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10, linksMargin: 10 });

                    expect(result).toMatchSnapshot();
                });

                it('При наличии html-тегов в тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10, linksMargin: 10 });

                    expect(result).toMatchSnapshot();
                });

                it('При наличии html-ссылок в тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10, linksMargin: 10 });

                    expect(result).toMatchSnapshot();
                });

                it('При наличии html-сущностей в тексте', () => {
                    const { text } = getFixtures(fixtures);
                    const result = replacer.replace(text, { linksLimit: 10, linksMargin: 10 });

                    expect(result).toMatchSnapshot();
                });
            });

            it('Учитывая лимит на количество ссылок', () => {
                const { text } = getFixtures(fixtures);
                const result = replacer.replace(text, { linksLimit: 3 });

                expect(result).toMatchSnapshot();
            });
        });
    });
});
