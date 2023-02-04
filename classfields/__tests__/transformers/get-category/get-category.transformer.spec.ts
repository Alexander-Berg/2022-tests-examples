import { Test, TestingModule } from '@nestjs/testing';

import {
    GetCategoryBlocksTransformer,
    ITransformedBlocksWithPosts,
} from '../../../transformers/get-category/category-blocks.transformer';
import { GetCategoryTransformer } from '../../../transformers/get-category/get-category.transformer';
import { Tag as TagModel } from '../../../../tag/tag.model';
import { Category as CategoryModel } from '../../../category.model';
import { CategoryModule } from '../../../category.module';
import { getFixtures } from '../../../../../tests/get-fixtures';
import { PostService } from '../../../../post/post.service';

import { fixtures } from './get-category.transformer.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Get category transformer', () => {
    let testingModule: TestingModule;
    let blocksTransformerSpy: jest.SpyInstance;
    let postServiceSpy: jest.SpyInstance;
    let transformer: GetCategoryTransformer;
    const postsCount = 10;
    const transformedBlocks: ITransformedBlocksWithPosts = {
        posts: {},
        blocks: [{ type: 'text', text: 'i am transformed' }],
    };

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [CategoryModule],
        }).compile();

        const blockTransformer = await testingModule.resolve(GetCategoryBlocksTransformer);

        blocksTransformerSpy = jest.spyOn(blockTransformer, 'transform').mockImplementation(() => {
            return Promise.resolve(transformedBlocks);
        });

        const postService = await testingModule.resolve(PostService);

        postServiceSpy = jest.spyOn(postService, 'getCount').mockImplementation(() => Promise.resolve(postsCount));

        transformer = await testingModule.resolve(GetCategoryTransformer);

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    it('Возвращает трансформированную категорию', async () => {
        const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

        const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

        const result = await transformer.transform(CATEGORY);

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(0);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, categories: [CATEGORY.urlPart] });
        expect(result).toMatchSnapshot();
    });

    it('Возвращает трансформированную категорию с тегами', async () => {
        const { CATEGORY_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

        const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);
        const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

        await CATEGORY.$add('tags', TAGS);
        await CATEGORY.reload({
            include: 'tags',
        });

        const result = await transformer.transform(CATEGORY);

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(0);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, categories: [CATEGORY.urlPart] });
        expect(result).toMatchSnapshot();
    });

    it('Может возвращать трансформированный черновик категории', async () => {
        const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

        const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

        const result = await transformer.transform(CATEGORY, { draft: true });

        expect(blocksTransformerSpy).toHaveBeenCalledTimes(0);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledWith({ onlyPublished: true, categories: [CATEGORY.urlPart] });
        expect(result).toMatchSnapshot();
    });

    it('Может возвращать категорию с трансформированными блоками', async () => {
        const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

        const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

        const result = await transformer.transform(CATEGORY, { withPostsModels: true });

        expect(blocksTransformerSpy).toHaveBeenCalledWith({
            categoryUrlPart: CATEGORY.urlPart,
            blocks: CATEGORY.blocks,
        });
        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(result).toMatchSnapshot();
    });

    it('Может возвращает категорию с трансформированными черновыми блоками', async () => {
        const { CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

        const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

        const result = await transformer.transform(CATEGORY, {
            draft: true,
            withPostsModels: true,
        });

        expect(blocksTransformerSpy).toHaveBeenCalledWith({
            categoryUrlPart: CATEGORY.urlPart,
            blocks: CATEGORY.draftBlocks,
        });
        expect(blocksTransformerSpy).toHaveBeenCalledTimes(1);
        expect(postServiceSpy).toHaveBeenCalledTimes(1);
        expect(result).toMatchSnapshot();
    });
});
