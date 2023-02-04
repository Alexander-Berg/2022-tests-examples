import { Test, TestingModule } from '@nestjs/testing';

import { Service } from '../../../../../types/common';
import { IMainBlock } from '../../../../../types/main-block';
import { Tag as TagModel } from '../../../../tag/tag.model';
import { MainTag as MainTagModel } from '../../../../main-tag/main-tag.model';
import {
    GetMainBlocksTransformer,
    ITransformedBlocksWithPosts,
} from '../../../transformers/get-main/main-blocks.transformer';
import { GetMainTransformer } from '../../../transformers/get-main/get-main.transformer';
import { Main as MainModel } from '../../../../main/main.model';
import { MainModule } from '../../../main.module';
import { getFixtures } from '../../../../../tests/get-fixtures';

import { fixtures } from './get-main.transformer.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Get main transformer', () => {
    let testingModule: TestingModule;
    let blocksTransformerSpy: jest.SpyInstance;
    let transformer: GetMainTransformer;
    const TRANSFORMED_BLOCKS: ITransformedBlocksWithPosts = {
        posts: {},
        blocks: [{ type: 'text', text: 'i am transformed' }],
    };

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [MainModule],
        }).compile();

        const blockTransformer = await testingModule.resolve(GetMainBlocksTransformer);

        blocksTransformerSpy = jest
            .spyOn(blockTransformer, 'transform')
            .mockImplementation(() => Promise.resolve(TRANSFORMED_BLOCKS));

        transformer = await testingModule.resolve(GetMainTransformer);

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    it('Возвращает трансформированную главную', async () => {
        const MAIN = await MainModel.create({ service: Service.autoru });

        const result = await transformer.transform(MAIN);

        expect(blocksTransformerSpy).toHaveBeenCalledWith([]);
        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(result).toEqual({
            tags: [],
            blocks: TRANSFORMED_BLOCKS.blocks,
            posts: TRANSFORMED_BLOCKS.posts,
        });
    });

    it('Возвращает главную с трансформированными блоками', async () => {
        const BLOCKS: IMainBlock[] = [
            {
                type: 'text',
                text: 'Компания Kia начала российские продажи седана Rio и кросс-хэтча Rio X нового модельного года',
            },
        ];
        const MAIN = await MainModel.create({
            service: Service.autoru,
            blocks: BLOCKS,
        });

        const result = await transformer.transform(MAIN);

        expect(blocksTransformerSpy).toHaveBeenCalledWith(BLOCKS);
        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(result).toEqual({
            tags: [],
            blocks: TRANSFORMED_BLOCKS.blocks,
            posts: TRANSFORMED_BLOCKS.posts,
        });
    });

    it('Может возвращает главную с трансформированными черновыми блоками', async () => {
        const BLOCKS: IMainBlock[] = [
            {
                type: 'text',
                text: 'Создатель McLaren F1 представил универсальную платформу',
            },
        ];
        const DRAFT_BLOCKS: IMainBlock[] = [
            {
                type: 'text',
                text: 'Компания Kia начала российские продажи седана Rio и кросс-хэтча Rio X нового модельного года',
            },
        ];
        const MAIN = await MainModel.create({
            service: Service.autoru,
            blocks: BLOCKS,
            draftBlocks: DRAFT_BLOCKS,
        });

        const result = await transformer.transform(MAIN, { draft: true });

        expect(blocksTransformerSpy).toHaveBeenCalledWith(DRAFT_BLOCKS);
        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(result).toEqual({
            tags: [],
            blocks: TRANSFORMED_BLOCKS.blocks,
            posts: TRANSFORMED_BLOCKS.posts,
        });
    });

    it('Возвращает главную с трансформированными тегами в правильном порядке', async () => {
        const { TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3 } = getFixtures(fixtures);

        const MAIN = await MainModel.create({
            service: Service.autoru,
        });

        await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);

        await MainTagModel.bulkCreate([
            { tag_key: TAG_ATTRIBUTES_1.urlPart, order: 3 },
            { tag_key: TAG_ATTRIBUTES_2.urlPart, order: 1 },
            { tag_key: TAG_ATTRIBUTES_3.urlPart, order: 2 },
        ]);

        const result = await transformer.transform(MAIN);

        expect(blocksTransformerSpy).toHaveBeenCalledWith([]);
        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(result).toMatchSnapshot();
    });
});
