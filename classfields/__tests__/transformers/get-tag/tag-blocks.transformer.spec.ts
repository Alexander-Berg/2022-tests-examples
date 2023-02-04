import { Test, TestingModule } from '@nestjs/testing';

import {
    ITagBlock,
    IIdeasBlock,
    ITextbookBlock,
    IReadMoreBlock,
    ISectionBlock,
    ITextBlock,
    IQuizBlock,
} from '../../../../../types/tag-block';
import { getFixtures } from '../../../../../tests/get-fixtures';
import { Tag as TagModel } from '../../../../tag/tag.model';
import { Post as PostModel } from '../../../../post/post.model';
import { Category as CategoryModel } from '../../../../category/category.model';
import { TagModule } from '../../../tag.module';
import { GetTagBlocksTransformer } from '../../../transformers/get-tag/tag-blocks.transformer';
import { ISubscriptionFormBlock } from '../../../../../types/tag-block';

import { fixtures } from './tag-block.transformer.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Get tag blocks transformer', () => {
    let testingModule: TestingModule;
    let blocksTransformer: GetTagBlocksTransformer;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [TagModule],
        }).compile();

        blocksTransformer = await testingModule.resolve(GetTagBlocksTransformer);

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    it('Блок "текст" возвращается без изменений', async () => {
        const { CONTEXT_TAG_ATTRIBUTES } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const TEXT_BLOCK: ITextBlock = {
            type: 'text',
            text: 'Компания Kia начала российские продажи седана Rio и кросс-хэтча Rio X нового модельного года',
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [TEXT_BLOCK],
        });

        expect(result).toEqual({
            blocks: [TEXT_BLOCK],
            posts: {},
        });
    });

    it('Блок "Форма подписки" возвращается без изменений', async () => {
        const { CONTEXT_TAG_ATTRIBUTES } = getFixtures(fixtures);

        const CONTEXT_TAG = await CategoryModel.create(CONTEXT_TAG_ATTRIBUTES);

        const SUBSCRIPTION_FORM_BLOCK: ISubscriptionFormBlock = {
            type: 'subscriptionForm',
            subscriptionForm: {},
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SUBSCRIPTION_FORM_BLOCK],
        });

        expect(result).toEqual({
            blocks: [SUBSCRIPTION_FORM_BLOCK],
            posts: {},
        });
    });

    it('Блоки в секциях дополняются постами из основного тега c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const CATEGORY_POSTS = await PostModel.bulkCreate([POST_ATTRIBUTES_2, POST_ATTRIBUTES_5, POST_ATTRIBUTES_6]);

        await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_3, POST_ATTRIBUTES_4]);

        await CONTEXT_TAG.$add('posts', CATEGORY_POSTS);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                blocks: [
                    {
                        type: 'two',
                        two: [null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с основным тегом и (одновременно) тегом секции c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            SECTION_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const SECTION_TAG = await TagModel.create(SECTION_TAG_ATTRIBUTES);

        const POSTS_WITH_CONTEXT_TAG = await PostModel.bulkCreate([
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_10,
        ]);

        const POSTS_WITH_SECTION_TAG = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
        ]);

        const POSTS_WITH_ALL = await PostModel.bulkCreate([POST_ATTRIBUTES_2, POST_ATTRIBUTES_5, POST_ATTRIBUTES_7]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_CONTEXT_TAG);
        await CONTEXT_TAG.$add('posts', POSTS_WITH_ALL);

        await SECTION_TAG.$add('posts', POSTS_WITH_SECTION_TAG);
        await SECTION_TAG.$add('posts', POSTS_WITH_ALL);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                tags: [SECTION_TAG.urlPart],
                blocks: [
                    {
                        type: 'two',
                        two: [null, null],
                    },
                    {
                        type: 'oneBig',
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с основным тегом и (одновременно) тегами секции c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            SECTION_TAG_ATTRIBUTES_1,
            SECTION_TAG_ATTRIBUTES_2,
            SECTION_TAG_ATTRIBUTES_3,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
        } = getFixtures(fixtures);

        const [CONTEXT_TAG, SECTION_TAG_1, SECTION_TAG_2, TAG_3] = await TagModel.bulkCreate([
            CONTEXT_TAG_ATTRIBUTES,
            SECTION_TAG_ATTRIBUTES_1,
            SECTION_TAG_ATTRIBUTES_2,
            SECTION_TAG_ATTRIBUTES_3,
        ]);

        const [
            POST_1,
            POST_WITH_ALL_1,
            POST_3,
            POST_4,
            POST_5,
            POST_WITH_ALL_2,
            POST_7,
            POST_8,
            POST_9,
            POST_WITH_ALL_3,
        ] = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
        ]);

        await POST_1?.$add('tags', [SECTION_TAG_1 as TagModel]);
        await POST_3?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel, TAG_3 as TagModel]);
        await POST_4?.$add('tags', [CONTEXT_TAG as TagModel, SECTION_TAG_1 as TagModel, TAG_3 as TagModel]);
        await POST_5?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel]);
        await POST_7?.$add('tags', [CONTEXT_TAG as TagModel, SECTION_TAG_1 as TagModel]);
        await POST_8?.$add('tags', [CONTEXT_TAG as TagModel, SECTION_TAG_2 as TagModel, TAG_3 as TagModel]);
        await POST_9?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel, TAG_3 as TagModel]);

        await POST_WITH_ALL_1?.$add('tags', [
            CONTEXT_TAG as TagModel,
            SECTION_TAG_1 as TagModel,
            SECTION_TAG_2 as TagModel,
        ]);
        await POST_WITH_ALL_2?.$add('tags', [
            CONTEXT_TAG as TagModel,
            SECTION_TAG_1 as TagModel,
            SECTION_TAG_2 as TagModel,
            TAG_3 as TagModel,
        ]);
        await POST_WITH_ALL_3?.$add('tags', [
            CONTEXT_TAG as TagModel,
            SECTION_TAG_1 as TagModel,
            SECTION_TAG_2 as TagModel,
        ]);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                tags: [SECTION_TAG_ATTRIBUTES_1.urlPart, SECTION_TAG_ATTRIBUTES_2.urlPart],
                blocks: [
                    {
                        type: 'threeBottom',
                        threeBottom: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG_ATTRIBUTES.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с основным тегом и (одновременно) категорией секции c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            SECTION_CATEGORY_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const SECTION_CATEGORY = await CategoryModel.create(SECTION_CATEGORY_ATTRIBUTES);

        const POSTS_WITH_CONTEXT_TAG = await PostModel.bulkCreate([
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_9,
        ]);
        const POSTS_WITH_SECTION_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);
        const POSTS_WITH_ALL = await PostModel.bulkCreate([POST_ATTRIBUTES_3, POST_ATTRIBUTES_7, POST_ATTRIBUTES_8]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_CONTEXT_TAG);
        await CONTEXT_TAG.$add('posts', POSTS_WITH_ALL);

        await SECTION_CATEGORY.$add('posts', POSTS_WITH_SECTION_CATEGORY);
        await SECTION_CATEGORY.$add('posts', POSTS_WITH_ALL);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                categories: [SECTION_CATEGORY.urlPart],
                blocks: [
                    {
                        type: 'threeRight',
                        threeRight: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с основным тегом и (одновременно) категориями секции c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            SECTION_CATEGORY_ATTRIBUTES_1,
            SECTION_CATEGORY_ATTRIBUTES_2,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const [SECTION_CATEGORY_1, SECTION_CATEGORY_2, CATEGORY_3] = await CategoryModel.bulkCreate([
            SECTION_CATEGORY_ATTRIBUTES_1,
            SECTION_CATEGORY_ATTRIBUTES_2,
            CATEGORY_ATTRIBUTES_1,
        ]);

        const [
            POST_1,
            POST_WITH_ALL_1,
            POST_3,
            POST_4,
            POST_5,
            POST_WITH_ALL_2,
            POST_7,
            POST_8,
            POST_9,
            POST_WITH_ALL_3,
        ] = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
        ]);

        await POST_1?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel]);
        await POST_3?.$add('categories', [
            SECTION_CATEGORY_1 as CategoryModel,
            SECTION_CATEGORY_2 as CategoryModel,
            CATEGORY_3 as CategoryModel,
        ]);
        await POST_4?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel, CATEGORY_3 as CategoryModel]);
        await POST_5?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel, SECTION_CATEGORY_2 as CategoryModel]);
        await POST_7?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel]);
        await POST_8?.$add('categories', [SECTION_CATEGORY_2 as CategoryModel, CATEGORY_3 as CategoryModel]);
        await POST_9?.$add('categories', [
            SECTION_CATEGORY_1 as CategoryModel,
            SECTION_CATEGORY_2 as CategoryModel,
            CATEGORY_3 as CategoryModel,
        ]);

        await POST_1?.$add('tag', CONTEXT_TAG);
        await POST_4?.$add('tag', CONTEXT_TAG);
        await POST_7?.$add('tag', CONTEXT_TAG);
        await POST_8?.$add('tag', CONTEXT_TAG);

        await POST_WITH_ALL_1?.$add('categories', [
            SECTION_CATEGORY_1 as CategoryModel,
            SECTION_CATEGORY_2 as CategoryModel,
        ]);
        await POST_WITH_ALL_1?.$add('tag', CONTEXT_TAG);

        await POST_WITH_ALL_2?.$add('categories', [
            SECTION_CATEGORY_1 as CategoryModel,
            SECTION_CATEGORY_2 as CategoryModel,
            CATEGORY_3 as CategoryModel,
        ]);
        await POST_WITH_ALL_2?.$add('tag', CONTEXT_TAG);

        await POST_WITH_ALL_3?.$add('categories', [
            SECTION_CATEGORY_1 as CategoryModel,
            SECTION_CATEGORY_2 as CategoryModel,
        ]);
        await POST_WITH_ALL_3?.$add('tag', CONTEXT_TAG);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                categories: [SECTION_CATEGORY_ATTRIBUTES_1.urlPart, SECTION_CATEGORY_ATTRIBUTES_2.urlPart],
                blocks: [
                    {
                        type: 'threeBottom',
                        threeBottom: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с несколькими тегами и категориями секции c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            CATEGORY_ATTRIBUTES_1,
            CATEGORY_ATTRIBUTES_2,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
            POST_ATTRIBUTES_11,
            POST_ATTRIBUTES_12,
            POST_ATTRIBUTES_13,
            POST_ATTRIBUTES_14,
            POST_ATTRIBUTES_15,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const [SECTION_TAG_1, SECTION_TAG_2] = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);
        const [SECTION_CATEGORY_1, SECTION_CATEGORY_2] = await CategoryModel.bulkCreate([
            CATEGORY_ATTRIBUTES_1,
            CATEGORY_ATTRIBUTES_2,
        ]);
        const SECTION_POSTS = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_7,
            POST_ATTRIBUTES_13,
            POST_ATTRIBUTES_14,
        ]);
        const POSTS = await PostModel.bulkCreate([
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_8,
            POST_ATTRIBUTES_9,
            POST_ATTRIBUTES_10,
            POST_ATTRIBUTES_11,
            POST_ATTRIBUTES_12,
            POST_ATTRIBUTES_15,
        ]);

        await SECTION_TAG_1?.$add('posts', [POSTS[1] as PostModel, POSTS[5] as PostModel, ...SECTION_POSTS]);
        await SECTION_TAG_2?.$add('posts', [POSTS[2] as PostModel, POSTS[7] as PostModel, ...SECTION_POSTS]);
        await SECTION_CATEGORY_1?.$add('posts', [POSTS[3] as PostModel, POSTS[4] as PostModel, ...SECTION_POSTS]);
        await SECTION_CATEGORY_2?.$add('posts', [POSTS[8] as PostModel, POSTS[9] as PostModel, ...SECTION_POSTS]);

        await CONTEXT_TAG?.$add('posts', [
            POSTS[1] as PostModel,
            SECTION_POSTS[0] as PostModel,
            SECTION_POSTS[2] as PostModel,
            SECTION_POSTS[3] as PostModel,
        ]);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_2.urlPart],
                categories: [CATEGORY_ATTRIBUTES_1.urlPart, CATEGORY_ATTRIBUTES_2.urlPart],
                blocks: [
                    {
                        type: 'threeRight',
                        threeRight: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях сохраняют выбранные явно посты на правильных позициях', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const POSTS = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_4]);

        await CONTEXT_TAG.$add('posts', POSTS);
        await SECTION_CATEGORY.$add('posts', POSTS);

        await PostModel.bulkCreate([POST_ATTRIBUTES_2, POST_ATTRIBUTES_3, POST_ATTRIBUTES_5]);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                blocks: [
                    {
                        type: 'two',
                        two: [POST_ATTRIBUTES_2.urlPart, POST_ATTRIBUTES_5.urlPart],
                    },
                    {
                        type: 'threeRight',
                        threeRight: [null, POST_ATTRIBUTES_3.urlPart, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях отфильтровываются при отсутствии достаточного количества постов', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const POSTS = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

        await CONTEXT_TAG.$add('posts', POSTS);
        await SECTION_CATEGORY.$add('posts', POSTS);

        await PostModel.bulkCreate([POST_ATTRIBUTES_4, POST_ATTRIBUTES_5]);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                categories: [SECTION_CATEGORY.urlPart],
                blocks: [
                    {
                        type: 'two',
                        two: [null, null],
                    },
                    {
                        type: 'oneBig',
                    },
                    {
                        type: 'threeRight',
                        threeRight: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются только опубликованными постами', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const POSTS = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        ]);

        await CONTEXT_TAG.$add('posts', POSTS);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                blocks: [
                    {
                        type: 'two',
                        two: [null, null],
                    },
                    {
                        type: 'oneBig',
                    },
                    {
                        type: 'oneBig',
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [SECTION_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется постами из основного тега c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const POSTS = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_4, POST_ATTRIBUTES_5]);

        await PostModel.bulkCreate([POST_ATTRIBUTES_2, POST_ATTRIBUTES_3, POST_ATTRIBUTES_6]);

        await CONTEXT_TAG.$add('posts', POSTS);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                posts: [null, null, null],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [READ_MORE_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется постами с основным тегом и (одновременно) категорией блока c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            BLOCK_CATEGORY_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const BLOCK_CATEGORY = await CategoryModel.create(BLOCK_CATEGORY_ATTRIBUTES);

        const [POST_1, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);

        const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);

        await CONTEXT_TAG.$add('posts', [POST_2 as PostModel, POST_3 as PostModel, POST_4 as PostModel]);
        await BLOCK_CATEGORY.$add('posts', [
            POST_1 as PostModel,
            POST_3 as PostModel,
            POST_4 as PostModel,
            POST_5 as PostModel,
        ]);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                category: BLOCK_CATEGORY.urlPart,
                posts: [null, null],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [READ_MORE_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется постами с основным тегом и (одновременно) тегом блока c правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            BLOCK_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const BLOCK_TAG = await TagModel.create(BLOCK_TAG_ATTRIBUTES);

        const [POST_1, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);

        const POST_2 = await PostModel.create(POST_ATTRIBUTES_2);

        await CONTEXT_TAG.$add('posts', [POST_2 as PostModel, POST_3 as PostModel, POST_4 as PostModel]);
        await BLOCK_TAG.$add('posts', [
            POST_1 as PostModel,
            POST_3 as PostModel,
            POST_4 as PostModel,
            POST_5 as PostModel,
        ]);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                tag: BLOCK_TAG.urlPart,
                posts: [null, null],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [READ_MORE_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется только опубликованными постами', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const CATEGORY_POSTS = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        ]);

        await CONTEXT_TAG.$add('posts', CATEGORY_POSTS);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                posts: [null, null, null, null],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [READ_MORE_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" сохраняет выбранные явно посты на правильных позициях', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const POSTS_WITH_CONTEXT_TAG = await PostModel.bulkCreate([
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_5,
        ]);

        await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_4]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_CONTEXT_TAG);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                posts: [null, null, POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_4.urlPart, null],
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [READ_MORE_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" не возвращается, если нет достаточного количества постов для дополнения', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
        } = getFixtures(fixtures);

        const TEXTBOOK_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2]);

        await TEXTBOOK_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        await PostModel.bulkCreate([POST_ATTRIBUTES_3]);

        const TEXTBOOK_BLOCK: ITextbookBlock = {
            type: 'textbook',
            textbook: {
                category: TEXTBOOK_CATEGORY.urlPart,
                tabs: [],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [TEXTBOOK_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" дополняется по основному тегу опубликованными постами с правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const POST_IN_BLOCK = await PostModel.create(POST_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_2);

        const POSTS_WITH_TAG = await PostModel.bulkCreate([POST_ATTRIBUTES_3, POST_ATTRIBUTES_4, POST_ATTRIBUTES_5]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_TAG);

        const TEXTBOOK_BLOCK: ITextbookBlock = {
            type: 'textbook',
            textbook: {
                tabs: [
                    {
                        tag: CONTEXT_TAG.urlPart,
                        posts: [null, POST_IN_BLOCK.urlPart],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [TEXTBOOK_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" дополняется по основному тегу и выбранной категории опубликованными постами с правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const TEXTBOOK_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        const POST_IN_BLOCK = await PostModel.create(POST_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_2);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        ]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_CATEGORY);
        await TEXTBOOK_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        const TEXTBOOK_BLOCK: ITextbookBlock = {
            type: 'textbook',
            textbook: {
                category: TEXTBOOK_CATEGORY.urlPart,
                tabs: [
                    {
                        tag: CONTEXT_TAG.urlPart,
                        posts: [null, POST_IN_BLOCK.urlPart],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [TEXTBOOK_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" возвращает неархивные модели тегов', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            TAG_ATTRIBUTES_3,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);
        await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

        const TEXTBOOK_BLOCK: ITextbookBlock = {
            type: 'textbook',
            textbook: {
                tabs: [
                    {
                        tag: TAG_ATTRIBUTES_1.urlPart,
                        posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart, POST_ATTRIBUTES_3.urlPart],
                    },
                    {
                        tag: TAG_ATTRIBUTES_1.urlPart,
                        posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart, POST_ATTRIBUTES_3.urlPart],
                    },
                    {
                        tag: TAG_ATTRIBUTES_1.urlPart,
                        posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart, POST_ATTRIBUTES_3.urlPart],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [TEXTBOOK_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" не возвращается, если нет достаточного количества постов для дополнения', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
        } = getFixtures(fixtures);

        const IDEAS_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2]);

        await IDEAS_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        await PostModel.bulkCreate([POST_ATTRIBUTES_3]);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                category: IDEAS_CATEGORY.urlPart,
                posts: [null, null],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [IDEAS_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" дополняется по основному тегу опубликованными постами с правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        const POST_IN_BLOCK = await PostModel.create(POST_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_2);

        const POSTS_WITH_TAG = await PostModel.bulkCreate([POST_ATTRIBUTES_3, POST_ATTRIBUTES_4, POST_ATTRIBUTES_5]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_TAG);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                category: CONTEXT_TAG.urlPart,
                posts: [null, POST_IN_BLOCK.urlPart],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [IDEAS_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" дополняется по основному тегу и выбранной категории опубликованными постами с правильной сортировкой', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const IDEAS_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        const POST_IN_BLOCK = await PostModel.create(POST_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_2);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        ]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_CATEGORY);
        await IDEAS_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                category: IDEAS_CATEGORY.urlPart,
                posts: [null, POST_IN_BLOCK.urlPart],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [IDEAS_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" возвращает неархивные модели тегов', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            TAG_ATTRIBUTES_3,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);
        await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart],
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_2.urlPart, TAG_ATTRIBUTES_3.urlPart],
            },
        };

        const result = await blocksTransformer.transform({ tagUrlPart: CONTEXT_TAG.urlPart, blocks: [IDEAS_BLOCK] });

        expect(result).toMatchSnapshot();
    });

    it('Блок "Квиз" не возвращается, если пост не опубликован', async () => {
        const { CONTEXT_TAG_ATTRIBUTES, POST_ATTRIBUTES } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        await PostModel.bulkCreate([POST_ATTRIBUTES]);

        const QUIZ_BLOCK: IQuizBlock = {
            type: 'quiz',
            quiz: {
                post: POST_ATTRIBUTES.urlPart,
                title: 'title',
                listingButtonText: 'listingButtonText',
                postButtonText: 'postButtonText',
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [QUIZ_BLOCK],
        });

        expect(result.blocks).toBeArrayOfSize(0);
    });

    it('Блок "Квиз" не возвращается, если у поста нет квиза', async () => {
        const { CONTEXT_TAG_ATTRIBUTES, POST_ATTRIBUTES } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        await PostModel.bulkCreate([POST_ATTRIBUTES]);

        const QUIZ_BLOCK: IQuizBlock = {
            type: 'quiz',
            quiz: {
                post: POST_ATTRIBUTES.urlPart,
                title: 'title',
                listingButtonText: 'listingButtonText',
                postButtonText: 'postButtonText',
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [QUIZ_BLOCK],
        });

        expect(result.blocks).toBeArrayOfSize(0);
    });

    it('Блок "Квиз" корректно трансформируется с постом и квизом', async () => {
        const { CONTEXT_TAG_ATTRIBUTES, POST_ATTRIBUTES } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);

        await PostModel.bulkCreate([POST_ATTRIBUTES]);

        const QUIZ_BLOCK: IQuizBlock = {
            type: 'quiz',
            quiz: {
                post: POST_ATTRIBUTES.urlPart,
                title: 'title',
                listingButtonText: 'listingButtonText',
                postButtonText: 'postButtonText',
            },
        };

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: [QUIZ_BLOCK],
        });

        expect(result).toMatchSnapshot();
    });

    it('Блоки не дополняются одним постом дважды', async () => {
        const {
            CONTEXT_TAG_ATTRIBUTES,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
        } = getFixtures(fixtures);

        const CONTEXT_TAG = await TagModel.create(CONTEXT_TAG_ATTRIBUTES);
        const POSTS_WITH_CONTEXT_TAG = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
        ]);

        await CONTEXT_TAG.$add('posts', POSTS_WITH_CONTEXT_TAG);

        const BLOCKS: ITagBlock[] = [
            {
                type: 'section',
                section: {
                    blocks: [
                        {
                            type: 'two',
                            two: [null, null],
                        },
                    ],
                },
            },
            {
                type: 'readMore',
                readMore: {
                    posts: [null, null],
                },
            },
        ];

        const result = await blocksTransformer.transform({
            tagUrlPart: CONTEXT_TAG.urlPart,
            blocks: BLOCKS,
        });

        expect(result).toMatchSnapshot();
    });
});
