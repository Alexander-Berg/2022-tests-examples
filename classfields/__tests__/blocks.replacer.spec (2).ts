import { Test, TestingModule } from '@nestjs/testing';

import { getFixtures } from '../../../tests/get-fixtures';
import { BlocksReplacer } from '../blocks.replacer';
import { StringsReplacer } from '../strings.replacer';
import { IReplaceResult } from '../types';
import { TextToLinksReplacerModule } from '../index';

import { fixtures } from './blocks.replacer.fixtures';

describe('BlocksReplacer', () => {
    let testingModule: TestingModule;
    let replacer: BlocksReplacer;

    const mockStringsReplacer = {
        replace: (): IReplaceResult<string> => ({
            result: 'REPLACED',
            newLinksNum: 3,
            newLinksList: [
                ['text1', 'link1'],
                ['text2', 'link2'],
                ['text3', 'link3'],
            ],
        }),
    };

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [TextToLinksReplacerModule],
        })
            .overrideProvider(StringsReplacer)
            .useValue(mockStringsReplacer)
            .compile();

        replacer = await testingModule.resolve(BlocksReplacer);
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('Метод replaceManyBlocks', () => {
        describe('Корректно возвращает "пустой" ответ', () => {
            it('При передаче пустого массива блоков', () => {
                const { result, newLinksNum } = replacer.replaceManyBlocks([], { linksLimit: 10 });

                expect(result).toBeArrayOfSize(0);
                expect(newLinksNum).toEqual(0);
            });
        });

        describe('Корректно возвращает блоки неизменными', () => {
            it('При отсутствии текстовых блоков', () => {
                const { blocks } = getFixtures(fixtures);
                const { result, newLinksNum } = replacer.replaceManyBlocks(blocks, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
                expect(newLinksNum).toEqual(0);
            });

            it('При нулевом лимите', () => {
                const { result, newLinksNum } = replacer.replaceManyBlocks([], { linksLimit: 0 });

                expect(result).toBeArrayOfSize(0);
                expect(newLinksNum).toEqual(0);
            });

            it('При отсутствии параметров', () => {
                const { result, newLinksNum } = replacer.replaceManyBlocks([]);

                expect(result).toBeArrayOfSize(0);
                expect(newLinksNum).toEqual(0);
            });
        });
    });

    describe('Метод replaceOneBlock', () => {
        describe('Корректно заменяет ссылки', () => {
            it('В блоке с типом text', () => {
                const { block } = getFixtures(fixtures);
                const { result, newLinksNum } = replacer.replaceOneBlock(block, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
                expect(newLinksNum).toEqual(3);
            });
            it('В блоке с типом card', () => {
                const { block } = getFixtures(fixtures);
                const { result, newLinksNum } = replacer.replaceOneBlock(block, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
                expect(newLinksNum).toEqual(3);
            });
            it('В блоке с типом bubble', () => {
                const { block } = getFixtures(fixtures);
                const { result, newLinksNum } = replacer.replaceOneBlock(block, { linksLimit: 10 });

                expect(result).toMatchSnapshot();
                expect(newLinksNum).toEqual(3);
            });
        });
        describe('Корректно возвращает ошибку', () => {
            it('При передаче недопустимого типа блока', () => {
                const { block } = getFixtures(fixtures);
                const replace = () => replacer.replaceOneBlock(block, { linksLimit: 10 });

                expect(replace).toThrowErrorMatchingSnapshot();
            });
        });
    });
});
