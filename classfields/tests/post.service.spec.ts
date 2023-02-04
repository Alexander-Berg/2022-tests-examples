import { Test, TestingModule } from '@nestjs/testing';
import { describe } from '@jest/globals';

import { getFixtures } from '../../../tests/get-fixtures';
import { Category as CategoryModel } from '../../category/category.model';
import { Tag as TagModel } from '../../tag/tag.model';
import { PostService } from '../post.service';
import { PostModule } from '../post.module';
import { Post as PostModel } from '../post.model';
import { ActionsLogService } from '../../actions-log/actions-log.service';
import { ActionLogAction, ActionLogEntity } from '../../../types/actions-log';
import { PostTag as PostTagModel } from '../../post-tag/post-tag.model';
import { PostCategory as PostCategoryModel } from '../../post-category/post-category.model';
import { Author as AuthorModel } from '../../author/author.model';
import { PostAuthor as PostAuthorModel } from '../../post-author/post-author.model';

import { fixtures } from './post.service.fixtures';

const DATE_NOW = '2021-09-08T12:30:35.000Z';

const mockDate = jest.fn().mockImplementation(() => new Date(DATE_NOW));

jest.mock('sequelize/lib/utils', () => {
    const utils = jest.requireActual('sequelize/lib/utils');

    return {
        ...utils,
        now: () => mockDate(),
    };
});

describe('Post service', () => {
    let testingModule: TestingModule;
    let postService: PostService;
    let actionsLogService: ActionsLogService;
    let spyCreateLog: jest.SpyInstance;
    let spyUpdatePostKeyLog: jest.SpyInstance;

    beforeEach(async () => {
        testingModule = await Test.createTestingModule({
            imports: [PostModule],
        }).compile();

        postService = await testingModule.resolve(PostService);
        actionsLogService = await testingModule.resolve(ActionsLogService);

        spyCreateLog = jest.spyOn(actionsLogService, 'create');
        spyUpdatePostKeyLog = jest.spyOn(actionsLogService, 'updatePostKey');

        Date.now = jest.fn().mockReturnValue(new Date(DATE_NOW));
    });

    afterEach(async () => {
        await testingModule.close();
    });

    describe('findAndCountForSection', () => {
        it('Возвращает rows = [] и count = 0, если посты не найдены', async () => {
            const { POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3 } = getFixtures(fixtures);

            await PostModel.bulkCreate([POST_ATTRIBUTES_1, POST_ATTRIBUTES_2, POST_ATTRIBUTES_3]);

            const posts = await postService.findAndCountForSection();

            expect(posts).toEqual({
                rows: [],
                count: 0,
            });
        });

        it('Возвращает 10 опубликованных постов, отсортированных по дате публикации', async () => {
            const {
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
            } = getFixtures(fixtures);

            await PostModel.bulkCreate([
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
            ]);

            const posts = await postService.findAndCountForSection();

            expect(posts).toMatchSnapshot();
        });

        it('Может возвращать опубликованные посты c заданным лимитом', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
            } = getFixtures(fixtures);

            await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
            ]);

            const posts = await postService.findAndCountForSection({
                pageSize: 3,
            });

            expect(posts).toMatchSnapshot();
        });

        it('Может возвращать опубликованные посты c заданными лимитом и страницей', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
            } = getFixtures(fixtures);

            await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
            ]);

            const posts = await postService.findAndCountForSection({
                pageSize: 2,
                pageNumber: 1,
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, включая связи с неархивными тегами и рубриками', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
            ]);

            const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            ]);

            const [CATEGORY_1, CATEGORY_2] = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
            ]);

            await POST_1?.$add('tags', [TAG_1 as TagModel]);
            await POST_2?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel]);
            await POST_3?.$add('tags', [TAG_2 as TagModel]);
            await POST_3?.$add('categories', [CATEGORY_1 as CategoryModel]);
            await POST_4?.$add('tags', [TAG_1 as TagModel, TAG_3 as TagModel]);
            await POST_4?.$add('categories', [CATEGORY_1 as CategoryModel]);
            await POST_5?.$add('categories', [CATEGORY_2 as CategoryModel]);

            const posts = await postService.findAndCountForSection();

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, не фильтруя по тегам, если tags = []', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
            ]);

            const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            ]);

            await POST_1?.$add('tag', TAG_1 as TagModel);
            await POST_2?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel]);
            await POST_3?.$add('tag', TAG_3 as TagModel);

            const posts = await postService.findAndCountForSection({
                tags: [],
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, у которых есть указанный тег', async () => {
            const {
                DRAFT_POST_ATTRIBUTES,
                PUBLISH_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_2,
                PUBLISH_POST_ATTRIBUTES_3,
                CLOSED_POST_ATTRIBUTES,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
            } = getFixtures(fixtures);

            const [DRAFT_POST, PUBLISH_POST_1, , PUBLISH_POST_3, CLOSED_POST] = await PostModel.bulkCreate([
                DRAFT_POST_ATTRIBUTES,
                PUBLISH_POST_ATTRIBUTES_1,
                PUBLISH_POST_ATTRIBUTES_2,
                PUBLISH_POST_ATTRIBUTES_3,
                CLOSED_POST_ATTRIBUTES,
            ]);

            const SEARCH_TAG = await TagModel.create(TAG_ATTRIBUTES_1);
            const OTHER_TAG = await TagModel.create(TAG_ATTRIBUTES_2);

            await DRAFT_POST?.$add('tags', [SEARCH_TAG]);
            await PUBLISH_POST_1?.$add('tags', [SEARCH_TAG, OTHER_TAG]);
            await PUBLISH_POST_3?.$add('tags', [SEARCH_TAG]);
            await CLOSED_POST?.$add('tags', [OTHER_TAG]);

            const posts = await postService.findAndCountForSection({
                tags: SEARCH_TAG.urlPart,
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, у которых есть все указанные теги', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);

            const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            ]);

            await POST_1?.$add('tag', TAG_1 as TagModel);
            await POST_2?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel]);
            await POST_3?.$add('tag', [TAG_1 as TagModel, TAG_3 as TagModel]);
            await POST_4?.$add('tags', [TAG_1 as TagModel, TAG_3 as TagModel]);
            await POST_5?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel, TAG_3 as TagModel]);

            const posts = await postService.findAndCountForSection({
                tags: [TAG_1?.urlPart as string, TAG_3?.urlPart as string],
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, не фильтруя по категориям, если categories = []', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
            ]);

            const [CATEGORY_1, CATEGORY_2, CATEGORY_3] = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            ]);

            await POST_1?.$add('category', CATEGORY_1 as CategoryModel);
            await POST_2?.$add('categories', [CATEGORY_1 as CategoryModel, CATEGORY_2 as CategoryModel]);
            await POST_3?.$add('category', CATEGORY_3 as CategoryModel);

            const posts = await postService.findAndCountForSection({
                categories: [],
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, у которых есть указанная категория', async () => {
            const {
                DRAFT_POST_ATTRIBUTES,
                PUBLISHED_POST_ATTRIBUTES_1,
                PUBLISHED_POST_ATTRIBUTES_2,
                PUBLISHED_POST_ATTRIBUTES_3,
                CLOSED_POST_ATTRIBUTES,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
            } = getFixtures(fixtures);

            const [DRAFT_POST, PUBLISHED_POST_1, , PUBLISHED_POST_3, CLOSED_POST] = await PostModel.bulkCreate([
                DRAFT_POST_ATTRIBUTES,
                PUBLISHED_POST_ATTRIBUTES_1,
                PUBLISHED_POST_ATTRIBUTES_2,
                PUBLISHED_POST_ATTRIBUTES_3,
                CLOSED_POST_ATTRIBUTES,
            ]);

            const SEARCH_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_1);
            const OTHER_CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES_2);

            await DRAFT_POST?.$add('categories', [SEARCH_CATEGORY]);
            await PUBLISHED_POST_1?.$add('categories', [SEARCH_CATEGORY, OTHER_CATEGORY]);
            await PUBLISHED_POST_3?.$add('categories', [SEARCH_CATEGORY]);
            await CLOSED_POST?.$add('categories', [OTHER_CATEGORY]);

            const posts = await postService.findAndCountForSection({
                categories: SEARCH_CATEGORY.urlPart,
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, у которых есть все указанные категории', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
            ]);

            const [CATEGORY_1, CATEGORY_2, CATEGORY_3] = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            ]);

            await POST_1?.$add('category', CATEGORY_1 as CategoryModel);
            await POST_2?.$add('categories', [CATEGORY_1 as CategoryModel, CATEGORY_2 as CategoryModel]);
            await POST_3?.$add('category', [CATEGORY_1 as CategoryModel, CATEGORY_3 as CategoryModel]);
            await POST_4?.$add('categories', [CATEGORY_1 as CategoryModel, CATEGORY_3 as CategoryModel]);
            await POST_5?.$add('categories', [
                CATEGORY_1 as CategoryModel,
                CATEGORY_2 as CategoryModel,
                CATEGORY_3 as CategoryModel,
            ]);

            const posts = await postService.findAndCountForSection({
                categories: [CATEGORY_1?.urlPart as string, CATEGORY_3?.urlPart as string],
            });

            expect(posts).toMatchSnapshot();
        });

        it('Возвращает опубликованные посты, у которых есть все указанные теги и категории', async () => {
            const {
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            } = getFixtures(fixtures);

            const [POST_1, POST_2, POST_3, POST_4, POST_5, POST_6] = await PostModel.bulkCreate([
                POST_ATTRIBUTES_1,
                POST_ATTRIBUTES_2,
                POST_ATTRIBUTES_3,
                POST_ATTRIBUTES_4,
                POST_ATTRIBUTES_5,
                POST_ATTRIBUTES_6,
                POST_ATTRIBUTES_7,
            ]);

            const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([
                TAG_ATTRIBUTES_1,
                TAG_ATTRIBUTES_2,
                TAG_ATTRIBUTES_3,
            ]);

            const [CATEGORY_1, CATEGORY_2, CATEGORY_3] = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            ]);

            await POST_1?.$add('categories', [CATEGORY_1 as CategoryModel]);

            await POST_2?.$add('tags', [TAG_1 as TagModel, TAG_3 as TagModel]);
            await POST_2?.$add('categories', [CATEGORY_1 as CategoryModel, CATEGORY_2 as CategoryModel]);

            await POST_3?.$add('tags', [TAG_1 as CategoryModel, TAG_2 as CategoryModel]);
            await POST_3?.$add('categories', [CATEGORY_2 as CategoryModel, CATEGORY_3 as CategoryModel]);

            await POST_4?.$add('categories', [
                CATEGORY_1 as CategoryModel,
                CATEGORY_2 as CategoryModel,
                CATEGORY_3 as CategoryModel,
            ]);
            await POST_4?.$add('tags', [TAG_1 as TagModel, TAG_3 as TagModel]);

            await POST_5?.$add('categories', [
                CATEGORY_1 as CategoryModel,
                CATEGORY_2 as CategoryModel,
                CATEGORY_3 as CategoryModel,
            ]);
            await POST_5?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel, TAG_3 as TagModel]);

            await POST_6?.$add('tags', [TAG_1 as TagModel, TAG_2 as TagModel, TAG_3 as TagModel]);
            await POST_6?.$add('categories', [CATEGORY_2 as CategoryModel]);

            const posts = await postService.findAndCountForSection({
                tags: [TAG_1?.urlPart as string, TAG_2?.urlPart as string],
                categories: [CATEGORY_2?.urlPart as string, CATEGORY_3?.urlPart as string],
            });

            expect(posts).toMatchSnapshot();
        });
    });

    describe('findByPk', () => {
        it('Возвращает модель поста без связей, если пост найден', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES, CATEGORY_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);
            const TAG = await TagModel.create(TAG_ATTRIBUTES);
            const CATEGORY = await CategoryModel.create(CATEGORY_ATTRIBUTES);

            await POST.$add('tag', TAG);
            await POST.$add('category', CATEGORY);

            const post = await postService.findByPk(POST.urlPart);

            expect(post).toBeInstanceOf(PostModel);
            expect(post?.toJSON()).toMatchSnapshot();
        });

        it('Возвращает null, если пост не найден', async () => {
            const URL_PART = 'non-existing-post';

            const post = await postService.findByPk(URL_PART);

            expect(post).toBe(null);
        });

        it('Может возвращать модель поста вместе с тегами поста', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            await POST.$add('tags', TAGS);

            const post = await postService.findByPk(POST.urlPart, { includeTags: true });

            expect(post).toBeInstanceOf(PostModel);
            expect(post?.toJSON()).toMatchSnapshot();
        });

        it('Может возвращать модель поста вместе с категориями поста', async () => {
            const { POST_ATTRIBUTES, CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const CATEGORIES = await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2]);

            await POST.$add('categories', CATEGORIES);

            const post = await postService.findByPk(POST.urlPart, { includeCategories: true });

            expect(post).toBeInstanceOf(PostModel);
            expect(post?.toJSON()).toMatchSnapshot();
        });

        it('Может возвращать модель поста вместе с авторами поста', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            await POST.$add('authors', AUTHORS);

            const post = await postService.findByPk(POST.urlPart, { includeAuthors: true });

            expect(post).toBeInstanceOf(PostModel);
            expect(post?.toJSON()).toMatchSnapshot();
        });

        it('Может возвращать модель поста с указанным набором атрибутов', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const post = await postService.findByPk(POST.urlPart, {
                attributes: ['title', 'indexOff', 'authorHelp'],
            });

            expect(post).toBeInstanceOf(PostModel);
            expect(post?.toJSON()).toMatchSnapshot();
        });
    });

    describe('create', () => {
        it('Создает новый пост', async () => {
            const { POST_ATTRIBUTES } = getFixtures(fixtures);

            const NEW_POST = await postService.create(POST_ATTRIBUTES);

            expect(NEW_POST).toBeInstanceOf(PostModel);
            expect(NEW_POST.toJSON()).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: POST_ATTRIBUTES.author,
                entity: ActionLogEntity.post,
                action: ActionLogAction.create,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
        });

        it('Создает новый пост и сохраняет теги поста', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            const NEW_POST = await postService.create({
                ...POST_ATTRIBUTES,
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_2.urlPart],
            });

            const POST_TAGS = await PostTagModel.findAll({
                where: {
                    post_key: NEW_POST.urlPart,
                },
            });

            expect(NEW_POST).toBeInstanceOf(PostModel);
            expect(NEW_POST.toJSON()).toMatchSnapshot();
            expect(POST_TAGS).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: POST_ATTRIBUTES.author,
                entity: ActionLogEntity.post,
                action: ActionLogAction.create,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
        });

        it('Создает новый пост и сохраняет категории поста', async () => {
            const { POST_ATTRIBUTES, CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2]);

            const NEW_POST = await postService.create({
                ...POST_ATTRIBUTES,
                categories: [CATEGORY_ATTRIBUTES_1.urlPart, CATEGORY_ATTRIBUTES_2.urlPart],
            });

            const POST_CATEGORIES = await PostCategoryModel.findAll({
                where: {
                    post_key: NEW_POST.urlPart,
                },
            });

            expect(NEW_POST).toBeInstanceOf(PostModel);
            expect(NEW_POST.toJSON()).toMatchSnapshot();
            expect(POST_CATEGORIES).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: POST_ATTRIBUTES.author,
                entity: ActionLogEntity.post,
                action: ActionLogAction.create,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
        });

        it('Создает новый пост и сохраняет авторов поста', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const [AUTHOR_1, AUTHOR_2] = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            const NEW_POST = await postService.create({
                ...POST_ATTRIBUTES,
                authors: [AUTHOR_1?.id, AUTHOR_2?.id],
            });

            const POST_AUTHORS = await PostAuthorModel.findAll({
                where: {
                    post_key: NEW_POST.urlPart,
                },
            });

            expect(NEW_POST).toBeInstanceOf(PostModel);
            expect(NEW_POST.toJSON()).toMatchSnapshot();
            expect(POST_AUTHORS).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: POST_ATTRIBUTES.author,
                entity: ActionLogEntity.post,
                action: ActionLogAction.create,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
        });
    });

    describe('update', () => {
        it('Возвращает null, если пост не найден', async () => {
            const URL_PART = 'non-existing-post';
            const USER_LOGIN = 'editor-1';

            const post = await postService.update(URL_PART, { userLogin: USER_LOGIN });

            expect(post).toBe(null);
            expect(spyCreateLog).not.toHaveBeenCalled();
            expect(spyUpdatePostKeyLog).not.toHaveBeenCalled();
        });

        it('Обновляет пост и возвращает модель поста', async () => {
            const { POST_ATTRIBUTES, UPDATE_ATTRIBUTES } = getFixtures(fixtures);

            await PostModel.create(POST_ATTRIBUTES);

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST_ATTRIBUTES.urlPart, UPDATE_ATTRIBUTES);

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(UPDATED_POST?.toJSON()).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: UPDATE_ATTRIBUTES.userLogin,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledWith({
                oldUrlPart: POST_ATTRIBUTES.urlPart,
                newUrlPart: UPDATE_ATTRIBUTES.urlPart,
            });
        });

        it('Добавляет теги поста', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_2.urlPart],
            });

            const POST_TAGS = await PostTagModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_TAGS).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Обновляет список тегов поста', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3 } = getFixtures(fixtures);

            const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2, TAG_ATTRIBUTES_3]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST?.$add('tags', TAGS);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                tags: [TAG_ATTRIBUTES_1.urlPart, TAG_ATTRIBUTES_3.urlPart],
            });

            const POST_TAGS = await PostTagModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_TAGS).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Удаляет теги поста', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST?.$add('tags', TAGS);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                tags: [],
            });

            const POST_TAGS_COUNT = await PostTagModel.count({
                where: {
                    post_key: POST.urlPart,
                },
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_TAGS_COUNT).toBe(0);
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Не удаляет теги поста, если их не указать', async () => {
            const { POST_ATTRIBUTES, TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2 } = getFixtures(fixtures);

            const TAGS = await TagModel.bulkCreate([TAG_ATTRIBUTES_1, TAG_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST?.$add('tags', TAGS);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
            });

            const POST_TAGS = await PostTagModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_TAGS).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Добавляет категории поста', async () => {
            const { POST_ATTRIBUTES, CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                categories: [CATEGORY_ATTRIBUTES_1.urlPart, CATEGORY_ATTRIBUTES_2.urlPart],
            });

            const POST_CATEGORIES = await PostCategoryModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_CATEGORIES).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Обновляет список категорий поста', async () => {
            const { POST_ATTRIBUTES, CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2, CATEGORY_ATTRIBUTES_3 } =
                getFixtures(fixtures);

            const CATEGORIES = await CategoryModel.bulkCreate([
                CATEGORY_ATTRIBUTES_1,
                CATEGORY_ATTRIBUTES_2,
                CATEGORY_ATTRIBUTES_3,
            ]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST?.$add('categories', CATEGORIES);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                categories: [CATEGORY_ATTRIBUTES_1.urlPart, CATEGORY_ATTRIBUTES_3.urlPart],
            });

            const POST_CATEGORIES = await PostCategoryModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_CATEGORIES).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Удаляет категории поста', async () => {
            const { POST_ATTRIBUTES, CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            const CATEGORIES = await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST?.$add('categories', CATEGORIES);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                categories: [],
            });

            const POST_CATEGORIES_COUNT = await PostCategoryModel.count({
                where: {
                    post_key: POST.urlPart,
                },
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_CATEGORIES_COUNT).toBe(0);
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Не удаляет категории поста, если их не указать', async () => {
            const { POST_ATTRIBUTES, CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2 } = getFixtures(fixtures);

            const CATEGORIES = await CategoryModel.bulkCreate([CATEGORY_ATTRIBUTES_1, CATEGORY_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST?.$add('categories', CATEGORIES);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
            });

            const POST_CATEGORIES = await PostCategoryModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_CATEGORIES).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Добавляет авторов поста', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const [AUTHOR_1, AUTHOR_2] = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                authors: [AUTHOR_1?.id as number, AUTHOR_2?.id as number],
            });

            const POST_AUTHORS = await PostAuthorModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_AUTHORS).toMatchSnapshot();
            expect(UPDATED_POST?.authorHelp).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Обновляет список авторов поста', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2, AUTHOR_ATTRIBUTES_3 } =
                getFixtures(fixtures);

            const AUTHORS = await AuthorModel.bulkCreate([
                AUTHOR_ATTRIBUTES_1,
                AUTHOR_ATTRIBUTES_2,
                AUTHOR_ATTRIBUTES_3,
            ]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST.$add('authors', AUTHORS);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                authors: [AUTHORS[0]?.id as number, AUTHORS[2]?.id as number],
            });

            const POST_AUTHORS = await PostAuthorModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_AUTHORS).toMatchSnapshot();
            expect(UPDATED_POST?.authorHelp).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Удаляет авторов поста', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST.$add('authors', AUTHORS);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
                authors: [],
            });

            const POST_AUTHORS_COUNT = await PostAuthorModel.count({
                where: {
                    post_key: POST.urlPart,
                },
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(UPDATED_POST?.authorHelp).toMatchSnapshot();
            expect(POST_AUTHORS_COUNT).toBe(0);
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });

        it('Не удаляет авторов поста, если их не указать', async () => {
            const { POST_ATTRIBUTES, AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2 } = getFixtures(fixtures);

            const AUTHORS = await AuthorModel.bulkCreate([AUTHOR_ATTRIBUTES_1, AUTHOR_ATTRIBUTES_2]);

            const POST = await PostModel.create(POST_ATTRIBUTES);

            await POST.$add('authors', AUTHORS);

            const USER_LOGIN = 'vasya';

            const UPDATE_DATE = '2021-09-10T14:30:25.000Z';

            mockDate.mockImplementation(() => new Date(UPDATE_DATE));

            const UPDATED_POST = await postService.update(POST.urlPart, {
                userLogin: USER_LOGIN,
            });

            const POST_AUTHORS = await PostAuthorModel.findAll({
                where: {
                    post_key: POST.urlPart,
                },
                order: ['id'],
            });

            expect(UPDATED_POST).toBeInstanceOf(PostModel);
            expect(POST_AUTHORS).toMatchSnapshot();
            expect(UPDATED_POST?.authorHelp).toMatchSnapshot();
            expect(spyCreateLog).toHaveBeenCalledWith({
                author: USER_LOGIN,
                entity: ActionLogEntity.post,
                action: ActionLogAction.update,
                urlPart: POST_ATTRIBUTES.urlPart,
            });
            expect(spyCreateLog).toHaveBeenCalledTimes(1);
            expect(spyUpdatePostKeyLog).toHaveBeenCalledTimes(0);
        });
    });

    describe('getCount', () => {
        it('Возвращает количество опубликованных постов', async () => {
            const testFixtures = getFixtures(fixtures);

            await PostModel.bulkCreate([
                testFixtures.POST_1,
                testFixtures.POST_2,
                testFixtures.POST_3,
                testFixtures.POST_4,
                testFixtures.POST_5,
            ]);

            const count = await postService.getCount();

            expect(count).toBe(3);
        });

        it('Может возвращать количество всех постов', async () => {
            const testFixtures = getFixtures(fixtures);

            await PostModel.bulkCreate([
                testFixtures.POST_1,
                testFixtures.POST_2,
                testFixtures.POST_3,
                testFixtures.POST_4,
                testFixtures.POST_5,
            ]);

            const count = await postService.getCount({ onlyPublished: false });

            expect(count).toBe(5);
        });

        it('Не фильтрует по тегам, если передан пустой массив тегов', async () => {
            const testFixtures = getFixtures(fixtures);

            const TAG_1 = await TagModel.create(testFixtures.TAG_1);

            const [POST_1] = await PostModel.bulkCreate([
                testFixtures.POST_1,
                testFixtures.POST_2,
                testFixtures.POST_3,
            ]);

            await POST_1?.$add('tag', TAG_1);

            const count = await postService.getCount({ tags: [] });

            expect(count).toBe(3);
        });

        describe('Может возвращать количество, фильтруя по тегам', () => {
            it('1 тег', async () => {
                const {
                    POST_1: POST_DATA_1,
                    POST_2: POST_DATA_2,
                    POST_3: POST_DATA_3,
                    POST_4: POST_DATA_4,
                    POST_5: POST_DATA_5,
                    TAG_1: TAG_DATA_1,
                    TAG_2: TAG_DATA_2,
                    TAG_3: TAG_DATA_3,
                } = getFixtures(fixtures);

                const [POST_1, POST_2, POST_3] = await PostModel.bulkCreate([
                    POST_DATA_1,
                    POST_DATA_2,
                    POST_DATA_3,
                    POST_DATA_4,
                    POST_DATA_5,
                ]);
                const [TAG_1] = await TagModel.bulkCreate([TAG_DATA_1, TAG_DATA_2, TAG_DATA_3]);

                if (TAG_1) {
                    await POST_1?.$add('tag', TAG_1);
                    await POST_2?.$add('tag', TAG_1);
                    await POST_3?.$add('tag', TAG_1);
                }

                const count = await postService.getCount({ tags: [TAG_DATA_1.urlPart] });

                expect(count).toBe(3);
            });

            it('2 тега', async () => {
                const {
                    POST_1: POST_DATA_1,
                    POST_2: POST_DATA_2,
                    POST_3: POST_DATA_3,
                    POST_4: POST_DATA_4,
                    POST_5: POST_DATA_5,
                    TAG_1: TAG_DATA_1,
                    TAG_2: TAG_DATA_2,
                    TAG_3: TAG_DATA_3,
                } = getFixtures(fixtures);

                const [POST_1, POST_2, POST_3, POST_4, POST_5] = await PostModel.bulkCreate([
                    POST_DATA_1,
                    POST_DATA_2,
                    POST_DATA_3,
                    POST_DATA_4,
                    POST_DATA_5,
                ]);
                const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([TAG_DATA_1, TAG_DATA_2, TAG_DATA_3]);

                if (TAG_1 && TAG_2 && TAG_3) {
                    await POST_1?.$add('tag', TAG_1);
                    await POST_2?.$add('tag', TAG_1);
                    await POST_2?.$add('tag', TAG_2);
                    await POST_3?.$add('tag', TAG_2);
                    await POST_4?.$add('tag', TAG_2);
                    await POST_5?.$add('tag', TAG_3);
                }

                const count = await postService.getCount({
                    tags: [TAG_DATA_1.urlPart, TAG_DATA_2.urlPart],
                });

                expect(count).toBe(4);
            });

            it('3 тега', async () => {
                const {
                    POST_1: POST_DATA_1,
                    POST_2: POST_DATA_2,
                    POST_3: POST_DATA_3,
                    POST_4: POST_DATA_4,
                    POST_5: POST_DATA_5,
                    TAG_1: TAG_DATA_1,
                    TAG_2: TAG_DATA_2,
                    TAG_3: TAG_DATA_3,
                } = getFixtures(fixtures);

                const [POST_1, POST_2, POST_3, POST_4] = await PostModel.bulkCreate([
                    POST_DATA_1,
                    POST_DATA_2,
                    POST_DATA_3,
                    POST_DATA_4,
                    POST_DATA_5,
                ]);
                const [TAG_1, TAG_2, TAG_3] = await TagModel.bulkCreate([TAG_DATA_1, TAG_DATA_2, TAG_DATA_3]);

                if (TAG_1 && TAG_2 && TAG_3) {
                    await POST_1?.$add('tag', TAG_1);
                    await POST_2?.$add('tag', TAG_3);
                    await POST_3?.$add('tags', [TAG_2, TAG_3]);
                    await POST_4?.$add('tag', TAG_2);
                }

                const count = await postService.getCount({
                    tags: [TAG_DATA_1.urlPart, TAG_DATA_2.urlPart, TAG_DATA_3.urlPart],
                });

                expect(count).toBe(4);
            });
        });
    });
});
