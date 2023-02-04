import { Test, TestingModule } from '@nestjs/testing';

import {
    IMainBlock,
    IReadMoreBlock,
    ISectionBlock,
    ITextBlock,
    ISubscriptionFormBlock,
    ITextbookBlock,
    IIdeasBlock,
    IQuizBlock,
} from '../../../../../types/main-block';
import { getFixtures } from '../../../../../tests/get-fixtures';
import { Category as CategoryModel } from '../../../../category/category.model';
import { Tag as TagModel } from '../../../../tag/tag.model';
import { Post as PostModel } from '../../../../post/post.model';
import { MainModule } from '../../../main.module';
import { GetMainBlocksTransformer } from '../../../transformers/get-main/main-blocks.transformer';

import { fixtures } from './main-block.transformer.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Get main blocks transformer', () => {
    let testingModule: TestingModule;
    let blocksTransformer: GetMainBlocksTransformer;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [MainModule],
        }).compile();

        blocksTransformer = await testingModule.resolve(GetMainBlocksTransformer);

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    it('Блок "текст" возвращается без изменений', async () => {
        const TEXT_BLOCK: ITextBlock = {
            type: 'text',
            text: 'Компания Kia начала российские продажи седана Rio и кросс-хэтча Rio X нового модельного года',
        };

        const result = await blocksTransformer.transform([TEXT_BLOCK]);

        expect(result).toEqual({
            blocks: [TEXT_BLOCK],
            posts: {},
        });
    });

    it('Блок "Форма подписки" возвращается без изменений', async () => {
        const SUBSCRIPTION_FORM_BLOCK: ISubscriptionFormBlock = {
            type: 'subscriptionForm',
            subscriptionForm: {},
        };

        const result = await blocksTransformer.transform([SUBSCRIPTION_FORM_BLOCK]);

        expect(result).toEqual({
            blocks: [SUBSCRIPTION_FORM_BLOCK],
            posts: {},
        });
    });

    it('Блоки в секциях дополняются постами с тегом секции c правильной сортировкой', async () => {
        const {
            TAG_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const SECTION_TAG = await TagModel.create(TAG_ATTRIBUTES_1);

        const POSTS_WITH_TAG = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        ]);

        await PostModel.create(POST_ATTRIBUTES_3);

        await SECTION_TAG.$add('posts', POSTS_WITH_TAG);

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
                        type: 'threeBottom',
                        threeBottom: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с тегами секции c правильной сортировкой', async () => {
        const {
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            TAG_ATTRIBUTES_3,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const [SECTION_TAG_1, SECTION_TAG_2, TAG_3] = await TagModel.bulkCreate([
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            TAG_ATTRIBUTES_3,
        ]);

        const [POST_1, POST_2, POST_3, POST_4, POST_5, POST_6] = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        ]);

        await POST_1?.$add('tags', [SECTION_TAG_1 as TagModel]);
        await POST_2?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel]);
        await POST_3?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel, TAG_3 as TagModel]);
        await POST_4?.$add('tags', [SECTION_TAG_1 as TagModel, TAG_3 as TagModel]);
        await POST_5?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel]);
        await POST_6?.$add('tags', [SECTION_TAG_1 as TagModel, SECTION_TAG_2 as TagModel]);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_2.urlPart],
                blocks: [
                    {
                        type: 'threeBottom',
                        threeBottom: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с категорией секции c правильной сортировкой', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        ]);

        await PostModel.create(POST_ATTRIBUTES_2);

        await SECTION_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                categories: [SECTION_CATEGORY.urlPart],
                blocks: [
                    {
                        type: 'threeRight',
                        threeRight: [null, null, null],
                    },
                    {
                        type: 'two',
                        two: [null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с категориями секции c правильной сортировкой', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            CATEGORY_ATTRIBUTES_2,
            CATEGORY_ATTRIBUTES_3,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        } = getFixtures(fixtures);

        const [SECTION_CATEGORY_1, SECTION_CATEGORY_2, CATEGORY_3] = await CategoryModel.bulkCreate([
            CATEGORY_ATTRIBUTES_1,
            CATEGORY_ATTRIBUTES_2,
            CATEGORY_ATTRIBUTES_3,
        ]);

        const [POST_1, POST_2, POST_3, POST_4, POST_5, POST_6] = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
        ]);

        await POST_1?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel, SECTION_CATEGORY_2 as CategoryModel]);
        await POST_2?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel]);
        await POST_3?.$add('categories', [SECTION_CATEGORY_2 as CategoryModel, CATEGORY_3 as CategoryModel]);
        await POST_4?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel, SECTION_CATEGORY_2 as CategoryModel]);
        await POST_5?.$add('categories', [
            SECTION_CATEGORY_1 as CategoryModel,
            SECTION_CATEGORY_2 as CategoryModel,
            CATEGORY_3 as CategoryModel,
        ]);
        await POST_6?.$add('categories', [SECTION_CATEGORY_1 as CategoryModel, SECTION_CATEGORY_2 as CategoryModel]);

        const SECTION_BLOCK: ISectionBlock = {
            type: 'section',
            section: {
                categories: [CATEGORY_ATTRIBUTES_1.urlPart, CATEGORY_ATTRIBUTES_2.urlPart],
                blocks: [
                    {
                        type: 'threeRight',
                        threeRight: [null, null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются постами с несколькими тегами и категориями секции c правильной сортировкой', async () => {
        const {
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
                    {
                        type: 'two',
                        two: [null, null],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях сохраняют выбранные явно посты на правильных позициях', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_4]);

        await SECTION_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

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

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях отфильтровываются при отсутствии достаточного количества постов', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
        ]);

        await SECTION_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

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

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки в секциях дополняются только опубликованными постами', async () => {
        const {
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        } = getFixtures(fixtures);

        await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        ]);

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

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется постами с выбранной категорией c правильной сортировкой', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);

        await PostModel.create(POST_ATTRIBUTES_2);

        await SECTION_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        const SECTION_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                category: SECTION_CATEGORY.urlPart,
                postsCount: '3',
                posts: [null, null, null],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется постами с выбранным тегом c правильной сортировкой', async () => {
        const {
            TAG_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const SECTION_TAG = await TagModel.create(TAG_ATTRIBUTES_1);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);

        await PostModel.create(POST_ATTRIBUTES_1);

        await SECTION_TAG.$add('posts', POSTS_WITH_CATEGORY);

        const SECTION_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                tag: SECTION_TAG.urlPart,
                postsCount: '3',
                posts: [null, null, null],
            },
        };

        const result = await blocksTransformer.transform([SECTION_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" дополняется только опубликованными постами', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        } = getFixtures(fixtures);

        const SECTION_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const CATEGORY_POSTS = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            DRAFT_POST_ATTRIBUTES_1,
            CLOSED_POST_ATTRIBUTES_1,
            DELAYED_POST_ATTRIBUTES_1,
        ]);

        await SECTION_CATEGORY.$add('posts', CATEGORY_POSTS);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                category: CATEGORY_ATTRIBUTES_1.urlPart,
                posts: [null, null, null, null],
            },
        };

        const result = await blocksTransformer.transform([READ_MORE_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Читать ещё" сохраняет выбранные явно посты на правильных позициях', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2: SELECTED_POST_1,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4: SELECTED_POST_2,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
        } = getFixtures(fixtures);

        const READ_MORE_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const CATEGORY_POSTS = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_5,
            POST_ATTRIBUTES_6,
            POST_ATTRIBUTES_7,
        ]);

        await PostModel.bulkCreate([SELECTED_POST_1, SELECTED_POST_2]);

        await READ_MORE_CATEGORY.$add('posts', CATEGORY_POSTS);

        const READ_MORE_BLOCK: IReadMoreBlock = {
            type: 'readMore',
            readMore: {
                category: READ_MORE_CATEGORY.urlPart,
                posts: [null, null, SELECTED_POST_1.urlPart, SELECTED_POST_2.urlPart, null],
            },
        };

        const result = await blocksTransformer.transform([READ_MORE_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" не возвращается, если нет достаточного количества постов для дополнения', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            TAG_ATTRIBUTES_1,
        } = getFixtures(fixtures);

        const TEXTBOOK_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_4,
        ]);

        await TEXTBOOK_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        await PostModel.bulkCreate([POST_ATTRIBUTES_3]);

        const TEXTBOOK_BLOCK: ITextbookBlock = {
            type: 'textbook',
            textbook: {
                category: TEXTBOOK_CATEGORY.urlPart,
                tabs: [
                    {
                        tag: TAG_ATTRIBUTES_1.urlPart,
                        posts: [],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([TEXTBOOK_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" дополняется по категории опубликованными постами с правильной сортировкой', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
            TAG_ATTRIBUTES_1,
        } = getFixtures(fixtures);

        const TEXTBOOK_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        const POST_IN_BLOCK = await PostModel.create(POST_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_2);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);

        await TEXTBOOK_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        const TEXTBOOK_BLOCK: ITextbookBlock = {
            type: 'textbook',
            textbook: {
                category: TEXTBOOK_CATEGORY.urlPart,
                tabs: [
                    {
                        tag: TAG_ATTRIBUTES_1.urlPart,
                        posts: [null, POST_IN_BLOCK.urlPart],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([TEXTBOOK_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Учебник" возвращает неархивные модели тегов', async () => {
        const {
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            TAG_ATTRIBUTES_3,
        } = getFixtures(fixtures);

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
                        tag: TAG_ATTRIBUTES_2.urlPart,
                        posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart, POST_ATTRIBUTES_3.urlPart],
                    },
                    {
                        tag: TAG_ATTRIBUTES_3.urlPart,
                        posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart, POST_ATTRIBUTES_3.urlPart],
                    },
                ],
            },
        };

        const result = await blocksTransformer.transform([TEXTBOOK_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" не возвращается, если нет достаточного количества постов для дополнения', async () => {
        const { CATEGORY_ATTRIBUTES_1, POST_ATTRIBUTES_1, POST_ATTRIBUTES_2 } = getFixtures(fixtures);

        const IDEAS_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_1);
        const POST_WITH_CATEGORY = await PostModel.create(POST_ATTRIBUTES_2);

        await IDEAS_CATEGORY.$add('post', POST_WITH_CATEGORY);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                category: IDEAS_CATEGORY.urlPart,
                posts: [null, null],
            },
        };

        const result = await blocksTransformer.transform([IDEAS_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" дополняется по категории опубликованными постами с правильной сортировкой', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        } = getFixtures(fixtures);

        const IDEAS_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);

        const POST_IN_BLOCK = await PostModel.create(POST_ATTRIBUTES_1);

        await PostModel.create(POST_ATTRIBUTES_2);

        const POSTS_WITH_CATEGORY = await PostModel.bulkCreate([
            POST_ATTRIBUTES_3,
            POST_ATTRIBUTES_4,
            POST_ATTRIBUTES_5,
        ]);

        await IDEAS_CATEGORY.$add('posts', POSTS_WITH_CATEGORY);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                category: IDEAS_CATEGORY.urlPart,
                posts: [null, POST_IN_BLOCK.urlPart],
            },
        };

        const result = await blocksTransformer.transform([IDEAS_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Идеи" возвращает неархивные модели тегов', async () => {
        const {
            POST_ATTRIBUTES_1,
            POST_ATTRIBUTES_2,
            POST_ATTRIBUTES_3,
            TAG_ATTRIBUTES_1,
            TAG_ATTRIBUTES_2,
            TAG_ATTRIBUTES_3,
        } = getFixtures(fixtures);

        await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);
        await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

        const IDEAS_BLOCK: IIdeasBlock = {
            type: 'ideas',
            ideas: {
                posts: [POST_ATTRIBUTES_1.urlPart, POST_ATTRIBUTES_2.urlPart],
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_2.urlPart, TAG_ATTRIBUTES_3.urlPart],
            },
        };

        const result = await blocksTransformer.transform([IDEAS_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блок "Квиз" не возвращается, если пост не опубликован', async () => {
        const { POST_ATTRIBUTES } = getFixtures(fixtures);

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

        const result = await blocksTransformer.transform([QUIZ_BLOCK]);

        expect(result.blocks).toBeArrayOfSize(0);
    });

    it('Блок "Квиз" не возвращается, если у поста нет квиза', async () => {
        const { POST_ATTRIBUTES } = getFixtures(fixtures);

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

        const result = await blocksTransformer.transform([QUIZ_BLOCK]);

        expect(result.blocks).toBeArrayOfSize(0);
    });

    it('Блок "Квиз" корректно трансформируется с постом и квизом', async () => {
        const { CATEGORY_ATTRIBUTES, POST_ATTRIBUTES } = getFixtures(fixtures);

        const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

        await PostModel.bulkCreate([POST_ATTRIBUTES]);

        const QUIZ_BLOCK: IQuizBlock = {
            type: 'quiz',
            quiz: {
                post: POST_ATTRIBUTES.urlPart,
                title: 'title',
                listingButtonText: 'listingButtonText',
                postButtonText: 'postButtonText',
                categoryOrTag: { category: CATEGORY.urlPart },
            },
        };

        const result = await blocksTransformer.transform([QUIZ_BLOCK]);

        expect(result).toMatchSnapshot();
    });

    it('Блоки не дополняются одним постом дважды', async () => {
        const {
            CATEGORY_ATTRIBUTES_1,
            TAG_ATTRIBUTES_1,
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
        } = getFixtures(fixtures);

        const COMMON_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
        const POSTS = await PostModel.bulkCreate([
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
        ]);

        await COMMON_CATEGORY.$add('posts', POSTS);

        const BLOCKS: IMainBlock[] = [
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
                    category: COMMON_CATEGORY.urlPart,
                    posts: [null, null],
                },
            },
            {
                type: 'ideas',
                ideas: {
                    category: COMMON_CATEGORY.urlPart,
                    posts: [null, null],
                },
            },
            {
                type: 'textbook',
                textbook: {
                    category: COMMON_CATEGORY.urlPart,
                    tabs: [
                        {
                            tag: TAG_ATTRIBUTES_1.urlPart,
                            posts: [],
                        },
                    ],
                },
            },
        ];

        const result = await blocksTransformer.transform(BLOCKS);

        expect(result).toMatchSnapshot();
    });
});
